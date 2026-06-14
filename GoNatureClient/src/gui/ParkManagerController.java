package gui;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.Park;
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

    // --- Request Update (Right Side) ---
    @FXML private TextField txtNewMaxCapacity;
    @FXML private TextField txtNewCasualGap;
    @FXML private TextField txtNewEstStay;
    
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
                    break;

                case "PARAMETER_DECISION_MADE":
                    // DeptManager approved or denied a request — re-fetch to see if ours was affected
                    if (currentUser != null && currentUser.getParkId() != null) {
                        ChatClient.getInstance().handleMessageFromClientUI(
                            new Message("FETCH_PARK_DETAILS", currentUser.getParkId()));
                        showStatus("Department Manager processed a parameter request. Parameters updated.", "#00b894");
                    }
                    break;
                    
                case "UPDATE_PARAMS_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894"); // Green success
                    // Clear the text fields
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
                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/gui/MainMenu.fxml"));
                        javafx.scene.Parent root = loader.load();
                        javafx.stage.Stage stage = (javafx.stage.Stage) lblStatus.getScene().getWindow();
                        WindowChrome.setContent(stage, root, "GoNature - Welcome");
                    } catch (Exception e) { e.printStackTrace(); }
                    break;
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
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