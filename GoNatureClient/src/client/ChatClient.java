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
    private volatile boolean isLoggingOut = false;

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
                // Propagate the IOException to the caller so they can show an error;
                // do NOT swallow it — a silent failure leaves the caller thinking they're connected.
                instance.openConnection();
            }
        }
        return instance;
    }

    public static ChatClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "ChatClient has not been initialized. Call getInstance(host, port, handler) first.");
        }
        return instance;
    }

    /** Returns the singleton if it has been initialized, null otherwise (never throws). */
    public static ChatClient getInstanceIfExists() {
        return instance;
    }

    public void setResponseHandler(Consumer<Message> responseHandler) {
        this.responseHandler = responseHandler;
    }

    public void handleMessageFromClientUI(Object message) {
        try {
            // Flag intentional logouts so connectionException doesn't treat the
            // subsequent socket close as a server crash.
            if (message instanceof Message
                    && "LOGOUT_REQUEST".equals(((Message) message).getCommand())) {
                isLoggingOut = true;
            }
            // --- THE AUTO-RECONNECT ENGINE ---
            if (!isConnected()) {
                openConnection();
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            sendToServer(message);
        } catch (Exception e) {
            if (responseHandler != null && !isLoggingOut) {
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
        if (isLoggingOut) {
            isLoggingOut = false;
            System.out.println("Logged out — connection closed cleanly.");
            return;
        }
        System.out.println("Connection to server closed cleanly.");
    }

    @Override
    protected void connectionException(Exception exception) {
        if (isLoggingOut) {
            isLoggingOut = false;
            System.out.println("Logged out — server closed the socket.");
            return;
        }
        System.err.println("WATCHDOG TRIGGERED: Sudden server disconnect detected.");
        if (responseHandler != null) {
            responseHandler.accept(new Message("SERVER_DISCONNECTED", "Crash detected"));
        }
    }
}