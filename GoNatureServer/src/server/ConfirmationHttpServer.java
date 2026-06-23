package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import database.DBController;
import entities.VisitOrder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Lightweight HTTP server that handles one-click email confirmation and
 * cancellation links. Runs on port 8080 alongside the OCSF TCP server.
 */
public class ConfirmationHttpServer {

    private HttpServer server;
    private final int port;

    public ConfirmationHttpServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/confirm", this::handleConfirm);
        server.createContext("/cancel",  this::handleCancel);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("✅ Confirmation HTTP server running on port " + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Confirmation HTTP server stopped.");
        }
    }

    // -------------------------------------------------------------------------

    private void handleConfirm(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String html;
        try {
            int orderId = Integer.parseInt(params.getOrDefault("id", "0"));
            if (orderId == 0) {
                html = errorPage("Invalid confirmation link.");
            } else {
                // confirmOrder SQL only updates status IN ('Pending Confirm','Waitlist Pending').
                // If the order is already Confirmed/Cancelled/etc, executeUpdate returns 0 → success=false.
                // This is the authoritative atomic check — no separate read-then-write race.
                boolean success = DBController.confirmOrder(orderId);
                if (success) {
                    // Fetch fresh data for the thank-you page
                    VisitOrder order = DBController.getOrderById(orderId);
                    html = order != null ? thankYouPage(order)
                                        : errorPage("Confirmed! (Could not load booking details.)");
                    // Broadcast live update to any connected park managers
                    EchoServer server = EchoServer.activeInstance;
                    if (server != null && order != null) server.broadcastParkUpdate(order.getParkId());
                } else {
                    // Read current status to give a meaningful error
                    VisitOrder order = DBController.getOrderById(orderId);
                    if (order == null) {
                        html = errorPage("Order #" + orderId + " not found.");
                    } else if (order.getStatus().equals("Confirmed") || order.getStatus().equals("In Park")
                            || order.getStatus().equals("Completed")) {
                        html = alreadyConfirmedPage(order);
                    } else if (order.getStatus().equals("Cancelled")) {
                        html = errorPage("Order #" + orderId + " has already been cancelled.");
                    } else {
                        html = errorPage("Could not confirm order #" + orderId + ". The confirmation window may have expired.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            html = errorPage("Invalid order ID.");
        }
        sendHtml(exchange, html);
    }

    private void handleCancel(HttpExchange exchange) throws IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
        String html;
        try {
            int orderId = Integer.parseInt(params.getOrDefault("id", "0"));
            if (orderId == 0) {
                html = errorPage("Invalid cancellation link.");
            } else {
                // Read parkId before cancel so we can broadcast
                VisitOrder order = DBController.getOrderById(orderId);
                boolean success = DBController.cancelOrder(orderId);
                if (success) {
                    html = cancelledPage(orderId);
                    EchoServer server = EchoServer.activeInstance;
                    if (server != null && order != null) server.broadcastParkUpdate(order.getParkId());
                } else {
                    if (order != null && order.getStatus().equals("Cancelled")) {
                        html = errorPage("Order #" + orderId + " has already been cancelled.");
                    } else if (order != null && order.getStatus().equals("In Park")) {
                        html = errorPage("Cannot cancel — visitor is currently inside the park.");
                    } else {
                        html = errorPage("Could not cancel order #" + orderId + ". It may have already been processed.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            html = errorPage("Invalid order ID.");
        }
        sendHtml(exchange, html);
    }

    // -------------------------------------------------------------------------
    // HTML page builders — matching GoNature dark theme
    // -------------------------------------------------------------------------

    private String thankYouPage(VisitOrder order) {
        String time = order.getVisitTime() != null
            ? order.getVisitTime().substring(0, Math.min(5, order.getVisitTime().length())) : "—";
        return page(
            "Visit Confirmed!",
            "✅",
            "#00b894",
            "Visit Confirmed!",
            "Thank you for confirming your visit. We look forward to seeing you!",
            "<div class='details'>"
            + row("Order #", String.valueOf(order.getOrderId()))
            + row("Date", order.getVisitDate())
            + row("Time", time)
            + row("Visitors", String.valueOf(order.getVisitorCount()))
            + row("Type", order.getOrderType())
            + "</div>"
            + "<p style='color:#8b949e;margin-top:20px;'>Bring your Order ID <strong style='color:#e6edf3'>#"
            + order.getOrderId() + "</strong> to the park entrance. See you on <strong style='color:#00b894'>"
            + order.getVisitDate() + " at " + time + "</strong>!</p>"
        );
    }

    private String alreadyConfirmedPage(VisitOrder order) {
        return page(
            "Already Confirmed",
            "☑️",
            "#0984e3",
            "Already Confirmed",
            "Order #" + order.getOrderId() + " was already confirmed. See you on " + order.getVisitDate() + "!",
            ""
        );
    }

    private String cancelledPage(int orderId) {
        return page(
            "Visit Cancelled",
            "❌",
            "#e17055",
            "Booking Cancelled",
            "Order #" + orderId + " has been cancelled. We hope to see you another time!",
            ""
        );
    }

    private String errorPage(String message) {
        return page("Error", "⚠️", "#d63031", "Something went wrong", message, "");
    }

    private String page(String title, String icon, String accentColor,
                        String heading, String body, String extra) {
        return "<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>GoNature — " + title + "</title>"
            + "<style>"
            + "body{margin:0;font-family:'Segoe UI',Arial,sans-serif;background:#0d1117;"
            + "color:#e6edf3;display:flex;align-items:center;justify-content:center;min-height:100vh;}"
            + ".card{background:#161b22;border:1px solid #30363d;border-radius:16px;padding:48px;"
            + "max-width:520px;width:90%;text-align:center;box-shadow:0 8px 32px rgba(0,0,0,.4);}"
            + ".icon{font-size:72px;margin-bottom:16px;}"
            + ".brand{color:#58a6ff;font-weight:700;font-size:22px;margin-bottom:28px;letter-spacing:.5px;}"
            + "h1{color:" + accentColor + ";font-size:26px;margin:0 0 12px;}"
            + "p{color:#8b949e;font-size:15px;line-height:1.6;}"
            + ".details{background:#0d1117;border-radius:10px;padding:20px;margin:24px 0;text-align:left;}"
            + ".row{display:flex;justify-content:space-between;padding:9px 0;border-bottom:1px solid #21262d;}"
            + ".row:last-child{border-bottom:none;}"
            + ".lbl{color:#8b949e;font-size:14px;}.val{color:#e6edf3;font-size:14px;font-weight:600;}"
            + "</style></head><body>"
            + "<div class='card'>"
            + "<div class='brand'>🌿 GoNature</div>"
            + "<div class='icon'>" + icon + "</div>"
            + "<h1>" + heading + "</h1>"
            + "<p>" + body + "</p>"
            + extra
            + "</div></body></html>";
    }

    private String row(String label, String value) {
        return "<div class='row'><span class='lbl'>" + label + "</span>"
             + "<span class='val'>" + value + "</span></div>";
    }

    // -------------------------------------------------------------------------

    private void sendHtml(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQuery(String query) {
        if (query == null || query.isEmpty()) return Map.of();
        return Arrays.stream(query.split("&"))
            .map(p -> p.split("=", 2))
            .filter(p -> p.length == 2)
            .collect(Collectors.toMap(p -> p[0], p -> p[1]));
    }
}
