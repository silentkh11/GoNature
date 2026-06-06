package server;

import database.DBController;
import entities.Message;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import java.util.function.Consumer;

/**
 * This class overrides some of the methods in the abstract superclass
 * in order to give more functionality to the server.
 */
public class EchoServer extends AbstractServer {

    // Callbacks to update the ServerUI
    private Consumer<String> uiLogger;
    private Consumer<ConnectionToClient> clientConnectedHandler;
    private Consumer<ConnectionToClient> clientDisconnectedHandler;

    /**
     * Constructs an instance of the echo server.
     */
    public EchoServer(int port, 
                      Consumer<String> uiLogger, 
                      Consumer<ConnectionToClient> clientConnectedHandler, 
                      Consumer<ConnectionToClient> clientDisconnectedHandler) {
        super(port);
        this.uiLogger = uiLogger;
        this.clientConnectedHandler = clientConnectedHandler;
        this.clientDisconnectedHandler = clientDisconnectedHandler;
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        try {
            if (msg instanceof Message) {
                Message request = (Message) msg;
                uiLogger.accept("> Received command: " + request.getCommand() + " from " + client.getInetAddress().getHostAddress() + "\n");

                // =========================================================================
                // --- 1. LOGIN ROUTING ---
                // =========================================================================
                if (request.getCommand().equals("LOGIN_REQUEST")) {
                    String[] credentials = (String[]) request.getData();
                    entities.Employee user = DBController.verifyLogin(credentials[0], credentials[1]);
                    
                    if (user != null) {
                        client.sendToClient(new Message("LOGIN_SUCCESS", user));
                        uiLogger.accept("> " + user.getRole() + " logged in successfully.\n");
                    } else {
                        client.sendToClient(new Message("LOGIN_FAILED", "Invalid username or password."));
                        uiLogger.accept("> Login failed (Invalid credentials).\n");
                    }
                }

                // =========================================================================
                // --- 2. VISITOR BOOKING ROUTING (WITH DUPLICATE SHIELD) ---
                // =========================================================================
                else if (request.getCommand().equals("NEW_ORDER_REQUEST")) {
                    entities.VisitOrder newOrder = (entities.VisitOrder) request.getData();
                    
                    // 1. THE DUPLICATE SHIELD: Check if they already have a booking for this day
                    if (DBController.hasDuplicateBooking(newOrder)) {
                        uiLogger.accept("> Rejected duplicate booking attempt for ID: " + newOrder.getVisitorId() + "\n");
                        client.sendToClient(new Message("ORDER_FAILED", "You already have an active reservation for this park on this date."));
                        return; // Stop the code here! Do not proceed to process the order.
                    }

                    // 2. Process through the Capacity Engine
                    entities.VisitOrder processedOrder = DBController.processNewOrder(newOrder);
                    
                    if (processedOrder != null) {
                        client.sendToClient(new Message("ORDER_CONFIRMED", processedOrder));
                        uiLogger.accept("> New booking processed. Status: " + processedOrder.getStatus() + "\n");
                    } else {
                        client.sendToClient(new Message("ORDER_FAILED", "Database error occurred during booking."));
                        uiLogger.accept("> Booking failed due to database error.\n");
                    }
                }

                // =========================================================================
                // --- 3. PARK GATE ENTRANCE ROUTING ---
                // =========================================================================
                else if (request.getCommand().equals("ENTER_PARK_REQUEST")) {
                    int orderIdToAdmit = (int) request.getData();
                    uiLogger.accept("> Park Gate requested entry for Order ID: " + orderIdToAdmit + "\n");
                    
                    String resultMessage = DBController.processParkEntry(orderIdToAdmit);
                    
                    if (resultMessage.startsWith("SUCCESS")) {
                        client.sendToClient(new Message("ENTRY_APPROVED", resultMessage));
                        uiLogger.accept("> Gate Entry APPROVED.\n");
                    } else {
                        client.sendToClient(new Message("ENTRY_DENIED", resultMessage));
                        uiLogger.accept("> Gate Entry DENIED: " + resultMessage + "\n");
                    }
                }

                // =========================================================================
                // --- 4. PARK MANAGER ROUTING ---
                // =========================================================================
                else if (request.getCommand().equals("FETCH_PARK_DETAILS")) {
                    int parkIdToFetch = (int) request.getData();
                    uiLogger.accept("> Fetching park details for Park ID: " + parkIdToFetch + "\n");
                    
                    entities.Park parkData = DBController.getParkById(parkIdToFetch);
                    
                    if (parkData != null) {
                        client.sendToClient(new Message("PARK_DETAILS_DATA", parkData));
                    } else {
                        client.sendToClient(new Message("PARK_DETAILS_ERROR", "Could not find park data."));
                    }
                }
                
                else if (request.getCommand().equals("UPDATE_PARK_PARAMS")) {
                    entities.Park requestedUpdate = (entities.Park) request.getData();
                    uiLogger.accept("> Park Manager requested parameter change. Sending to Dept Manager for approval.\n");
                    
                    boolean success = DBController.submitParameterRequest(requestedUpdate);
                    
                    if (success) {
                        client.sendToClient(new Message("UPDATE_PARAMS_SUCCESS", "Request forwarded to Department Manager for approval."));
                    } else {
                        client.sendToClient(new Message("UPDATE_PARAMS_FAILED", "Failed to submit request."));
                    }
                }

                // =========================================================================
                // --- 5. DEPARTMENT MANAGER ROUTING ---
                // =========================================================================
                else if (request.getCommand().equals("FETCH_PENDING_REQUESTS")) {
                    uiLogger.accept("> Fetching pending parameter requests for Department Manager.\n");
                    java.util.ArrayList<entities.ParameterRequest> requests = DBController.getPendingRequests();
                    client.sendToClient(new Message("PENDING_REQUESTS_DATA", requests));
                }
                
                else if (request.getCommand().equals("PROCESS_REQUEST_DECISION")) {
                    String[] data = (String[]) request.getData();
                    int requestId = Integer.parseInt(data[0]);
                    String decision = data[1];
                    
                    uiLogger.accept("> Dept Manager " + decision + " request #" + requestId + "\n");
                    boolean success = DBController.processParameterDecision(requestId, decision);
                    
                    if (success) {
                        client.sendToClient(new Message("DECISION_SUCCESS", decision));
                    } else {
                        client.sendToClient(new Message("DECISION_FAILED", "Database error."));
                    }
                }

                // =========================================================================
                // --- 6. SERVICE REPRESENTATIVE ROUTING ---
                // =========================================================================
                else if (request.getCommand().equals("REGISTER_SUBSCRIBER_REQUEST")) {
                    entities.Subscriber newSub = (entities.Subscriber) request.getData();
                    uiLogger.accept("> Service Rep attempting to register Subscriber ID: " + newSub.getVisitorId() + "\n");
                    
                    String resultMsg = DBController.registerNewSubscriber(newSub);
                    
                    if (resultMsg.startsWith("SUCCESS")) {
                        client.sendToClient(new Message("REGISTER_SUCCESS", resultMsg));
                    } else {
                        client.sendToClient(new Message("REGISTER_FAILED", resultMsg));
                    }
                }

                // =========================================================================
                // --- 7. GUEST PORTAL ROUTING ---
                // =========================================================================
                else if (request.getCommand().equals("FETCH_GUEST_ORDERS")) {
                    String visitorId = (String) request.getData();
                    uiLogger.accept("> Fetching orders for Visitor ID: " + visitorId + "\n");
                    
                    java.util.ArrayList<entities.VisitOrder> orders = DBController.getGuestOrders(visitorId);
                    client.sendToClient(new Message("GUEST_ORDERS_DATA", orders));
                }
                
                else if (request.getCommand().equals("CANCEL_ORDER")) {
                    int orderIdToCancel = (int) request.getData();
                    uiLogger.accept("> Request to cancel Order ID: " + orderIdToCancel + "\n");
                    
                    boolean success = DBController.cancelOrder(orderIdToCancel);
                    
                    if (success) {
                        client.sendToClient(new Message("CANCEL_SUCCESS", "Order #" + orderIdToCancel + " has been cancelled."));
                    } else {
                        client.sendToClient(new Message("CANCEL_FAILED", "Failed to cancel Order #" + orderIdToCancel + "."));
                    }
                }

                // =========================================================================
                // --- UNKNOWN COMMAND FALLBACK ---
                // =========================================================================
                else {
                    uiLogger.accept("> WARNING: Unknown command received: " + request.getCommand() + "\n");
                }
            } else {
                uiLogger.accept("> WARNING: Received unidentifiable object from client.\n");
            }
        } catch (Exception e) {
            uiLogger.accept("> ERROR processing message from client: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // =========================================================================
    // --- SERVER LIFECYCLE HOOKS ---
    // =========================================================================

    @Override
    protected void serverStarted() {
        uiLogger.accept("> Server listening for connections on port " + getPort() + "\n");
        // Force the DB singleton to initialize now so the notification engine starts immediately
        database.DBController.getInstance();
        uiLogger.accept("> Notification engine started.\n");
    }

    @Override
    protected void serverStopped() {
        uiLogger.accept("> Server has stopped listening for connections.\n");
        // Cleanly shut down HikariCP when you hit the 'Stop' button!
        DBController.getInstance().closePool();
    }
    
    @Override
    protected void clientConnected(ConnectionToClient client) {
        uiLogger.accept("> Client connected: " + client.getInetAddress().getHostAddress() + "\n");
        if (clientConnectedHandler != null) {
            clientConnectedHandler.accept(client);
        }
    }
    
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        uiLogger.accept("> Client disconnected.\n");
        if (clientDisconnectedHandler != null) {
            clientDisconnectedHandler.accept(client);
        }
    }
}