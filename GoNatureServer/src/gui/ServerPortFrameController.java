package gui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import ocsf.server.ConnectionToClient;
import server.DBController;
import server.EchoServer;

public class ServerPortFrameController {

    @FXML private Label statusIndicator;
    @FXML private TextField portField;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private TextArea consoleText;

    // --- Connected customers table ---
    @FXML private TableView<ClientInfo> clientsTable;
    @FXML private TableColumn<ClientInfo, String> ipCol;
    @FXML private TableColumn<ClientInfo, String> hostCol;
    @FXML private TableColumn<ClientInfo, String> statusCol;

    private ObservableList<ClientInfo> connectedClients = FXCollections.observableArrayList();
    private EchoServer server;
    private static final int DEFAULT_PORT = 5555;

    private final String startBtnStyle = "-fx-background-color: #00b894; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
    private final String stopBtnStyle = "-fx-background-color: #d63031; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;";
    private final String disabledBtnStyle = "-fx-background-color: #b2bec3; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 5;";

    @FXML
    public void initialize() {
        consoleText.appendText("> Welcome to GoNature Server Console.\n> System initialized. Ready to start.\n");
        startBtn.setStyle(startBtnStyle);
        stopBtn.setStyle(disabledBtnStyle);
        
        portField.setText(String.valueOf(DEFAULT_PORT));
        portField.setEditable(false);
        portField.setDisable(true);
        
        // Defining the columns of the customer table
        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        clientsTable.setItems(connectedClients);
    }

    @FXML
    void startServer(ActionEvent event) {
        try {
            int port = DEFAULT_PORT;
            
            // 1. Connect to DB FIRST. If this fails, it jumps straight to the catch block!
            DBController.connectToDB();
            consoleText.appendText("> Database connected successfully.\n");
            
            // 2. Initialize and start server
            server = new EchoServer(port, 
                (String logMsg) -> {
                    javafx.application.Platform.runLater(() -> consoleText.appendText(logMsg));
                },
                (ConnectionToClient client) -> {
                    javafx.application.Platform.runLater(() -> connectedClients.add(new ClientInfo(client)));
                },
                (ConnectionToClient client) -> {
                    javafx.application.Platform.runLater(() -> {
                        connectedClients.removeIf(info -> info.getConnection().equals(client));
                    });
                }
            );
            
            server.listen();
            
            // 3. Update UI visually
            statusIndicator.setText("ONLINE (Port: " + port + ")");
            statusIndicator.setStyle("-fx-text-fill: #00b894;"); 
            
            startBtn.setDisable(true);
            startBtn.setStyle(disabledBtnStyle);
            stopBtn.setDisable(false);
            stopBtn.setStyle(stopBtnStyle);
            
        } catch (Exception ex) {
            // Prints our clean message directly to the UI box!
            consoleText.appendText("> ERROR: " + ex.getMessage() + "\n");
            
            // Ensures the UI buttons stay in the "Offline" state
            statusIndicator.setText("OFFLINE");
            statusIndicator.setStyle("-fx-text-fill: #d63031;");
            startBtn.setDisable(false);
            startBtn.setStyle(startBtnStyle);
            stopBtn.setDisable(true);
            stopBtn.setStyle(disabledBtnStyle);
        }
    }

    @FXML
    void stopServer(ActionEvent event) {
        try {
            if (server != null) {
                server.close();
            }
            
            statusIndicator.setText("OFFLINE");
            statusIndicator.setStyle("-fx-text-fill: #d63031;");
            
            startBtn.setDisable(false);
            startBtn.setStyle(startBtnStyle);
            stopBtn.setDisable(true);
            stopBtn.setStyle(disabledBtnStyle);
            
            // Reset the list when the server goes down
            connectedClients.clear();
            
            consoleText.appendText("> Server stopped.\n");
        } catch (Exception ex) {
            consoleText.appendText("> ERROR: Could not stop the server properly.\n");
        }
    }

    @FXML
    void clearLogs(ActionEvent event) {
        consoleText.setText("> Logs cleared.\n");
    }

    // --- Internal department for managing information in the table ---
    public static class ClientInfo {
        private String ip;
        private String host;
        private String status;
        private ConnectionToClient connection;

        public ClientInfo(ConnectionToClient connection) {
            this.connection = connection;
            this.ip = connection.getInetAddress().getHostAddress();
            this.host = connection.getInetAddress().getHostName();
            this.status = "Connected";
        }

        public String getIp() { return ip; }
        public String getHost() { return host; }
        public String getStatus() { return status; }
        public ConnectionToClient getConnection() { return connection; }
    }
}