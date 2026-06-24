package client;

import entities.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Loads from the new 'guest' package location
        Parent root = FXMLLoader.load(getClass().getResource("/gui/guest/MainMenu.fxml"));

        // Uses the new 'core' package location
        Scene scene = gui.core.WindowChrome.install(primaryStage, root, 850, 650, "GoNature - Welcome");
        gui.core.ThemeManager.getInstance().applyTo(scene);

        primaryStage.setScene(scene);
        primaryStage.setResizable(true); 
        
        // --- CLEAN SHUTDOWN ON WINDOW CLOSE ---
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing client application...");
            try {
                // Use null-safe getter — user may close before ever making a network request
                ChatClient networkClient = ChatClient.getInstanceIfExists();
                if (networkClient != null && networkClient.isConnected()) {
                    // Send logout so the server cleans up session maps
                    networkClient.handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
                    // Give the packet 150 ms to reach the server before we kill the JVM.
                    // The server will also close the TCP socket from its side upon receiving
                    // LOGOUT_REQUEST, but we close from our side too as a safety net so the
                    // server table clears immediately even in edge cases.
                    Thread.sleep(150);
                    try { networkClient.closeConnection(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.err.println("Cleanup on exit failed: " + e.getMessage());
            }
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
    }
}