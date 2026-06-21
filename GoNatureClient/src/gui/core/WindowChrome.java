package gui.core;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * Builds a borderless window shell with a custom title bar (minimize / maximize / close)
 * that follows the active light/dark theme instead of the OS window frame.
 */
public final class WindowChrome {

    private static final double RESIZE_MARGIN = 6;
    private static final double ICON_SIZE = 10;
    private static final String APP_ICON = "/gui/icons/app-icon.png";

    private WindowChrome() {}

    /** Creates the scene wrapped in the custom chrome and switches the stage to undecorated mode. */
    public static Scene install(Stage stage, Parent content, double width, double height, String title) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(title);
        if (stage.getMinWidth() <= 0) stage.setMinWidth(Math.min(420, width));
        if (stage.getMinHeight() <= 0) stage.setMinHeight(Math.min(320, height));

        java.io.InputStream iconStream = WindowChrome.class.getResourceAsStream(APP_ICON);
        if (iconStream != null) {
            stage.getIcons().add(new Image(iconStream));
        }

        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setTop(buildTitleBar(stage, title));
        shell.setCenter(content);

        Scene scene = new Scene(shell, width, height);

        if (stage.isResizable()) {
            enableEdgeResize(stage, shell);
        }

        return scene;
    }

    /** Swaps the page content while keeping the existing title bar in place. */
    public static void setContent(Stage stage, Parent content) {
        ((BorderPane) stage.getScene().getRoot()).setCenter(content);
    }

    /** Swaps the page content and updates the title shown on the bar and the OS taskbar. */
    public static void setContent(Stage stage, Parent content, String title) {
        BorderPane shell = (BorderPane) stage.getScene().getRoot();
        shell.setCenter(content);
        stage.setTitle(title);
        Label titleLabel = (Label) shell.lookup(".title-bar-label");
        if (titleLabel != null) {
            titleLabel.setText("🌿  " + title);
        }
    }

    private static HBox buildTitleBar(Stage stage, String title) {
        Label titleLabel = new Label("🌿  " + title);
        titleLabel.getStyleClass().add("title-bar-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button minBtn = new Button();
        Button maxBtn = new Button();
        Button closeBtn = new Button();
        minBtn.setGraphic(minimizeIcon());
        maxBtn.setGraphic(maximizeIcon());
        closeBtn.setGraphic(closeIcon());
        minBtn.getStyleClass().addAll("title-bar-btn", "title-bar-btn-min");
        maxBtn.getStyleClass().addAll("title-bar-btn", "title-bar-btn-max");
        closeBtn.getStyleClass().addAll("title-bar-btn", "title-bar-btn-close");

        // Tracks our own "maximized" state and the bounds to restore to, since the real
        // maximize covers the taskbar and we instead snap to the screen's visual bounds.
        boolean[] maximized = {false};
        double[] restoreBounds = new double[4]; // x, y, width, height

        minBtn.setOnAction(e -> stage.setIconified(true));
        maxBtn.setOnAction(e -> toggleMaximize(stage, restoreBounds, maximized, maxBtn));
        closeBtn.setOnAction(e -> {
            // This fires the "Close Request", which triggers the auto-logout code we put in ClientUI!
            stage.fireEvent(new javafx.stage.WindowEvent(stage, javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
        });

        HBox bar = new HBox(6, titleLabel, spacer, minBtn, maxBtn, closeBtn);
        bar.getStyleClass().add("title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 8, 0, 14));

        enableDrag(stage, bar, restoreBounds, maximized, maxBtn);
        bar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                toggleMaximize(stage, restoreBounds, maximized, maxBtn);
            }
            e.consume();
        });

        return bar;
    }

    /** Toggles between the window's normal bounds and the screen's visual bounds (taskbar excluded). */
    private static void toggleMaximize(Stage stage, double[] restoreBounds, boolean[] maximized, Button maxBtn) {
        if (maximized[0]) {
            stage.setX(restoreBounds[0]);
            stage.setY(restoreBounds[1]);
            stage.setWidth(restoreBounds[2]);
            stage.setHeight(restoreBounds[3]);
            maximized[0] = false;
        } else {
            restoreBounds[0] = stage.getX();
            restoreBounds[1] = stage.getY();
            restoreBounds[2] = stage.getWidth();
            restoreBounds[3] = stage.getHeight();

            Rectangle2D bounds = Screen.getScreensForRectangle(
                    stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                    .get(0).getVisualBounds();

            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            maximized[0] = true;
        }
        maxBtn.setGraphic(maximized[0] ? restoreIcon() : maximizeIcon());
    }

    /** A single horizontal line — the minimize glyph. */
    private static Node minimizeIcon() {
        Line line = new Line(0, 0, ICON_SIZE, 0);
        line.getStyleClass().add("title-bar-icon");
        return iconPane(line);
    }

    /** An empty square outline — the maximize glyph. */
    private static Node maximizeIcon() {
        Rectangle rect = new Rectangle(ICON_SIZE, ICON_SIZE);
        rect.setFill(Color.TRANSPARENT);
        rect.getStyleClass().add("title-bar-icon-stroke");
        return iconPane(rect);
    }

    /** Two overlapping square outlines — the restore-down glyph. */
    private static Node restoreIcon() {
        double size = ICON_SIZE - 2.5;
        double offset = ICON_SIZE - size;

        Rectangle back = new Rectangle(offset, 0, size, size);
        back.setFill(Color.TRANSPARENT);
        back.getStyleClass().add("title-bar-icon-stroke");

        Rectangle front = new Rectangle(0, offset, size, size);
        front.setFill(Color.TRANSPARENT);
        front.getStyleClass().add("title-bar-icon-stroke");

        Pane pane = new Pane(back, front);
        pane.setPrefSize(ICON_SIZE, ICON_SIZE);
        pane.setMinSize(ICON_SIZE, ICON_SIZE);
        pane.setMaxSize(ICON_SIZE, ICON_SIZE);
        return pane;
    }

    /** Two crossing diagonal lines — the close glyph. */
    private static Node closeIcon() {
        Line l1 = new Line(0, 0, ICON_SIZE, ICON_SIZE);
        Line l2 = new Line(0, ICON_SIZE, ICON_SIZE, 0);
        l1.getStyleClass().add("title-bar-icon");
        l2.getStyleClass().add("title-bar-icon");
        return iconPane(l1, l2);
    }

    private static StackPane iconPane(Node... shapes) {
        StackPane pane = new StackPane(shapes);
        pane.setPrefSize(ICON_SIZE, ICON_SIZE);
        pane.setMinSize(ICON_SIZE, ICON_SIZE);
        return pane;
    }

    private static void enableDrag(Stage stage, HBox bar, double[] restoreBounds, boolean[] maximized, Button maxBtn) {
        final double[] offset = new double[2];
        bar.setOnMousePressed(e -> {
            offset[0] = e.getSceneX();
            offset[1] = e.getSceneY();
            e.consume();
        });
        bar.setOnMouseDragged(e -> {
            if (maximized[0]) {
                // Dragging a "maximized" window restores it first, keeping it under the cursor.
                double ratio = e.getSceneX() / Math.max(stage.getWidth(), 1);
                double restoredWidth = restoreBounds[2];

                stage.setWidth(restoredWidth);
                stage.setHeight(restoreBounds[3]);
                maximized[0] = false;
                maxBtn.setGraphic(maximizeIcon());

                offset[0] = ratio * restoredWidth;
            }
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
            e.consume();
        });
    }

    /** Lets the user resize the borderless window by dragging its left/right/bottom edges and corners. */
    private static void enableEdgeResize(Stage stage, Region root) {
        final int[] dir = new int[2]; // -1/0/1 for left-right and (only) bottom edge
        final double[] start = new double[4]; // screenX, screenY, stageWidth, stageHeight

        // 1. Intercept Mouse Movement to show the resize cursor
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
            int x = edge(e.getX(), root.getWidth());
            int y = e.getY() > root.getHeight() - RESIZE_MARGIN ? 1 : 0;
            if (x != 0 || y != 0) {
                root.setCursor(cursorFor(x, y));
            } else {
                root.setCursor(Cursor.DEFAULT);
            }
        });

        // 2. Intercept the Click BEFORE the ScrollPane steals it
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            int x = edge(e.getX(), root.getWidth());
            int y = e.getY() > root.getHeight() - RESIZE_MARGIN ? 1 : 0;
            
            if (x != 0 || y != 0) { // If the user clicked on an edge...
                dir[0] = x;
                dir[1] = y;
                start[0] = e.getScreenX();
                start[1] = e.getScreenY();
                start[2] = stage.getWidth();
                start[3] = stage.getHeight();
                e.consume(); 
            } else {
                dir[0] = 0;
                dir[1] = 0;
            }
        });

        // 3. Intercept the Drag to resize the window AND anchor the layout
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, e -> {
            if (dir[0] == 0 && dir[1] == 0) return;

            double dx = e.getScreenX() - start[0];
            double dy = e.getScreenY() - start[1];

            if (dir[0] == 1) {
                double newWidth = Math.max(stage.getMinWidth(), start[2] + dx);
                stage.setWidth(newWidth);
                root.setPrefWidth(newWidth); // <--- THE FIX: Anchors layout against popup snap-backs
            } else if (dir[0] == -1) {
                double newWidth = Math.max(stage.getMinWidth(), start[2] - dx);
                stage.setX(stage.getX() + (stage.getWidth() - newWidth));
                stage.setWidth(newWidth);
                root.setPrefWidth(newWidth); // <--- THE FIX: Anchors layout against popup snap-backs
            }
            
            if (dir[1] == 1) {
                double newHeight = Math.max(stage.getMinHeight(), start[3] + dy);
                stage.setHeight(newHeight);
                root.setPrefHeight(newHeight); // <--- THE FIX: Anchors layout against popup snap-backs
            }
            
            e.consume(); 
        });

        // 4. Reset our variables when the user lets go of the mouse
        root.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, e -> {
            dir[0] = 0;
            dir[1] = 0;
        });
    }

    private static int edge(double x, double width) {
        if (x < RESIZE_MARGIN) return -1;
        if (x > width - RESIZE_MARGIN) return 1;
        return 0;
    }

    private static Cursor cursorFor(int x, int y) {
        if (x == -1 && y == 1) return Cursor.SW_RESIZE;
        if (x == 1 && y == 1) return Cursor.SE_RESIZE;
        if (x == -1) return Cursor.W_RESIZE;
        if (x == 1) return Cursor.E_RESIZE;
        if (y == 1) return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }
}
