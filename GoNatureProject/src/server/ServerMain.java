package server;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class ServerMain extends Application {

    private EchoServer server;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("GoNature Server Configuration");

        // --- STYLING CONSTANTS ---
        String cardStyle = "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #dcdde1; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);";
        String startBtnStyle = "-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String stopBtnStyle = "-fx-background-color: #d63031; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String disabledBtnStyle = "-fx-background-color: #b2bec3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;";

        // --- TITLE ---
        Label headerLabel = new Label("Server Control Panel");
        headerLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        headerLabel.setStyle("-fx-text-fill: #2d3436;");

        // --- TOP CARD: Config & Controls ---
        VBox controlCard = new VBox(15);
        controlCard.setPadding(new Insets(20));
        controlCard.setStyle(cardStyle);

        // Status Bar
        Label statusText = new Label("Current Status: ");
        statusText.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        Label statusIndicator = new Label("OFFLINE");
        statusIndicator.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        statusIndicator.setStyle("-fx-text-fill: #d63031;"); // Starts Red
        HBox statusBox = new HBox(5, statusText, statusIndicator);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // Port Input & Buttons
        Label portLabel = new Label("Listen Port:");
        portLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3436;");
        
        TextField portField = new TextField("5555");
        portField.setPrefWidth(80);

        Button startBtn = new Button("Start Server");
        startBtn.setStyle(startBtnStyle);
        
        Button stopBtn = new Button("Stop Server");
        stopBtn.setStyle(disabledBtnStyle);
        stopBtn.setDisable(true);

        HBox buttonBox = new HBox(15, portLabel, portField, startBtn, stopBtn);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        controlCard.getChildren().addAll(statusBox, buttonBox);

        // --- BOTTOM CARD: System Logs ---
        VBox logCard = new VBox(10);
        logCard.setPadding(new Insets(15));
        logCard.setStyle(cardStyle);
        VBox.setVgrow(logCard, Priority.ALWAYS);

        // Log Header & Clear Button
        Label logTitle = new Label("System Logs");
        logTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        
        Region spacer = new Region(); // Pushes the clear button to the right
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button clearBtn = new Button("Clear Logs");
        clearBtn.setStyle("-fx-background-color: #dfe6e9; -fx-text-fill: #2d3436; -fx-font-size: 11px; -fx-cursor: hand;");

        HBox logHeaderBox = new HBox(logTitle, spacer, clearBtn);
        logHeaderBox.setAlignment(Pos.CENTER_LEFT);

        // Terminal Console
        TextArea consoleText = new TextArea();
        consoleText.setEditable(false);
        consoleText.setWrapText(true);
        VBox.setVgrow(consoleText, Priority.ALWAYS);
        // Removes the default blue focus ring and gray border, making it look flush
        consoleText.setStyle("-fx-control-inner-background: #2d3436; -fx-background-color: transparent; -fx-text-fill: #55efc4; -fx-font-family: 'Consolas'; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 5;");
        consoleText.appendText("> Welcome to GoNature Server Console.\n> System initialized. Ready to start.\n");

        logCard.getChildren().addAll(logHeaderBox, consoleText);

        // --- EVENT LISTENERS ---

        clearBtn.setOnAction(e -> consoleText.setText("> Logs cleared.\n"));

        startBtn.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                server = new EchoServer(port);
                
                // Connect to DB
                DBController.connectToDB();
                consoleText.appendText("> Database connected successfully.\n");
                
                // Start Server
                server.listen();
                
                // Update UI visually
                statusIndicator.setText("ONLINE (Port: " + port + ")");
                statusIndicator.setStyle("-fx-text-fill: #00b894;"); // Green
                
                startBtn.setDisable(true);
                startBtn.setStyle(disabledBtnStyle);
                portField.setDisable(true); // Prevent changing port while running
                
                stopBtn.setDisable(false);
                stopBtn.setStyle(stopBtnStyle);
                
                consoleText.appendText("> Server started. Waiting for clients...\n");
            } catch (NumberFormatException ex) {
                consoleText.appendText("> ERROR: Invalid port number!\n");
            } catch (Exception ex) {
                consoleText.appendText("> ERROR: Could not listen for clients! Port may be in use.\n");
            }
        });

        stopBtn.setOnAction(e -> {
            try {
                server.close();
                
                // Update UI visually
                statusIndicator.setText("OFFLINE");
                statusIndicator.setStyle("-fx-text-fill: #d63031;"); // Red
                
                startBtn.setDisable(false);
                startBtn.setStyle(startBtnStyle);
                portField.setDisable(false); // Allow changing port again
                
                stopBtn.setDisable(true);
                stopBtn.setStyle(disabledBtnStyle);
                
                consoleText.appendText("> Server stopped.\n");
            } catch (Exception ex) {
                consoleText.appendText("> ERROR: Could not stop the server properly.\n");
            }
        });

        // --- MAIN LAYOUT ---
        VBox rootLayout = new VBox(20);
        rootLayout.setPadding(new Insets(25));
        rootLayout.setStyle("-fx-background-color: #f5f6fa;"); // Matches the client app background
        rootLayout.getChildren().addAll(headerLabel, controlCard, logCard);

        Scene scene = new Scene(rootLayout, 600, 500); 
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}