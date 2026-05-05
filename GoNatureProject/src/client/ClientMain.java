package client;

import firstPackage.Message;
import firstPackage.Order;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Date;
import java.util.ArrayList;

public class ClientMain extends Application {

    private TableView<Order> table;
    private ObservableList<Order> orderData;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GoNature Client Prototype");

        // --- TOP: Connection Setup ---
        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server IP");
        TextField portField = new TextField("5555");
        portField.setPromptText("Port");
        Button connectBtn = new Button("Connect & Load Data");
        
        HBox connectionBox = new HBox(10, new Label("IP:"), ipField, new Label("Port:"), portField, connectBtn);

        // --- MIDDLE: The Table ---
        table = new TableView<>();
        orderData = FXCollections.observableArrayList();
        table.setItems(orderData);

        // Define Table Columns (Must match the exact variable names in your Order class!)
        TableColumn<Order, Integer> numCol = new TableColumn<>("Order Number");
        numCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));

        TableColumn<Order, Date> dateCol = new TableColumn<>("Order Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));

        TableColumn<Order, Integer> visitorsCol = new TableColumn<>("Visitors");
        visitorsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfVisitors"));

        table.getColumns().addAll(numCol, dateCol, visitorsCol);
        table.setPrefHeight(200);

        // --- BOTTOM: Update Area ---
        TextField newDateInput = new TextField();
        newDateInput.setPromptText("YYYY-MM-DD");
        TextField newVisitorsInput = new TextField();
        newVisitorsInput.setPromptText("New Visitors Count");
        Button updateBtn = new Button("Update Selected Order");
        updateBtn.setDisable(true); // Disabled until connected

        HBox updateBox = new HBox(10, new Label("New Date:"), newDateInput, new Label("New Visitors:"), newVisitorsInput, updateBtn);

        // --- EVENT LISTENERS ---

        // 1. Connect to Server
        connectBtn.setOnAction(e -> {
            try {
                String ip = ipField.getText();
                int port = Integer.parseInt(portField.getText());

                // Initialize Singleton Client and pass it what to do when it gets a message
                ChatClient.getInstance(ip, port, (Message msg) -> {
                    // Platform.runLater is REQUIRED when updating JavaFX UI from a network thread!
                    Platform.runLater(() -> handleServerResponse(msg));
                });

                // Request the data immediately after connecting
                ChatClient.getInstance().handleMessageFromClientUI(new Message("GET_ORDERS", null));
                
                connectBtn.setDisable(true);
                updateBtn.setDisable(false);

            } catch (Exception ex) {
                System.out.println("Error connecting: " + ex.getMessage());
            }
        });

        // 2. Select an item in the table to populate update fields
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                newDateInput.setText(newSelection.getOrderDate().toString());
                newVisitorsInput.setText(String.valueOf(newSelection.getNumberOfVisitors()));
            }
        });

        // 3. Send Update Request
        updateBtn.setOnAction(e -> {
            Order selectedOrder = table.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                try {
                    // Update the local object
                    selectedOrder.setOrderDate(Date.valueOf(newDateInput.getText()));
                    selectedOrder.setNumberOfVisitors(Integer.parseInt(newVisitorsInput.getText()));

                    // Send the updated object to the server
                    Message msg = new Message("UPDATE_ORDER", selectedOrder);
                    ChatClient.getInstance().handleMessageFromClientUI(msg);
                } catch (IllegalArgumentException ex) {
                    System.out.println("Invalid date or number format!");
                }
            }
        });

        // --- LAYOUT ---
        VBox layout = new VBox(15, connectionBox, table, new Label("Update Selected Row:"), updateBox);
        layout.setPadding(new Insets(20));

        primaryStage.setScene(new Scene(layout, 600, 400));
        primaryStage.show();
    }

    // --- Process Responses from Server ---
    private void handleServerResponse(Message msg) {
        if (msg.getCommand().equals("ORDERS_DATA")) {
            // Server sent us the list of orders, put them in the table
            ArrayList<Order> orders = (ArrayList<Order>) msg.getData();
            orderData.clear();
            orderData.addAll(orders);
            System.out.println("Table populated with data from server!");
        } 
        else if (msg.getCommand().equals("UPDATE_SUCCESS")) {
            System.out.println("Update successful! Refreshing table...");
            // Ask the server for the fresh data so the table updates
            ChatClient.getInstance().handleMessageFromClientUI(new Message("GET_ORDERS", null));
        }
    }
}