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

        consoleText.appendText("> Welcome to GoNature Server Console.\n> System initialized. Ready to start.\n");

        setServerStopped();

        portField.setText(String.valueOf(DEFAULT_PORT));
        portField.setEditable(false);
        portField.setDisable(true);

        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        clientsTable.setItems(connectedClients);
    }

    @FXML
    void handleToggleTheme(ActionEvent event) {
        ThemeManager.getInstance().toggle(((Node) event.getSource()).getScene());
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML
    void startServer(ActionEvent event) {
        // Grab the password typed by the user on the screen
        String dbPass = dbPassField != null ? dbPassField.getText().trim() : "";
        
        if (dbPass.isEmpty()) {
            consoleText.appendText("> ERROR: Please enter the MySQL Database password.\n");
            return;
        }

        try {
            int port = DEFAULT_PORT;

            // 1. Try to connect to the database first using the password!
            boolean dbConnected = DBController.connect(dbPass);
            if (!dbConnected) {
                consoleText.appendText("> ERROR: Database connection failed. Incorrect password or MySQL is offline.\n");
                return; // Stop the server from booting if DB fails
            }
            
            consoleText.appendText("> Database connected successfully.\n");

            // 2. Start the Network Server
            server = new EchoServer(port,
                (String logMsg) -> Platform.runLater(() -> consoleText.appendText(logMsg)),
                (ConnectionToClient client) -> Platform.runLater(() -> connectedClients.add(new ClientInfo(client))),
                (ConnectionToClient client) -> Platform.runLater(() ->
                    connectedClients.removeIf(info -> info.getConnection().equals(client)))
            );

            server.listen();

            statusIndicator.setText("ONLINE  Â·  Port " + port);
            statusIndicator.setStyle("-fx-text-fill: #66bb6a; -fx-font-weight: bold;");

            startBtn.setDisable(true);
            stopBtn.setDisable(false);
            if (dbPassField != null) dbPassField.setDisable(true); // Lock the password box while running

        } catch (Exception ex) {
            consoleText.appendText("> ERROR: " + ex.getMessage() + "\n");
            setServerStopped();
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