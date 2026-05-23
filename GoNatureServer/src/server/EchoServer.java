package server;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import java.util.function.Consumer;

import database.DBController;
import entities.Message;

public class EchoServer extends AbstractServer {

    private Consumer<String> uiLogger;
    private Consumer<ConnectionToClient> onClientConnected;
    private Consumer<ConnectionToClient> onClientDisconnected;

    public EchoServer(int port, Consumer<String> uiLogger, Consumer<ConnectionToClient> onConnect, Consumer<ConnectionToClient> onDisconnect) {
        super(port);
        this.uiLogger = uiLogger;
        this.onClientConnected = onConnect;
        this.onClientDisconnected = onDisconnect;
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        uiLogger.accept("> Message received: " + ((Message)msg).getCommand() + " from " + client.getInetAddress().getHostAddress() + "\n");

        if (msg instanceof Message) {
            Message request = (Message) msg;
            try {
                
                // --- LOGIN ROUTING ---
                if (request.getCommand().equals("LOGIN_REQUEST")) {
                    uiLogger.accept("> Client attempting to log in...\n");
                    
                    // The client will send the username and password as a String array: [username, password]
                    String[] credentials = (String[]) request.getData();
                    String username = credentials[0];
                    String password = credentials[1];
                    
                    // Ask the DBController to check the database
                    entities.Employee loggedInUser = DBController.verifyLogin(username, password);
                    
                    if (loggedInUser != null) {
                        uiLogger.accept("> Login SUCCESS for user: " + username + " (Role: " + loggedInUser.getRole() + ")\n");
                        client.sendToClient(new Message("LOGIN_SUCCESS", loggedInUser));
                    } else {
                        uiLogger.accept("> Login FAILED for user: " + username + " (Invalid credentials)\n");
                        client.sendToClient(new Message("LOGIN_FAILED", "Incorrect username or password."));
                    }
                }// --- PARK PARAMETER ROUTING ---
                else if (request.getCommand().equals("FETCH_PARK_INFO")) {
                    int parkId = (int) request.getData();
                    uiLogger.accept("> Fetching data for Park ID: " + parkId + "\n");
                    
                    entities.Park parkData = DBController.getParkById(parkId);
                    client.sendToClient(new Message("PARK_INFO_DATA", parkData));
                }
                else if (request.getCommand().equals("UPDATE_PARK_PARAMS")) {
                    entities.Park requestedUpdate = (entities.Park) request.getData();
                    uiLogger.accept("> Park ID " + requestedUpdate.getParkId() + " requested parameter update.\n");
                    
                    boolean success = DBController.updateParkParameters(requestedUpdate);
                    if (success) {
                        client.sendToClient(new Message("UPDATE_PARAMS_SUCCESS", requestedUpdate));
                    } else {
                        client.sendToClient(new Message("UPDATE_PARAMS_FAILED", "Database error during update."));
                    }
                }// --- BOOKING ENGINE ROUTING ---
                else if (request.getCommand().equals("SUBMIT_ORDER")) {
                    entities.VisitOrder incomingOrder = (entities.VisitOrder) request.getData();
                    uiLogger.accept("> Processing new order for Park ID: " + incomingOrder.getParkId() + "\n");
                    
                    entities.VisitOrder processedOrder = DBController.processNewOrder(incomingOrder);
                    
                    if (processedOrder != null) {
                        uiLogger.accept("> Order #" + processedOrder.getOrderId() + " processed with status: " + processedOrder.getStatus() + "\n");
                        client.sendToClient(new Message("ORDER_SUCCESS", processedOrder));
                    } else {
                        uiLogger.accept("> Order processing FAILED (Database Error).\n");
                        client.sendToClient(new Message("ORDER_FAILED", "Failed to process order in the database."));
                    }
                }
                
            } catch (Exception e) {
                uiLogger.accept("> ERROR processing client request.\n");
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        uiLogger.accept("> NEW CLIENT CONNECTED! IP: " + ip + " | Host: " + host + "\n");
        
        if (onClientConnected != null) {
            onClientConnected.accept(client);
        }
    }
    
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        if (client.getInfo("Disconnected") == null) { 
            client.setInfo("Disconnected", true);
            uiLogger.accept("> Client disconnected normally.\n");
            if (onClientDisconnected != null) {
                onClientDisconnected.accept(client);
            }
        }
    }

    @Override
    synchronized protected void clientException(ConnectionToClient client, Throwable exception) {
        if (client.getInfo("Disconnected") == null) { 
            client.setInfo("Disconnected", true);
            uiLogger.accept("> Client disconnected (Connection Lost).\n");
            if (onClientDisconnected != null) {
                onClientDisconnected.accept(client);
            }
        }
    }

    @Override
    protected void serverStarted() {
        uiLogger.accept("> Server listening for connections on port " + getPort() + "\n");
    }

    @Override
    protected void serverStopped() {
        uiLogger.accept("> Server has stopped listening for connections.\n");
        // Cleanly shut down HikariCP when you hit the 'Stop' button!
        DBController.getInstance().closePool();
    }
}