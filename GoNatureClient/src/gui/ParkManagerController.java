package gui;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.Park;
import entities.Promotion;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ParkManagerController {

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
    @FXML private Button themeBtn;

    private Employee currentUser;
    private Park currentPark;

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

    /**
     * This is called by the LoginController right after the scene switches.
     * It passes the logged-in user so we know which park to fetch!
     */
    public void setUser(Employee user) {
        this.currentUser = user;
        
        if (currentUser.getParkId() != null) {
            // Ask the server for this specific park's data
            ChatClient.getInstance().handleMessageFromClientUI(
                new Message("FETCH_PARK_DETAILS", currentUser.getParkId())
            );
        } else {
            showStatus("Error: No park assigned to this manager.", "#d63031");
        }
    }

    @FXML
    void submitParameters(ActionEvent event) {
        if (currentPark == null) {
            showStatus("Network error: Cannot update. Park data not loaded.", "#d63031");
            return;
        }

        try {
            int newMax = Integer.parseInt(txtNewMaxCapacity.getText().trim());
            int newGap = Integer.parseInt(txtNewCasualGap.getText().trim());
            int newStay = Integer.parseInt(txtNewEstStay.getText().trim());

            // Build a temporary Park object to hold the requested changes
            Park requestedUpdate = new Park(
                currentPark.getParkId(),
                currentPark.getName(),
                newMax,
                newGap,
                newStay,
                currentPark.getCurrentVisitors()
            );

            // Send to the server's "holding pen"
            ChatClient.getInstance().handleMessageFromClientUI(
                new Message("UPDATE_PARK_PARAMS", requestedUpdate)
            );
            
            showStatus("Submitting request...", "#0984e3"); // Blue processing text

        } catch (NumberFormatException e) {
            showStatus("Please enter valid numbers only.", "#d63031");
        }
    }

    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                
                case "PARK_DETAILS_DATA":
                    this.currentPark = (Park) msg.getData();
                    lblParkName.setText(currentPark.getName());
                    lblMaxCapacity.setText(String.valueOf(currentPark.getMaxCapacity()));
                    lblCasualGap.setText(String.valueOf(currentPark.getCasualGap()));
                    lblEstStay.setText(String.valueOf(currentPark.getEstimatedStayTime()));
                    lblCurrentVisitors.setText(currentPark.getCurrentVisitors() + " / " + currentPark.getMaxCapacity());
                    if (currentPark.getActiveDiscount() > 0) {
                        lblActiveDiscount.setText(String.format("%.0f%%", currentPark.getActiveDiscount()));
                        lblActiveDiscount.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
                    } else {
                        lblActiveDiscount.setText("None");
                        lblActiveDiscount.setStyle("");
                    }
                    break;

                case "PARAMETER_DECISION_MADE":
                    if (currentUser != null && currentUser.getParkId() != null) {
                        ChatClient.getInstance().handleMessageFromClientUI(
                            new Message("FETCH_PARK_DETAILS", currentUser.getParkId()));
                        showStatus("Department Manager processed a parameter request. Parameters updated.", "#00b894");
                    }
                    break;

                case "PROMOTION_DECISION_MADE":
                    if (currentUser != null && currentUser.getParkId() != null) {
                        ChatClient.getInstance().handleMessageFromClientUI(
                            new Message("FETCH_PARK_DETAILS", currentUser.getParkId()));
                        String decision = (String) msg.getData();
                        showStatus("Your promotion request was " + decision + " by the Department Manager.", "#00b894");
                    }
                    break;

                case "PROMOTION_SUBMIT_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894");
                    txtDiscountPercent.clear();
                    btnSubmitPromotion.setDisable(false);
                    break;

                case "PROMOTION_SUBMIT_FAILED":
                    showStatus((String) msg.getData(), "#d63031");
                    btnSubmitPromotion.setDisable(false);
                    break;
                    
                case "UPDATE_PARAMS_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894");
                    txtNewMaxCapacity.clear();
                    txtNewCasualGap.clear();
                    txtNewEstStay.clear();
                    break;
                    
                case "UPDATE_PARAMS_FAILED":
                case "PARK_DETAILS_ERROR":
                    showStatus((String) msg.getData(), "#d63031");
                    break;

                case "KICKED":
                    showStatus("Disconnected by the Department Manager.", "#d63031");
                    forceUIToMainMenu();
                    break;

                // --- WATCHDOG AUTO-LOGOUT ---
                case "SERVER_DISCONNECTED":
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Network Security Alert");
                    alert.setHeaderText("Server Connection Lost");
                    alert.setContentText("Connection to the server was lost. For security, you have been logged out.");
                    alert.showAndWait();
                    forceUIToMainMenu();
                    break;
            }
        });
    }

    private void forceUIToMainMenu() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/MainMenu.fxml"));
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
    
    @FXML
    void handleSubmitPromotion(ActionEvent event) {
        if (currentPark == null) {
            showStatus("Park data not loaded yet.", "#d63031");
            return;
        }
        String pctStr = txtDiscountPercent.getText().trim();
        if (pctStr.isEmpty()) {
            showStatus("Please enter a discount percentage.", "#d63031");
            return;
        }
        try {
            double pct = Double.parseDouble(pctStr);
            if (pct <= 0 || pct > 100) {
                showStatus("Discount must be between 1% and 100%.", "#d63031");
                return;
            }
            Promotion promo = new Promotion(0, currentPark.getParkId(), currentPark.getName(), pct, "Pending");
            btnSubmitPromotion.setDisable(true);
            showStatus("Submitting promotion request...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("SUBMIT_PROMOTION_REQUEST", promo));
        } catch (NumberFormatException e) {
            showStatus("Please enter a valid number.", "#d63031");
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            ChatClient.getInstance().handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
        } catch (Exception ignored) {}
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
    void handleViewReports(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/ParkManagerReports.fxml"));
            javafx.scene.Parent root = loader.load();
            
            // Pass the user to the reports controller
            ParkManagerReportsController reportsController = loader.getController();
            reportsController.setUser(currentUser);
            
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Park Manager Reports");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}