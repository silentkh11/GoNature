package server;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Loads the visual design from the gui package
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));
        
        Scene scene = new Scene(root, 850, 550);
        primaryStage.setTitle("GoNature Server Configuration");
        
        // --- ADD THIS BLOCK ---
        // This forces the entire application and all background threads to close when the X is clicked.
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Shutting down GoNature server...");
            Platform.exit(); // Closes the JavaFX platform
            System.exit(0);  // Forcibly terminates the Java Virtual Machine and all background threads
        });
        // ----------------------

        primaryStage.setScene(scene);
        primaryStage.show();
    }
}