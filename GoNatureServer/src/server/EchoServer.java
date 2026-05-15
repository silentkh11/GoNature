package server;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import firstPackage.Message;
import firstPackage.Order;
import java.util.ArrayList;
import java.util.function.Consumer; // Added import

public class EchoServer extends AbstractServer {

    // Added a callback to send logs to the UI
    private Consumer<String> uiLogger;

    // Updated constructor to accept the UI logger
    public EchoServer(int port, Consumer<String> uiLogger) {
        super(port);
        this.uiLogger = uiLogger;
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
        // Send this directly to the Server UI!
        uiLogger.accept("> NEW CLIENT CONNECTED! IP: " + ip + " | Host: " + host + "\n");
    }
    
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        uiLogger.accept("> Client disconnected.\n");
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