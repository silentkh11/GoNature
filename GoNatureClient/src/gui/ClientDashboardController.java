package gui;

import client.ChatClient;
import firstPackage.Message;
import firstPackage.Order;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;

public class ClientDashboardController {

    // --- FXML UI Elements ---
    @FXML private TextField ipField;
    @FXML private TextField portField;
    @FXML private Button connectBtn; // Brought the button back!
    
    @FXML private TableView<Order> table;
    @FXML private TableColumn<Order, Integer> numCol;
    @FXML private TableColumn<Order, Date> dateCol;
    @FXML private TableColumn<Order, Integer> visitorsCol;
    
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> visitorSpinner;
    @FXML private Button updateBtn;
    @FXML private Label statusMsg;

    private ObservableList<Order> orderData = FXCollections.observableArrayList();

    // --- STYLING CONSTANTS ---
    private final String successColor = "-fx-text-fill: #2ecc71; -fx-font-style: italic; -fx-font-weight: bold;";
    private final String errorColor = "-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-weight: bold;";
    private final String infoColor = "-fx-text-fill: #0984e3; -fx-font-style: italic;";

    @FXML
    public void initialize() {
        // 1. Setup Table Columns
        numCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        visitorsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfVisitors"));
        table.setItems(orderData);

        // 2. Setup Spinner Configuration (Min 1, Max 100, Default 1)
        visitorSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));

        // 3. Setup Table Selection Listener
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                if (newSelection.getOrderDate() != null) {
                    datePicker.setValue(newSelection.getOrderDate().toLocalDate());
                }
                visitorSpinner.getValueFactory().setValue(newSelection.getNumberOfVisitors());
                
                statusMsg.setStyle(infoColor);
                statusMsg.setText("Order #" + newSelection.getOrderNumber() + " selected for editing.");
            }
        });

        // Make fields read-only so the user knows they are locked
        ipField.setEditable(false);
        ipField.setDisable(true);
        portField.setEditable(false);
        portField.setDisable(true);

        // 4. Try connecting automatically right when the app opens
        attemptConnection();
    }

    // This runs when the user clicks the "Retry / Refresh" button
    @FXML
    void connectToServer(ActionEvent event) {
        attemptConnection();
    }

    // Centralized connection logic
    private void attemptConnection() {
        try {
            String ip = ipField.getText();
            int port = Integer.parseInt(portField.getText());

            ChatClient client = ChatClient.getInstance(ip, port, (Message msg) -> {
                Platform.runLater(() -> handleServerResponse(msg));
            });

            // If the client lost connection, force it to reopen
            if (!client.isConnected()) {
                client.openConnection();
            }

            // Request the data
            client.handleMessageFromClientUI(new Message("GET_ORDERS", null));
            
            updateBtn.setDisable(false); 
            statusMsg.setStyle(successColor);
            statusMsg.setText("Connected successfully. Loading data...");

        } catch (Exception ex) {
            statusMsg.setStyle(errorColor);
            statusMsg.setText("Server offline. Start server and click Retry.");
        }
    }

    @FXML
    void updateSelectedOrder(ActionEvent event) {
        Order selectedOrder = table.getSelectionModel().getSelectedItem();
        if (selectedOrder != null) {
            try {
                LocalDate selectedDate = datePicker.getValue();
                if (selectedDate == null) throw new IllegalArgumentException("Date cannot be empty.");
                
                selectedOrder.setOrderDate(Date.valueOf(selectedDate));
                selectedOrder.setNumberOfVisitors(visitorSpinner.getValue());

                Message msg = new Message("UPDATE_ORDER", selectedOrder);
                ChatClient.getInstance().handleMessageFromClientUI(msg);
            } catch (Exception ex) {
                statusMsg.setStyle(errorColor);
                statusMsg.setText("Invalid input! Please ensure all fields are filled correctly.");
            }
        } else {
            statusMsg.setStyle(errorColor);
            statusMsg.setText("Please select an order from the table first.");
        }
    }

    private void handleServerResponse(Message msg) {
        if (msg.getCommand().equals("ORDERS_DATA")) {
            @SuppressWarnings("unchecked")
            ArrayList<Order> orders = (ArrayList<Order>) msg.getData();
            orderData.clear();
            orderData.addAll(orders);
            
            statusMsg.setStyle(infoColor);
            statusMsg.setText("Data loaded successfully.");
        } 
        else if (msg.getCommand().equals("UPDATE_SUCCESS")) {
            statusMsg.setStyle(successColor);
            statusMsg.setText("Update successful! Refreshing table...");
            attemptConnection(); // Refresh data after update
        }
    }
}