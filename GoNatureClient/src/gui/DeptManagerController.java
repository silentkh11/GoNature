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
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.ArrayList;

public class DeptManagerController {

    @FXML private TableView<ParameterRequest> requestsTable;
    @FXML private TableColumn<ParameterRequest, Integer> colReqId;
    @FXML private TableColumn<ParameterRequest, String>  colPark;
    @FXML private TableColumn<ParameterRequest, Integer> colCapacity;
    @FXML private TableColumn<ParameterRequest, Integer> colGap;
    @FXML private TableColumn<ParameterRequest, Integer> colTime;

    @FXML private Button btnApprove;
    @FXML private Button btnDeny;
    @FXML private Button themeBtn;
    @FXML private Label  lblStatus;

    @FXML private ListView<String> connectedUsersList;
    @FXML private Label lblUserCount;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());

        colReqId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colPark.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("newMaxCapacity"));
        colGap.setCellValueFactory(new PropertyValueFactory<>("newCasualGap"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("newEstimatedStayTime"));

        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PENDING_REQUESTS", null));

        // Right-click context menu on connected users list
        ContextMenu contextMenu = new ContextMenu();
        MenuItem kickItem = new MenuItem("🔌 Force Disconnect");
        kickItem.setOnAction(e -> {
            String selected = connectedUsersList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showStatus("Disconnecting " + selected + "...", "#e17055");
                ChatClient.getInstance().handleMessageFromClientUI(new Message("KICK_USER", selected));
            }
        });
        contextMenu.getItems().add(kickItem);
        connectedUsersList.setContextMenu(contextMenu);

        // Load connected users on open
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CONNECTED_USERS", null));
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleRefreshUsers(ActionEvent event) {
        showStatus("Refreshing connected users...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CONNECTED_USERS", null));
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
        ParameterRequest selected = requestsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select a request from the table first.", "#d63031");
            return;
        }
        String[] data = { String.valueOf(selected.getRequestId()), decision };
        ChatClient.getInstance().handleMessageFromClientUI(new Message("PROCESS_REQUEST_DECISION", data));
    }

    @FXML
    void handleViewReports(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/gui/DeptManagerReports.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Dept Manager Reports");
        } catch (Exception e) {
            showStatus("Error opening reports screen.", "#d63031");
            e.printStackTrace();
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            ChatClient.getInstance().handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
        } catch (Exception ignored) {}
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/gui/MainMenu.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                case "PENDING_REQUESTS_DATA":
                    ArrayList<ParameterRequest> list = (ArrayList<ParameterRequest>) msg.getData();
                    ObservableList<ParameterRequest> obs = FXCollections.observableArrayList(list);
                    requestsTable.setItems(obs);
                    break;

                case "DECISION_SUCCESS":
                    showStatus("Request successfully " + msg.getData() + "!", "#00b894");
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PENDING_REQUESTS", null));
                    break;

                case "DECISION_FAILED":
                    showStatus("Error: Database failed to process the decision.", "#d63031");
                    break;

                case "CONNECTED_USERS_DATA":
                    ArrayList<String> users = (ArrayList<String>) msg.getData();
                    connectedUsersList.setItems(FXCollections.observableArrayList(users));
                    lblUserCount.setText(users.size() + " user(s) online");
                    if (lblStatus.getText().equals("Refreshing connected users..."))
                        showStatus("", "");
                    break;

                case "KICK_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894");
                    // Refresh the list after kicking
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CONNECTED_USERS", null));
                    break;

                case "KICK_FAILED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}
