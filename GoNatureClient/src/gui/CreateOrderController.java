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
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timeCombo;
    @FXML private TextField visitorsField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Label statusLabel;
    @FXML private Button submitBtn;
    @FXML private Button themeBtn;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        try {
            ChatClient.getInstance("127.0.0.1", 5555, this::handleServerResponse);
        } catch (Exception e) {
            showStatus("Error: Cannot connect to the server.", "#d63031");
            submitBtn.setDisable(true);
            e.printStackTrace();
        }

        parkCombo.getItems().addAll("1 - Carmel National Park"); 
        timeCombo.getItems().addAll(
            "08:00", "09:00", "10:00", "11:00", "12:00", 
            "13:00", "14:00", "15:00", "16:00","16:45", "17:00","17:10","17:25","17:30", "17:50",
            "18:00","18:30","18:41", "19:09", "19:34", "20:00", "22:38", "22:44"
        );
        typeCombo.getItems().addAll("Solo", "Family", "Group");
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((javafx.scene.Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("GoNature - Welcome");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    void submitOrder(ActionEvent event) {
        if (parkCombo.getValue() == null || timeCombo.getValue() == null || typeCombo.getValue() == null) {
            showStatus("Please make a selection in all dropdown menus.", "#d63031");
            return;
        }
        
        String visitorId = visitorIdField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (visitorId.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showStatus("ID, Email, and Phone fields cannot be empty.", "#d63031");
            return;
        }

        if (!email.contains("@")) {
            showStatus("Please enter a valid email address.", "#d63031");
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

        int parkId = Integer.parseInt(parkCombo.getValue().split(" - ")[0]);
        String visitDate = selectedDate.toString(); 
        String visitTime = timeCombo.getValue() + ":00"; 
        String orderType = typeCombo.getValue();
        
        // --- NEW: Injecting the email and phone using our new constructor! ---
        VisitOrder newOrder = new VisitOrder(0, parkId, visitorId, visitDate, visitTime, visitorCount, orderType, "Pending", 0.0, email, phone);

        submitBtn.setDisable(true);
        showStatus("Checking availability with server...", "#0984e3");
        
        ChatClient.getInstance().handleMessageFromClientUI(new Message("NEW_ORDER_REQUEST", newOrder));
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            submitBtn.setDisable(false);
            
            if (msg.getCommand().equals("ORDER_CONFIRMED")) {
                VisitOrder finalizedOrder = (VisitOrder) msg.getData();
                
                if (finalizedOrder.getStatus().equals("Waitlisted")) {
                    showStatus("Park is full! You are WAITLISTED. Order #" + finalizedOrder.getOrderId(), "#e17055");
                } else {
                    showStatus("Booking CONFIRMED! Receipt #" + finalizedOrder.getOrderId() + " | Total: ₪" + finalizedOrder.getPrice(), "#00b894");
                    clearForm(); 
                }
                
            } else if (msg.getCommand().equals("ORDER_FAILED")) {
                String errorMsg = (String) msg.getData();
                showStatus("Error: " + errorMsg, "#d63031");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }

    private void clearForm() {
        parkCombo.setValue(null);
        visitorIdField.clear();
        emailField.clear();
        phoneField.clear();
        datePicker.setValue(null);
        timeCombo.setValue(null);
        visitorsField.clear();
        typeCombo.setValue(null);
    }
}