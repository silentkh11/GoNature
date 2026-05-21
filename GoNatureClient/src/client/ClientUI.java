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
        
        // --- 1. DEVELOPER FIX: Jump-start the network connection first! ---
        try {
            // We establish the connection to localhost:5555 with an empty placeholder listener
            ChatClient.getInstance("127.0.0.1", 5555, (Message msg) -> {});
            System.out.println("Developer bypass: Network connected successfully.");
        } catch (Exception e) {
            System.err.println("Developer bypass: Could not connect to server. Is it running?");
        }

        // --- 2. Load the UI ---
        Parent root = FXMLLoader.load(getClass().getResource("/gui/CreateOrder.fxml"));
        Scene scene = new Scene(root);
        
        primaryStage.setTitle("GoNature - Developer Booking Test");
        primaryStage.setScene(scene);
        
        // Ensure all background network threads die when the user closes the window
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing client application...");
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
    }
}