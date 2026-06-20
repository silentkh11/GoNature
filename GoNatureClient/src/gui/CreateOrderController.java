package gui;

import client.ChatClient;
import entities.Message;
import entities.VisitOrder;
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
    private String pendingWaitlistDate = null;

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
        
        // Initialize Time Combo
        ObservableList<String> times = FXCollections.observableArrayList(
            "08:00:00", "09:00:00", "10:00:00", "11:00:00", "12:00:00",
            "13:00:00", "14:00:00", "15:00:00", "16:00:00", "17:00:00"
        );
        timeCombo.setItems(times);

        // Initialize Type Combo
        typeCombo.getItems().addAll("Solo", "Family", "Group");

        // Restrict visitorsField to numbers only
        visitorsField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                visitorsField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });
        
        // Restrict visitorIdField to numbers only
        visitorIdField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("\\d*")) {
                visitorIdField.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        // --- THE CRITICAL FIX ---
        try {
            // We MUST pass the IP and Port here so the client initializes if this is the first screen opened!
            ChatClient.getInstance("127.0.0.1", 5555, this::handleServerResponse);
            
            // Now that we are 100% sure the connection exists, ask for the parks
            ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
        } catch (Exception e) {
            showStatus("Error connecting to server. Is it running?", "#d63031");
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
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/MainMenu.fxml"));
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
        String visitDate = datePicker.getValue().toString();
        String visitTime = timeCombo.getValue();
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

            // Populate the Dropdown dynamically when the server sends the parks
            if (msg.getCommand().equals("ALL_PARKS_DATA")) {
                ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                ObservableList<String> parkNames = FXCollections.observableArrayList();
                parkMap.clear(); 
                
                for (Park p : parks) {
                    parkNames.add(p.getName());
                    parkMap.put(p.getName(), p.getParkId()); // Map name -> ID
                }
                
                parkCombo.setItems(parkNames);
                if (!parkNames.isEmpty() && statusLabel.getText().startsWith("Reconnecting")) {
                    showStatus("Parks loaded! Please complete your booking.", "#0984e3");
                }
            }

            else if (msg.getCommand().equals("ORDER_CONFIRMED")) {
                VisitOrder confirmed = (VisitOrder) msg.getData();
                if (confirmed.getStatus().equals("Waitlisted")) {
                    showStatus("Park is full! You have been added to the Waitlist (Order #" + confirmed.getOrderId() + ").\n"
                        + "We will notify you at " + confirmed.getEmail() + " if a spot opens up.", "#e17055");
                    
                    int parkId = parkMap.get(parkCombo.getValue());
                    String[] data = { String.valueOf(parkId), pendingWaitlistDate };
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_AVAILABLE_SLOTS", data));
                    
                } else {
                    showStatus("Booking Confirmed! Order #" + confirmed.getOrderId() + "\n"
                        + "Total Price: ₪" + String.format("%.2f", confirmed.getPrice())
                        + "  |  A reminder will be sent 24h before your visit.", "#00b894");
                    priceEstimateLabel.setText(""); 
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
                getClass().getResource("/gui/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) statusLabel.getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
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