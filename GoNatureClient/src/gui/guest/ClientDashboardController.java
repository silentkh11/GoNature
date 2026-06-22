package gui.guest;

import client.ChatClient;
import entities.Message;
import entities.Order;
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
    // Button completely removed!
    
    @FXML private TableView<Order> table;
    @FXML private TableColumn<Order, Integer> numCol;
    @FXML private TableColumn<Order, Date> dateCol;
    @FXML private TableColumn<Order, Integer> visitorsCol;
    
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> visitorSpinner;
    @FXML private Button updateBtn;
    @FXML private Label statusMsg;

    private ObservableList<Order> orderData = FXCollections.observableArrayList();
    private boolean wasConnected = false;

    // --- STYLING CONSTANTS ---
    private final String successColor = "-fx-text-fill: #2ecc71; -fx-font-style: italic; -fx-font-weight: bold;";
    private final String errorColor = "-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-weight: bold;";
    private final String infoColor = "-fx-text-fill: #0984e3; -fx-font-style: italic;";

    @FXML
    public void initialize() {
        // 1. Setup Table
        numCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        visitorsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfVisitors"));
        table.setItems(orderData);

        // 2. Setup Spinner Configuration
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

        // 4. Lock fields and disable update button until connected
        ipField.setEditable(false);
        ipField.setDisable(true);
        portField.setEditable(false);
        portField.setDisable(true);
        updateBtn.setDisable(true);

        // 5. Grab the IP and Port safely on the UI thread
        String ip = ipField.getText();
        int port = Integer.parseInt(portField.getText());

        // 6. Start the background monitor (Pass the IP and Port to it)
        startConnectionMonitor(ip, port);
    }

    // --- THE WATCHDOG THREAD (THREAD-SAFE) ---
    private void startConnectionMonitor(String ip, int port) {
        Thread monitor = new Thread(() -> {
            
            // Give the UI 0.5 seconds to pop up and draw itself beautifully on screen first!
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            while (true) {
                if (!wasConnected) {
                    try {
                        // 1. Get or create the client
                        ChatClient client = ChatClient.getInstance(ip, port, (Message msg) -> {
                            Platform.runLater(() -> handleServerResponse(msg));
                        });

                        // 2. Try to connect! (If server is offline, this blocks for a second, but the UI is safe!)
                        if (!client.isConnected()) {
                            client.openConnection(); 
                        }

                        // 3. If we reach here, connection is SUCCESSFUL! Tell the UI.
                        Platform.runLater(() -> {
                            wasConnected = true;
                            updateBtn.setDisable(false);
                            statusMsg.setStyle(successColor);
                            statusMsg.setText("Connected to server! Loading data...");
                            client.handleMessageFromClientUI(new Message("GET_ORDERS", null));
                        });

                    } catch (Exception e) {
                        // 4. Connection FAILED. Tell the UI we are still waiting.
                        Platform.runLater(() -> {
                            wasConnected = false;
                            updateBtn.setDisable(true);
                            statusMsg.setStyle(errorColor);
                            statusMsg.setText("Server offline. Waiting to auto-connect...");
                        });
                    }
                }
                
                // Wait 2 seconds before trying to connect again
                try { Thread.sleep(2000); } catch (InterruptedException e) { break; }
            }
        });
        monitor.setDaemon(true); // Kills the thread when you click the X button
        monitor.start();
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

 // --- HANDLE MESSAGES FROM SERVER OR OCSF ---
    private void handleServerResponse(Message msg) {
        if (msg.getCommand().equals("ORDERS_DATA")) {
            @SuppressWarnings("unchecked")
            ArrayList<Order> orders = (ArrayList<Order>) msg.getData();
            orderData.clear();
            orderData.addAll(orders);
            
            statusMsg.setStyle(infoColor);
            statusMsg.setText("Data synchronized with server.");
        } 
        else if (msg.getCommand().equals("UPDATE_SUCCESS")) {
            statusMsg.setStyle(successColor);
            statusMsg.setText("Update successful! Refreshing table...");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("GET_ORDERS", null));
        }
        // Immediate detection from the OCSF that the connection has been dropped!
        else if (msg.getCommand().equals("SERVER_DISCONNECTED")) {
            wasConnected = false;
            updateBtn.setDisable(true);
            orderData.clear(); // Clears the table immediately

            statusMsg.setStyle(errorColor);
            statusMsg.setText("Connection lost! Table cleared. Waiting for server...");
        }
    }
}