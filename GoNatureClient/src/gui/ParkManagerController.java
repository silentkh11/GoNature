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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.HashMap;

public class ParkManagerController {

    // --- NEW: Park Selection ---
    @FXML private ComboBox<String> cmbParkSelect;
    private HashMap<String, Integer> parkNameToId = new HashMap<>();

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
            
            ParkManagerReportsController reportsController = loader.getController();
            reportsController.setUser(currentUser);
            
            javafx.stage.Stage stage = (javafx.stage.Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Park Manager Reports");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- NEW: Handle Dropdown Selection ---
    @FXML
    void handleParkSelection(ActionEvent event) {
        String selectedPark = cmbParkSelect.getValue();
        if (selectedPark != null && parkNameToId.containsKey(selectedPark)) {
            int parkId = parkNameToId.get(selectedPark);
            showStatus("Fetching data for " + selectedPark + "...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PARK_DETAILS", parkId));
        }
    }

    @FXML
    void submitParameters(ActionEvent event) {
        if (currentPark == null) {
            showStatus("Error: Please select a park from the dropdown first.", "#d63031");
            return;
        }

        try {
            int newMax = Integer.parseInt(txtNewMaxCapacity.getText().trim());
            int newGap = Integer.parseInt(txtNewCasualGap.getText().trim());
            int newStay = Integer.parseInt(txtNewEstStay.getText().trim());

            if (newMax <= 0 || newGap < 0 || newStay <= 0) {
                showStatus("Values must be positive numbers.", "#d63031");
                return;
            }

            // Create a dummy Park object to hold the requested changes
            Park requestedUpdate = new Park(
                currentPark.getParkId(),
                currentPark.getName(),
                newMax,
                newGap,
                newStay,
                currentPark.getCurrentVisitors()
            );

            showStatus("Submitting request to Dept Manager...", "#0984e3"); 
            ChatClient.getInstance().handleMessageFromClientUI(new Message("UPDATE_PARK_PARAMS", requestedUpdate));

        } catch (NumberFormatException e) {
            showStatus("Please enter valid numeric values.", "#d63031");
        }
    }

    public void setUser(Employee user) {
        this.currentUser = user;
        showStatus("Fetching available parks...", "#0984e3");
        // Ask the server for the master list of parks instead of just one!
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                
                // --- NEW: Populate the Dropdown ---
                case "ALL_PARKS_DATA":
                    ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                    cmbParkSelect.getItems().clear();
                    parkNameToId.clear();
                    
                    for (Park p : parks) {
                        cmbParkSelect.getItems().add(p.getName());
                        parkNameToId.put(p.getName(), p.getParkId());
                    }
                    
                    // Automatically select the manager's assigned default park to save them time
                    if (currentUser != null && currentUser.getParkId() != null) {
                        for (Park p : parks) {
                            if (p.getParkId() == currentUser.getParkId()) {
                                cmbParkSelect.setValue(p.getName());
                                // Trigger the data fetch immediately
                                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PARK_DETAILS", p.getParkId()));
                                break;
                            }
                        }
                    } else if (!parks.isEmpty()) {
                        showStatus("Select a park from the dropdown to begin.", "#0984e3");
                    }
                    break;

                case "PARK_DETAILS_DATA":
                    this.currentPark = (Park) msg.getData();
                    lblParkName.setText(currentPark.getName() + " Parameter Dashboard");
                    lblMaxCapacity.setText(String.valueOf(currentPark.getMaxCapacity()));
                    lblCasualGap.setText(String.valueOf(currentPark.getCasualGap()));
                    lblEstStay.setText(String.valueOf(currentPark.getEstimatedStayTime()));
                    
                    if (lblCurrentVisitors != null) {
                        lblCurrentVisitors.setText(String.valueOf(currentPark.getCurrentVisitors()));
                    }
                    
                    showStatus("Park data synchronized.", "#00b894");
                    break;
                    
                case "PARAMETER_DECISION_MADE":
                    showStatus("A parameter request was " + msg.getData() + " by the Dept Manager. Refreshing...", "#0984e3");
                    if (currentPark != null) {
                        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PARK_DETAILS", currentPark.getParkId()));
                    }
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
                    
                // =========================================================================
                // --- INSTANT NETWORK DISCONNECT REACTION (AUTO-LOGOUT) ---
                // =========================================================================
                case "SERVER_DISCONNECTED":
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Connection Lost");
                    alert.setHeaderText("Server Disconnected");
                    alert.setContentText("The connection to the server was lost. For your security, you have been logged out.");
                    alert.showAndWait();

                    try {
                        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/MainMenu.fxml"));
                        javafx.scene.Parent root = loader.load();
                        javafx.stage.Stage stage = (javafx.stage.Stage) themeBtn.getScene().getWindow();
                        gui.WindowChrome.setContent(stage, root, "GoNature - Welcome");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }
}