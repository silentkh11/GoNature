package gui;

import client.ChatClient;
import entities.Employee;
import entities.Message;
import entities.Park;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ParkManagerController {

    // --- Current Parameters (Left Side) ---
    @FXML private Label lblParkName;
    @FXML private Label lblMaxCapacity;
    @FXML private Label lblCasualGap;
    @FXML private Label lblEstStay;

    // --- Request Update (Right Side) ---
    @FXML private TextField txtNewMaxCapacity;
    @FXML private TextField txtNewCasualGap;
    @FXML private TextField txtNewEstStay;
    
    @FXML private Label lblStatus;

    private Employee currentUser;
    private Park currentPark;

    @FXML
    public void initialize() {
        // Route network responses to this screen
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
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
                    // Populate the left side of the UI!
                    lblParkName.setText(currentPark.getName());
                    lblMaxCapacity.setText(String.valueOf(currentPark.getMaxCapacity()));
                    lblCasualGap.setText(String.valueOf(currentPark.getCasualGap()));
                    lblEstStay.setText(String.valueOf(currentPark.getEstimatedStayTime()));
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
                    showStatus((String) msg.getData(), "#d63031"); // Red error
                    break;
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }
}