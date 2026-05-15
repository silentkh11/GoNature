package gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server.DBController;
import server.EchoServer;

public class ServerPortFrameController {

    // --- FXML UI Elements ---
    @FXML private Label statusIndicator;
    @FXML private TextField portField;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea consoleText;

    private EchoServer server;

    // --- CONFIGURATION VARIABLES ---
    // Change this variable to easily update the server port in the future
    private static final int DEFAULT_PORT = 5555;

    // --- STYLING CONSTANTS ---
    private final String startBtnStyle = "-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
    private final String stopBtnStyle = "-fx-background-color: #d63031; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
    private final String disabledBtnStyle = "-fx-background-color: #b2bec3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;";

    // This method runs automatically when the window opens
    @FXML
    public void initialize() {
        consoleText.appendText("> Welcome to GoNature Server Console.\n> System initialized. Ready to start.\n");
        
        // BUG FIX: Actually disable the Stop button logically, not just visually
        startBtn.setDisable(false);
        startBtn.setStyle(startBtnStyle);
        
        stopBtn.setDisable(true); 
        stopBtn.setStyle(disabledBtnStyle);
        
        // Lock the port field so the user cannot change it
        portField.setText(String.valueOf(DEFAULT_PORT));
        portField.setEditable(false);
        portField.setDisable(true); 
    }

    @FXML
    void startServer(ActionEvent event) {
        try {
            int port = DEFAULT_PORT;
            
            // Initialize server with the UI Logger callback
            server = new EchoServer(port, (String logMsg) -> {
                javafx.application.Platform.runLater(() -> {
                    consoleText.appendText(logMsg);
                });
            });
            
            // Connect to DB
            DBController.connectToDB();
            consoleText.appendText("> Database connected successfully.\n");
            
            // Start Server
            server.listen();
            
            // Update UI visually
            statusIndicator.setText("ONLINE (Port: " + port + ")");
            statusIndicator.setStyle("-fx-text-fill: #00b894;"); 
            
            // Update Buttons (Lock Start, Unlock Stop)
            startBtn.setDisable(true);
            startBtn.setStyle(disabledBtnStyle);
            
            stopBtn.setDisable(false);
            stopBtn.setStyle(stopBtnStyle);
            
        } catch (Exception ex) {
            consoleText.appendText("> ERROR: Could not start server. Port may be in use.\n");
        }
    }

    @FXML
    void stopServer(ActionEvent event) {
        try {
            if (server != null) {
                server.close();
            }
            
            // Optional: If you have a disconnect method in DBController, call it here
            // DBController.disconnectFromDB(); 
            
            // Update UI visually
            statusIndicator.setText("OFFLINE");
            statusIndicator.setStyle("-fx-text-fill: #d63031;"); // Red
            
            // Update Buttons (Unlock Start, Lock Stop)
            startBtn.setDisable(false);
            startBtn.setStyle(startBtnStyle);
            
            stopBtn.setDisable(true);
            stopBtn.setStyle(disabledBtnStyle);
            
            consoleText.appendText("> Server stopped.\n");
        } catch (Exception ex) {
            consoleText.appendText("> ERROR: Could not stop the server properly.\n");
        }
    }

    @FXML
    void clearLogs(ActionEvent event) {
        consoleText.setText("> Logs cleared.\n");
    }
}