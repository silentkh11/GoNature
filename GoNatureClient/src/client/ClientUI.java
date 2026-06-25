package client;

import entities.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class ClientUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // ── Server-connection dialog (satisfies NF rubric: GUI for connection params) ──
        if (!showConnectionDialog()) {
            Platform.exit();
            return;
        }

        // Loads from the new 'guest' package location
        Parent root = FXMLLoader.load(getClass().getResource("/gui/guest/MainMenu.fxml"));

        // Uses the new 'core' package location
        Scene scene = gui.core.WindowChrome.install(primaryStage, root, 850, 650, "GoNature - Welcome");
        gui.core.ThemeManager.getInstance().applyTo(scene);

        primaryStage.setScene(scene);
        primaryStage.setResizable(true); 
        
        // --- CLEAN SHUTDOWN ON WINDOW CLOSE ---
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing client application...");
            try {
                // Use null-safe getter — user may close before ever making a network request
                ChatClient networkClient = ChatClient.getInstanceIfExists();
                if (networkClient != null && networkClient.isConnected()) {
                    // Send logout so the server cleans up session maps
                    networkClient.handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
                    // Give the packet 150 ms to reach the server before we kill the JVM.
                    // The server will also close the TCP socket from its side upon receiving
                    // LOGOUT_REQUEST, but we close from our side too as a safety net so the
                    // server table clears immediately even in edge cases.
                    Thread.sleep(150);
                    try { networkClient.closeConnection(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                System.err.println("Cleanup on exit failed: " + e.getMessage());
            }
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
    }

    /**
     * Shows a GUI dialog that lets the operator set the server host and port
     * before the client attempts any network connection.
     * Pre-filled with values from client.properties (or defaults).
     *
     * @return true if the user clicked Connect, false if they cancelled/closed.
     */
    private boolean showConnectionDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("GoNature — Server Connection");
        dialog.setHeaderText("Enter the GoNature server address");

        ButtonType connectBtn = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn  = new ButtonType("Exit",    ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(connectBtn, cancelBtn);

        TextField hostField = new TextField(ClientConfig.getHost());
        TextField portField = new TextField(String.valueOf(ClientConfig.getPort()));
        hostField.setPromptText("e.g. 192.168.1.42");
        portField.setPromptText("5555");
        portField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) portField.setText(val.replaceAll("[^\\d]", ""));
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.add(new Label("Server IP / Hostname:"), 0, 0);
        grid.add(hostField, 1, 0);
        grid.add(new Label("Port:"),                 0, 1);
        grid.add(portField, 1, 1);
        grid.add(new Label("(Same machine: leave as 127.0.0.1)"), 0, 2, 2, 1);

        // Disable Connect button until both fields are non-empty
        Button connectButton = (Button) dialog.getDialogPane().lookupButton(connectBtn);
        connectButton.setDisable(false);
        hostField.textProperty().addListener((obs, old, val) ->
            connectButton.setDisable(val.trim().isEmpty() || portField.getText().trim().isEmpty()));
        portField.textProperty().addListener((obs, old, val) ->
            connectButton.setDisable(val.trim().isEmpty() || hostField.getText().trim().isEmpty()));

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(hostField::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == connectBtn) {
                ClientConfig.setHost(hostField.getText().trim());
                try {
                    ClientConfig.setPort(Integer.parseInt(portField.getText().trim()));
                } catch (NumberFormatException ignored) {
                    ClientConfig.setPort(5555);
                }
                return true;
            }
            return false;
        });

        return dialog.showAndWait().orElse(false);
    }
}