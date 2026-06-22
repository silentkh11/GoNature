package gui;

import database.DBController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import ocsf.server.ConnectionToClient;
import server.EchoServer;

public class ServerPortFrameController {

    @FXML private Label statusIndicator;
    @FXML private TextField portField;
    @FXML private Button startBtn;
    @FXML private Button stopBtn;
    @FXML private Button themeBtn;
    @FXML private TextArea consoleText;
    @FXML private javafx.scene.control.PasswordField dbPassField;

    @FXML private TableView<ClientInfo> clientsTable;
    @FXML private TableColumn<ClientInfo, String> ipCol;
    @FXML private TableColumn<ClientInfo, String> hostCol;
    @FXML private TableColumn<ClientInfo, String> statusCol;

    private ObservableList<ClientInfo> connectedClients = FXCollections.observableArrayList();
    private EchoServer server;
    private static final int DEFAULT_PORT = 5555;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        portField.setText(String.valueOf(DEFAULT_PORT));
        setServerStopped();

        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        clientsTable.setItems(connectedClients);
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        javafx.scene.Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void startServer(ActionEvent event) {
        if (server != null && server.isListening()) {
            consoleText.appendText("> Server is already running.\n");
            return;
        }

        try {
            int port = Integer.parseInt(portField.getText().trim());
            
            // 1. Establish Database Connection FIRST
            String dbPass = dbPassField.getText();
            if(dbPass.isEmpty()) {
                consoleText.appendText("> ERROR: Database password cannot be empty.\n");
                return;
            }
            
            if(!DBController.connect(dbPass)) {
                consoleText.appendText("> CRITICAL ERROR: Could not connect to the MySQL database. Check your password.\n");
                return;
            }
            consoleText.appendText("> Database connected successfully.\n");

            // 2. Start the Network Server 
            server = new EchoServer(port, 
                (String logMsg) -> Platform.runLater(() -> consoleText.appendText(logMsg)),
                (ConnectionToClient client) -> Platform.runLater(() -> connectedClients.add(new ClientInfo(client))),
                (ConnectionToClient client) -> Platform.runLater(() -> connectedClients.removeIf(info -> info.getConnection().equals(client)))
            );
            
            server.listen(); 
            
            statusIndicator.setText("ONLINE");
            statusIndicator.setStyle("-fx-text-fill: #00b894; -fx-font-weight: bold;");
            startBtn.setDisable(true);
            stopBtn.setDisable(false);
            consoleText.appendText("> Server started on port " + port + ".\n");

        } catch (NumberFormatException e) {
            consoleText.appendText("> ERROR: Invalid port number.\n");
        } catch (Exception ex) {
            consoleText.appendText("> ERROR: Could not start server. Port might be in use.\n");
            ex.printStackTrace();
        }
    }

    @FXML
    void stopServer(ActionEvent event) {
        try {
            if (server != null) server.close();
            connectedClients.clear();
            consoleText.appendText("> Server stopped.\n");
        } catch (Exception ex) {
            consoleText.appendText("> ERROR: Could not stop the server properly.\n");
        }
        setServerStopped();
    }

    @FXML
    void clearLogs(ActionEvent event) {
        consoleText.setText("> Logs cleared.\n");
    }

    private void setServerStopped() {
        statusIndicator.setText("OFFLINE");
        statusIndicator.setStyle("-fx-text-fill: #ef5350; -fx-font-weight: bold;");
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
    }

    public static class ClientInfo {
        private final String ip;
        private final String host;
        private final String status;
        private final ConnectionToClient connection;

        public ClientInfo(ConnectionToClient connection) {
            this.connection = connection;
            this.ip = connection.getInetAddress().getHostAddress();
            this.host = connection.getInetAddress().getHostName();
            this.status = "Connected";
        }

        public String getIp()     { return ip; }
        public String getHost()   { return host; }
        public String getStatus() { return status; }
        public ConnectionToClient getConnection() { return connection; }
    }
}