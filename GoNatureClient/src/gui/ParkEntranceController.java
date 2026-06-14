package gui;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.VisitOrder;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ParkEntranceController {

    @FXML private TextField txtOrderId;
    @FXML private Button btnAdmit;
    @FXML private Button btnExit;
    @FXML private Button themeBtn;
    @FXML private Label lblStatus;

    @FXML private TextField txtWalkInCount;
    @FXML private ComboBox<String> cmbWalkInType;
    @FXML private Button btnWalkIn;
    @FXML private Label lblWalkInStatus;

    private Integer parkId;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
        cmbWalkInType.getItems().addAll("Personal/Family", "Group");

        // Digits-only filter for walk-in count
        txtWalkInCount.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9]*"))
                txtWalkInCount.setText(val.replaceAll("[^0-9]", ""));
        });
    }

    public void setUser(Employee user) {
        this.parkId = user.getParkId();
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleAdmit(ActionEvent event) {
        processGateAction("ENTER_PARK_REQUEST", "Verifying ticket...", true);
    }

    @FXML
    void handleExit(ActionEvent event) {
        processGateAction("EXIT_PARK_REQUEST", "Registering exit...", true);
    }

    private void processGateAction(String command, String loadingMessage, boolean isBooked) {
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

    @FXML
    void handleWalkIn(ActionEvent event) {
        if (parkId == null) {
            showWalkInStatus("Error: Gate worker has no park assigned.", "#d63031");
            return;
        }
        String countStr = txtWalkInCount.getText().trim();
        String type = cmbWalkInType.getValue();

        if (countStr.isEmpty()) {
            showWalkInStatus("Please enter the number of visitors.", "#d63031");
            return;
        }
        if (type == null) {
            showWalkInStatus("Please select a visit type.", "#d63031");
            return;
        }
        try {
            int count = Integer.parseInt(countStr);
            if (count < 1 || count > 50) {
                showWalkInStatus("Visitor count must be between 1 and 50.", "#d63031");
                return;
            }
            // Map display name to internal order type
            String orderType = type.equals("Group") ? "Group" : "Solo";
            String[] data = { String.valueOf(parkId), String.valueOf(count), orderType };
            btnWalkIn.setDisable(true);
            showWalkInStatus("Checking park capacity...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("WALKIN_REQUEST", data));
        } catch (NumberFormatException e) {
            showWalkInStatus("Please enter a valid number.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            btnAdmit.setDisable(false);
            btnExit.setDisable(false);
            btnWalkIn.setDisable(false);

            switch (msg.getCommand()) {
                case "ENTRY_APPROVED":
                    showStatus((String) msg.getData(), "#00b894");
                    txtOrderId.clear();
                    break;
                case "ENTRY_DENIED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;
                case "EXIT_APPROVED":
                    showStatus((String) msg.getData(), "#00b894");
                    txtOrderId.clear();
                    break;
                case "EXIT_DENIED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;
                case "WALKIN_APPROVED":
                    VisitOrder ticket = (VisitOrder) msg.getData();
                    showWalkInStatus(
                        "ADMITTED!  Ticket #" + ticket.getOrderId()
                        + "  |  " + ticket.getVisitorCount() + " visitors"
                        + "  |  Bill: ₪" + String.format("%.0f", ticket.getPrice()),
                        "#00b894");
                    txtWalkInCount.clear();
                    cmbWalkInType.setValue(null);
                    break;
                case "WALKIN_DENIED":
                    showWalkInStatus((String) msg.getData(), "#d63031");
                    break;
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }

    private void showWalkInStatus(String message, String hexColor) {
        lblWalkInStatus.setText(message);
        lblWalkInStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}
