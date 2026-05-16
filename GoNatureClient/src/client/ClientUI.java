package client;

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
        // Loads the visual design from the gui package
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ClientDashboard.fxml"));
        
        Scene scene = new Scene(root, 750, 650);
        primaryStage.setTitle("GoNature Client Management");
        
        // --- THE FIX ---
        // This forces the client to disconnect from the server and close all threads when 'X' is clicked
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Shutting down GoNature client...");
            
            // Tell the network client to close its connection safely
            ChatClient client = ChatClient.getInstance();
            if (client != null && client.isConnected()) {
                client.quit(); 
            }
            
            Platform.exit(); // Closes the JavaFX platform
            System.exit(0);  // Forcibly terminates the Java Virtual Machine
        });
        // ---------------

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}