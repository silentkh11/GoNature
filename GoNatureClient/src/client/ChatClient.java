package client;

import entities.Message;
import ocsf.client.AbstractClient;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * ChatClient handles the client-side OCSF network operations.
 * Includes a Global Watchdog and a Buffered Auto-Reconnect Engine.
 */
public class ChatClient extends AbstractClient {

    private static ChatClient instance;
    private Consumer<Message> responseHandler;

    // --- Singleton Pattern ---
    private ChatClient(String host, int port, Consumer<Message> responseHandler) throws IOException {
        super(host, port);
        this.responseHandler = responseHandler;
        openConnection(); 
    }

    public static ChatClient getInstance(String host, int port, Consumer<Message> responseHandler) throws IOException {
        if (instance == null) {
            instance = new ChatClient(host, port, responseHandler);
        } else {
            instance.setResponseHandler(responseHandler);
            if (!instance.isConnected()) {
                try {
                    instance.openConnection();
                } catch (IOException ignored) {} 
            }
        }
        return instance;
    }

    public static ChatClient getInstance() {
        if (instance == null) {
            System.err.println("CRITICAL: ChatClient accessed before initialization!");
        }
        return instance;
    }

    public void setResponseHandler(Consumer<Message> responseHandler) {
        this.responseHandler = responseHandler;
    }

    public void handleMessageFromClientUI(Object message) {
        try {
            // --- THE AUTO-RECONNECT ENGINE ---
            if (!isConnected()) {
                openConnection(); 
                // CRITICAL FIX: Give OCSF's internal streams 200ms to wake up before firing the message!
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            sendToServer(message);
        } catch (Exception e) {
            // Watchdog: If it STILL fails to send, the server is truly offline. Alert the UI.
            if (responseHandler != null) {
                responseHandler.accept(new Message("SERVER_DISCONNECTED", "Network link dropped."));
            }
        }
    }

    @Override
    protected void handleMessageFromServer(Object msg) {
        if (responseHandler != null && msg instanceof Message) {
            responseHandler.accept((Message) msg);
        }
    }

    // =========================================================================
    // --- GLOBAL NETWORK WATCHDOG ---
    // =========================================================================
    
    @Override
    protected void connectionClosed() {
        System.out.println("Connection to server closed cleanly.");
    }

    @Override
    protected void connectionException(Exception exception) {
        System.err.println("WATCHDOG TRIGGERED: Sudden server disconnect detected.");
        if (responseHandler != null) {
            responseHandler.accept(new Message("SERVER_DISCONNECTED", "Crash detected"));
        }
    }
}