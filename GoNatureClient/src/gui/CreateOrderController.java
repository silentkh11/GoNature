package gui;

import client.ChatClient;
import entities.Message;
import entities.VisitOrder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.LocalDate;

public class CreateOrderController {

    @FXML private ComboBox<String> parkCombo;
    @FXML private TextField visitorIdField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private TextField visitorsField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label statusLabel;
    @FXML private Button submitBtn;

    @FXML
    public void initialize() {
        // 1. Tell the network engine to route responses to THIS screen now
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);

        // 2. Populate the dropdown menus
        parkCombo.getItems().addAll("1 - Carmel National Park"); // Hardcoded for now. In Phase 3, we fetch this from the DB!
        timeCombo.getItems().addAll("08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00");
        typeCombo.getItems().addAll("Solo", "Family", "Group");
    }

    @FXML
    void submitOrder(ActionEvent event) {
        // --- STRICT UI VALIDATION ---
        if (parkCombo.getValue() == null || timeCombo.getValue() == null || typeCombo.getValue() == null) {
            showStatus("Please make a selection in all dropdown menus.", "#d63031");
            return;
        }
        
        String visitorId = visitorIdField.getText().trim();
        if (visitorId.isEmpty()) {
            showStatus("Visitor ID cannot be empty.", "#d63031");
            return;
        }

        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null || selectedDate.isBefore(LocalDate.now())) {
            showStatus("Please select a valid future date.", "#d63031");
            return;
        }

        int visitorCount;
        try {
            visitorCount = Integer.parseInt(visitorsField.getText().trim());
            if (visitorCount <= 0) {
                showStatus("Visitor count must be at least 1.", "#d63031");
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Please enter a valid number for Total Visitors.", "#d63031");
            return;
        }

        // --- PREPARE THE ORDER ---
        // Extract just the ID number from the park combo box string (e.g., "1 - Carmel" -> 1)
        int parkId = Integer.parseInt(parkCombo.getValue().split(" - ")[0]);
        String visitDate = selectedDate.toString(); // "YYYY-MM-DD"
        String visitTime = timeCombo.getValue() + ":00"; // Add seconds for MySQL TIME format (e.g., "08:00:00")
        String orderType = typeCombo.getValue();
        
        // We set ID to 0 and Status to "Pending". The Server will calculate the real ones.
        VisitOrder newOrder = new VisitOrder(0, parkId, visitorId, visitDate, visitTime, visitorCount, orderType, "Pending");

        submitBtn.setDisable(true);
        showStatus("Checking availability with server...", "#0984e3");
        
        // --- FIRE THE NETWORK REQUEST ---
        ChatClient.getInstance().handleMessageFromClientUI(new Message("SUBMIT_ORDER", newOrder));
    }

    /**
     * Handles the Server's reply specifically for the booking process.
     */
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            submitBtn.setDisable(false);
            
            if (msg.getCommand().equals("ORDER_SUCCESS")) {
                VisitOrder finalizedOrder = (VisitOrder) msg.getData();
                
                if (finalizedOrder.getStatus().equals("Waitlisted")) {
                    showStatus("Park is full! You are WAITLISTED. Order #" + finalizedOrder.getOrderId(), "#e17055");
                } else {
                    showStatus("Booking CONFIRMED! Your receipt is Order #" + finalizedOrder.getOrderId(), "#00b894");
                    clearForm(); // Clear the form on a successful confirmed booking
                }
                
            } else if (msg.getCommand().equals("ORDER_FAILED")) {
                String errorMsg = (String) msg.getData();
                showStatus("Error: " + errorMsg, "#d63031");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + ";");
    }

    private void clearForm() {
        parkCombo.setValue(null);
        visitorIdField.clear();
        datePicker.setValue(null);
        timeCombo.setValue(null);
        visitorsField.clear();
        typeCombo.setValue(null);
    }
}