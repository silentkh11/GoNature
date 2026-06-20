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

    // Duplicate-login prevention: track which username owns each connection
    private final java.util.Set<String> loggedInUsers =
        java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final java.util.Map<ConnectionToClient, String> clientUserMap =
        java.util.Collections.synchronizedMap(new java.util.HashMap<>());

    // Real-time sync: track the full Employee object so we can push by role/park
    private final java.util.Map<ConnectionToClient, entities.Employee> clientEmployeeMap =
        java.util.Collections.synchronizedMap(new java.util.HashMap<>());

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
                    String username = credentials[0];
                    entities.Employee user = DBController.verifyLogin(username, credentials[1]);

                    if (user != null) {
                        if (loggedInUsers.contains(username)) {
                            client.sendToClient(new Message("LOGIN_FAILED",
                                "This account is already logged in from another workstation."));
                            uiLogger.accept("> Login rejected — " + username + " already connected.\n");
                        } else {
                            loggedInUsers.add(username);
                            clientUserMap.put(client, username);
                            clientEmployeeMap.put(client, user);
                            client.sendToClient(new Message("LOGIN_SUCCESS", user));
                            uiLogger.accept("> " + user.getRole() + " (" + username + ") logged in.\n");
                        }
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

                    // 2. Group bookings require a registered guide
                    if (newOrder.getOrderType().equalsIgnoreCase("Group")
                            && !DBController.isRegisteredGuide(newOrder.getVisitorId())) {
                        client.sendToClient(new Message("ORDER_FAILED",
                            "Group bookings require a registered Tour Guide ID. Please contact a Service Representative to register as a guide."));
                        uiLogger.accept("> Group booking rejected — visitor is not a registered guide.\n");
                        return;
                    }

                    // 3. Process through the Capacity Engine
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
                        pushParkUpdate(orderIdToAdmit);
                    } else {
                        client.sendToClient(new Message("ENTRY_DENIED", resultMessage));
                        uiLogger.accept("> Gate Entry DENIED: " + resultMessage + "\n");
                    }
                }

                else if (request.getCommand().equals("EXIT_PARK_REQUEST")) {
                    int orderIdToExit = (int) request.getData();
                    uiLogger.accept("> Park Gate requested exit for Order ID: " + orderIdToExit + "\n");
                    String resultMessage = DBController.processParkExit(orderIdToExit);
                    if (resultMessage.startsWith("SUCCESS")) {
                        client.sendToClient(new Message("EXIT_APPROVED", resultMessage));
                        uiLogger.accept("> Gate Exit APPROVED.\n");
                        pushParkUpdate(orderIdToExit);
                    } else {
                        client.sendToClient(new Message("EXIT_DENIED", resultMessage));
                        uiLogger.accept("> Gate Exit DENIED: " + resultMessage + "\n");
                    }
                }

                else if (request.getCommand().equals("WALKIN_REQUEST")) {
                    String[] data = (String[]) request.getData();
                    int parkId     = Integer.parseInt(data[0]);
                    int count      = Integer.parseInt(data[1]);
                    String type    = data[2];
                    uiLogger.accept("> Walk-in request: " + count + " visitors (" + type + ") at park " + parkId + "\n");
                    entities.VisitOrder walkin = DBController.processWalkIn(parkId, count, type);
                    if (walkin != null) {
                        client.sendToClient(new Message("WALKIN_APPROVED", walkin));
                        uiLogger.accept("> Walk-in APPROVED. Ticket #" + walkin.getOrderId() + "\n");
                        // Push live visitor count to ParkManagers of this park
                        entities.Park updatedPark = DBController.getParkById(parkId);
                        if (updatedPark != null) broadcastToPark(parkId, new Message("PARK_DETAILS_DATA", updatedPark));
                    } else {
                        client.sendToClient(new Message("WALKIN_DENIED", "Park is at full capacity. Cannot admit walk-in visitors."));
                        uiLogger.accept("> Walk-in DENIED — park full.\n");
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
                        // Push live update: DeptManagers see the new request immediately
                        java.util.ArrayList<entities.ParameterRequest> freshRequests = DBController.getPendingRequests();
                        broadcastToRole("DeptManager", new Message("PENDING_REQUESTS_DATA", freshRequests));
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
                        // Push live update: ParkManagers re-fetch their park data to see if their request was processed
                        broadcastToRole("ParkManager", new Message("PARAMETER_DECISION_MADE", decision));
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
                // =========================================================================
                // --- 8. STATISTICAL REPORTING ROUTING ---
                // =========================================================================
                else if (request.getCommand().equals("GENERATE_MONTHLY_REPORT")) {
                    // The client will send a String array: [parkId, month, year]
                    String[] requestData = (String[]) request.getData();
                    int parkId = Integer.parseInt(requestData[0]);
                    String month = requestData[1];
                    String year = requestData[2];
                    
                    uiLogger.accept("> Generating Monthly Report for Park " + parkId + " (" + month + "/" + year + ")\n");
                    
                    entities.ReportData generatedReport = DBController.generateMonthlyReport(parkId, month, year);
                    
                    if (generatedReport != null) {
                        client.sendToClient(new Message("REPORT_DATA_SUCCESS", generatedReport));
                    } else {
                        client.sendToClient(new Message("REPORT_DATA_FAILED", "Could not generate the report. Database error."));
                    }
                }else if (request.getCommand().equals("SAVE_MONTHLY_REPORT")) {
                    entities.ReportData reportToSave = (entities.ReportData) request.getData();
                    uiLogger.accept("> Park Manager submitting report for Park " + reportToSave.getParkId() + "\n");
                    
                    String resultMsg = DBController.saveMonthlyReport(reportToSave);
                    
                    if (resultMsg.startsWith("SUCCESS")) {
                        client.sendToClient(new Message("SAVE_REPORT_SUCCESS", resultMsg));
                        // Push live update: DeptManagers are notified a new report is available
                        broadcastToRole("DeptManager", new Message("REPORT_SUBMITTED_NOTIFICATION",
                            "📋 A new monthly report was submitted by Park " + reportToSave.getParkId() + "."));
                    } else {
                        client.sendToClient(new Message("SAVE_REPORT_FAILED", resultMsg));
                    }
                }
                else if (request.getCommand().equals("CANCEL_ORDER_REQUEST")) {
                    int orderIdToCancel = (int) request.getData();
                    uiLogger.accept("> Request to cancel Order ID: " + orderIdToCancel + "\n");
                    boolean success = DBController.cancelOrder(orderIdToCancel);
                    if (success) {
                        client.sendToClient(new Message("CANCEL_SUCCESS", "Order #" + orderIdToCancel + " has been cancelled."));
                    } else {
                        client.sendToClient(new Message("CANCEL_FAILED", "Failed to cancel Order #" + orderIdToCancel + "."));
                    }
                }

                else if (request.getCommand().equals("CONFIRM_ORDER_REQUEST")) {
                    int orderIdToConfirm = (int) request.getData();
                    uiLogger.accept("> Request to confirm Order ID: " + orderIdToConfirm + "\n");
                    boolean success = DBController.confirmOrder(orderIdToConfirm);
                    if (success) {
                        client.sendToClient(new Message("CONFIRM_SUCCESS", "Order #" + orderIdToConfirm + " is now Confirmed."));
                    } else {
                        client.sendToClient(new Message("CONFIRM_FAILED", "Failed to confirm Order #" + orderIdToConfirm + "."));
                    }
                }

                else if (request.getCommand().equals("FETCH_USAGE_REPORT")) {
                    String[] data = (String[]) request.getData();
                    int parkId = Integer.parseInt(data[0]);
                    int month  = Integer.parseInt(data[1]);
                    int year   = Integer.parseInt(data[2]);
                    java.util.ArrayList<String[]> rows = DBController.getUsageReport(parkId, month, year);
                    client.sendToClient(new Message("USAGE_REPORT_DATA", rows));
                    uiLogger.accept("> Usage report for Park " + parkId + " — " + month + "/" + year + " (" + rows.size() + " below-capacity days)\n");
                }

                else if (request.getCommand().equals("FETCH_AVAILABLE_SLOTS")) {
                    String[] data = (String[]) request.getData();
                    int parkId = Integer.parseInt(data[0]);
                    String date = data[1];
                    java.util.ArrayList<String> slots = DBController.getAvailableSlots(parkId, date);
                    client.sendToClient(new Message("AVAILABLE_SLOTS_DATA", slots));
                }

                else if (request.getCommand().equals("FETCH_ALL_PARKS")) {
                    java.util.ArrayList<entities.Park> parks = DBController.getAllParks();
                    client.sendToClient(new Message("ALL_PARKS_DATA", parks));
                }

                else if (request.getCommand().equals("FETCH_DEPT_REPORTS")) {
                    String[] data = (String[]) request.getData();
                    int parkId = Integer.parseInt(data[0]);
                    String month = data[1];
                    String year  = data[2];
                    uiLogger.accept("> Dept Manager fetching submitted report for park " + parkId + " (" + month + "/" + year + ")\n");
                    entities.ReportData report = DBController.getDeptMonthlyReport(parkId, month, year);
                    if (report != null) {
                        client.sendToClient(new Message("DEPT_REPORT_SUCCESS", report));
                    } else {
                        client.sendToClient(new Message("DEPT_REPORT_NOT_FOUND", "No submitted report found for that park and period."));
                    }
                }

                else if (request.getCommand().equals("FETCH_CANCELLATIONS_REPORT")) {
                    String[] data = (String[]) request.getData();
                    String month = data[0];
                    String year  = data[1];
                    uiLogger.accept("> Dept Manager generating cancellations report for " + month + "/" + year + "\n");
                    entities.ReportData report = DBController.getCancellationsReport(month, year);
                    client.sendToClient(new Message("CANCELLATIONS_REPORT_DATA", report));
                }

                else if (request.getCommand().equals("LOGOUT_REQUEST")) {
                    String username = clientUserMap.get(client);
                    if (username != null) {
                        loggedInUsers.remove(username);
                        clientUserMap.remove(client);
                        uiLogger.accept("> " + username + " logged out gracefully.\n");
                    }
                }

                else if (request.getCommand().equals("FETCH_CONNECTED_USERS")) {
                    java.util.ArrayList<String> users = new java.util.ArrayList<>(loggedInUsers);
                    client.sendToClient(new Message("CONNECTED_USERS_DATA", users));
                    uiLogger.accept("> Dept Manager requested connected users list (" + users.size() + " active).\n");
                }

                else if (request.getCommand().equals("KICK_USER")) {
                    String targetUsername = (String) request.getData();
                    ConnectionToClient targetClient = null;
                    synchronized (clientUserMap) {
                        for (java.util.Map.Entry<ConnectionToClient, String> entry : clientUserMap.entrySet()) {
                            if (entry.getValue().equals(targetUsername)) {
                                targetClient = entry.getKey();
                                break;
                            }
                        }
                    }
                    if (targetClient != null) {
                        loggedInUsers.remove(targetUsername);
                        clientUserMap.remove(targetClient);
                        try {
                            targetClient.sendToClient(new Message("KICKED", "Your session was terminated by the Department Manager."));
                            targetClient.close();
                        } catch (Exception ignored) {}
                        client.sendToClient(new Message("KICK_SUCCESS", targetUsername + " has been disconnected."));
                        uiLogger.accept("> Dept Manager force-disconnected: " + targetUsername + "\n");
                    } else {
                        client.sendToClient(new Message("KICK_FAILED", "User '" + targetUsername + "' is not connected."));
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
        String username = clientUserMap.remove(client);
        clientEmployeeMap.remove(client);
        if (username != null) {
            loggedInUsers.remove(username);
            uiLogger.accept("> " + username + " disconnected — session released.\n");
        } else {
            uiLogger.accept("> Client disconnected.\n");
        }
        if (clientDisconnectedHandler != null) {
            clientDisconnectedHandler.accept(client);
        }
    }

    // =========================================================================
    // --- REAL-TIME BROADCAST HELPERS ---
    // =========================================================================

    /** Push a message to every connected client of the given role. */
    private void broadcastToRole(String role, Message msg) {
        synchronized (clientEmployeeMap) {
            for (java.util.Map.Entry<ConnectionToClient, entities.Employee> entry : clientEmployeeMap.entrySet()) {
                if (role.equals(entry.getValue().getRole())) {
                    try { entry.getKey().sendToClient(msg); } catch (Exception ignored) {}
                }
            }
        }
    }

    /** Push a message to every ParkManager whose park matches parkId. */
    private void broadcastToPark(int parkId, Message msg) {
        synchronized (clientEmployeeMap) {
            for (java.util.Map.Entry<ConnectionToClient, entities.Employee> entry : clientEmployeeMap.entrySet()) {
                entities.Employee emp = entry.getValue();
                if ("ParkManager".equals(emp.getRole())
                        && emp.getParkId() != null
                        && emp.getParkId().intValue() == parkId) {
                    try { entry.getKey().sendToClient(msg); } catch (Exception ignored) {}
                }
            }
        }
    }

    /** Look up the park for an order and push fresh park data to that park's managers. */
    private void pushParkUpdate(int orderId) {
        int parkId = DBController.getOrderParkId(orderId);
        if (parkId > 0) {
            entities.Park updatedPark = DBController.getParkById(parkId);
            if (updatedPark != null) broadcastToPark(parkId, new Message("PARK_DETAILS_DATA", updatedPark));
        }
    }
}