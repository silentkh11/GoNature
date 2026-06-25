package gui.management;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.Park;
import entities.Promotion;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.HashMap;

public class ParkManagerController {

    // --- NEW: The Park Selector ---
    @FXML private ComboBox<String> parkCombo; 

    // --- Current Parameters (Left Side) ---
    @FXML private Label lblParkName;
    @FXML private Label lblMaxCapacity;
    @FXML private Label lblCasualGap;
    @FXML private Label lblEstStay;
    @FXML private Label lblCurrentVisitors;
    @FXML private Label lblActiveDiscount;

    // --- Request Update (Right Side) ---
    @FXML private TextField txtNewMaxCapacity;
    @FXML private TextField txtNewCasualGap;
    @FXML private TextField txtNewEstStay;

    // --- Promotional Discount ---
    @FXML private TextField txtDiscountPercent;
    @FXML private Button btnSubmitPromotion;

    @FXML private Label lblStatus;
    @FXML private Label lblEmployeeName;
    @FXML private Button themeBtn;

    private Employee currentUser;
    
    // We store the full Park objects here so we can swap data instantly without asking the DB again
    private HashMap<String, Park> parkMap = new HashMap<>();
    private Park currentPark;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);

        // --- NEW: Auto-update the dashboard when a new park is selected ---
        if (parkCombo != null) {
            parkCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    currentPark = parkMap.get(newVal);
                    updateParkDisplay();
                    showStatus("Editing: " + currentPark.getName(), "#0984e3");
                }
            });
        }
    }

    public void setUser(Employee user) {
        this.currentUser = user;
        if (lblEmployeeName != null)
            lblEmployeeName.setText("👤 " + user.getFirstName() + " " + user.getLastName());
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
    }

    @FXML
    void handleViewMyProfile(ActionEvent event) {
        if (currentUser == null) return;
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle("My Profile");
        alert.setHeaderText("Employee Information");
        alert.setContentText(
            "Name:       " + currentUser.getFirstName() + " " + currentUser.getLastName() + "\n" +
            "Employee ID: " + currentUser.getEmployeeId() + "\n" +
            "Email:       " + (currentUser.getEmail() != null ? currentUser.getEmail() : "—") + "\n" +
            "Role:        " + currentUser.getRole() + "\n" +
            "Park ID:     " + (currentUser.getParkId() != null ? currentUser.getParkId() : "—")
        );
        alert.showAndWait();
    }

    private void updateParkDisplay() {
        if (currentPark == null) return;
        
        lblParkName.setText(currentPark.getName());
        lblMaxCapacity.setText(String.valueOf(currentPark.getMaxCapacity()));
        lblCasualGap.setText(String.valueOf(currentPark.getCasualGap()));
        lblEstStay.setText(String.valueOf(currentPark.getEstimatedStayTime()));
        lblCurrentVisitors.setText(String.valueOf(currentPark.getCurrentVisitors()));
        lblActiveDiscount.setText(currentPark.getActiveDiscount() > 0 ? currentPark.getActiveDiscount() + "%" : "None");
        
        // Clear the input fields so they don't accidentally submit old data for a new park
        txtNewMaxCapacity.clear();
        txtNewCasualGap.clear();
        txtNewEstStay.clear();
        txtDiscountPercent.clear();
    }

    @FXML
    void submitParameters(ActionEvent event) {
        // Safety net: Prevent submitting if no park is selected
        if (currentPark == null) {
            showStatus("Please select a park from the dropdown first.", "#d63031");
            return;
        }
        
        try {
            int maxCap = Integer.parseInt(txtNewMaxCapacity.getText().trim());
            int gap = Integer.parseInt(txtNewCasualGap.getText().trim());
            int stay = Integer.parseInt(txtNewEstStay.getText().trim());

            Park updatedPark = new Park(currentPark.getParkId(), currentPark.getName(), maxCap, gap, stay,
                                        currentPark.getCurrentVisitors(), currentPark.getActiveDiscount());
            ChatClient.getInstance().handleMessageFromClientUI(new Message("UPDATE_PARK_PARAMS", updatedPark));
            showStatus("Sending parameter request to Department Manager...", "#0984e3");
        } catch (NumberFormatException e) {
            showStatus("Error: Parameters must be whole numbers.", "#d63031");
        }
    }

    @FXML
    void handleSubmitPromotion(ActionEvent event) {
        if (currentPark == null) {
            showStatus("Please select a park from the dropdown first.", "#d63031");
            return;
        }
        
        try {
            double discount = Double.parseDouble(txtDiscountPercent.getText().trim());
            if (discount <= 0 || discount > 100) {
                showStatus("Discount must be between 1 and 100.", "#d63031");
                return;
            }

            Promotion promo = new Promotion(0, currentPark.getParkId(), currentPark.getName(), discount, "Pending");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("SUBMIT_PROMOTION_REQUEST", promo));
            showStatus("Sending promotion request to Department Manager...", "#0984e3");
        } catch (NumberFormatException e) {
            showStatus("Error: Discount must be a number.", "#d63031");
        }
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            
            // --- WATCHDOG & KICK INTERCEPT ---
            if (msg.getCommand().equals("SERVER_DISCONNECTED") || msg.getCommand().equals("KICKED")) {
                if (msg.getCommand().equals("SERVER_DISCONNECTED")) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Network Security Alert");
                    alert.setHeaderText("Server Connection Lost");
                    alert.setContentText("Connection to the server was lost. For security, you have been logged out.");
                    alert.showAndWait();
                }
                forceUIToMainMenu();
                return;
            }

            // --- POPULATE THE PARK DROPDOWN ---
            if (msg.getCommand().equals("ALL_PARKS_DATA")) {
                ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                parkCombo.getItems().clear();
                parkMap.clear();

                for (Park p : parks) {
                    parkMap.put(p.getName(), p); 
                    parkCombo.getItems().add(p.getName());
                }
                if (!parks.isEmpty()) {
                    showStatus("Parks loaded. Please select a park to edit.", "#00b894");
                }
            }
            
            // --- RESPONSES FROM PARAMETER SUBMISSIONS ---
            else if (msg.getCommand().equals("UPDATE_PARAMS_SUCCESS")) {
                showStatus("Parameter request sent! Awaiting Department Manager approval.", "#00b894");
            }
            else if (msg.getCommand().equals("UPDATE_PARAMS_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            }

            // --- RESPONSES FROM PROMOTION SUBMISSIONS ---
            else if (msg.getCommand().equals("PROMOTION_SUBMIT_SUCCESS")) {
                showStatus("Promotion request sent! Awaiting Department Manager approval.", "#00b894");
            }
            else if (msg.getCommand().equals("PROMOTION_SUBMIT_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            }

            // --- DEPT MANAGER DECISION NOTIFICATIONS ---
            else if (msg.getCommand().equals("PARAMETER_DECISION_MADE")) {
                String decision = (String) msg.getData();
                showStatus("Your parameter request was " + decision + " by the Department Manager.", "#0984e3");
                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
            }
            else if (msg.getCommand().equals("PROMOTION_DECISION_MADE")) {
                String decision = (String) msg.getData();
                showStatus("Your promotion request was " + decision + " by the Department Manager.", "#0984e3");
                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
            }

            // --- REAL-TIME PARK UPDATES (live visitor count from gate entries/exits) ---
            else if (msg.getCommand().equals("PARK_DETAILS_DATA")) {
                Park updated = (Park) msg.getData();
                parkMap.put(updated.getName(), updated);

                if (currentPark != null && currentPark.getParkId() == updated.getParkId()) {
                    currentPark = updated;
                    updateParkDisplay();
                }
            }
        });
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        forceUIToMainMenu();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            ChatClient.getInstance().handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
        } catch (Exception ignored) {}
        forceUIToMainMenu();
    }

    @FXML
    void handleViewReports(ActionEvent event) {
        try {
            // Updated path to match your new package architecture!
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/management/ParkManagerReports.fxml"));
            javafx.scene.Parent root = loader.load();
            
            ParkManagerReportsController reportsController = loader.getController();
            reportsController.setUser(currentUser);
            
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Park Manager Reports");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void forceUIToMainMenu() {
        try {
            // Updated path to match your new package architecture!
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/guest/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = null;
            if (lblStatus != null && lblStatus.getScene() != null) {
                stage = (javafx.stage.Stage) lblStatus.getScene().getWindow();
            } else {
                stage = (javafx.stage.Stage) javafx.stage.Window.getWindows().stream()
                            .filter(javafx.stage.Window::isShowing)
                            .findFirst()
                            .orElse(null);
            }
            
            if (stage != null) {
                WindowChrome.setContent(stage, root, "GoNature - Welcome");
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }
}