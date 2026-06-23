package gui.gate;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.VisitOrder;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Font;

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

    @FXML private TextField txtManualExitCount;
    @FXML private Button btnManualExit;
    @FXML private Label lblManualExitStatus;

    private Integer parkId;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
        cmbWalkInType.getItems().addAll("Personal/Family", "Group");

        txtWalkInCount.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9]*"))
                txtWalkInCount.setText(val.replaceAll("[^0-9]", ""));
        });

        txtManualExitCount.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.matches("[0-9]*"))
                txtManualExitCount.setText(val.replaceAll("[^0-9]", ""));
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
    void handleLogout(ActionEvent event) {
        try {
            ChatClient.getInstance().handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
        } catch (Exception ignored) {}
        forceUIToMainMenu();
    }

    @FXML
    void handleAdmit(ActionEvent event) {
        processGateAction("ENTER_PARK_REQUEST", "Verifying ticket...");
    }

    @FXML
    void handleExit(ActionEvent event) {
        processGateAction("EXIT_PARK_REQUEST", "Registering exit...");
    }

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
            String orderType = type.equals("Group") ? "Group" : "Solo";
            String[] data = { String.valueOf(parkId), String.valueOf(count), orderType };
            btnWalkIn.setDisable(true);
            showWalkInStatus("Checking park capacity...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("WALKIN_REQUEST", data));
        } catch (NumberFormatException e) {
            showWalkInStatus("Please enter a valid number.", "#d63031");
        }
    }

    @FXML
    void handleManualExit(ActionEvent event) {
        if (parkId == null) {
            showManualExitStatus("Error: Gate worker has no park assigned.", "#d63031");
            return;
        }
        String countStr = txtManualExitCount.getText().trim();
        if (countStr.isEmpty()) {
            showManualExitStatus("Please enter the number of visitors exiting.", "#d63031");
            return;
        }
        try {
            int count = Integer.parseInt(countStr);
            if (count < 1) {
                showManualExitStatus("Exit count must be at least 1.", "#d63031");
                return;
            }
            String[] data = { String.valueOf(parkId), String.valueOf(count) };
            btnManualExit.setDisable(true);
            showManualExitStatus("Registering exit...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("MANUAL_EXIT_REQUEST", data));
        } catch (NumberFormatException e) {
            showManualExitStatus("Please enter a valid number.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            btnAdmit.setDisable(false);
            btnExit.setDisable(false);
            btnWalkIn.setDisable(false);
            btnManualExit.setDisable(false);

            switch (msg.getCommand()) {

                case "ENTRY_APPROVED": {
                    // Data format: orderId|visitors|orderType|price|date|time
                    String data = (String) msg.getData();
                    String[] parts = data.split("\\|", -1);
                    if (parts.length >= 6) {
                        String ordId  = parts[0];
                        String vis    = parts[1];
                        String type   = parts[2];
                        String price  = parts[3];
                        String date   = parts[4];
                        String time   = parts[5];
                        showStatus("Order #" + ordId + " admitted — collect ₪" + price, "#00b894");
                        showReceiptDialog(buildPreBookedReceipt(ordId, vis, type, price, date, time));
                    } else {
                        showStatus(data, "#00b894");
                    }
                    txtOrderId.clear();
                    break;
                }

                case "EXIT_APPROVED":
                    showStatus((String) msg.getData(), "#00b894");
                    txtOrderId.clear();
                    break;

                case "ENTRY_DENIED":
                case "EXIT_DENIED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;

                case "WALKIN_APPROVED": {
                    VisitOrder ticket = (VisitOrder) msg.getData();
                    showWalkInStatus(
                        "Ticket #" + ticket.getOrderId() + " admitted — collect ₪"
                        + String.format("%.0f", ticket.getPrice()),
                        "#00b894");
                    showReceiptDialog(buildWalkInReceipt(ticket));
                    txtWalkInCount.clear();
                    cmbWalkInType.setValue(null);
                    break;
                }

                case "WALKIN_DENIED":
                    showWalkInStatus((String) msg.getData(), "#d63031");
                    break;

                case "MANUAL_EXIT_SUCCESS":
                    showManualExitStatus((String) msg.getData(), "#00b894");
                    txtManualExitCount.clear();
                    break;

                case "MANUAL_EXIT_FAILED":
                    showManualExitStatus((String) msg.getData(), "#d63031");
                    break;

                case "KICKED":
                    showStatus("Disconnected by the Department Manager.", "#d63031");
                    forceUIToMainMenu();
                    break;

                case "SERVER_DISCONNECTED": {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Network Security Alert");
                    alert.setHeaderText("Server Connection Lost");
                    alert.setContentText("The gate terminal has lost connection to the GoNature server. For security, you are being logged out.");
                    alert.showAndWait();
                    forceUIToMainMenu();
                    break;
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Receipt helpers
    // -------------------------------------------------------------------------

    private String buildPreBookedReceipt(String orderId, String visitors,
                                          String orderType, String price,
                                          String date, String time) {
        return String.join("\n",
            "  GoNature — Visitor Receipt",
            "  ──────────────────────────────",
            "  Order #:      " + orderId,
            "  Visit Type:   Pre-Booked (" + orderType + ")",
            "  Date:         " + date,
            "  Time:         " + time,
            "  Visitors:     " + visitors,
            "  ──────────────────────────────",
            "  TOTAL DUE:    ₪" + price,
            "  ──────────────────────────────",
            "  Collect payment and stamp ticket."
        );
    }

    private String buildWalkInReceipt(VisitOrder ticket) {
        String typeLabel = "Group".equalsIgnoreCase(ticket.getOrderType())
            ? "Spontaneous (Group)" : "Spontaneous (Personal/Family)";
        return String.join("\n",
            "  GoNature — Walk-in Receipt",
            "  ──────────────────────────────",
            "  Ticket #:     " + ticket.getOrderId(),
            "  Visit Type:   " + typeLabel,
            "  Date:         " + ticket.getVisitDate(),
            "  Time:         " + ticket.getVisitTime(),
            "  Visitors:     " + ticket.getVisitorCount(),
            "  ──────────────────────────────",
            "  TOTAL DUE:    ₪" + String.format("%.0f", ticket.getPrice()),
            "  ──────────────────────────────",
            "  Collect payment and admit visitors."
        );
    }

    private void showReceiptDialog(String receiptText) {
        Alert receipt = new Alert(Alert.AlertType.INFORMATION);
        receipt.setTitle("GoNature — Payment Receipt");
        receipt.setHeaderText("Visitor Admitted");
        TextArea area = new TextArea(receiptText);
        area.setEditable(false);
        area.setWrapText(false);
        area.setFont(Font.font("Monospace", 13));
        area.setPrefSize(380, 230);
        receipt.getDialogPane().setContent(area);
        receipt.showAndWait();
    }

    // -------------------------------------------------------------------------

    private void forceUIToMainMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/guest/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) lblStatus.getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }

    private void showWalkInStatus(String message, String hexColor) {
        lblWalkInStatus.setText(message);
        lblWalkInStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold; -fx-font-size: 14px;");
    }

    private void showManualExitStatus(String message, String hexColor) {
        lblManualExitStatus.setText(message);
        lblManualExitStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }
}
