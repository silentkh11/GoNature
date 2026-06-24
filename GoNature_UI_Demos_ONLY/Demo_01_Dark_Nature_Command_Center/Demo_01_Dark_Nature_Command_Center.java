import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * GoNature UI Demo 01 — DARK NATURE COMMAND CENTER
 * ------------------------------------------------------------
 * A premium operational dashboard for the Park Manager / Dept Manager.
 * Sidebar navigation + KPI row + live park-occupancy cards + reservations
 * table + alerts panel + reports preview.
 *
 * DEMO-ONLY. Self-contained. No DB, no network, no original-project imports.
 * Run: java --module-path <javafx>/lib --add-modules javafx.controls Demo_01_Dark_Nature_Command_Center.java
 */
public class Demo_01_Dark_Nature_Command_Center extends Application {

    /* ---- palette ---- */
    static final String BG       = "#0e1116";
    static final String PANEL    = "#161b22";
    static final String CARD     = "#1b222c";
    static final String STROKE   = "#262e3a";
    static final String TEXT     = "#e6edf3";
    static final String MUTED    = "#8b97a7";
    static final String ACCENT   = "#3ddc97"; // premium nature green
    static final String CYAN     = "#34c3ff";
    static final String AMBER    = "#f5b14c";
    static final String RED      = "#ff6b6b";

    private final StackPane contentHost = new StackPane();
    private Label activeNav;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setLeft(buildSidebar());
        root.setTop(buildTopBar());

        contentHost.setPadding(new Insets(24));
        ScrollPane sp = new ScrollPane(contentHost);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");
        root.setCenter(sp);

        showDashboard();

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Command Center (Demo 01)");
        stage.show();
    }

    /* =====================================================  SIDEBAR  */
    private VBox buildSidebar() {
        VBox bar = new VBox(6);
        bar.setPrefWidth(232);
        bar.setPadding(new Insets(22, 14, 22, 14));
        bar.setStyle("-fx-background-color:" + PANEL + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 1 0 0;");

        HBox brand = new HBox(11);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 0, 22, 6));
        Circle dot = new Circle(15, Color.web(ACCENT));
        Label leaf = new Label("▲");
        leaf.setStyle("-fx-text-fill:#0e1116;-fx-font-size:13px;-fx-font-weight:bold;");
        StackPane logo = new StackPane(dot, leaf);
        VBox bt = new VBox(-2);
        Label b1 = new Label("GoNature");
        b1.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:17px;-fx-font-weight:bold;");
        Label b2 = new Label("Operations Suite");
        b2.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;");
        bt.getChildren().addAll(b1, b2);
        brand.getChildren().addAll(logo, bt);

        bar.getChildren().add(brand);
        bar.getChildren().add(navSection("OVERVIEW"));
        bar.getChildren().add(navItem("📊  Dashboard", true));
        bar.getChildren().add(navItem("🏞  Park Occupancy", false));
        bar.getChildren().add(navItem("📅  Reservations", false));
        bar.getChildren().add(navSection("MANAGEMENT"));
        bar.getChildren().add(navItem("⚠  Alerts", false));
        bar.getChildren().add(navItem("📈  Reports", false));
        bar.getChildren().add(navItem("🎟  Promotions", false));

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        bar.getChildren().add(grow);

        // server status pill
        HBox srv = new HBox(9);
        srv.setAlignment(Pos.CENTER_LEFT);
        srv.setPadding(new Insets(11, 13, 11, 13));
        srv.setStyle("-fx-background-color:#11261c;-fx-background-radius:9;-fx-border-color:#1f7a52;-fx-border-radius:9;");
        Circle live = new Circle(5, Color.web(ACCENT));
        VBox st = new VBox(-1);
        Label s1 = new Label("Server Online");
        s1.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12px;-fx-font-weight:bold;");
        Label s2 = new Label("OCSF · port 5555 · 14 clients");
        s2.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10px;");
        st.getChildren().addAll(s1, s2);
        srv.getChildren().addAll(live, st);
        bar.getChildren().add(srv);
        return bar;
    }

    private Label navSection(String t) {
        Label l = new Label(t);
        l.setPadding(new Insets(16, 0, 6, 8));
        l.setStyle("-fx-text-fill:#5d6776;-fx-font-size:10px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
        return l;
    }

    private Label navItem(String text, boolean active) {
        Label l = new Label(text);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setPadding(new Insets(11, 14, 11, 14));
        styleNav(l, active);
        if (active) activeNav = l;
        l.setOnMouseClicked(e -> {
            if (activeNav != null) styleNav(activeNav, false);
            styleNav(l, true);
            activeNav = l;
            // every nav item routes to the dashboard view in this demo
            showDashboard();
        });
        l.setOnMouseEntered(e -> { if (l != activeNav) l.setStyle(navCss("#1e2632", TEXT, false)); });
        l.setOnMouseExited(e -> { if (l != activeNav) styleNav(l, false); });
        return l;
    }
    private void styleNav(Label l, boolean active) { l.setStyle(navCss(active ? "#10271d" : "transparent", active ? ACCENT : MUTED, active)); }
    private String navCss(String bg, String fg, boolean active) {
        return "-fx-background-color:" + bg + ";-fx-background-radius:9;-fx-text-fill:" + fg
             + ";-fx-font-size:13.5px;-fx-cursor:hand;" + (active ? "-fx-font-weight:bold;" : "");
    }

    /* =====================================================  TOP BAR  */
    private HBox buildTopBar() {
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(18, 26, 18, 26));
        top.setStyle("-fx-background-color:" + BG + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");

        VBox title = new VBox(2);
        Label h = new Label("Operations Dashboard");
        h.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:21px;-fx-font-weight:bold;");
        Label sub = new Label("Tuesday, 24 June 2026  ·  All parks nominal");
        sub.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        title.getChildren().addAll(h, sub);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        Label search = new Label("🔍   Search reservations, visitors, parks…");
        search.setPrefWidth(330);
        search.setPadding(new Insets(10, 14, 10, 14));
        search.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:9;-fx-border-color:" + STROKE
                + ";-fx-border-radius:9;-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");

        StackPane bell = pill("🔔", AMBER);
        StackPane avatar = new StackPane();
        Circle a = new Circle(19, Color.web("#244"));
        Label ai = new Label("DC");
        ai.setStyle("-fx-text-fill:" + CYAN + ";-fx-font-weight:bold;-fx-font-size:13px;");
        avatar.getChildren().addAll(a, ai);

        HBox right = new HBox(14, search, bell, avatar);
        right.setAlignment(Pos.CENTER_RIGHT);
        top.getChildren().addAll(title, grow, right);
        return top;
    }
    private StackPane pill(String glyph, String color) {
        StackPane p = new StackPane();
        p.setPrefSize(40, 40);
        p.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:9;-fx-border-color:" + STROKE + ";-fx-border-radius:9;-fx-cursor:hand;");
        Label g = new Label(glyph);
        g.setStyle("-fx-font-size:15px;-fx-text-fill:" + color + ";");
        p.getChildren().add(g);
        return p;
    }

    /* =====================================================  DASHBOARD  */
    private void showDashboard() {
        VBox col = new VBox(22);

        // KPI row
        HBox kpis = new HBox(18);
        kpis.getChildren().addAll(
            kpi("Today's Visitors", "1,284", "+12.4%", ACCENT, "vs. last Tuesday"),
            kpi("Active Reservations", "342", "+5.1%", CYAN, "across 4 parks"),
            kpi("Avg. Occupancy", "68%", "Healthy", ACCENT, "live capacity"),
            kpi("Pending Approvals", "7", "Action needed", AMBER, "parameter + promo")
        );
        for (var n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        col.getChildren().add(kpis);

        // two-column: occupancy + alerts
        HBox mid = new HBox(18);
        VBox occ = card("Live Park Occupancy");
        occ.getChildren().add(occupancyRow("Masada National Park", 642, 800));
        occ.getChildren().add(occupancyRow("Caesarea National Park", 518, 600));
        occ.getChildren().add(occupancyRow("Timna Valley Park", 121, 400));
        occ.getChildren().add(occupancyRow("Nahal Ayun Nature Reserve", 196, 200));
        HBox.setHgrow(occ, Priority.ALWAYS);

        VBox alerts = card("Alerts & Notifications");
        alerts.setPrefWidth(360);
        alerts.getChildren().add(alertRow(RED,   "Nahal Ayun near capacity", "98% full · waitlist now active", "2m"));
        alerts.getChildren().add(alertRow(AMBER, "Promotion awaiting approval", "Timna Valley · 15% weekend discount", "18m"));
        alerts.getChildren().add(alertRow(CYAN,  "Capacity change requested", "Masada max 800 → 850 by D. Cohen", "1h"));
        alerts.getChildren().add(alertRow(ACCENT,"Visitor-count report submitted", "Caesarea · May 2026", "3h"));
        mid.getChildren().addAll(occ, alerts);
        col.getChildren().add(mid);

        // reservations table
        VBox table = card("Upcoming Reservations");
        table.getChildren().add(tableHeader());
        table.getChildren().add(row("#10428", "Yossi Avraham", "Masada", "24 Jun · 09:00", "4", "Confirmed", ACCENT));
        table.getChildren().add(row("#10429", "Tamar Shapira", "Caesarea", "24 Jun · 10:30", "2", "Pending Confirm", AMBER));
        table.getChildren().add(row("#10430", "Eitan Barak", "Timna Valley", "24 Jun · 11:00", "12", "Confirmed", ACCENT));
        table.getChildren().add(row("#10431", "Rivka Mizrahi", "Nahal Ayun", "24 Jun · 13:00", "3", "Waitlisted", CYAN));
        table.getChildren().add(row("#10432", "Group: Hod Tours", "Masada", "25 Jun · 08:30", "28", "Booked", MUTED));
        col.getChildren().add(table);

        contentHost.getChildren().setAll(col);
    }

    private Region kpi(String label, String value, String delta, String deltaColor, String foot) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(20));
        c.setStyle(cardCss());
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label v = new Label(value);
        v.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:30px;-fx-font-weight:bold;");
        HBox d = new HBox(8);
        d.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(delta);
        badge.setPadding(new Insets(2, 8, 2, 8));
        badge.setStyle("-fx-background-color:" + deltaColor + "22;-fx-text-fill:" + deltaColor
                + ";-fx-background-radius:20;-fx-font-size:11px;-fx-font-weight:bold;");
        Label f = new Label(foot);
        f.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;");
        d.getChildren().addAll(badge, f);
        c.getChildren().addAll(l, v, d);
        return c;
    }

    private VBox occupancyRow(String name, int now, int max) {
        double pct = (double) now / max;
        String c = pct >= 0.95 ? RED : pct >= 0.8 ? AMBER : ACCENT;
        VBox box = new VBox(7);
        box.setPadding(new Insets(12, 4, 12, 4));
        HBox head = new HBox();
        Label n = new Label(name);
        n.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label v = new Label(now + " / " + max + "   ·   " + Math.round(pct * 100) + "%");
        v.setStyle("-fx-text-fill:" + c + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        head.getChildren().addAll(n, g, v);

        StackPane track = new StackPane();
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPrefHeight(9);
        track.setStyle("-fx-background-color:#0c1118;-fx-background-radius:20;");
        Region fill = new Region();
        fill.setPrefHeight(9);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(pct));
        fill.setStyle("-fx-background-color:linear-gradient(to right," + c + "aa," + c + ");-fx-background-radius:20;");
        track.getChildren().add(fill);
        box.getChildren().addAll(head, track);
        return box;
    }

    private HBox alertRow(String color, String title, String detail, String time) {
        HBox r = new HBox(12);
        r.setPadding(new Insets(12, 6, 12, 6));
        r.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Region bar = new Region();
        bar.setPrefSize(4, 38);
        bar.setStyle("-fx-background-color:" + color + ";-fx-background-radius:4;");
        VBox t = new VBox(2);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13px;-fx-font-weight:bold;");
        Label b = new Label(detail);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        b.setWrapText(true);
        t.getChildren().addAll(a, b);
        HBox.setHgrow(t, Priority.ALWAYS);
        Label ts = new Label(time);
        ts.setStyle("-fx-text-fill:#5d6776;-fx-font-size:11px;");
        r.getChildren().addAll(bar, t, ts);
        return r;
    }

    private HBox tableHeader() {
        HBox h = new HBox();
        h.setPadding(new Insets(4, 8, 12, 8));
        String[] cols = {"ORDER", "VISITOR", "PARK", "WHEN", "PAX", "STATUS"};
        double[] w = {90, 200, 150, 170, 60, 150};
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i]);
            l.setPrefWidth(w[i]);
            l.setStyle("-fx-text-fill:#5d6776;-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
            h.getChildren().add(l);
        }
        return h;
    }

    private HBox row(String id, String visitor, String park, String when, String pax, String status, String color) {
        HBox r = new HBox();
        r.setAlignment(Pos.CENTER_LEFT);
        r.setPadding(new Insets(13, 8, 13, 8));
        r.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;-fx-cursor:hand;");
        r.setOnMouseEntered(e -> r.setStyle("-fx-background-color:#10161f;-fx-background-radius:8;-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;-fx-cursor:hand;"));
        r.setOnMouseExited(e -> r.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;-fx-cursor:hand;"));
        r.getChildren().add(cell(id, 90, CYAN, true));
        r.getChildren().add(cell(visitor, 200, TEXT, false));
        r.getChildren().add(cell(park, 150, MUTED, false));
        r.getChildren().add(cell(when, 170, MUTED, false));
        r.getChildren().add(cell(pax, 60, TEXT, false));
        Label chip = new Label(status);
        chip.setPadding(new Insets(4, 11, 4, 11));
        chip.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";-fx-background-radius:20;-fx-font-size:11.5px;-fx-font-weight:bold;");
        HBox cw = new HBox(chip); cw.setPrefWidth(150);
        r.getChildren().add(cw);
        return r;
    }
    private Label cell(String text, double w, String color, boolean bold) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:13px;" + (bold ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    /* ---- card helpers ---- */
    private VBox card(String title) {
        VBox c = new VBox(6);
        c.setPadding(new Insets(20));
        c.setStyle(cardCss());
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:15px;-fx-font-weight:bold;");
        VBox.setMargin(t, new Insets(0, 0, 8, 0));
        c.getChildren().add(t);
        return c;
    }
    private String cardCss() {
        return "-fx-background-color:" + CARD + ";-fx-background-radius:14;-fx-border-color:" + STROKE
             + ";-fx-border-radius:14;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 18, 0, 0, 6);";
    }

    public static void main(String[] args) { launch(args); }
}
