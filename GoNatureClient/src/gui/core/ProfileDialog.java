package gui.core;

import entities.Employee;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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
 * Shared utility for displaying an employee's profile in a themed floating popup.
 * All four role dashboards (Park Manager, Dept Manager, Gate Worker, Service Rep) call
 * {@link #show(entities.Employee)} instead of each maintaining their own dialog.
 */
public final class ProfileDialog {

    private ProfileDialog() {}

    public static void show(Employee user) {
        if (user == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setResizable(false);

        // ── Drag support ──────────────────────────────────────────────────────
        double[] drag = {0, 0};

        // ── Close button ─────────────────────────────────────────────────────
        Label closeX = new Label("✕");
        closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.30); -fx-cursor: hand;");
        closeX.setOnMouseEntered(e -> closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.80); -fx-cursor: hand;"));
        closeX.setOnMouseExited(e ->  closeX.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.30); -fx-cursor: hand;"));
        closeX.setOnMouseClicked(e -> dialog.close());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(topSpacer, closeX);
        topRow.setAlignment(Pos.CENTER_RIGHT);
        topRow.setOnMousePressed(e -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        topRow.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - drag[0]);
            dialog.setY(e.getScreenY() - drag[1]);
        });

        // ── Avatar & identity ─────────────────────────────────────────────────
        Label avatarLbl = new Label("👤");
        avatarLbl.setStyle("-fx-font-size: 38px;");

        String roleEmoji = roleEmoji(user.getRole());
        Label nameLbl = new Label(user.getFirstName() + " " + user.getLastName());
        nameLbl.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #D5E3EF;");

        Label roleLbl = new Label(roleEmoji + "  " + user.getRole());
        roleLbl.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #1DC98A;" +
            "-fx-font-weight: 600;");

        VBox identityBox = new VBox(3, nameLbl, roleLbl);
        identityBox.setAlignment(Pos.CENTER_LEFT);

        HBox identityRow = new HBox(14, avatarLbl, identityBox);
        identityRow.setAlignment(Pos.CENTER_LEFT);

        // ── Fields ────────────────────────────────────────────────────────────
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: rgba(29,201,138,0.18); -fx-border-color: rgba(29,201,138,0.18);");

        VBox fieldsBox = new VBox(10);
        fieldsBox.getChildren().add(fieldRow("🪪", "Employee ID", "#" + user.getEmployeeId()));
        fieldsBox.getChildren().add(fieldRow("✉", "Email",
            user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "—"));
        if (user.getParkId() != null) {
            fieldsBox.getChildren().add(fieldRow("🏞", "Assigned Park", "Park " + user.getParkId()));
        }

        Separator sep2 = new Separator();
        sep2.setStyle("-fx-background-color: rgba(29,201,138,0.18); -fx-border-color: rgba(29,201,138,0.18);");

        // ── Close button ──────────────────────────────────────────────────────
        String closeBtnNormal =
            "-fx-background-color: #1DC98A;" +
            "-fx-text-fill: #0B1422;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 40;" +
            "-fx-cursor: hand;";
        String closeBtnHover =
            "-fx-background-color: #25E89E;" +
            "-fx-text-fill: #0B1422;" +
            "-fx-font-weight: 700;" +
            "-fx-font-size: 13px;" +
            "-fx-background-radius: 8;" +
            "-fx-padding: 10 40;" +
            "-fx-cursor: hand;";

        Button closeBtn = new Button("Close");
        closeBtn.setStyle(closeBtnNormal);
        closeBtn.setDefaultButton(true);
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(closeBtnHover));
        closeBtn.setOnMouseExited(e  -> closeBtn.setStyle(closeBtnNormal));
        closeBtn.setOnAction(e -> dialog.close());

        HBox btnRow = new HBox(closeBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        // ── Card ──────────────────────────────────────────────────────────────
        VBox card = new VBox(14, topRow, identityRow, sep1, fieldsBox, sep2, btnRow);
        card.setPadding(new Insets(20, 28, 26, 28));
        card.setPrefWidth(360);
        card.setStyle(
            "-fx-background-color: #162338;" +
            "-fx-background-radius: 16;" +
            "-fx-border-color: rgba(29,201,138,0.15);" +
            "-fx-border-radius: 16;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 40, 0.08, 0, 10);"
        );
        card.setOnMousePressed(e -> { drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        card.setOnMouseDragged(e -> {
            dialog.setX(e.getScreenX() - drag[0]);
            dialog.setY(e.getScreenY() - drag[1]);
        });

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: transparent;");
        root.setPadding(new Insets(16));

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        try {
            String css = ProfileDialog.class.getResource("/gui/assets/dark-theme.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        Platform.runLater(closeBtn::requestFocus);
        dialog.showAndWait();
    }

    private static HBox fieldRow(String icon, String label, String value) {
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 14px;");
        iconLbl.setMinWidth(22);

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A98B2; -fx-font-weight: 600;");
        labelLbl.setMinWidth(110);

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #D5E3EF;");
        valueLbl.setWrapText(true);

        HBox row = new HBox(10, iconLbl, labelLbl, valueLbl);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String roleEmoji(String role) {
        if (role == null) return "👤";
        return switch (role.toLowerCase()) {
            case "park manager"   -> "🏕";
            case "dept manager"   -> "📈";
            case "gate worker"    -> "🚪";
            case "service rep"    -> "🎫";
            default               -> "👤";
        };
    }
}
