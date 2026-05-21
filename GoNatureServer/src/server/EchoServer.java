package server;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

import java.util.ArrayList;
import java.util.function.Consumer;

import entities.Message;
import entities.Order;

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
                if (request.getCommand().equals("GET_ORDERS")) {
                    uiLogger.accept("> Client requested orders.\n");
                    ArrayList<Order> orders = DBController.getOrders();
                    client.sendToClient(new Message("ORDERS_DATA", orders));
                }
                else if (request.getCommand().equals("UPDATE_ORDER")) {
                    Order orderToUpdate = (Order) request.getData();
                    uiLogger.accept("> Client requested to update order: #" + orderToUpdate.getOrderNumber() + "\n");
                    DBController.updateOrder(orderToUpdate);
                    client.sendToClient(new Message("UPDATE_SUCCESS", null));
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
        // Prevents double treatment as directed by the instructor
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
        // Prevents double handling in the event of a client crash
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
    }
}