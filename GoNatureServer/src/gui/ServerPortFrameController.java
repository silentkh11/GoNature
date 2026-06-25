package gui;

import database.DBController;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * FXML controller for the server configuration and monitoring screen.
 * Handles database connection setup, OCSF server start/stop, and displays
 * a live table of connected clients with their login status and role.
 */
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
    @FXML private TableColumn<ClientInfo, String> userCol;
    @FXML private TableColumn<ClientInfo, String> roleCol;
    @FXML private TableColumn<ClientInfo, String> statusCol;

    private final ObservableList<ClientInfo> connectedClients = FXCollections.observableArrayList();
    // Fast O(1) lookup: connection → table row, so the login handler can update the row in-place
    private final Map<ConnectionToClient, ClientInfo> clientInfoMap = new ConcurrentHashMap<>();
    private EchoServer server;
    private static final int DEFAULT_PORT = 5555;

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        portField.setText(String.valueOf(DEFAULT_PORT));
        setServerStopped();

        ipCol.setCellValueFactory(new PropertyValueFactory<>("ip"));
        hostCol.setCellValueFactory(new PropertyValueFactory<>("host"));
        // Use property-accessor lambdas so JavaFX observes changes and updates cells automatically
        userCol.setCellValueFactory(d -> d.getValue().usernameProperty());
        roleCol.setCellValueFactory(d -> d.getValue().roleProperty());
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
            if (dbPass.isEmpty()) {
                consoleText.appendText("> ERROR: Database password cannot be empty.\n");
                return;
            }

            if (!DBController.connect(dbPass)) {
                consoleText.appendText("> CRITICAL ERROR: Could not connect to the MySQL database. Check your password.\n");
                return;
            }
            consoleText.appendText("> Database connected successfully.\n");

            // 2. Start the Network Server
            server = new EchoServer(port,
                // uiLogger
                (String logMsg) -> Platform.runLater(() -> consoleText.appendText(logMsg)),

                // clientConnectedHandler — TCP connect: add row to table
                (ConnectionToClient client) -> Platform.runLater(() -> {
                    ClientInfo info = new ClientInfo(client);
                    clientInfoMap.put(client, info);
                    connectedClients.add(info);
                }),

                // clientDisconnectedHandler — TCP drop: remove row from table
                (ConnectionToClient client) -> Platform.runLater(() -> {
                    ClientInfo info = clientInfoMap.remove(client);
                    if (info != null) connectedClients.remove(info);
                }),

                // clientLoginHandler — user authenticated: update row with username and role in-place
                (ConnectionToClient client) -> Platform.runLater(() -> {
                    ClientInfo info = clientInfoMap.get(client);
                    if (info != null && server != null) {
                        entities.Employee emp = server.getClientEmployee(client);
                        if (emp != null) {
                            info.setUsername(emp.getFirstName() + " " + emp.getLastName());
                            info.setRole(emp.getRole());
                        }
                    }
                })
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
            clientInfoMap.clear();
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

    // -------------------------------------------------------------------------

    public static class ClientInfo {
        private final String ip;
        private final String host;
        private final String status;
        private final SimpleStringProperty username;
        private final SimpleStringProperty role;
        private final ConnectionToClient connection;

        public ClientInfo(ConnectionToClient connection) {
            this.connection = connection;
            this.ip = connection.getInetAddress().getHostAddress();
            this.host = connection.getInetAddress().getHostName();
            this.status = "Connected";
            this.username = new SimpleStringProperty("—");
            this.role = new SimpleStringProperty("—");
        }

        public String getIp()     { return ip; }
        public String getHost()   { return host; }
        public String getStatus() { return status; }
        public String getUsername() { return username.get(); }
        public String getRole()     { return role.get(); }
        public ConnectionToClient getConnection() { return connection; }

        /** Called on the JavaFX thread when the user successfully logs in. */
        public void setUsername(String u) { username.set(u); }
        public void setRole(String r)     { role.set(r); }

        public SimpleStringProperty usernameProperty() { return username; }
        public SimpleStringProperty roleProperty()     { return role; }
    }
}
