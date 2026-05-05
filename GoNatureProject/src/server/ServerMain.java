package server;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ServerMain extends Application {

    private EchoServer server;
    private final int DEFAULT_PORT = 5555; // Standard port for OCSF

    // This is the actual execution point!
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GoNature Server");

        // GUI Elements
        Label statusLabel = new Label("Server Status: OFF");
        Button startBtn = new Button("Start Server");
        Button stopBtn = new Button("Stop Server");
        stopBtn.setDisable(true); // Disabled until started
        
        TextArea consoleText = new TextArea();
        consoleText.setEditable(false);
        consoleText.appendText("Welcome to GoNature Server Console.\n");

        // Start Button Logic
        startBtn.setOnAction(e -> {
            server = new EchoServer(DEFAULT_PORT);
            try {
                // Connect to the DB first!
                DBController.connectToDB();
                consoleText.appendText("Database connected successfully.\n");
                
                // Then start the server
                server.listen();
                statusLabel.setText("Server Status: ON (Port: " + DEFAULT_PORT + ")");
                startBtn.setDisable(true);
                stopBtn.setDisable(false);
                consoleText.appendText("Server started. Waiting for clients...\n");
            } catch (Exception ex) {
                consoleText.appendText("ERROR: Could not listen for clients!\n");
            }
        });

        // Stop Button Logic
        stopBtn.setOnAction(e -> {
            try {
                server.close();
                statusLabel.setText("Server Status: OFF");
                startBtn.setDisable(false);
                stopBtn.setDisable(true);
                consoleText.appendText("Server stopped.\n");
            } catch (Exception ex) {
                consoleText.appendText("ERROR: Could not stop the server properly.\n");
            }
        });

        // Layout
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(statusLabel, startBtn, stopBtn, consoleText);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}