package gui.guest;

import client.ChatClient;
import client.ClientConfig;
import entities.Message;
import entities.VisitOrder;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import entities.Subscriber;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class GuestPortalController {

    // --- Search & Table ---
    @FXML private TextField txtVisitorId;
    @FXML private TableView<VisitOrder> ordersTable;
    @FXML private TableColumn<VisitOrder, Integer> colOrderId;
    @FXML private TableColumn<VisitOrder, Integer> colParkId;
    @FXML private TableColumn<VisitOrder, String>  colDate;
    @FXML private TableColumn<VisitOrder, String>  colTime;
    @FXML private TableColumn<VisitOrder, Integer> colVisitors;
    @FXML private TableColumn<VisitOrder, String>  colType;
    @FXML private TableColumn<VisitOrder, String>  colStatus;
    @FXML private Label  lblStatus;
    @FXML private Button themeBtn;

    // --- Edit Panel (Confirmed / Waitlisted) ---
    @FXML private VBox      pnlEdit;
    @FXML private DatePicker editDate;
    @FXML private ComboBox<String> editTime;
    @FXML private TextField editCount;
    @FXML private Label     lblEditStatus;
    @FXML private Label     lblCurrentPrice;

    // --- Confirm Panel (Pending Confirm / Waitlist Pending) ---
    @FXML private VBox  pnlConfirm;
    @FXML private Label lblConfirmHeading;
    @FXML private Label lblConfirmDetail;

    // --- Read-Only Panel (Confirmed / In Park / Completed / Cancelled) ---
    @FXML private VBox   pnlReadOnly;
    @FXML private Label  lblReadOnlyStatus;
    @FXML private Button btnReadOnlyCancel;

    // --- Subscriber Profile Panel ---
    @FXML private VBox      pnlSubscriberProfile;
    @FXML private TextField subFirstName;
    @FXML private TextField subLastName;
    @FXML private TextField subEmail;
    @FXML private TextField subPhone;
    @FXML private TextField subFamilySize;
    @FXML private Label     lblSubId;
    @FXML private Label     lblSubGuide;

    private String currentSearchedId = "";
    private Subscriber currentSubscriber = null;

    private static final String[] TIME_SLOTS = {
        "08:00:00", "09:00:00", "10:00:00", "11:00:00", "12:00:00",
        "13:00:00", "14:00:00", "15:00:00", "16:00:00", "17:00:00"
    };

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());

        colOrderId .setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colParkId  .setCellValueFactory(new PropertyValueFactory<>("parkId"));
        colDate    .setCellValueFactory(new PropertyValueFactory<>("visitDate"));
        colTime    .setCellValueFactory(new PropertyValueFactory<>("visitTime"));
        colVisitors.setCellValueFactory(new PropertyValueFactory<>("visitorCount"));
        colType    .setCellValueFactory(new PropertyValueFactory<>("orderType"));
        colStatus  .setCellValueFactory(new PropertyValueFactory<>("status"));

        editTime.setItems(FXCollections.observableArrayList(TIME_SLOTS));

        ordersTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSel, newSel) -> updateDetailPanel(newSel));

        hideAllPanels();

        try {
            ChatClient.getInstance(ClientConfig.getHost(), ClientConfig.getPort(), this::handleServerResponse);
        } catch (Exception e) {
            showStatus("Error: Cannot connect to server at " + ClientConfig.getHost() + ":" + ClientConfig.getPort(), "#d63031");
        }
    }

    // -------------------------------------------------------------------------
    // Selection listener — drives which bottom panel is shown
    // -------------------------------------------------------------------------

    private void updateDetailPanel(VisitOrder order) {
        hideAllPanels();
        if (order == null) return;

        String status = order.getStatus();

        if (status.equals("Booked")) {
            // Booking exists and is not yet attendance-confirmed — customer can edit date/time/count
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            try {
                editDate.setValue(LocalDate.parse(order.getVisitDate(), fmt));
            } catch (Exception e) {
                editDate.setValue(LocalDate.now().plusDays(1));
            }
            String t = order.getVisitTime();
            if (t != null && t.length() >= 8) {
                editTime.setValue(t.substring(0, 8));
            } else if (t != null) {
                editTime.setValue(t + ":00");
            }
            editCount.setText(String.valueOf(order.getVisitorCount()));
            lblCurrentPrice.setText(String.format("₪ %.2f", order.getPrice()));
            lblEditStatus.setText("(Booked)");
            showPanel(pnlEdit);

        } else if (status.equals("Confirmed")) {
            // Customer has confirmed attendance — locked, no editing
            lblReadOnlyStatus.setText(
                "✅ Attendance confirmed for " + order.getVisitDate() +
                " at " + shortTime(order.getVisitTime()) + " (" + order.getVisitorCount() + " visitors).\n" +
                "Your spot is guaranteed. You may still cancel if you can no longer make it."
            );
            btnReadOnlyCancel.setVisible(true);
            btnReadOnlyCancel.setManaged(true);
            showPanel(pnlReadOnly);

        } else if (status.equals("Waitlisted")) {
            // On waitlist — no editing, only cancel
            lblReadOnlyStatus.setText(
                "⏳ You are on the waitlist for " + order.getVisitDate() +
                " at " + shortTime(order.getVisitTime()) + " (" + order.getVisitorCount() + " visitors).\n" +
                "We will notify you by email if a spot opens up. You may cancel below."
            );
            btnReadOnlyCancel.setVisible(true);
            btnReadOnlyCancel.setManaged(true);
            showPanel(pnlReadOnly);

        } else if (status.equals("Pending Confirm")) {
            lblConfirmHeading.setText("⏰ Attendance Confirmation Required");
            lblConfirmDetail.setText(
                "Your visit is within 24 hours! You have 2 hours from when the reminder was sent to confirm.\n" +
                "Order #" + order.getOrderId() + "  ·  " + order.getVisitDate() + " at " +
                shortTime(order.getVisitTime()) + "  ·  " + order.getVisitorCount() + " visitors"
            );
            showPanel(pnlConfirm);

        } else if (status.equals("Waitlist Pending")) {
            lblConfirmHeading.setText("🎉 A Spot Opened — Confirm Now!");
            lblConfirmDetail.setText(
                "You have been moved from the waitlist. Confirm within 1 hour or the spot passes to the next person.\n" +
                "Order #" + order.getOrderId() + "  ·  " + order.getVisitDate() + " at " +
                shortTime(order.getVisitTime()) + "  ·  " + order.getVisitorCount() + " visitors"
            );
            showPanel(pnlConfirm);

        } else {
            String msg = switch (status) {
                case "In Park"   -> "🟢 You are currently inside the park. Visit is in progress.";
                case "Completed" -> "✅ This visit has been completed. Thank you for visiting GoNature!";
                case "Cancelled" -> "❌ This booking has been cancelled.";
                default          -> "ℹ Status: " + status;
            };
            lblReadOnlyStatus.setText(msg);
            btnReadOnlyCancel.setVisible(false);
            btnReadOnlyCancel.setManaged(false);
            showPanel(pnlReadOnly);
        }
    }

    private void hideAllPanels() {
        setPanel(pnlEdit,     false);
        setPanel(pnlConfirm,  false);
        setPanel(pnlReadOnly, false);
        // Note: pnlSubscriberProfile stays visible once loaded — it persists per session
        btnReadOnlyCancel.setVisible(false);
        btnReadOnlyCancel.setManaged(false);
    }

    private void showPanel(VBox panel) { setPanel(panel, true); }

    private void setPanel(VBox panel, boolean visible) {
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    // -------------------------------------------------------------------------
    // Action handlers
    // -------------------------------------------------------------------------

    @FXML
    void handleFindOrders(ActionEvent event) {
        currentSearchedId = txtVisitorId.getText().trim();
        if (currentSearchedId.isEmpty()) {
            showStatus("Please enter a Visitor ID to search.", "#d63031");
            return;
        }
        hideAllPanels();
        showStatus("Fetching your orders...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", currentSearchedId));
        // Also fetch subscriber profile so they can view/edit their details
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_SUBSCRIBER_REQUEST", currentSearchedId));
    }

    @FXML
    void handleSaveEdit(ActionEvent event) {
        VisitOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (editDate.getValue() == null) {
            showStatus("Please select a visit date.", "#d63031"); return;
        }
        if (editTime.getValue() == null) {
            showStatus("Please select a visit time.", "#d63031"); return;
        }
        int count;
        try {
            count = Integer.parseInt(editCount.getText().trim());
            if (count < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showStatus("Visitor count must be a positive number.", "#d63031"); return;
        }

        if (editDate.getValue().isBefore(LocalDate.now())) {
            showStatus("Visit date cannot be in the past.", "#d63031"); return;
        }

        VisitOrder updated = new VisitOrder(
            selected.getOrderId(), selected.getParkId(), selected.getVisitorId(),
            editDate.getValue().toString(), editTime.getValue(),
            count, selected.getOrderType(), selected.getStatus(), selected.getPrice()
        );

        showStatus("Saving changes...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("UPDATE_ORDER_REQUEST", updated));
    }

    @FXML
    void handleConfirmOrder(ActionEvent event) {
        VisitOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select an order to confirm.", "#d63031"); return;
        }
        String s = selected.getStatus();
        if (!s.equals("Pending Confirm") && !s.equals("Waitlist Pending")) {
            showStatus("Only 'Pending Confirm' or 'Waitlist Pending' orders can be confirmed here.", "#d63031");
            return;
        }
        showStatus("Confirming your attendance...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("CONFIRM_ORDER_REQUEST", selected.getOrderId()));
    }

    @FXML
    void handleCancelOrder(ActionEvent event) {
        VisitOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select an order to cancel.", "#d63031"); return;
        }
        String s = selected.getStatus();
        if (s.equals("Cancelled"))  { showStatus("This order is already cancelled.", "#d63031"); return; }
        if (s.equals("In Park"))    { showStatus("Cannot cancel — visitor is currently inside the park.", "#d63031"); return; }
        if (s.equals("Completed"))  { showStatus("Cannot cancel a completed visit.", "#d63031"); return; }
        ChatClient.getInstance().handleMessageFromClientUI(new Message("CANCEL_ORDER_REQUEST", selected.getOrderId()));
    }

    @FXML
    void handleSaveSubscriberProfile(ActionEvent event) {
        if (currentSubscriber == null) {
            showStatus("No subscriber profile loaded. Enter your ID and click Find Orders first.", "#d63031");
            return;
        }
        String sizeStr = subFamilySize.getText().trim();
        int familySize;
        try {
            familySize = Integer.parseInt(sizeStr);
            if (familySize < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showStatus("Family size must be a positive number.", "#d63031");
            return;
        }
        Subscriber updated = new Subscriber(
            currentSubscriber.getVisitorId(),
            subFirstName.getText().trim(),
            subLastName.getText().trim(),
            subEmail.getText().trim(),
            subPhone.getText().trim(),
            familySize,
            currentSubscriber.getCreditCard(),
            currentSubscriber.isGuide()
        );
        showStatus("Saving profile changes...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("UPDATE_SUBSCRIBER_REQUEST", updated));
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/guest/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -------------------------------------------------------------------------
    // Server response handler
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            if (msg.getCommand().equals("SERVER_DISCONNECTED")) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Network Security Alert");
                alert.setHeaderText("Server Connection Lost");
                alert.setContentText("Connection to the server was lost. Returning to the main menu.");
                alert.showAndWait();
                forceUIToMainMenu();
                return;
            }

            switch (msg.getCommand()) {
                case "GUEST_ORDERS_DATA" -> {
                    ArrayList<VisitOrder> list = (ArrayList<VisitOrder>) msg.getData();
                    hideAllPanels();
                    if (list.isEmpty()) {
                        showStatus("No active orders found for this ID.", "#636e72");
                    } else {
                        showStatus("Found " + list.size() + " order(s). Select one to manage it.", "#00b894");
                    }
                    ordersTable.setItems(FXCollections.observableArrayList(list));
                }
                case "CANCEL_SUCCESS", "CONFIRM_SUCCESS" -> {
                    showStatus((String) msg.getData(), "#00b894");
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", currentSearchedId));
                }
                case "CANCEL_FAILED", "CONFIRM_FAILED" -> {
                    showStatus((String) msg.getData(), "#d63031");
                }
                case "UPDATE_ORDER_SUCCESS" -> {
                    String result = (String) msg.getData();
                    // result = "SUCCESS:newStatus:newPrice"
                    String[] parts = result.split(":");
                    String newStatus = parts.length > 1 ? parts[1] : "";
                    String newPrice  = parts.length > 2 ? parts[2] : "—";
                    showStatus("Order updated! Status: " + newStatus + "  |  New price: ₪" + newPrice, "#00b894");
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", currentSearchedId));
                }
                case "UPDATE_ORDER_FAILED" -> {
                    showStatus((String) msg.getData(), "#d63031");
                }
                case "SUBSCRIBER_DATA" -> {
                    Subscriber sub = (Subscriber) msg.getData();
                    currentSubscriber = sub;
                    // Populate and show the subscriber profile panel
                    lblSubId.setText("Member #" + sub.getSubscriberId() + "  (Visitor ID: " + sub.getVisitorId() + ")");
                    subFirstName.setText(sub.getFirstName() != null ? sub.getFirstName() : "");
                    subLastName.setText(sub.getLastName() != null ? sub.getLastName() : "");
                    subEmail.setText(sub.getEmail() != null ? sub.getEmail() : "");
                    subPhone.setText(sub.getPhone() != null ? sub.getPhone() : "");
                    subFamilySize.setText(String.valueOf(sub.getFamilySize()));
                    lblSubGuide.setText(sub.isGuide() ? "✅ Certified Guide" : "Not a guide");
                    lblSubGuide.setStyle(sub.isGuide()
                        ? "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #00b894;"
                        : "-fx-font-size: 13px; -fx-text-fill: #636e72;");
                    pnlSubscriberProfile.setVisible(true);
                    pnlSubscriberProfile.setManaged(true);
                    showStatus("✅ Subscriber profile loaded — you can view and update your details below.", "#00b894");
                }
                case "UPDATE_SUBSCRIBER_SUCCESS" -> {
                    showStatus((String) msg.getData(), "#00b894");
                }
                case "UPDATE_SUBSCRIBER_FAILED" -> {
                    showStatus((String) msg.getData(), "#d63031");
                }
                default -> { /* ignore unrelated server messages */ }
            }
        });
    }

    // -------------------------------------------------------------------------

    private void forceUIToMainMenu() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/guest/MainMenu.fxml"));
            Stage stage = (Stage) lblStatus.getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }

    private String shortTime(String t) {
        if (t == null) return "—";
        return t.length() >= 5 ? t.substring(0, 5) : t;
    }
}
