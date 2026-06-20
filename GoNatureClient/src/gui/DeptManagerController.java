package gui;

import client.ChatClient;
import entities.Message;
import entities.ParameterRequest;
import entities.Park;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;

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

    @FXML private ListView<String> connectedUsersList;
    @FXML private Label lblUserCount;

    // --- NEW DIRECT OVERRIDE ELEMENTS ---
    @FXML private ComboBox<String> cmbOverridePark;
    @FXML private TextField txtOverrideMax;
    @FXML private TextField txtOverrideGap;
    @FXML private TextField txtOverrideStay;
    @FXML private Button btnForceUpdate;

    private HashMap<String, Integer> parkNameToId = new HashMap<>();

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);

        colReqId.setCellValueFactory(new PropertyValueFactory<>("requestId"));
        colPark.setCellValueFactory(new PropertyValueFactory<>("parkName"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("newMaxCapacity"));
        colGap.setCellValueFactory(new PropertyValueFactory<>("newCasualGap"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("newEstimatedStayTime"));

        // Fetch initial data
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PENDING_REQUESTS", null));
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CONNECTED_USERS", null));
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null)); // Populate dropdown

        setupContextMenu();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem kickItem = new MenuItem("Kick User");
        kickItem.setOnAction(event -> {
            String selectedUser = connectedUsersList.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                showStatus("Disconnecting " + selectedUser + "...", "#0984e3");
                ChatClient.getInstance().handleMessageFromClientUI(new Message("KICK_USER", selectedUser));
            }
        });
        contextMenu.getItems().add(kickItem);
        connectedUsersList.setContextMenu(contextMenu);
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleViewReports(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/gui/DeptManagerReports.fxml"));
            javafx.scene.Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Department Manager Reports");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            showStatus("Please select a request first.", "#d63031");
            return;
        }
        showStatus("Processing...", "#0984e3");
        String[] data = {String.valueOf(selected.getRequestId()), decision};
        ChatClient.getInstance().handleMessageFromClientUI(new Message("PROCESS_REQUEST_DECISION", data));
    }

    @FXML
    void handleRefreshUsers(ActionEvent event) {
        showStatus("Refreshing connected users...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CONNECTED_USERS", null));
    }

    // --- NEW DIRECT OVERRIDE LOGIC ---
    @FXML
    void handleForceUpdate(ActionEvent event) {
        String selectedPark = cmbOverridePark.getValue();
        if (selectedPark == null) {
            showStatus("Please select a park to override.", "#d63031");
            return;
        }

        try {
            int parkId = parkNameToId.get(selectedPark);
            int newMax = Integer.parseInt(txtOverrideMax.getText().trim());
            int newGap = Integer.parseInt(txtOverrideGap.getText().trim());
            int newStay = Integer.parseInt(txtOverrideStay.getText().trim());

            if (newMax <= 0 || newGap < 0 || newStay <= 0) {
                showStatus("Values must be positive numbers.", "#d63031");
                return;
            }

            // Create a temporary Park object to hold the new parameters
            Park updateData = new Park(parkId, selectedPark, newMax, newGap, newStay, 0);

            btnForceUpdate.setDisable(true);
            showStatus("Forcing parameter update for " + selectedPark + "...", "#0984e3");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("FORCE_UPDATE_PARK_PARAMS", updateData));

        } catch (NumberFormatException e) {
            showStatus("Please enter valid numeric values for the parameters.", "#d63031");
        }
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            switch (msg.getCommand()) {
                case "PENDING_REQUESTS_DATA":
                    ArrayList<ParameterRequest> list = (ArrayList<ParameterRequest>) msg.getData();
                    requestsTable.setItems(FXCollections.observableArrayList(list));
                    break;

                case "DECISION_SUCCESS":
                    String decision = (String) msg.getData();
                    showStatus("Request successfully " + decision + "!", "#00b894");
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_PENDING_REQUESTS", null));
                    break;

                case "DECISION_FAILED":
                    showStatus("Error: Database failed to process the decision.", "#d63031");
                    break;

                case "CONNECTED_USERS_DATA":
                    ArrayList<String> users = (ArrayList<String>) msg.getData();
                    connectedUsersList.setItems(FXCollections.observableArrayList(users));
                    if (lblUserCount != null) {
                        lblUserCount.setText(users.size() + " user(s) online");
                    }
                    if (lblStatus.getText().equals("Refreshing connected users...")) {
                        showStatus("", "");
                    }
                    break;

                case "KICK_SUCCESS":
                    showStatus((String) msg.getData(), "#00b894");
                    ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_CONNECTED_USERS", null));
                    break;

                case "KICK_FAILED":
                    showStatus((String) msg.getData(), "#d63031");
                    break;

                case "REPORT_SUBMITTED_NOTIFICATION":
                    showStatus((String) msg.getData(), "#0984e3");
                    break;

                // --- NEW DIRECT OVERRIDE RESPONSES ---
                case "ALL_PARKS_DATA":
                    ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
                    cmbOverridePark.getItems().clear();
                    parkNameToId.clear();
                    for (Park p : parks) {
                        cmbOverridePark.getItems().add(p.getName());
                        parkNameToId.put(p.getName(), p.getParkId());
                    }
                    break;

                case "FORCE_UPDATE_SUCCESS":
                    btnForceUpdate.setDisable(false);
                    showStatus((String) msg.getData(), "#00b894");
                    txtOverrideMax.clear();
                    txtOverrideGap.clear();
                    txtOverrideStay.clear();
                    break;

                case "FORCE_UPDATE_FAILED":
                    btnForceUpdate.setDisable(false);
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
                        Stage stage = (Stage) themeBtn.getScene().getWindow();
                        WindowChrome.setContent(stage, root, "GoNature - Welcome");
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