import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/**
 * GoNature UI Demo 04 — MAP-FIRST PARK OPERATIONS
 * ------------------------------------------------------------
 * A logistics / control-room view. A stylised map canvas with clickable park
 * markers coloured by capacity heat, plus a live details panel (occupancy,
 * upcoming visits, entrance/check-in status). Clicking a marker updates the panel.
 *
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_04_Map_First_Park_Operations extends Application {

    static final String BG     = "#0a0e13";
    static final String PANEL  = "#10151c";
    static final String CARD   = "#161d27";
    static final String STROKE = "#232c39";
    static final String TEXT   = "#e8eef6";
    static final String MUTED  = "#8593a6";
    static final String GREEN  = "#43d6a0";
    static final String CYAN   = "#3fb6ff";
    static final String AMBER  = "#f6b454";
    static final String RED    = "#ff6b6b";

    // park: name, region, x%, y%, now, max
    private final Object[][] parks = {
        {"Masada National Park", "Judean Desert", 0.52, 0.62, 642, 800},
        {"Caesarea National Park", "Med. Coast", 0.30, 0.34, 518, 600},
        {"Timna Valley Park", "Arava · Eilat", 0.46, 0.90, 121, 400},
        {"Nahal Ayun Nature Reserve", "Upper Galilee", 0.58, 0.10, 196, 200},
    };
    private final VBox details = new VBox(16);
    private Pane mapPane;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(topBar());

        HBox body = new HBox(0);
        body.getChildren().addAll(mapArea(), detailsPanel());
        root.setCenter(body);

        selectPark(0);

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Map-First Operations (Demo 04)");
        stage.show();
    }

    private HBox topBar() {
        HBox t = new HBox(14);
        t.setAlignment(Pos.CENTER_LEFT);
        t.setPadding(new Insets(16, 24, 16, 24));
        t.setStyle("-fx-background-color:" + PANEL + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Circle dot = new Circle(14, Color.web(CYAN));
        Label leaf = new Label("◎");
        leaf.setStyle("-fx-text-fill:#06121f;-fx-font-size:13px;-fx-font-weight:bold;");
        StackPane logo = new StackPane(dot, leaf);
        Label brand = new Label("GoNature  ·  Park Operations Map");
        brand.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:16px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        t.getChildren().addAll(logo, brand, g,
            legend(GREEN, "Open"), legend(AMBER, "Filling"), legend(RED, "Near full"),
            new Region(){{ setPrefWidth(10);}}, statusPill());
        return t;
    }
    private HBox legend(String c, String label) {
        HBox h = new HBox(6);
        h.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web(c));
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        h.getChildren().addAll(dot, l);
        HBox.setMargin(h, new Insets(0, 8, 0, 0));
        return h;
    }
    private HBox statusPill() {
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(8, 13, 8, 13));
        h.setStyle("-fx-background-color:#11261c;-fx-background-radius:8;-fx-border-color:#1f7a52;-fx-border-radius:8;");
        Circle c = new Circle(5, Color.web(GREEN));
        Label l = new Label("4 / 4 parks online");
        l.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:12px;-fx-font-weight:bold;");
        h.getChildren().addAll(c, l);
        return h;
    }

    private StackPane mapArea() {
        mapPane = new Pane();
        mapPane.setStyle("-fx-background-color:radial-gradient(center 45% 40%, radius 70%, #122031, #0a0e13);");

        // faint grid
        for (int i = 1; i < 12; i++) {
            Line v = new Line(); v.setStartX(i * 90); v.setEndX(i * 90); v.setStartY(0); v.setEndY(2000);
            v.setStroke(Color.web("#ffffff", 0.03));
            Line h = new Line(); h.setStartX(0); h.setEndX(2000); h.setStartY(i * 70); h.setEndY(i * 70);
            h.setStroke(Color.web("#ffffff", 0.03));
            mapPane.getChildren().addAll(v, h);
        }
        // stylised landmass
        Region land = new Region();
        land.setStyle("-fx-background-color:linear-gradient(to bottom,#13202c,#0e1822);-fx-background-radius:120 90 60 140;-fx-border-color:rgba(63,182,255,0.18);-fx-border-radius:120 90 60 140;-fx-border-width:1.5;");
        land.prefWidthProperty().bind(mapPane.widthProperty().multiply(0.46));
        land.prefHeightProperty().bind(mapPane.heightProperty().multiply(0.86));
        land.layoutXProperty().bind(mapPane.widthProperty().multiply(0.27));
        land.layoutYProperty().bind(mapPane.heightProperty().multiply(0.07));
        mapPane.getChildren().add(land);

        for (int i = 0; i < parks.length; i++) addMarker(i);

        Label hint = new Label("◍  Click a marker to inspect a park");
        hint.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        hint.layoutXProperty().bind(mapPane.widthProperty().multiply(0).add(20));
        hint.setLayoutY(18);
        mapPane.getChildren().add(hint);

        StackPane wrap = new StackPane(mapPane);
        HBox.setHgrow(wrap, Priority.ALWAYS);
        return wrap;
    }

    private void addMarker(int idx) {
        int now = (int) parks[idx][4], max = (int) parks[idx][5];
        double load = (double) now / max;
        String c = load >= 0.95 ? RED : load >= 0.75 ? AMBER : GREEN;

        Circle halo = new Circle(26, Color.web(c, 0.16));
        Circle pin = new Circle(11, Color.web(c));
        pin.setStroke(Color.web("#0a0e13")); pin.setStrokeWidth(2.5);
        Label name = new Label((String) parks[idx][0]);
        name.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:11.5px;-fx-font-weight:bold;-fx-background-color:rgba(10,14,19,0.7);-fx-background-radius:6;-fx-padding:3 8;");

        VBox marker = new VBox(5);
        marker.setAlignment(Pos.CENTER);
        StackPane head = new StackPane(halo, pin);
        marker.getChildren().addAll(head, name);
        marker.setStyle("-fx-cursor:hand;");
        marker.layoutXProperty().bind(mapPane.widthProperty().multiply((double) parks[idx][2]).subtract(50));
        marker.layoutYProperty().bind(mapPane.heightProperty().multiply((double) parks[idx][3]).subtract(26));
        marker.setOnMouseClicked(e -> selectPark(idx));
        marker.setOnMouseEntered(e -> halo.setRadius(32));
        marker.setOnMouseExited(e -> halo.setRadius(26));
        mapPane.getChildren().add(marker);
    }

    private VBox detailsPanel() {
        details.setPrefWidth(380);
        details.setPadding(new Insets(22));
        details.setStyle("-fx-background-color:" + PANEL + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 0 1;");
        return details;
    }

    private void selectPark(int idx) {
        String name = (String) parks[idx][0], region = (String) parks[idx][1];
        int now = (int) parks[idx][4], max = (int) parks[idx][5];
        double load = (double) now / max;
        String c = load >= 0.95 ? RED : load >= 0.75 ? AMBER : GREEN;
        String state = load >= 0.95 ? "Near full" : load >= 0.75 ? "Filling up" : "Open";

        details.getChildren().clear();

        VBox head = new VBox(4);
        Label tag = new Label("◉  SELECTED PARK");
        tag.setStyle("-fx-text-fill:" + CYAN + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
        Label n = new Label(name);
        n.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:20px;-fx-font-weight:bold;");
        n.setWrapText(true);
        Label r = new Label("📍 " + region);
        r.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        head.getChildren().addAll(tag, n, r);
        details.getChildren().add(head);

        // capacity gauge
        VBox cap = new VBox(10);
        cap.setPadding(new Insets(18));
        cap.setStyle(cardCss());
        HBox capTop = new HBox();
        Label cl = new Label("Live occupancy");
        cl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label chip = new Label("●  " + state);
        chip.setPadding(new Insets(3, 10, 3, 10));
        chip.setStyle("-fx-background-color:" + c + "22;-fx-text-fill:" + c + ";-fx-background-radius:20;-fx-font-size:11px;-fx-font-weight:bold;");
        capTop.getChildren().addAll(cl, g, chip);
        Label big = new Label(now + " / " + max);
        big.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:30px;-fx-font-weight:bold;");
        StackPane track = new StackPane();
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPrefHeight(10);
        track.setStyle("-fx-background-color:#0a0f15;-fx-background-radius:20;");
        Region fill = new Region();
        fill.setPrefHeight(10);
        fill.setMaxWidth(340 * load);
        fill.setStyle("-fx-background-color:linear-gradient(to right," + c + "aa," + c + ");-fx-background-radius:20;");
        track.getChildren().add(fill);
        Label pct = new Label(Math.round(load * 100) + "% of capacity · est. stay 3h");
        pct.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        cap.getChildren().addAll(capTop, big, track, pct);
        details.getChildren().add(cap);

        // entrance status
        VBox ent = new VBox(10);
        ent.setPadding(new Insets(18));
        ent.setStyle(cardCss());
        Label et = new Label("Entrance / Check-in");
        et.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        ent.getChildren().add(et);
        ent.getChildren().add(kv("Gate A", "Open · A. Mizrahi", GREEN));
        ent.getChildren().add(kv("Gate B", load >= 0.95 ? "Holding (capacity)" : "Open", load >= 0.95 ? AMBER : GREEN));
        ent.getChildren().add(kv("Walk-ins", load >= 0.75 ? "Paused" : "Accepting", load >= 0.75 ? AMBER : GREEN));
        details.getChildren().add(ent);

        // upcoming visits
        VBox up = new VBox(8);
        up.setPadding(new Insets(18));
        up.setStyle(cardCss());
        Label ut = new Label("Next arrivals");
        ut.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        up.getChildren().add(ut);
        up.getChildren().add(visit("09:00", "Yossi Avraham", "4 pax · Confirmed"));
        up.getChildren().add(visit("10:30", "Hod Tours (group)", "28 pax · Booked"));
        up.getChildren().add(visit("13:00", "Rivka Mizrahi", "3 pax · Waitlisted"));
        details.getChildren().add(up);
    }

    private HBox kv(String k, String v, String color) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        Label a = new Label(k);
        a.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label b = new Label("● " + v);
        b.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        h.getChildren().addAll(a, g, b);
        return h;
    }
    private HBox visit(String time, String who, String meta) {
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(8, 4, 8, 4));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Label t = new Label(time);
        t.setStyle("-fx-text-fill:" + CYAN + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        t.setMinWidth(46);
        VBox v = new VBox(-1);
        Label a = new Label(who);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        Label b = new Label(meta);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;");
        v.getChildren().addAll(a, b);
        h.getChildren().addAll(t, v);
        return h;
    }

    private String cardCss() {
        return "-fx-background-color:" + CARD + ";-fx-background-radius:14;-fx-border-color:" + STROKE + ";-fx-border-radius:14;";
    }

    public static void main(String[] args) { launch(args); }
}
