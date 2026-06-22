package gui.guest;

import client.ChatClient;
import entities.Message;
import entities.Park;
import gui.core.ThemeManager;
import gui.core.WindowChrome;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MainMenuController {

    @FXML private Button    themeBtn, loginBtn, guestBtn, manageOrdersBtn;
    @FXML private Pane      parksView, openZone, returnZone;
    @FXML private StackPane mainView;
    @FXML private HBox      parkStrip;

    private static final double FOLD = 90;

    private Scale    mainScale;
    private Rotate   mainRotate;
    private Timeline currentAnim;
    private boolean  lockedToParks = false;

    // ── Dashboard strip: 4 featured parks (fixed) ────────────────────────────

    private static final String[] STRIP_NAMES  = {
        "Carmel National Park", "Carmel Nature Reserve",
        "Ein Gedi Nature Reserve", "Banias Nature Reserve"
    };
    private static final String[] STRIP_COLORS = { "#1ecf8c", "#c8d44a", "#e8795a", "#3aaee0" };
    private static final String[] STRIP_ICONS  = { "🌲", "🌿", "🌊", "💧" };

    // ── Map: cycling palette for any number of parks ──────────────────────────

    private static final String[] MAP_COLORS = {
        "#1ecf8c", "#c8d44a", "#e8795a", "#3aaee0",
        "#f0b429", "#a78bfa", "#fb923c", "#34d399",
        "#60a5fa", "#f472b6", "#4ade80", "#fbbf24"
    };
    private static final String[] MAP_ICONS = {
        "🌲", "🌿", "🌊", "💧", "🏔", "🌺", "🦅", "🌋", "🏜", "🌸", "🌵", "⛰"
    };

    // ── World canvas ──────────────────────────────────────────────────────────

    private static final double WORLD_W   = 2000;
    private static final double WORLD_H   = 1500;
    private static final double ZOOM_MIN  = 0.30;
    private static final double ZOOM_MAX  = 4.0;
    private static final double ZOOM_STEP = 1.15;

    // ── Map state ─────────────────────────────────────────────────────────────

    private ArrayList<Park> mapParks  = new ArrayList<>();
    private StackPane[]     parkNodes = new StackPane[0];
    private double[]        mapPosX, mapPosY;

    private Pane worldCanvas;
    private int  starCount = 0;

    /*
     * Transform strategy: two explicit transforms on worldCanvas with NO setScaleX/Y
     * or setTranslateX/Y on the node (those would compose on top and break the math).
     *
     * transforms list = [worldPan(index 0), worldZoom(index 1)]
     * JavaFX applies list in reverse order for point transforms:
     *   parentPoint = worldPan × worldZoom × localPoint
     *   => worldZoom (scale at pivot 0,0) applied first, worldPan (translate) applied second
     *   => screenX = worldX * zoomLevel + panX   ✓ exact, no compensation needed
     */
    private final Translate worldPan  = new Translate();
    private final Scale     worldZoom = new Scale(1.0, 1.0, 0, 0);

    // Logical pan (matches the formula above)
    private double panX = 0, panY = 0;
    private double zoomLevel = 1.0;

    // Cached viewport size — updated in updateLayout so handlers always see current dims
    private double viewW = 850, viewH = 615;

    // Drag state
    private double dragStartX, dragStartY;
    private double panStartX, panStartY;
    private double totalDragDistance;

    private Label mapTitleLabel;
    private int   selectedPark = -1;

    // Info panel (viewport-fixed)
    private VBox  infoPanel;
    private Label infoParkIcon, infoParkName, infoParkVisitors;
    private Label infoParkCapacity, infoParkStay, infoParkDiscount;
    private Pane  infoAccentBar;

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
        mainScale  = new Scale(1, 1, 0, 0);
        mainRotate = new Rotate(0, 0, 0);
        mainView.getTransforms().addAll(mainScale, mainRotate);
        returnZone.setOpacity(0);
        parksView.setMouseTransparent(true);
        Platform.runLater(this::applyClips);
        buildParksView();
        buildParkStrip();
        buildZoneHints();
        fetchAllParks();
    }

    // ── Server communication ──────────────────────────────────────────────────

    private void fetchAllParks() {
        try {
            ChatClient client = ChatClient.getInstance("127.0.0.1", 5555, this::handleServerResponse);
            client.handleMessageFromClientUI(new Message("FETCH_ALL_PARKS", null));
        } catch (Exception e) {
            // Server not reachable; map stays in loading state
        }
    }

    public void handleServerResponse(Message msg) {
        if ("ALL_PARKS_DATA".equals(msg.getCommand())) {
            @SuppressWarnings("unchecked")
            ArrayList<Park> parks = (ArrayList<Park>) msg.getData();
            Platform.runLater(() -> refreshMapParks(parks));
        }
    }

    // ── Resize / clip ─────────────────────────────────────────────────────────

    private void applyClips() {
        double w = mainView.getWidth();
        double h = mainView.getHeight();
        if (w <= 0) { w = 850; h = 615; }

        openZone.setClip(new Polygon(0, 0, FOLD, 0, 0, FOLD));
        returnZone.setClip(new Polygon(150 - FOLD, 90, 150, 0, 150, 90));

        updateLayout(w, h);

        Scene scene = mainView.getScene();
        scene.widthProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> updateLayout(scene.getWidth(), scene.getHeight())));
        scene.heightProperty().addListener((obs, o, n) ->
            Platform.runLater(() -> updateLayout(scene.getWidth(), scene.getHeight())));
    }

    private void updateLayout(double w, double h) {
        if (w <= 0 || h <= 0) return;

        // Cache current viewport dimensions so handlers always use up-to-date values
        viewW = w;
        viewH = h;

        mainScale.setPivotX(w);  mainScale.setPivotY(h);
        mainRotate.setPivotX(w); mainRotate.setPivotY(h);

        mainView.setClip(new Polygon(FOLD, 0, w, 0, w, h, 0, h, 0, FOLD));

        // Guard clip polygon against degenerate cases at very small window heights
        double foldH = Math.max(0, h - FOLD);
        double foldW = Math.max(0, w - FOLD);
        parksView.setClip(new Polygon(0, 0, w, 0, w, foldH, foldW, h, 0, h));

        if (mapTitleLabel != null)
            mapTitleLabel.setLayoutX((w - 300) / 2.0);

        if (worldCanvas != null) {
            panX = clampPan(panX, WORLD_W, viewW, zoomLevel);
            panY = clampPan(panY, WORLD_H, viewH, zoomLevel);
            applyWorldTransform();
        }
    }

    // ── World-canvas transform ────────────────────────────────────────────────

    /**
     * Applies pan and zoom via explicit transforms on worldCanvas.
     * Formula: screenX = worldX * zoomLevel + panX  (exact, no pivot compensation needed
     * because worldZoom's pivot is (0,0) and worldPan is a Translate, not setTranslateX).
     */
    private void applyWorldTransform() {
        worldZoom.setX(zoomLevel);
        worldZoom.setY(zoomLevel);
        worldPan.setX(panX);
        worldPan.setY(panY);
    }

    /** Clamps logical pan so the world never fully leaves the viewport (25% slack). */
    private double clampPan(double pan, double worldSize, double viewSize, double scale) {
        double effective = worldSize * scale;
        if (effective <= viewSize)
            return (viewSize - effective) / 2.0; // world fits — centre it
        double slack = viewSize * 0.25;
        return Math.max(viewSize - effective - slack, Math.min(slack, pan));
    }

    /** Centres the world in the viewport at current zoomLevel and clamps. */
    private void centreWorld(double vw, double vh) {
        panX = clampPan(vw / 2.0 - WORLD_W * zoomLevel / 2.0, WORLD_W, vw, zoomLevel);
        panY = clampPan(vh / 2.0 - WORLD_H * zoomLevel / 2.0, WORLD_H, vh, zoomLevel);
        applyWorldTransform();
    }

    // ── Open Zone ────────────────────────────────────────────────────────────

    @FXML void onOpenHover(MouseEvent e)    { if (!lockedToParks) animateTo(0.0, 420); }
    @FXML void onOpenHoverEnd(MouseEvent e) { if (!lockedToParks) animateTo(1.0, 320); }

    @FXML void onOpenClick(MouseEvent e) {
        lockedToParks = true;
        animateTo(0.0, 260);
        openZone.setMouseTransparent(true);
        returnZone.setMouseTransparent(false);
        parksView.setMouseTransparent(false);
        fadeZoneHints(0.0, 1.0);
        if (mapParks.isEmpty()) fetchAllParks();
    }

    // ── Return Zone ──────────────────────────────────────────────────────────

    @FXML void onCloseHover(MouseEvent e)    { if (lockedToParks) animateTo(1.0, 420); }
    @FXML void onCloseHoverEnd(MouseEvent e) { if (lockedToParks) animateTo(0.0, 320); }

    @FXML void onCloseClick(MouseEvent e) {
        lockedToParks = false;
        hideParkInfo();
        animateTo(1.0, 260);
        openZone.setMouseTransparent(false);
        returnZone.setMouseTransparent(true);
        parksView.setMouseTransparent(true);
        fadeZoneHints(1.0, 0.0);
    }

    // ── Page-flip animation ───────────────────────────────────────────────────

    private void animateTo(double target, int ms) {
        if (currentAnim != null) currentAnim.stop();
        Interpolator interp = target == 0.0 ? Interpolator.EASE_IN : Interpolator.EASE_OUT;
        currentAnim = new Timeline(new KeyFrame(Duration.millis(ms),
            new KeyValue(mainScale.xProperty(),      target,                   interp),
            new KeyValue(mainScale.yProperty(),      target,                   interp),
            new KeyValue(mainView.opacityProperty(), target,                   interp),
            new KeyValue(mainRotate.angleProperty(), target == 0 ? 12.0 : 0.0, interp)
        ));
        currentAnim.play();
    }

    private void fadeZoneHints(double openTarget, double returnTarget) {
        FadeTransition fo = new FadeTransition(Duration.millis(220), openZone);
        fo.setToValue(openTarget); fo.play();
        FadeTransition fr = new FadeTransition(Duration.millis(220), returnZone);
        fr.setToValue(returnTarget); fr.play();
    }

    // ── Parks view ────────────────────────────────────────────────────────────

    private void buildParksView() {
        worldCanvas = new Pane();
        worldCanvas.setPrefSize(WORLD_W, WORLD_H);
        worldCanvas.setCursor(Cursor.OPEN_HAND);

        // Attach transforms — worldZoom(index 1) applied first, worldPan(index 0) applied after
        // Do NOT call setScaleX/Y or setTranslateX/Y on worldCanvas — they compose on top of these
        worldCanvas.getTransforms().addAll(worldPan, worldZoom);

        // Stars scattered across the full world canvas
        Random rng = new Random(42);
        for (int i = 0; i < 260; i++) {
            double x = rng.nextDouble() * WORLD_W;
            double y = rng.nextDouble() * WORLD_H;
            double r = 0.7 + rng.nextDouble() * 1.9;
            double a = 0.10 + rng.nextDouble() * 0.45;
            Circle star = new Circle(x, y, r, Color.web("#ffffff", a));
            if (rng.nextDouble() > 0.78)
                star.setEffect(new DropShadow(r * 3, Color.web("#aaddff", 0.40)));
            worldCanvas.getChildren().add(star);
        }
        starCount = worldCanvas.getChildren().size();

        // ── Pan handlers ────────────────────────────────────────────────────
        worldCanvas.setOnMousePressed(e -> {
            dragStartX   = e.getSceneX();
            dragStartY   = e.getSceneY();
            panStartX    = panX;
            panStartY    = panY;
            totalDragDistance = 0;
            worldCanvas.setCursor(Cursor.CLOSED_HAND);
            e.consume();
        });
        worldCanvas.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - dragStartX;
            double dy = e.getSceneY() - dragStartY;
            totalDragDistance = Math.hypot(dx, dy);
            panX = clampPan(panStartX + dx, WORLD_W, viewW, zoomLevel);
            panY = clampPan(panStartY + dy, WORLD_H, viewH, zoomLevel);
            applyWorldTransform();
            e.consume();
        });
        worldCanvas.setOnMouseReleased(e -> {
            worldCanvas.setCursor(Cursor.OPEN_HAND);
            e.consume();
        });
        worldCanvas.setOnMouseClicked(e -> {
            if (totalDragDistance < 5) hideParkInfo();
            e.consume();
        });

        centreWorld(viewW, viewH);
        parksView.getChildren().add(worldCanvas);

        // ── Zoom: scroll anywhere on parksView ──────────────────────────────
        parksView.setOnScroll((ScrollEvent e) -> {
            if (!lockedToParks) return;

            double oldZoom = zoomLevel;
            double factor  = e.getDeltaY() > 0 ? ZOOM_STEP : 1.0 / ZOOM_STEP;
            zoomLevel = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, zoomLevel * factor));

            // Keep the world point under the cursor fixed:
            //   mouseX = wx * oldZoom + oldPanX  =>  wx = (mouseX - oldPanX) / oldZoom
            //   newPanX = mouseX - wx * newZoom
            double mouseX = e.getX();
            double mouseY = e.getY();
            panX = mouseX - (mouseX - panX) * (zoomLevel / oldZoom);
            panY = mouseY - (mouseY - panY) * (zoomLevel / oldZoom);

            panX = clampPan(panX, WORLD_W, viewW, zoomLevel);
            panY = clampPan(panY, WORLD_H, viewH, zoomLevel);
            applyWorldTransform();
            e.consume();
        });

        // ── Viewport-fixed overlays ──────────────────────────────────────────
        mapTitleLabel = new Label("I S R A E L  N A T I O N A L  P A R K S");
        mapTitleLabel.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:rgba(255,255,255,0.16);");
        mapTitleLabel.setLayoutX(230); mapTitleLabel.setLayoutY(24);
        parksView.getChildren().add(mapTitleLabel);

        Label hint = new Label("✦  drag to pan  ·  scroll to zoom");
        hint.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.20);");
        hint.setMaxWidth(Double.MAX_VALUE); hint.setAlignment(Pos.BOTTOM_CENTER);
        hint.setPrefHeight(570); hint.setMaxHeight(570); hint.setLayoutX(0);
        parksView.getChildren().add(hint);

        Label loading = new Label("loading parks…");
        loading.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.22);");
        loading.setLayoutX(350); loading.setLayoutY(290);
        loading.setId("loadingLabel");
        parksView.getChildren().add(loading);

        buildInfoPanel();
        parksView.getChildren().add(infoPanel);
    }

    // ── Dynamic park map ──────────────────────────────────────────────────────

    private void refreshMapParks(ArrayList<Park> parks) {
        mapParks = parks;
        int n = parks.size();

        if (worldCanvas.getChildren().size() > starCount)
            worldCanvas.getChildren().subList(starCount, worldCanvas.getChildren().size()).clear();

        parksView.getChildren().removeIf(nd -> "loadingLabel".equals(nd.getId()));
        parksView.getChildren().remove(infoPanel);

        if (n == 0) {
            parksView.getChildren().add(infoPanel);
            return;
        }

        generatePositions(n);
        buildConstellationLines(n);

        parkNodes = new StackPane[n];
        for (int i = 0; i < n; i++) {
            String color = MAP_COLORS[i % MAP_COLORS.length];
            String icon  = MAP_ICONS[i % MAP_ICONS.length];
            parkNodes[i] = makeParkCircle(i, parks.get(i), color, icon);
            worldCanvas.getChildren().add(parkNodes[i]);
        }

        parksView.getChildren().add(infoPanel);

        // Reset zoom and re-centre using current cached viewport size
        zoomLevel = 1.0;
        centreWorld(viewW, viewH);
    }

    // Phyllotaxis/sunflower spiral — distributes N points evenly in an ellipse
    private void generatePositions(int n) {
        mapPosX = new double[n];
        mapPosY = new double[n];
        double cx = WORLD_W / 2.0, cy = WORLD_H / 2.0;
        double rx = 750, ry = 530;
        double golden = 2.39996323;
        for (int i = 0; i < n; i++) {
            if (n == 1) { mapPosX[i] = cx; mapPosY[i] = cy; }
            else {
                double r = Math.sqrt((double)(i + 0.5) / n);
                mapPosX[i] = cx + rx * r * Math.cos(i * golden);
                mapPosY[i] = cy + ry * r * Math.sin(i * golden);
            }
        }
    }

    // Nearest-neighbour spanning graph
    private void buildConstellationLines(int n) {
        boolean[][] seen = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            double minD = Double.MAX_VALUE; int nb = -1;
            for (int j = 0; j < n; j++) {
                if (j == i || seen[i][j]) continue;
                double d = Math.hypot(mapPosX[i]-mapPosX[j], mapPosY[i]-mapPosY[j]);
                if (d < minD) { minD = d; nb = j; }
            }
            if (nb >= 0 && !seen[i][nb]) {
                seen[i][nb] = seen[nb][i] = true;
                Line l = new Line(mapPosX[i], mapPosY[i], mapPosX[nb], mapPosY[nb]);
                l.setStroke(Color.web("#6ab4e8", 0.18));
                l.setStrokeWidth(1.0);
                l.getStrokeDashArray().addAll(6.0, 5.0);
                l.setMouseTransparent(true);
                worldCanvas.getChildren().add(l);
            }
        }
    }

    // ── Park circle ───────────────────────────────────────────────────────────

    private StackPane makeParkCircle(int idx, Park park, String color, String icon) {
        double r = 54;

        Circle ring = new Circle(r * 1.38);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web(color, 0.60));
        ring.setStrokeWidth(1.8);
        ring.getStrokeDashArray().addAll(5.0, 4.0);
        ring.setOpacity(0);
        ring.setMouseTransparent(true);

        Circle bg = new Circle(r);
        bg.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.85, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(color, 1.00)),
            new Stop(1, Color.web(color, 0.68))
        ));
        DropShadow glow = new DropShadow(32, Color.web(color, 0.65));
        glow.setSpread(0.05);
        bg.setEffect(glow);

        Circle inner = new Circle(r * 0.60);
        inner.setFill(new RadialGradient(0, 0, 0.4, 0.35, 0.9, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ffffff", 0.22)),
            new Stop(1, Color.web("#ffffff", 0.00))
        ));
        inner.setMouseTransparent(true);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:20px;");

        Label nameLbl = new Label(park.getName());
        nameLbl.setStyle("-fx-text-fill:rgba(255,255,255,0.92); -fx-font-size:9.5px; -fx-font-weight:bold;");
        nameLbl.setMaxWidth(r * 1.55); nameLbl.setWrapText(true);
        nameLbl.setAlignment(Pos.CENTER);

        VBox content = new VBox(4, iconLbl, nameLbl);
        content.setAlignment(Pos.CENTER);
        content.setMouseTransparent(true);

        StackPane node = new StackPane(ring, bg, inner, content);
        node.setPrefSize(r * 2.76, r * 2.76);
        node.setLayoutX(mapPosX[idx] - r * 1.38);
        node.setLayoutY(mapPosY[idx] - r * 1.38);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(2000 + idx * 200), bg);
        pulse.setFromX(1.00); pulse.setFromY(1.00);
        pulse.setToX(1.05);  pulse.setToY(1.05);
        pulse.setAutoReverse(true); pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.setDelay(Duration.millis(idx * 300));
        pulse.play();

        node.setOnMouseEntered(e -> {
            node.setCursor(Cursor.HAND);
            FadeTransition ftIn = new FadeTransition(Duration.millis(180), ring); ftIn.setToValue(1.0); ftIn.play();
            ScaleTransition stIn = new ScaleTransition(Duration.millis(200), ring); stIn.setToX(1.08); stIn.setToY(1.08); stIn.play();
            DropShadow h = new DropShadow(48, Color.web(color, 0.90)); h.setSpread(0.15);
            bg.setEffect(h);
        });
        node.setOnMouseExited(e -> {
            node.setCursor(Cursor.DEFAULT);
            FadeTransition ftOut = new FadeTransition(Duration.millis(220), ring); ftOut.setToValue(selectedPark == idx ? 0.55 : 0.0); ftOut.play();
            ScaleTransition stOut = new ScaleTransition(Duration.millis(220), ring); stOut.setToX(1.0); stOut.setToY(1.0); stOut.play();
            bg.setEffect(glow);
        });
        node.setOnMouseDragged(e -> e.consume());
        node.setOnMouseClicked(e -> {
            if (totalDragDistance < 5) {
                if (selectedPark == idx) hideParkInfo();
                else                     showParkInfo(idx);
            }
            e.consume();
        });

        return node;
    }

    // ── Park info panel ───────────────────────────────────────────────────────

    private void buildInfoPanel() {
        infoParkIcon = new Label("🌲");
        infoParkIcon.setStyle("-fx-font-size:30px;");

        infoParkName = new Label();
        infoParkName.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:16px; -fx-font-weight:bold;" +
                              "-fx-text-fill:white; -fx-wrap-text:true;");
        infoParkName.setWrapText(true); infoParkName.setMaxWidth(230);

        infoAccentBar = new Pane();
        infoAccentBar.setPrefHeight(1.5); infoAccentBar.setMaxWidth(Double.MAX_VALUE);

        infoParkVisitors = new Label();
        infoParkVisitors.setStyle("-fx-font-size:11.5px; -fx-text-fill:rgba(255,255,255,0.82);");

        infoParkCapacity = new Label();
        infoParkCapacity.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.55);");

        infoParkStay = new Label();
        infoParkStay.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.55);");

        infoParkDiscount = new Label();
        infoParkDiscount.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#f0b429;");
        infoParkDiscount.setVisible(false);

        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(255,255,255,0.40); -fx-cursor:hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(255,255,255,0.85); -fx-cursor:hand;"));
        closeBtn.setOnMouseExited(e ->  closeBtn.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(255,255,255,0.40); -fx-cursor:hand;"));
        closeBtn.setOnMouseClicked(e -> { hideParkInfo(); e.consume(); });

        HBox header = new HBox();
        header.setAlignment(Pos.TOP_LEFT);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(infoParkIcon, spacer, closeBtn);

        infoPanel = new VBox(10, header, infoParkName, infoAccentBar,
                             infoParkVisitors, infoParkCapacity, infoParkStay, infoParkDiscount);
        infoPanel.setStyle(
            "-fx-background-color:rgba(4,12,28,0.52);" +
            "-fx-border-color:rgba(255,255,255,0.10);" +
            "-fx-border-width:1;" +
            "-fx-border-radius:14;" +
            "-fx-background-radius:14;" +
            "-fx-padding:18 20 22 20;"
        );
        infoPanel.setEffect(new DropShadow(30, Color.web("#000", 0.55)));
        infoPanel.setPrefWidth(268); infoPanel.setMaxWidth(268);
        infoPanel.setOpacity(0);
        infoPanel.setMouseTransparent(true);
        infoPanel.setLayoutX(490); infoPanel.setLayoutY(170);
    }

    private void showParkInfo(int idx) {
        if (idx < 0 || idx >= mapParks.size()) return;
        selectedPark = idx;
        Park   park  = mapParks.get(idx);
        String color = MAP_COLORS[idx % MAP_COLORS.length];
        String icon  = MAP_ICONS[idx % MAP_ICONS.length];

        for (int j = 0; j < parkNodes.length; j++) {
            if (parkNodes[j] == null) continue;
            FadeTransition ft = new FadeTransition(Duration.millis(240), parkNodes[j]);
            ft.setToValue(j == idx ? 1.0 : 0.28); ft.play();
        }

        infoParkIcon.setText(icon);
        infoParkName.setText(park.getName());
        infoAccentBar.setStyle("-fx-background-color:" + color + "; -fx-background-radius:1;");
        infoParkVisitors.setText("👥  " + park.getCurrentVisitors() + " visitors now");
        infoParkCapacity.setText("🏕  Capacity: " + park.getMaxCapacity() +
                                 "  ·  Gap: " + park.getCasualGap());
        infoParkStay.setText("⏱  Avg visit: ~" + park.getEstimatedStayTime() + " min");
        if (park.getActiveDiscount() > 0) {
            infoParkDiscount.setText("🏷  " + (int) park.getActiveDiscount() + "% discount active");
            infoParkDiscount.setVisible(true);
        } else {
            infoParkDiscount.setVisible(false);
        }
        infoPanel.setStyle(
            "-fx-background-color:rgba(4,12,28,0.52);" +
            "-fx-border-color:" + color + ";" +
            "-fx-border-width:1.5;" +
            "-fx-border-radius:14;" +
            "-fx-background-radius:14;" +
            "-fx-padding:18 20 22 20;"
        );

        // screenX = worldX * zoomLevel + panX
        double sx = mapPosX[idx] * zoomLevel + panX;
        double sy = mapPosY[idx] * zoomLevel + panY;
        double panelX = sx > viewW * 0.5 ? 40 : viewW - 310;
        double panelY = Math.max(48, Math.min(viewH - 280, sy - 90));
        infoPanel.setLayoutX(panelX);
        infoPanel.setLayoutY(panelY);
        infoPanel.setTranslateX(panelX > viewW * 0.5 ? 28 : -28);
        infoPanel.setMouseTransparent(false);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), infoPanel);
        fadeIn.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), infoPanel);
        slide.setToX(0); slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fadeIn, slide).play();
    }

    private void hideParkInfo() {
        if (selectedPark == -1) return;
        selectedPark = -1;
        for (StackPane node : parkNodes) {
            if (node == null) continue;
            FadeTransition ftReset = new FadeTransition(Duration.millis(240), node); ftReset.setToValue(1.0); ftReset.play();
        }
        FadeTransition out = new FadeTransition(Duration.millis(200), infoPanel);
        out.setToValue(0.0);
        out.setOnFinished(e -> infoPanel.setMouseTransparent(true));
        out.play();
    }

    // ── Dashboard strip (4 featured parks, hardcoded) ─────────────────────────

    private void buildParkStrip() {
        for (int i = 0; i < STRIP_NAMES.length; i++) {
            VBox card = new VBox(7);
            String base  = "-fx-background-color:rgba(100,70,25,0.07);" +
                           "-fx-border-color:#c4a558; -fx-border-radius:8; -fx-background-radius:8;" +
                           "-fx-padding:12 14;";
            String hover = "-fx-background-color:rgba(196,165,88,0.16);" +
                           "-fx-border-color:" + STRIP_COLORS[i] + ";" +
                           "-fx-border-radius:8; -fx-background-radius:8;" +
                           "-fx-padding:12 14;" +
                           "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.10),8,0,0,2);";
            card.setStyle(base);
            card.setPrefWidth(168); card.setMinWidth(168);
            Label icon = new Label(STRIP_ICONS[i]); icon.setStyle("-fx-font-size:18px;");
            Label lbl  = new Label(STRIP_NAMES[i]);
            lbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#3d1f05;");
            lbl.setWrapText(true); lbl.setMaxWidth(142);
            card.getChildren().addAll(icon, lbl);
            card.setOnMouseEntered(e -> card.setStyle(hover));
            card.setOnMouseExited(e  -> card.setStyle(base));
            parkStrip.getChildren().add(card);
        }
    }

    // ── Zone corner labels ────────────────────────────────────────────────────

    private void buildZoneHints() {
        Label expText = new Label("E X P L O R E");
        expText.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:8px; -fx-font-weight:bold;" +
                         "-fx-text-fill:rgba(170,225,255,0.95);");
        expText.setRotate(-45); expText.setLayoutX(-6); expText.setLayoutY(29);
        expText.setMouseTransparent(true);
        Circle expDot = new Circle(72, 8, 3, Color.web("#70d0ff", 0.85));
        expDot.setEffect(new DropShadow(12, Color.web("#70d0ff", 0.9)));
        expDot.setMouseTransparent(true);
        openZone.getChildren().addAll(expText, expDot);

        Label retText = new Label("BACK");
        retText.setStyle("-fx-font-family:'Segoe UI'; -fx-font-size:11px; -fx-font-weight:bold;" +
                         "-fx-text-fill:rgba(100,55,5,0.95);");
        retText.setRotate(-45); retText.setLayoutX(103); retText.setLayoutY(54);
        retText.setMouseTransparent(true);
        returnZone.getChildren().add(retText);
    }

    // ── Theme & Navigation ────────────────────────────────────────────────────

    @FXML void handleToggleTheme(ActionEvent event) {
        Scene scene = ((Node) event.getSource()).getScene();
        ThemeManager.getInstance().toggle(scene);
        themeBtn.setText(ThemeManager.getInstance().toggleLabel());
    }

    @FXML void handleGuestBooking(ActionEvent event) {
        switchScene(event, "/gui/guest/CreateOrder.fxml", "GoNature - Guest Booking");
    }

    @FXML void handleEmployeeLogin(ActionEvent event) {
        switchScene(event, "/gui/auth/Login.fxml", "GoNature - Employee Login");
    }

    @FXML void handleManageOrders(ActionEvent event) {
        switchScene(event, "/gui/guest/GuestPortal.fxml", "GoNature - Manage Orders");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage  stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            WindowChrome.setContent(stage, root, title);
        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not load FXML file -> " + fxmlPath);
            e.printStackTrace();
        }
    }
}
