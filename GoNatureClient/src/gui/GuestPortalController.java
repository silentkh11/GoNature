package gui;

import client.ChatClient;
import entities.Message;
import entities.VisitOrder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.ArrayList;

public class GuestPortalController {

    @FXML private TextField txtVisitorId;
    @FXML private TableView<VisitOrder> ordersTable;
    @FXML private TableColumn<VisitOrder, Integer> colOrderId;
    @FXML private TableColumn<VisitOrder, Integer> colParkId;
    @FXML private TableColumn<VisitOrder, String> colDate;
    @FXML private TableColumn<VisitOrder, String> colTime;
    @FXML private TableColumn<VisitOrder, Integer> colVisitors;
    @FXML private TableColumn<VisitOrder, String> colStatus;
    
    @FXML private Button btnConfirmOrder;
    @FXML private Button btnCancelOrder;
    @FXML private Button themeBtn;
    @FXML private Label lblStatus;

    private String currentSearchedId = "";

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colParkId.setCellValueFactory(new PropertyValueFactory<>("parkId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("visitTime"));
        colVisitors.setCellValueFactory(new PropertyValueFactory<>("visitorCount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        ChatClient.getInstance().setResponseHandler(this::handleServerResponse);
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void handleSearch(ActionEvent event) {
        currentSearchedId = txtVisitorId.getText().trim();
        if (currentSearchedId.isEmpty()) {
            showStatus("Please enter a Visitor ID to search.", "#d63031");
            return;
        }
        showStatus("Fetching your orders...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", currentSearchedId));
    }

    @FXML
    void handleConfirmOrder(ActionEvent event) {
        VisitOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select an order to confirm.", "#d63031");
            return;
        }
        if (!selected.getStatus().equals("Pending Confirm")) {
            showStatus("Only orders marked as 'Pending Confirm' need to be verified.", "#d63031");
            return;
        }
        ChatClient.getInstance().handleMessageFromClientUI(new Message("CONFIRM_ORDER_REQUEST", selected.getOrderId()));
    }

    @FXML
    void handleCancelOrder(ActionEvent event) {
        VisitOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("Please select an order to cancel.", "#d63031");
            return;
        }
        ChatClient.getInstance().handleMessageFromClientUI(new Message("CANCEL_ORDER_REQUEST", selected.getOrderId()));
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, "GoNature - Welcome");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void handleServerResponse(Message msg) {
        Platform.runLater(() -> {
            if (msg.getCommand().equals("GUEST_ORDERS_DATA")) {
                ArrayList<VisitOrder> list = (ArrayList<VisitOrder>) msg.getData();
                if (list.isEmpty()) {
                    showStatus("No active orders found for this ID.", "#636e72");
                } else {
                    showStatus("Orders loaded successfully.", "#00b894");
                }
                ObservableList<VisitOrder> observableList = FXCollections.observableArrayList(list);
                ordersTable.setItems(observableList);
                
            } else if (msg.getCommand().equals("CANCEL_SUCCESS") || msg.getCommand().equals("CONFIRM_SUCCESS")) {
                showStatus((String) msg.getData(), "#00b894");
                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", currentSearchedId));
                
            } else if (msg.getCommand().equals("CANCEL_FAILED") || msg.getCommand().equals("CONFIRM_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-weight: bold;");
    }
}