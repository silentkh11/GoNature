package client;

import firstPackage.Message;
import firstPackage.Order;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;

public class ClientMain extends Application {

    private TableView<Order> table;
    private ObservableList<Order> orderData;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GoNature Client Management");

        // --- STYLING CONSTANTS ---
        String cardStyle = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dcdde1; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);";
        String primaryBtnStyle = "-fx-background-color: #0984e3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String updateBtnStyle = "-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String labelStyle = "-fx-font-weight: bold; -fx-text-fill: #2d3436;";
        
        // Status Message Colors
        String successColor = "-fx-text-fill: #2ecc71; -fx-font-style: italic; -fx-font-weight: bold;"; // Green
        String errorColor = "-fx-text-fill: #e74c3c; -fx-font-style: italic; -fx-font-weight: bold;";   // Red
        String infoColor = "-fx-text-fill: #0984e3; -fx-font-style: italic;";                           // Blue

        // --- TITLE ---
        Label headerLabel = new Label("Order Management Dashboard");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        headerLabel.setStyle("-fx-text-fill: #2c3e50;");

        // --- TOP: Connection Setup ---
        VBox connectionCard = new VBox(10);
        connectionCard.setPadding(new Insets(15));
        connectionCard.setStyle(cardStyle);

        Label connTitle = new Label("Server Connection");
        connTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        TextField ipField = new TextField("localhost");
        ipField.setPromptText("Server IP");
        ipField.setPrefWidth(150);

        TextField portField = new TextField("5555");
        portField.setPromptText("Port");
        portField.setPrefWidth(100);

        Button connectBtn = new Button("Connect & Load");
        connectBtn.setStyle(primaryBtnStyle);
        
        Label connIpLabel = new Label("IP Address:");
        connIpLabel.setStyle(labelStyle);
        Label connPortLabel = new Label("Port:");
        connPortLabel.setStyle(labelStyle);

        HBox connectionBox = new HBox(15, connIpLabel, ipField, connPortLabel, portField, connectBtn);
        connectionBox.setAlignment(Pos.CENTER_LEFT);
        connectionCard.getChildren().addAll(connTitle, connectionBox);

        // --- MIDDLE: The Table ---
        VBox tableCard = new VBox(10);
        tableCard.setPadding(new Insets(15));
        tableCard.setStyle(cardStyle);
        VBox.setVgrow(tableCard, Priority.ALWAYS); 

        Label tableTitle = new Label("Active Orders");
        tableTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        table = new TableView<>();
        orderData = FXCollections.observableArrayList();
        table.setItems(orderData);
        
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size: 14px; -fx-base: #f1f2f6;");

        TableColumn<Order, Integer> numCol = new TableColumn<>("Order Number");
        numCol.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        numCol.setStyle("-fx-alignment: CENTER;"); // Centers the text

        TableColumn<Order, Date> dateCol = new TableColumn<>("Order Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        dateCol.setStyle("-fx-alignment: CENTER;"); // Centers the text

        TableColumn<Order, Integer> visitorsCol = new TableColumn<>("Visitors");
        visitorsCol.setCellValueFactory(new PropertyValueFactory<>("numberOfVisitors"));
        visitorsCol.setStyle("-fx-alignment: CENTER;"); // Centers the text

        table.getColumns().addAll(numCol, dateCol, visitorsCol);
        VBox.setVgrow(table, Priority.ALWAYS); 

        tableCard.getChildren().addAll(tableTitle, table);

        // --- BOTTOM: Update Area ---
        VBox updateCard = new VBox(10);
        updateCard.setPadding(new Insets(15));
        updateCard.setStyle(cardStyle);

        Label updateTitle = new Label("Update Selected Order");
        updateTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        // NEW: DatePicker instead of TextField
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Select Date");
        datePicker.setPrefWidth(150);

        // NEW: Number Spinner instead of TextField (Min: 1, Max: 100, Initial: 1)
        Spinner<Integer> visitorSpinner = new Spinner<>(1, 100, 1);
        visitorSpinner.setPrefWidth(120);
        visitorSpinner.setEditable(true);

        Button updateBtn = new Button("Confirm Update");
        updateBtn.setStyle(updateBtnStyle);
        updateBtn.setDisable(true); 

        Label upDateLabel = new Label("New Date:");
        upDateLabel.setStyle(labelStyle);
        Label upVisLabel = new Label("New Visitors:");
        upVisLabel.setStyle(labelStyle);

        HBox updateBox = new HBox(15, upDateLabel, datePicker, upVisLabel, visitorSpinner, updateBtn);
        updateBox.setAlignment(Pos.CENTER_LEFT);
        
        Label statusMsg = new Label("Ready to connect."); 
        statusMsg.setStyle(infoColor);
        
        updateCard.getChildren().addAll(updateTitle, updateBox, statusMsg);

        // --- EVENT LISTENERS ---

        // 1. Connect to Server
        connectBtn.setOnAction(e -> {
            try {
                String ip = ipField.getText();
                int port = Integer.parseInt(portField.getText());

                ChatClient.getInstance(ip, port, (Message msg) -> {
                    Platform.runLater(() -> handleServerResponse(msg, statusMsg));
                });

                ChatClient.getInstance().handleMessageFromClientUI(new Message("GET_ORDERS", null));
                
                connectBtn.setDisable(true);
                connectBtn.setStyle("-fx-background-color: #b2bec3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;");
                updateBtn.setDisable(false);
                
                statusMsg.setStyle(successColor); // Set to Green
                statusMsg.setText("Connected successfully.");

            } catch (Exception ex) {
                statusMsg.setStyle(errorColor); // Set to Red
                statusMsg.setText("Error connecting: " + ex.getMessage());
            }
        });

        // 2. Select an item
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                // Convert java.sql.Date to java.time.LocalDate for the DatePicker
                if (newSelection.getOrderDate() != null) {
                    datePicker.setValue(newSelection.getOrderDate().toLocalDate());
                }
                
                // Set the Spinner value
                visitorSpinner.getValueFactory().setValue(newSelection.getNumberOfVisitors());
                
                statusMsg.setStyle(infoColor); // Set to Blue
                statusMsg.setText("Order #" + newSelection.getOrderNumber() + " selected for editing.");
            }
        });

        // 3. Send Update Request
        updateBtn.setOnAction(e -> {
            Order selectedOrder = table.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                try {
                    // Extract values securely from the Picker and Spinner
                    LocalDate selectedDate = datePicker.getValue();
                    if (selectedDate == null) throw new IllegalArgumentException("Date cannot be empty.");
                    
                    selectedOrder.setOrderDate(Date.valueOf(selectedDate));
                    selectedOrder.setNumberOfVisitors(visitorSpinner.getValue());

                    Message msg = new Message("UPDATE_ORDER", selectedOrder);
                    ChatClient.getInstance().handleMessageFromClientUI(msg);
                } catch (Exception ex) {
                    statusMsg.setStyle(errorColor); // Set to Red
                    statusMsg.setText("Invalid input! Please ensure all fields are filled correctly.");
                }
            } else {
                statusMsg.setStyle(errorColor); // Set to Red
                statusMsg.setText("Please select an order from the table first.");
            }
        });

        // --- MAIN LAYOUT ---
        VBox rootLayout = new VBox(20);
        rootLayout.setPadding(new Insets(25));
        rootLayout.setStyle("-fx-background-color: #f5f6fa;"); 
        rootLayout.getChildren().addAll(headerLabel, connectionCard, tableCard, updateCard);

        Scene scene = new Scene(rootLayout, 750, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- Process Responses from Server ---
    private void handleServerResponse(Message msg, Label statusMsg) {
        if (msg.getCommand().equals("ORDERS_DATA")) {
            ArrayList<Order> orders = (ArrayList<Order>) msg.getData();
            orderData.clear();
            orderData.addAll(orders);
            
            statusMsg.setStyle("-fx-text-fill: #0984e3; -fx-font-style: italic;"); // Blue
            statusMsg.setText("Data refreshed from server.");
        } 
        else if (msg.getCommand().equals("UPDATE_SUCCESS")) {
            statusMsg.setStyle("-fx-text-fill: #2ecc71; -fx-font-style: italic; -fx-font-weight: bold;"); // Green
            statusMsg.setText("Update successful! Refreshing table...");
            ChatClient.getInstance().handleMessageFromClientUI(new Message("GET_ORDERS", null));
        }
    }
}