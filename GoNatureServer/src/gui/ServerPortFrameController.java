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

    // --- STYLING CONSTANTS ---
    private final String startBtnStyle = "-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
    private final String stopBtnStyle = "-fx-background-color: #d63031; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
    private final String disabledBtnStyle = "-fx-background-color: #b2bec3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;";

    // This method runs automatically when the window opens
    @FXML
    public void initialize() {
        consoleText.appendText("> Welcome to GoNature Server Console.\n> System initialized. Ready to start.\n");
        startBtn.setStyle(startBtnStyle);
        stopBtn.setStyle(disabledBtnStyle);
    }

    @FXML
    void startServer(ActionEvent event) {
        try {
            int port = Integer.parseInt(portField.getText());
            server = new EchoServer(port);
            
            // Connect to DB (Ensure DBController is imported correctly)
            DBController.connectToDB();
            consoleText.appendText("> Database connected successfully.\n");
            
            // Start Server
            server.listen();
            
            // Update UI visually
            statusIndicator.setText("ONLINE (Port: " + port + ")");
            statusIndicator.setStyle("-fx-text-fill: #00b894;"); // Green
            
            startBtn.setDisable(true);
            startBtn.setStyle(disabledBtnStyle);
            portField.setDisable(true); // Prevent changing port while running
            
            stopBtn.setDisable(false);
            stopBtn.setStyle(stopBtnStyle);
            
            consoleText.appendText("> Server started. Waiting for clients...\n");
        } catch (NumberFormatException ex) {
            consoleText.appendText("> ERROR: Invalid port number!\n");
        } catch (Exception ex) {
            consoleText.appendText("> ERROR: Could not listen for clients! Port may be in use.\n");
        }
    }

    @FXML
    void stopServer(ActionEvent event) {
        try {
            if (server != null) {
                server.close();
            }
            
            // Update UI visually
            statusIndicator.setText("OFFLINE");
            statusIndicator.setStyle("-fx-text-fill: #d63031;"); // Red
            
            startBtn.setDisable(false);
            startBtn.setStyle(startBtnStyle);
            portField.setDisable(false); // Allow changing port again
            
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