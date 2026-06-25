package gui.guest;

import client.ChatClient;
import client.ClientConfig;
import entities.Message;
import entities.VisitOrder;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import entities.Park;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.HashMap;
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

    // Maps the visual Park Name to the actual Database Park ID
    private HashMap<String, Integer> parkMap = new HashMap<>();
    // Maps Park Name to full Park object (needed for discount in price estimate)
    private HashMap<String, Park> parkDataMap = new HashMap<>();
    private String pendingWaitlistDate = null;
    private boolean isSubscriber = false;
    private String lastCheckedSubscriberId = "";

    // Base price per person matches the server's DBController value
    private static final int BASE_PRICE = 100;

    // Full ordered list of bookable time slots — used to rebuild the combo on date change
    private static final java.util.List<String> ALL_TIMES = java.util.Arrays.asList(
        "08:00:00", "09:00:00", "10:00:00", "11:00:00", "12:00:00",
        "13:00:00", "14:00:00", "15:00:00", "16:00:00", "17:00:00"
    );

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    // Israeli mobile: starts with 05, exactly 10 digits total
    private static final Pattern PHONE_PATTERN =
        Pattern.compile("^05[0-9]{8}$");

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        
        // Wipe any hardcoded FXML "Ghost Items" instantly
        parkCombo.getItems().clear(); 
        
        // Initialize Time Combo — show all slots; past slots are hidden when today is selected
        timeCombo.setItems(FXCollections.observableArrayList(ALL_TIMES));

        // Initialize Type Combo
        typeCombo.getItems().addAll("Solo", "Family", "Group");

        // Restrict visitorsField to numbers only
        visitorsField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                visitorsField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        
        // Restrict visitorIdField to numbers only, max 9 digits (Israeli ID)
        visitorIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 9) digits = digits.substring(0, 9);
            if (!newVal.equals(digits)) visitorIdField.setText(digits);
        });

        // Restrict phoneField to digits only, max 10 digits (Israeli mobile)
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("[^\\d]", "");
            if (digits.length() > 10) digits = digits.substring(0, 10);
            if (!newVal.equals(digits)) phoneField.setText(digits);
            validateField(phoneField, !digits.isEmpty() && PHONE_PATTERN.matcher(digits).matches());
        });

        // Live field validation listeners
        visitorIdField.textProperty().addListener((obs, o, n) -> {
            String v = (n == null ? "" : n).trim();
            validateField(visitorIdField, v.length() == 9);
            if (v.length() == 9 && !v.equals(lastCheckedSubscriberId)) {
                lastCheckedSubscriberId = v;
                try {
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("CHECK_SUBSCRIBER", v));
                } catch (Exception ignored) {}
            } else if (v.length() < 9) {
                isSubscriber = false;
                lastCheckedSubscriberId = "";
                updatePriceEstimate();
            }
        });
        emailField.textProperty().addListener((obs, o, n) -> {
            String v = (n == null ? "" : n).trim();
            validateField(emailField, !v.isEmpty() && EMAIL_PATTERN.matcher(v).matches());
        });
        visitorsField.textProperty().addListener((obs, o, n) -> {
            try {
                int cnt = Integer.parseInt((n == null ? "" : n).trim());
                validateField(visitorsField, cnt >= 1 && cnt <= 15);
            } catch (NumberFormatException ex) {
                validateField(visitorsField, false);
            }
        });
        datePicker.valueProperty().addListener((obs, o, n) -> {
            java.time.LocalDate today = java.time.LocalDate.now();
            boolean ok = n != null && !n.isBefore(today)
                         && !n.isAfter(today.plusYears(1));
            if (datePicker.getEditor() != null)
                validateField(datePicker.getEditor(), ok);
            filterTimeSlotsForDate(n);
        });

        // Real-time price estimate whenever park, count, or type change
        parkCombo.valueProperty().addListener((obs, o, n) -> updatePriceEstimate());
        visitorsField.textProperty().addListener((obs, o, n) -> updatePriceEstimate());
        typeCombo.valueProperty().addListener((obs, o, n) -> updatePriceEstimate());

        try {
            ChatClient.getInstance(ClientConfig.getHost(), ClientConfig.getPort(), this::handleServerResponse);
            ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
        } catch (Exception e) {
            showStatus("Error connecting to server at " + ClientConfig.getHost() + ":" + ClientConfig.getPort() + ". Is it running?", "#d63031");
        }
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/guest/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void submitOrder(ActionEvent event) {
        showStatus("Reconnecting to server... Checking availability...", "#0984e3");

        // Basic Validation
        if (parkCombo.getValue() == null || visitorIdField.getText().trim().isEmpty() ||
            datePicker.getValue() == null || timeCombo.getValue() == null ||
            visitorsField.getText().trim().isEmpty() || typeCombo.getValue() == null) {
            showStatus("Please fill in all required fields.", "#d63031");
            return;
        }

        // Email/Phone Validation
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            showStatus("Please enter a valid email address.", "#d63031");
            return;
        }
        if (phone.isEmpty() || !PHONE_PATTERN.matcher(phone).matches()) {
            showStatus("Please enter a valid 10-digit mobile number (e.g., 0501234567).", "#d63031");
            return;
        }

        String visitorId = visitorIdField.getText().trim();
        if (visitorId.length() != 9) {
            showStatus("Visitor ID must be exactly 9 digits (Israeli ID).", "#d63031");
            return;
        }

        // Date must be today or in the future (not a past date, not more than 1 year ahead)
        java.time.LocalDate selectedDate = datePicker.getValue();
        java.time.LocalDate today = java.time.LocalDate.now();
        if (selectedDate.isBefore(today)) {
            showStatus("Visit date must be today or a future date.", "#d63031");
            return;
        }
        if (selectedDate.isAfter(today.plusYears(1))) {
            showStatus("Visit date cannot be more than 1 year in advance.", "#d63031");
            return;
        }
        String visitDate = selectedDate.toString();
        String visitTime = timeCombo.getValue();

        // For today's date, the selected time slot must not have already passed
        if (selectedDate.equals(today) && visitTime != null) {
            try {
                java.time.LocalTime slot = java.time.LocalTime.parse(visitTime.substring(0, 5));
                if (java.time.LocalTime.now().isAfter(slot)) {
                    showStatus("The " + visitTime.substring(0, 5) + " slot has already passed. Please choose a later time.", "#d63031");
                    filterTimeSlotsForDate(today);
                    return;
                }
            } catch (Exception ignored) {}
        }
        int visitorCount;
        try {
            visitorCount = Integer.parseInt(visitorsField.getText().trim());
            if (visitorCount < 1 || visitorCount > 15) {
                showStatus("Visitor count must be between 1 and 15.", "#d63031");
                return;
            }
        } catch (NumberFormatException e) {
            showStatus("Invalid visitor count.", "#d63031");
            return;
        }
        String orderType = typeCombo.getValue();

        // Safe Unboxing to prevent crashes if the server was offline!
        Integer parkId = parkMap.get(parkCombo.getValue());
        if (parkId == null) {
            showStatus("Synchronizing parks with server... Please select a park again.", "#d63031");
            try {
                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
            } catch (Exception ignored) {}
            return;
        }

        // Build the order safely
        VisitOrder order = new VisitOrder(0, parkId, visitorId, visitDate, visitTime, visitorCount, orderType, "Pending", 0.0, email, phone);

        // Disable button to prevent spam clicks
        submitBtn.setDisable(true);
        pendingWaitlistDate = visitDate; 
        
        // Send to Server
        ChatClient.getInstance().handleMessageFromClientUI(new Message("NEW_ORDER_REQUEST", order));
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            
            // Always unblock the submit button whenever any response comes back
            if (submitBtn != null) {
                submitBtn.setDisable(false);
            }

            // --- WATCHDOG AUTO-LOGOUT ---
            if (msg.getCommand().equals("SERVER_DISCONNECTED")) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Network Security Alert");
                alert.setHeaderText("Server Connection Lost");
                alert.setContentText("Connection to the server was lost. Returning to the main menu.");
                alert.showAndWait();
                forceUIToMainMenu();
                return;
            }

            if (msg.getCommand().equals("SUBSCRIBER_STATUS")) {
                boolean isSub = (Boolean) msg.getData();
                // Guard against stale responses — only apply if ID still matches
                String currentId = visitorIdField.getText().trim();
                if (currentId.equals(lastCheckedSubscriberId)) {
                    isSubscriber = isSub;
                    updatePriceEstimate();
                    if (isSub) {
                        showStatus("✓ Subscriber — 10% subscriber discount applied.", "#00b894");
                    } else {
                        showStatus("Standard pricing — visitor is not a subscriber.", "#7A98B2");
                    }
                }
                return;
            }

            // Populate the Dropdown dynamically when the server sends the parks
            if (msg.getCommand().equals("ALL_PARKS_DATA")) {
                ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                ObservableList<String> parkNames = FXCollections.observableArrayList();
                parkMap.clear();
                parkDataMap.clear();

                for (Park p : parks) {
                    parkNames.add(p.getName());
                    parkMap.put(p.getName(), p.getParkId());
                    parkDataMap.put(p.getName(), p);
                }

                parkCombo.setItems(parkNames);
                if (!parkNames.isEmpty() && statusLabel.getText().startsWith("Reconnecting")) {
                    showStatus("Parks loaded! Please complete your booking.", "#0984e3");
                }
                updatePriceEstimate();
            }

            else if (msg.getCommand().equals("ORDER_CONFIRMED")) {
                VisitOrder confirmed = (VisitOrder) msg.getData();
                if (confirmed.getStatus().equals("Waitlisted")) {
                    showStatus("Park is full! You have been added to the Waitlist (Order #" + confirmed.getOrderId() + ").\n"
                        + "We will notify you at " + confirmed.getEmail() + " if a spot opens up.", "#e17055");
                    
                    Integer parkId = parkMap.get(parkCombo.getValue());
                    if (parkId == null) return;
                    String[] data = { String.valueOf(parkId), pendingWaitlistDate };
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_AVAILABLE_SLOTS", data));
                    
                } else {
                    showStatus("Booking Confirmed! Order #" + confirmed.getOrderId()
                        + "  |  A reminder will be sent 24h before your visit.", "#00b894");
                    priceEstimateLabel.setText(
                        String.format("₪%.0f confirmed", confirmed.getPrice()));
                    priceEstimateLabel.setStyle(
                        "-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #1DC98A;");
                    clearForm();
                }

            } else if (msg.getCommand().equals("ORDER_FAILED")) {
                showStatus("Error: " + (String) msg.getData(), "#d63031");

            } else if (msg.getCommand().equals("AVAILABLE_SLOTS_DATA")) {
                ArrayList<String> slots = (ArrayList<String>) msg.getData();
                if (slots.isEmpty()) {
                    priceEstimateLabel.setText(
                        "No other available slots on " + pendingWaitlistDate + ".\n"
                        + "💡 Tip: pick an alternative date above and click Submit again "
                        + "to book a different day.");
                } else {
                    priceEstimateLabel.setText(
                        "Other available slots on " + pendingWaitlistDate
                        + ":\n" + String.join("  |  ", slots) + "\n"
                        + "💡 Tip: you can also change the date above and resubmit "
                        + "to book an alternative day.");
                }
                priceEstimateLabel.setStyle("-fx-text-fill: #64b5f6; -fx-font-weight: bold;");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }

    private void forceUIToMainMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/gui/guest/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) statusLabel.getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filterTimeSlotsForDate(java.time.LocalDate selected) {
        String currentSelection = timeCombo.getValue();
        ObservableList<String> available;

        if (selected != null && selected.equals(java.time.LocalDate.now())) {
            java.time.LocalTime now = java.time.LocalTime.now();
            available = FXCollections.observableArrayList();
            for (String t : ALL_TIMES) {
                try {
                    if (!java.time.LocalTime.parse(t.substring(0, 5)).isBefore(now)) {
                        available.add(t);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            available = FXCollections.observableArrayList(ALL_TIMES);
        }

        timeCombo.setItems(available);
        // Keep the current selection only if it's still in the valid list
        if (currentSelection != null && available.contains(currentSelection)) {
            timeCombo.setValue(currentSelection);
        } else {
            timeCombo.setValue(null);
        }
    }

    private void updatePriceEstimate() {
        String parkName = parkCombo.getValue();
        String countStr = visitorsField.getText().trim();
        String type     = typeCombo.getValue();

        if (parkName == null || countStr.isEmpty() || type == null) {
            priceEstimateLabel.setText("—");
            priceEstimateLabel.setStyle(
                "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #7A98B2;");
            return;
        }
        int count;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            return;
        }
        if (count < 1 || count > 15) return;

        double discount = 0.0;
        Park park = parkDataMap.get(parkName);
        if (park != null) discount = park.getActiveDiscount();

        // Mirror the server's DBController pricing:
        // Solo/Family pre-booked: visitors × base × 0.85 (15% advance discount)
        // Group pre-booked: (visitors-1) × base × 0.75 × 0.88 (guide free, group+advance)
        // Then apply park active discount on top.
        double estimate;
        String note;
        if ("Group".equals(type)) {
            int paying = Math.max(0, count - 1);
            estimate = paying * (BASE_PRICE * 0.75 * 0.88);
            if (isSubscriber) {
                estimate *= 0.90;
                note = "(guide free · certified guide · 10% subscriber discount)";
            } else {
                note = "(guide goes free · certified guide required)";
            }
        } else {
            if (isSubscriber) {
                estimate = count * (BASE_PRICE * 0.85 * 0.90);
                note = "(15% advance + 10% subscriber discount)";
            } else {
                estimate = count * (BASE_PRICE * 0.85);
                note = "(15% advance booking discount)";
            }
        }
        if (discount > 0) {
            estimate *= (1.0 - discount / 100.0);
            note += String.format(" · %.0f%% park promo", discount);
        }
        priceEstimateLabel.setText(String.format("~₪%.0f", estimate));
        priceEstimateLabel.setStyle(
            "-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #1DC98A;");
        // Show the pricing note as a subtle hint (only when no important server message is active)
        String current = statusLabel.getText();
        if (current.isEmpty() || current.startsWith("~") || current.startsWith("Parks loaded")
                || current.startsWith("Standard pricing")) {
            showStatus("~ " + note, "#7A98B2");
        }
    }

    private void validateField(javafx.scene.control.Control field, boolean valid) {
        if (valid) {
            field.setStyle("-fx-border-color: #1DC98A; -fx-border-width: 1.5; -fx-border-radius: 8;");
        } else {
            field.setStyle("-fx-border-color: #E54040; -fx-border-width: 1.5; -fx-border-radius: 8;");
        }
    }

    private void clearForm() {
        isSubscriber = false;
        lastCheckedSubscriberId = "";
        parkCombo.setValue(null);
        visitorIdField.clear();
        emailField.clear();
        phoneField.clear();
        datePicker.setValue(null);
        timeCombo.setValue(null);
        visitorsField.clear();
        typeCombo.setValue(null);
        priceEstimateLabel.setText("—");
        priceEstimateLabel.setStyle(
            "-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #7A98B2;");
        // Reset field border colours
        for (javafx.scene.control.Control f : new javafx.scene.control.Control[]{
                visitorIdField, emailField, phoneField, visitorsField}) {
            f.setStyle("");
        }
        if (datePicker.getEditor() != null) datePicker.getEditor().setStyle("");
    }
}