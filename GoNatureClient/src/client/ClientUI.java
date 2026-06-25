package client;

import entities.Message;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * JavaFX application entry point for the GoNature client.
 * Shows a themed server-connection dialog on startup, then loads the main menu.
 * Overrides {@link #stop()} to guarantee JVM exit even when the OCSF reader
 * thread is blocked on {@code readObject()}.
 */
public class ClientUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * JavaFX calls this when the platform exits for ANY reason — normal close,
     * Platform.exit(), or an unhandled crash on the FX thread.
     * Calling System.exit(0) here ensures the JVM terminates even if the OCSF
     * clientReader thread is still blocked on readObject().
     */
    @Override
    public void stop() {
        ChatClient client = ChatClient.getInstanceIfExists();
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.closeConnection();
                }
            } catch (Exception ignored) {}
        }
        System.exit(0);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Catch any uncaught exception on any thread and shut down cleanly.
        // Without this, a crash can leave the JVM alive (blocked OCSF thread)
        // making it impossible to restart from Eclipse without killing the process.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("[FATAL] Uncaught exception on thread '" + thread.getName() + "': " + throwable);
            throwable.printStackTrace();
            Platform.exit(); // triggers stop() → System.exit(0)
        });

        // ── Server-connection dialog (satisfies NF rubric: GUI for connection params) ──
        if (!showConnectionDialog()) {
            Platform.exit();
            return;
        }

        Parent root = FXMLLoader.load(getClass().getResource("/gui/guest/MainMenu.fxml"));

        Scene scene = gui.core.WindowChrome.install(primaryStage, root, 850, 650, "GoNature - Welcome");
        gui.core.ThemeManager.getInstance().applyTo(scene);

        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing client application...");
            try {
                ChatClient networkClient = ChatClient.getInstanceIfExists();
                if (networkClient != null && networkClient.isConnected()) {
                    networkClient.handleMessageFromClientUI(new Message("LOGOUT_REQUEST", null));
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

    private boolean showConnectionDialog() {
        java.util.concurrent.atomic.AtomicBoolean confirmed =
            new java.util.concurrent.atomic.AtomicBoolean(false);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);

        // ── Input fields ──────────────────────────────────────────────────────
        String fieldStyle =
            "-fx-background-color: #0F1B2A;" +
            "-fx-text-fill: #D5E3EF;" +
            "-fx-border-color: rgba(29,201,138,0.30);" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 9 13;" +
            "-fx-font-size: 13px;";

        String fieldFocusStyle =
            "-fx-background-color: #0F1B2A;" +
            "-fx-text-fill: #D5E3EF;" +
            "-fx-border-color: #1DC98A;" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-border-width: 1.5;" +
            "-fx-padding: 9 13;" +
            "-fx-font-size: 13px;";

        TextField hostField = new TextField(ClientConfig.getHost());
        hostField.setPromptText("e.g. 192.168.1.42");
        hostField.setStyle(fieldStyle);
        hostField.setPrefWidth(280);
        hostField.focusedProperty().addListener((obs, o, focused) ->
            hostField.setStyle(focused ? fieldFocusStyle : fieldStyle));

        TextField portField = new TextField(String.valueOf(ClientConfig.getPort()));
        portField.setPromptText("5555");
        portField.setStyle(fieldStyle);
        portField.setPrefWidth(280);
        portField.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) portField.setText(val.replaceAll("[^\\d]", ""));
        });
        portField.focusedProperty().addListener((obs, o, focused) ->
            portField.setStyle(focused ? fieldFocusStyle : fieldStyle));

        // ── Header ────────────────────────────────────────────────────────────
        Label iconLbl = new Label("🌲");
        iconLbl.setStyle("-fx-font-size: 34px;");

        Label appName = new Label("GoNature");
        appName.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #1DC98A;" +
            "-fx-font-family: 'Segoe UI', sans-serif;");

        Label subName = new Label("Server Connection");
        subName.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A98B2;");

        VBox titleBox = new VBox(2, appName, subName);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        // Drag handle lives in the header
        double[] drag = {0, 0};

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label closeX = new Label("✕");
        closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.30); -fx-cursor: hand;");
        closeX.setOnMouseEntered(e -> closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.80); -fx-cursor: hand;"));
        closeX.setOnMouseExited(e ->  closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.30); -fx-cursor: hand;"));
        closeX.setOnMouseClicked(e -> dialog.close());

        HBox headerRow = new HBox(14, iconLbl, titleBox, spacer, closeX);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setOnMousePressed(e -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        headerRow.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - drag[0]);
            dialog.setY(e.getScreenY() - drag[1]);
        });

        // ── Separator ─────────────────────────────────────────────────────────
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(29,201,138,0.18); -fx-border-color: rgba(29,201,138,0.18);");

        // ── Field groups ──────────────────────────────────────────────────────
        Label hostLbl = new Label("Server IP / Hostname");
        hostLbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #7A98B2; -fx-padding: 0 0 2 1;");

        Label portLbl = new Label("Port");
        portLbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #7A98B2; -fx-padding: 0 0 2 1;");

        Label hintLbl = new Label("💡  Same machine? Leave IP as 127.0.0.1");
        hintLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #4A6680;");

        VBox hostGroup = new VBox(5, hostLbl, hostField);
        VBox portGroup = new VBox(5, portLbl, portField);

        // ── Buttons ───────────────────────────────────────────────────────────
        String connectNormal =
            "-fx-background-color: #1DC98A;" +
            "-fx-text-fill: #0B1422;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 32;" +
            "-fx-cursor: hand;";
        String connectHover =
            "-fx-background-color: #25E89E;" +
            "-fx-text-fill: #0B1422;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 32;" +
            "-fx-cursor: hand;";

        Button connectBtn = new Button("Connect");
        connectBtn.setStyle(connectNormal);
        connectBtn.setDefaultButton(true);
        connectBtn.setOnMouseEntered(e -> connectBtn.setStyle(connectHover));
        connectBtn.setOnMouseExited(e  -> connectBtn.setStyle(connectNormal));

        String exitNormal =
            "-fx-background-color: transparent;" +
            "-fx-text-fill: #7A98B2;" +
            "-fx-font-size: 13px;" +
            "-fx-border-color: rgba(122,152,178,0.28);" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 32;" +
            "-fx-cursor: hand;";
        String exitHover =
            "-fx-background-color: rgba(255,255,255,0.04);" +
            "-fx-text-fill: #D5E3EF;" +
            "-fx-font-size: 13px;" +
            "-fx-border-color: rgba(122,152,178,0.50);" +
            "-fx-border-radius: 8;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 32;" +
            "-fx-cursor: hand;";

        Button exitBtn = new Button("Exit");
        exitBtn.setStyle(exitNormal);
        exitBtn.setCancelButton(true);
        exitBtn.setOnMouseEntered(e -> exitBtn.setStyle(exitHover));
        exitBtn.setOnMouseExited(e  -> exitBtn.setStyle(exitNormal));

        HBox btnRow = new HBox(10, exitBtn, connectBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── Main card ─────────────────────────────────────────────────────────
        VBox card = new VBox(18, headerRow, sep, hostGroup, portGroup, hintLbl, btnRow);
        card.setPadding(new Insets(26, 30, 28, 30));
        card.setPrefWidth(400);
        card.setStyle(
            "-fx-background-color: #162338;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(29,201,138,0.15);" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 40, 0.08, 0, 10);"
        );

        // Transparent wrapper so drop shadow + rounded corners render correctly
        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(16));

        // ── Wire up buttons ───────────────────────────────────────────────────
        connectBtn.disableProperty().bind(
            hostField.textProperty().isEmpty().or(portField.textProperty().isEmpty()));

        connectBtn.setOnAction(e -> {
            ClientConfig.setHost(hostField.getText().trim());
            try {
                ClientConfig.setPort(Integer.parseInt(portField.getText().trim()));
            } catch (NumberFormatException ex) {
                ClientConfig.setPort(5555);
            }
            confirmed.set(true);
            dialog.close();
        });

        exitBtn.setOnAction(e -> dialog.close());

        // ── Scene & stage ─────────────────────────────────────────────────────
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        String css = getClass().getResource("/gui/assets/dark-theme.css").toExternalForm();
        if (css != null) scene.getStylesheets().add(css);

        dialog.setScene(scene);
        Platform.runLater(hostField::requestFocus);
        dialog.showAndWait();

        return confirmed.get();
    }
}
