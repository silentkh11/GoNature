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
        String input = txtOrderId.getText().trim();
        
        if (input.isEmpty()) {
            showStatus("Please enter an Order ID.", "#d63031"); // Red error
            return;
        }

        try {
            int orderId = Integer.parseInt(input);
            btnAdmit.setDisable(true);
            showStatus("Verifying ticket...", "#0984e3"); // Blue processing
            
            ChatClient.getInstance().handleMessageFromClientUI(new Message("ENTER_PARK_REQUEST", orderId));
            
        } catch (NumberFormatException e) {
            showStatus("Order ID must be a valid number.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            btnAdmit.setDisable(false);
            
            if (msg.getCommand().equals("ENTRY_APPROVED")) {
                String successMsg = (String) msg.getData();
                showStatus(successMsg, "#00b894"); // Green success
                txtOrderId.clear(); // Clear the scanner for the next person
                
            } else if (msg.getCommand().equals("ENTRY_DENIED")) {
                String errorMsg = (String) msg.getData();
                showStatus(errorMsg, "#d63031"); // Red error
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}