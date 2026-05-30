package gui;

import client.ChatClient;
import entities.Message;
import entities.ParameterRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.ArrayList;

public class DeptManagerController {

    @FXML private TableView<ParameterRequest> requestsTable;
    @FXML private TableColumn<ParameterRequest, Integer> colReqId;
    @FXML private TableColumn<ParameterRequest, String> colPark;
    @FXML private TableColumn<ParameterRequest, Integer> colCapacity;
    @FXML private TableColumn<ParameterRequest, Integer> colGap;
    @FXML private TableColumn<ParameterRequest, Integer> colTime;
    
    @FXML private Button btnApprove;
    @FXML private Button btnDeny;
    @FXML private Button themeBtn;
    @FXML private Label lblStatus;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        // 1. Link the columns to the variables inside your ParameterRequest.java file
        colReqId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colPark.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("newMaxCapacity"));
        colGap.setCellValueFactory(new PropertyValueFactory<>("newCasualGap"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("newEstimatedStayTime"));

        // 2. Set up the network listener
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
        
        // 3. Immediately ask the Server for the pending requests when the screen opens
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PENDING_REQUESTS", null));
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleApprove(ActionEvent event) {
        processDecision("Approved");
    }

    @FXML
    void handleDeny(ActionEvent event) {
        processDecision("Denied");
    }

    private void processDecision(String decision) {
        // Grab the row the manager clicked on
        ParameterRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        
        if (selected == null) {
            showStatus("Please select a request from the table first.", "#d63031");
            return;
        }

        // Send a String array to the Server: ["RequestID", "Decision"]
        String[] data = {String.valueOf(selected.getRequestId()), decision};
        ChatClient.getInstance().handleMessageFromClientUI(new Message("PROCESS_REQUEST_DECISION", data));
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            
            if (msg.getCommand().equals("PENDING_REQUESTS_DATA")) {
                // Populate the table with the data from the database
                ArrayList<ParameterRequest> list = (ArrayList<ParameterRequest>) msg.getData();
                ObservableList<ParameterRequest> observableList = FXCollections.observableArrayList(list);
                requestsTable.setItems(observableList);
                
            } else if (msg.getCommand().equals("DECISION_SUCCESS")) {
                String decision = (String) msg.getData();
                showStatus("Request successfully " + decision + "!", "#00b894");
                
                // Refresh the table to remove the request we just processed
                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PENDING_REQUESTS", null));
                
            } else if (msg.getCommand().equals("DECISION_FAILED")) {
                showStatus("Error: Database failed to process the decision.", "#d63031");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}