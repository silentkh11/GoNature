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
    @FXML private Button btnCancelOrder;
    @FXML private Label lblStatus;

    private String currentSearchedId = "";

    @FXML
    public void initialize() {
        // Link columns to VisitOrder entity properties
        colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        colParkId.setCellValueFactory(new PropertyValueFactory<>("parkId"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("visitDate"));
        colTime.setCellValueFactory(new PropertyValueFactory<>("visitTime"));
        colVisitors.setCellValueFactory(new PropertyValueFactory<>("visitorCount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Enable cancel button only when an order is selected
        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            btnCancelOrder.setDisable(newSelection == null);
        });

        try {
            ChatClient.getInstance("127.0.0.1", 5555, this::handleServerResponse);
        } catch (Exception e) {
            showStatus("Error: Cannot connect to the server.", "#d63031");
            e.printStackTrace();
        }
    }

    @FXML
    void handleSearch(ActionEvent event) {
        String visitorId = txtVisitorId.getText().trim();
        if (visitorId.isEmpty()) {
            showStatus("Please enter an ID number.", "#d63031");
            return;
        }

        currentSearchedId = visitorId;
        showStatus("Fetching orders...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", visitorId));
    }

    @FXML
    void handleCancelOrder(ActionEvent event) {
        VisitOrder selected = ordersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (selected.getStatus().equals("Cancelled")) {
            showStatus("This order is already cancelled.", "#d63031");
            return;
        }

        showStatus("Canceling order...", "#0984e3");
        ChatClient.getInstance().handleMessageFromClientUI(new Message("CANCEL_ORDER", selected.getOrderId()));
    }

    @FXML
    void handleGoBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/gui/MainMenu.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("GoNature - Welcome");
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
                
            } else if (msg.getCommand().equals("CANCEL_SUCCESS")) {
                showStatus((String) msg.getData(), "#00b894");
                // Refresh the table automatically
                ChatClient.getInstance().handleMessageFromClientUI(new Message("FETCH_GUEST_ORDERS", currentSearchedId));
                
            } else if (msg.getCommand().equals("CANCEL_FAILED")) {
                showStatus((String) msg.getData(), "#d63031");
            }
        });
    }

    private void showStatus(String message, String hexColor) {
        lblStatus.setText(message);
        lblStatus.setStyle("-fx-text-fill: " + hexColor + ";");
    }
}