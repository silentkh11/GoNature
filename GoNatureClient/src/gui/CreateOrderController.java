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
import java.util.regex.Pattern;

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
    @FXML private Label priceEstimateLabel;
    @FXML private Button submitBtn;
    @FXML private Button themeBtn;

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    // Israeli mobile: starts with 05, exactly 10 digits total
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^05[0-9]{8}$");

    // Israeli ID: exactly 9 digits
    private static final Pattern ID_PATTERN =
        Pattern.compile("^[0-9]{9}$");

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
            "13:00", "14:00", "15:00", "16:00", "16:45", "17:00", "17:10", "17:25", "17:30", "17:50",
            "18:00", "18:30", "18:41", "19:09", "20:00", "20:22", "22:20"
        );
        typeCombo.getItems().addAll("Solo", "Family", "Group");

        // Live price estimate — updates as visitor count or type changes
        visitorsField.textProperty().addListener((obs, old, val) -> updatePriceEstimate());
        typeCombo.valueProperty().addListener((obs, old, val) -> updatePriceEstimate());

        // Phone: digits only, max 10 characters
        phoneField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9]*")) {
                phoneField.setText(val.replaceAll("[^0-9]", ""));
            } else if (val != null && val.length() > 10) {
                phoneField.setText(val.substring(0, 10));
            }
        });

        // Visitor ID: digits only, max 9 characters
        visitorIdField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9]*")) {
                visitorIdField.setText(val.replaceAll("[^0-9]", ""));
            } else if (val != null && val.length() > 9) {
                visitorIdField.setText(val.substring(0, 9));
            }
        });

        // Visitor count: digits only
        visitorsField.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9]*")) {
                visitorsField.setText(val.replaceAll("[^0-9]", ""));
            }
        });
    }

    private void updatePriceEstimate() {
        try {
            String countText = visitorsField.getText().trim();
            String type = typeCombo.getValue();
            if (countText.isEmpty() || type == null) {
                priceEstimateLabel.setText("");
                return;
            }
            int count = Integer.parseInt(countText);
            if (count <= 0) {
                priceEstimateLabel.setText("");
                return;
            }
            double price;
            if (type.equals("Group")) {
                int paying = Math.max(0, count - 1);
                price = paying * 75.0;
                priceEstimateLabel.setText(String.format("~₪%.0f  (group: %d paying × ₪75)", price, paying));
            } else {
                price = count * 85.0;
                priceEstimateLabel.setText(String.format("~₪%.0f  (%d visitor%s × ₪85)", price, count, count > 1 ? "s" : ""));
            }
        } catch (NumberFormatException e) {
            priceEstimateLabel.setText("");
        }
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
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
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
        String email     = emailField.getText().trim();
        String phone     = phoneField.getText().trim();

        if (visitorId.isEmpty()) {
            showStatus("Visitor ID cannot be empty.", "#d63031");
            return;
        }
        if (!ID_PATTERN.matcher(visitorId).matches()) {
            showStatus("Visitor ID must be exactly 9 digits.", "#d63031");
            return;
        }

        if (email.isEmpty()) {
            showStatus("Email address cannot be empty.", "#d63031");
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showStatus("Please enter a valid email (e.g. name@example.com).", "#d63031");
            return;
        }

        if (phone.isEmpty()) {
            showStatus("Phone number cannot be empty.", "#d63031");
            return;
        }
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            showStatus("Phone must be a valid Israeli mobile number (e.g. 0521234567).", "#d63031");
            return;
        }

        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null || !selectedDate.isAfter(LocalDate.now())) {
            showStatus("Please select a future date.", "#d63031");
            return;
        }

        int visitorCount;
        try {
            visitorCount = Integer.parseInt(visitorsField.getText().trim());
            if (visitorCount < 1 || visitorCount > 15) {
                showStatus("Visitor count must be between 1 and 15.", "#d63031");
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Please enter a valid number for Total Visitors.", "#d63031");
            return;
        }

        int parkId       = Integer.parseInt(parkCombo.getValue().split(" - ")[0]);
        String visitDate = selectedDate.toString();
        String visitTime = timeCombo.getValue() + ":00";
        String orderType = typeCombo.getValue();

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
                    showStatus("Park is full — you are WAITLISTED. Order #" + finalizedOrder.getOrderId()
                        + "  |  You will be notified by email & SMS if a spot opens.", "#e17055");
                } else {
                    showStatus("Booking CONFIRMED!  Order #" + finalizedOrder.getOrderId()
                        + "  |  Total: ₪" + String.format("%.0f", finalizedOrder.getPrice())
                        + "  |  A reminder will be sent 24h before your visit.", "#00b894");
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
        priceEstimateLabel.setText("");
    }
}
