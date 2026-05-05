package client;

import ocsf.client.AbstractClient;
import firstPackage.Message;

// A functional interface to pass messages back to the JavaFX GUI
import java.util.function.Consumer; 

public class ChatClient extends AbstractClient {

    // SINGLETON INSTANCE
    private static ChatClient instance;
    
    // Callback to update the UI
    private Consumer<Message> uiUpdater;

    // Private constructor (part of Singleton pattern)
    private ChatClient(String host, int port, Consumer<Message> uiUpdater) {
        super(host, port);
        this.uiUpdater = uiUpdater;
    }

    // Get the single instance of the client
    public static ChatClient getInstance(String host, int port, Consumer<Message> uiUpdater) throws Exception {
        if (instance == null) {
            instance = new ChatClient(host, port, uiUpdater);
            instance.openConnection(); // Connect to server when created
        }
        return instance;
    }
    
    // Just get the instance if it already exists
    public static ChatClient getInstance() {
        return instance;
    }

    // ---------------------------------------------------------
    // This method handles messages coming FROM the Server
    // ---------------------------------------------------------
    @Override
    protected void handleMessageFromServer(Object msg) {
        if (msg instanceof Message) {
            Message response = (Message) msg;
            System.out.println("Received from server: " + response.getCommand());
            
            // Pass the message back to the GUI to update the screen
            uiUpdater.accept(response);
        }
    }

    // Helper method to send messages to the server
    public void handleMessageFromClientUI(Object message) {
        try {
            sendToServer(message);
        } catch (Exception e) {
            System.out.println("Could not send message to server. Terminating client.");
            quit();
        }
    }

    public void quit() {
        try {
            closeConnection();
        } catch (Exception e) {
        }
        System.exit(0);
    }
}