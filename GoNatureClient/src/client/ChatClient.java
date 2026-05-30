package client;

import ocsf.client.AbstractClient;
import entities.Message;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * The network engine for the GoNature Client.
 * Handles all communication with the Server and routes responses to the active UI Controller.
 */
public class ChatClient extends AbstractClient {

    // The single static instance (Singleton)
    private static ChatClient instance;
    
    // The active callback function. This tells the client WHERE to send the server's reply.
    private Consumer<Message> responseHandler;

    /**
     * Private constructor to prevent multiple instances.
     * Automatically opens the connection to the server.
     */
    private ChatClient(String host, int port, Consumer<Message> responseHandler) throws IOException {
        super(host, port);
        this.responseHandler = responseHandler;
        openConnection();
    }

    /**
     * Called by the FIRST screen (Login) to initialize the connection.
     */
    public static ChatClient getInstance(String host, int port, Consumer<Message> handler) throws IOException {
        if (instance == null || !instance.isConnected()) {
            // Create a fresh connection — handles first-time setup AND reconnection
            // after the server was not running when the screen first opened.
            instance = new ChatClient(host, port, handler);
        } else {
            instance.setResponseHandler(handler);
        }
        return instance;
    }

    /**
     * Called by ALL OTHER screens (Dashboards, Booking) to grab the active connection.
     */
    public static ChatClient getInstance() {
        if (instance == null) {
            System.err.println("CRITICAL ERROR: ChatClient not initialized. Cannot send message.");
        }
        return instance;
    }

    /**
     * CRITICAL: Updates the listener. 
     * When you change scenes (e.g., Login to Dashboard), the new controller calls this 
     * so that server responses are routed to the NEW screen, not the old one.
     */
    public void setResponseHandler(Consumer<Message> responseHandler) {
        this.responseHandler = responseHandler;
    }

    /**
     * Triggered automatically by OCSF when data arrives from the server.
     */
    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Message && responseHandler != null) {
            // Instantly pass the Message object to the active controller's handleServerResponse() method
            responseHandler.accept((Message) msg);
        } else {
            System.out.println("CLIENT: Received unknown object or no active UI handler is set.");
        }
    }

    /**
     * Triggered by UI Controllers to send a message TO the server.
     */
    public void handleMessageFromClientUI(Object message) {
        try {
            sendToServer(message);
        } catch (IOException e) {
            System.out.println("CLIENT: Could not send message to server. Terminating client.");
            quit();
        }
    }
    
    /**
     * Hook method triggered if the server abruptly closes the connection.
     */
    @Override
    protected void connectionClosed() {
        System.out.println("CLIENT: Connection to server closed.");
    }

    /**
     * Hook method triggered if the client loses connection to the server unexpectedly.
     */
    @Override
    protected void connectionException(Exception exception) {
        System.out.println("CLIENT: Server has disconnected abruptly (Connection Lost).");
    }

    /**
     * Safely terminates the network connection.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
            System.exit(0);
        }
    }
}