package gui;

import client.ChatClient;
import entities.Message;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ParkEntranceController {

    @FXML private TextField txtOrderId;
    @FXML private Button btnAdmit;
    @FXML private Button btnExit;
    @FXML private Button themeBtn;
    @FXML private Label lblStatus;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleAdmit(ActionEvent event) {
        processGateAction("ENTER_PARK_REQUEST", "Verifying ticket...");
    }

    @FXML
    void handleExit(ActionEvent event) {
        processGateAction("EXIT_PARK_REQUEST", "Registering exit...");
    }
    
    /**
     * Unified method to handle the logic for both Entry and Exit.
     */
    private void processGateAction(String command, String loadingMessage) {
        String input = txtOrderId.getText().trim();
        
        if (input.isEmpty()) {
            showStatus("Please enter an Order ID.", "#d63031");
            return;
        }

        try {
            int orderId = Integer.parseInt(input);
            btnAdmit.setDisable(true);
            btnExit.setDisable(true);
            showStatus(loadingMessage, "#0984e3"); 
            
            ChatClient.getInstance().handleMessageFromClientUI(new Message(command, orderId));
            
        } catch (NumberFormatException e) {
            showStatus("Order ID must be a valid number.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            btnAdmit.setDisable(false);
            btnExit.setDisable(false);
            
            if (msg.getCommand().equals("ENTRY_APPROVED") || msg.getCommand().equals("EXIT_APPROVED")) {
                String successMsg = (String) msg.getData();
                showStatus(successMsg, "#00b894"); 
                txtOrderId.clear(); // Clear scanner for the next person
                
            } else if (msg.getCommand().equals("ENTRY_DENIED") || msg.getCommand().equals("EXIT_DENIED")) {
                String errorMsg = (String) msg.getData();
                showStatus(errorMsg, "#d63031"); 
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}