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
        
        // --- THE AUTO-LOGOUT ON EXIT FIX ---
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing client application...");
            try {
                // 1. Check if the network client exists and is connected
                ChatClient networkClient = ChatClient.getInstance();
                if (networkClient != null && networkClient.isConnected()) {
                    
                    // 2. Fire the logout request to the server
                    networkClient.handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
                    
                    // 3. CRITICAL: Give the network stream 150 milliseconds to actually transmit 
                    // the packet before Java violently kills the application!
                    Thread.sleep(150);
                }
            } catch (Exception e) {
                System.err.println("Logout on exit failed: " + e.getMessage());
            }
            
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
    }
}