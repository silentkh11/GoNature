import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * GoNature UI Demo 10 — FUTURE WEB APP DESKTOP
 * ------------------------------------------------------------
 * A modern web / SaaS look recreated in JavaFX — what GoNature could become if it
 * went web-based. Top navbar with page routing (Dashboard / Reports / Settings),
 * responsive-style card grid, search, and a right-hand booking drawer.
 *
 * Click navbar items to switch pages; "New booking" slides in a drawer.
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_10_Future_Web_App_Desktop extends Application {

    static final String BG     = "#0e1117";
    static final String CARD   = "#161b23";
    static final String SOFT   = "#1d2530";
    static final String STROKE = "#272f3b";
    static final String TEXT   = "#eef2f7";
    static final String MUTED  = "#8b96a7";
    static final String ACCENT = "#6d8bff";
    static final String GREEN  = "#42d6a0";
    static final String PINK   = "#ff7eb6";
    static final String AMBER  = "#f7b955";

    private final String[] pages = {"Dashboard", "Bookings", "Reports", "Settings"};
    private int page = 0;
    private HBox navItems;
    private final StackPane pageHost = new StackPane();
    private StackPane drawerLayer;
    private StackPane rootStack;

    @Override
    public void start(Stage stage) {
        rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color:" + BG + ";");
        BorderPane main = new BorderPane();
        main.setTop(navbar());
        pageHost.setPadding(new Insets(26, 34, 34, 34));
        pageHost.setAlignment(Pos.TOP_LEFT);
        ScrollPane sp = new ScrollPane(pageHost);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");
        main.setCenter(sp);
        rootStack.getChildren().add(main);
        renderPage();

        Scene scene = new Scene(rootStack, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Future Web App (Demo 10)");
        stage.show();
    }

    private HBox navbar() {
        HBox h = new HBox(0);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 30, 14, 24));
        h.setStyle("-fx-background-color:" + CARD + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        StackPane logo = new StackPane(new Circle(15, Color.web(GREEN)), new Label("▲"){{ setStyle("-fx-text-fill:#06241a;-fx-font-size:12px;-fx-font-weight:bold;"); }});
        Label brand = new Label("GoNature");
        brand.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-weight:bold;-fx-padding:0 0 0 11;");
        navItems = new HBox(4);
        navItems.setAlignment(Pos.CENTER_LEFT);
        navItems.setPadding(new Insets(0, 0, 0, 30));
        for (int i = 0; i < pages.length; i++) navItems.getChildren().add(navItem(i));
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label search = new Label("🔍   Search…");
        search.setPrefWidth(220);
        search.setPadding(new Insets(9, 14, 9, 14));
        search.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:9;-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;-fx-border-color:" + STROKE + ";-fx-border-radius:9;");
        Label create = new Label("+  New booking");
        create.setPadding(new Insets(9, 18, 9, 18));
        create.setStyle("-fx-background-color:" + ACCENT + ";-fx-text-fill:#0a1124;-fx-background-radius:9;-fx-font-weight:bold;-fx-font-size:12.5px;-fx-cursor:hand;");
        create.setOnMouseClicked(e -> openDrawer());
        HBox.setMargin(create, new Insets(0, 14, 0, 12));
        StackPane avatar = new StackPane(new Circle(18, Color.web("#23304f")), new Label("MA"){{ setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-weight:bold;-fx-font-size:12px;"); }});
        h.getChildren().addAll(logo, brand, navItems, g, search, create, avatar);
        return h;
    }
    private Label navItem(int i) {
        boolean on = i == page;
        Label l = new Label(pages[i]);
        l.setPadding(new Insets(9, 16, 9, 16));
        l.setStyle("-fx-background-radius:9;-fx-cursor:hand;-fx-font-size:13.5px;-fx-text-fill:" + (on ? TEXT : MUTED) + ";"
                + (on ? "-fx-background-color:" + SOFT + ";-fx-font-weight:bold;" : ""));
        final int idx = i;
        l.setOnMouseClicked(e -> { page = idx; refreshNav(); renderPage(); });
        return l;
    }
    private void refreshNav() {
        navItems.getChildren().clear();
        for (int i = 0; i < pages.length; i++) navItems.getChildren().add(navItem(i));
    }

    private void renderPage() {
        switch (page) {
            case 0:  pageHost.getChildren().setAll(dashboardPage()); break;
            case 1:  pageHost.getChildren().setAll(bookingsPage()); break;
            case 2:  pageHost.getChildren().setAll(reportsPage()); break;
            default: pageHost.getChildren().setAll(settingsPage());
        }
    }

    /* ---------------- DASHBOARD ---------------- */
    private VBox dashboardPage() {
        VBox v = new VBox(22);
        v.getChildren().add(pageHead("Welcome back, Maya", "Here's what's happening across your parks today."));
        HBox kpis = new HBox(18);
        kpis.getChildren().addAll(
            statCard("Today's bookings", "342", "+18", GREEN),
            statCard("Visitors on-site", "1,284", "68% cap", ACCENT),
            statCard("Pending approvals", "7", "review", AMBER),
            statCard("Revenue (today)", "₪94.2k", "+9%", PINK));
        for (var n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        v.getChildren().add(kpis);
        Label gt = sectionLabel("Parks");
        v.getChildren().add(gt);
        FlowPane grid = new FlowPane(18, 18);
        grid.getChildren().addAll(
            parkCard("Masada National Park", 642, 800, GREEN),
            parkCard("Caesarea National Park", 518, 600, AMBER),
            parkCard("Timna Valley Park", 121, 400, GREEN),
            parkCard("Nahal Ayun Nature Reserve", 196, 200, AMBER));
        v.getChildren().add(grid);
        return v;
    }
    private VBox parkCard(String name, int now, int max, String color) {
        double load = (double) now / max;
        VBox c = new VBox(12);
        c.setPrefWidth(280);
        c.setPadding(new Insets(20));
        c.setStyle(cardCss() + "-fx-cursor:hand;");
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        StackPane ic = new StackPane(new Circle(18, Color.web(color, 0.16)), new Label("⛰"){{ setStyle("-fx-font-size:16px;"); }});
        VBox t = new VBox(-1);
        Label a = new Label(name);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14px;-fx-font-weight:bold;");
        a.setWrapText(true);
        Label b = new Label(Math.round(load * 100) + "% occupancy");
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        t.getChildren().addAll(a, b);
        top.getChildren().addAll(ic, t);
        StackPane track = new StackPane();
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPrefHeight(8);
        track.setStyle("-fx-background-color:#0c1118;-fx-background-radius:20;");
        Region fill = new Region();
        fill.setPrefHeight(8);
        fill.setMaxWidth(240 * load);
        fill.setStyle("-fx-background-color:" + color + ";-fx-background-radius:20;");
        track.getChildren().add(fill);
        Label foot = new Label(now + " / " + max + " visitors");
        foot.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        c.getChildren().addAll(top, track, foot);
        return c;
    }

    /* ---------------- BOOKINGS ---------------- */
    private VBox bookingsPage() {
        VBox v = new VBox(20);
        v.getChildren().add(pageHead("Bookings", "All reservations across every park."));
        HBox filters = new HBox(10);
        filters.getChildren().addAll(filterChip("All statuses", true), filterChip("Confirmed", false), filterChip("Pending", false), filterChip("Waitlisted", false));
        v.getChildren().add(filters);
        VBox table = new VBox(0);
        table.setPadding(new Insets(8, 20, 14, 20));
        table.setStyle(cardCss());
        table.getChildren().add(bkHead());
        table.getChildren().add(bkRow("#10428", "Yossi Avraham", "Masada", "24 Jun · 09:00", "Confirmed", GREEN));
        table.getChildren().add(bkRow("#10429", "Tamar Shapira", "Caesarea", "24 Jun · 10:30", "Pending Confirm", AMBER));
        table.getChildren().add(bkRow("#10430", "Eitan Barak", "Timna Valley", "24 Jun · 11:00", "Confirmed", GREEN));
        table.getChildren().add(bkRow("#10431", "Rivka Mizrahi", "Nahal Ayun", "24 Jun · 13:00", "Waitlisted", ACCENT));
        table.getChildren().add(bkRow("#10432", "Hod Tours (group)", "Masada", "25 Jun · 08:30", "Booked", MUTED));
        v.getChildren().add(table);
        return v;
    }
    private HBox bkHead() {
        HBox h = new HBox();
        h.setPadding(new Insets(8, 6, 12, 6));
        String[] c = {"ORDER", "VISITOR", "PARK", "WHEN", "STATUS"};
        double[] w = {90, 220, 160, 170, 150};
        for (int i = 0; i < c.length; i++) { Label l = new Label(c[i]); l.setPrefWidth(w[i]); l.setStyle("-fx-text-fill:#5d6776;-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;"); h.getChildren().add(l); }
        return h;
    }
    private HBox bkRow(String id, String who, String park, String when, String status, String color) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(13, 6, 13, 6));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;-fx-cursor:hand;");
        h.setOnMouseEntered(e -> h.setStyle("-fx-background-color:#10161f;-fx-background-radius:8;-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;-fx-cursor:hand;"));
        h.setOnMouseExited(e -> h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;-fx-cursor:hand;"));
        h.getChildren().add(bcell(id, 90, ACCENT, true));
        h.getChildren().add(bcell(who, 220, TEXT, false));
        h.getChildren().add(bcell(park, 160, MUTED, false));
        h.getChildren().add(bcell(when, 170, MUTED, false));
        Label chip = new Label(status);
        chip.setPadding(new Insets(4, 11, 4, 11));
        chip.setStyle("-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";-fx-background-radius:20;-fx-font-size:11.5px;-fx-font-weight:bold;");
        HBox cw = new HBox(chip); cw.setPrefWidth(150);
        h.getChildren().add(cw);
        return h;
    }
    private Label bcell(String t, double w, String c, boolean bold) {
        Label l = new Label(t); l.setPrefWidth(w);
        l.setStyle("-fx-text-fill:" + c + ";-fx-font-size:13px;" + (bold ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    /* ---------------- REPORTS ---------------- */
    private VBox reportsPage() {
        VBox v = new VBox(20);
        v.getChildren().add(pageHead("Reports", "Generate and download operational reports."));
        FlowPane grid = new FlowPane(18, 18);
        grid.getChildren().addAll(
            reportCard("Visit Report", "Stay times & entry-hour distribution by visitor type.", GREEN),
            reportCard("Cancellations Report", "Cancellations & no-shows, distributed by day.", PINK),
            reportCard("Visitor-Count Report", "Monthly totals per park, submitted to admin.", ACCENT),
            reportCard("Usage Report", "Daily capacity utilisation — full & below-capacity days.", AMBER));
        v.getChildren().add(grid);
        return v;
    }
    private VBox reportCard(String title, String desc, String color) {
        VBox c = new VBox(12);
        c.setPrefWidth(290);
        c.setPadding(new Insets(22));
        c.setStyle(cardCss());
        StackPane ic = new StackPane(new Circle(20, Color.web(color, 0.16)), new Label("📄"){{ setStyle("-fx-font-size:17px;"); }});
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:15.5px;-fx-font-weight:bold;");
        Label d = new Label(desc);
        d.setWrapText(true);
        d.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        HBox btns = new HBox(10);
        Label gen = new Label("Generate");
        gen.setPadding(new Insets(9, 18, 9, 18));
        gen.setStyle("-fx-background-color:" + color + ";-fx-text-fill:#0a1019;-fx-background-radius:8;-fx-font-weight:bold;-fx-font-size:12.5px;-fx-cursor:hand;");
        Label dl = new Label("⬇  PDF");
        dl.setPadding(new Insets(9, 16, 9, 16));
        dl.setStyle("-fx-background-color:" + SOFT + ";-fx-text-fill:" + TEXT + ";-fx-background-radius:8;-fx-border-color:" + STROKE + ";-fx-border-radius:8;-fx-font-size:12.5px;-fx-cursor:hand;");
        btns.getChildren().addAll(gen, dl);
        c.getChildren().addAll(ic, t, d, btns);
        return c;
    }

    /* ---------------- SETTINGS ---------------- */
    private VBox settingsPage() {
        VBox v = new VBox(20);
        v.getChildren().add(pageHead("Settings", "Manage your profile and workspace preferences."));
        HBox row = new HBox(18);
        VBox profile = new VBox(16);
        profile.setPadding(new Insets(24));
        profile.setStyle(cardCss());
        HBox.setHgrow(profile, Priority.ALWAYS);
        Label pt = new Label("Profile");
        pt.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:15.5px;-fx-font-weight:bold;");
        HBox who = new HBox(16);
        who.setAlignment(Pos.CENTER_LEFT);
        StackPane av = new StackPane(new Circle(30, Color.web("#23304f")), new Label("MA"){{ setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-weight:bold;-fx-font-size:18px;"); }});
        VBox wt = new VBox(-1);
        Label n = new Label("Maya Levi");
        n.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:17px;-fx-font-weight:bold;");
        Label r = new Label("Department Manager · GoNature HQ");
        r.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        wt.getChildren().addAll(n, r);
        who.getChildren().addAll(av, wt);
        profile.getChildren().addAll(pt, who, field("Full name", "Maya Levi"), field("Email", "maya.levi@gonature.gov.il"), field("Role", "Department Manager"));

        VBox prefs = new VBox(14);
        prefs.setPrefWidth(360);
        prefs.setPadding(new Insets(24));
        prefs.setStyle(cardCss());
        Label prt = new Label("Preferences");
        prt.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:15.5px;-fx-font-weight:bold;");
        prefs.getChildren().addAll(prt, toggle("Dark mode", true), toggle("Email notifications", true), toggle("Real-time park alerts", true), toggle("Weekly report digest", false));
        row.getChildren().addAll(profile, prefs);
        v.getChildren().add(row);
        return v;
    }
    private HBox toggle(String label, boolean on) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(11, 4, 11, 4));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        StackPane sw = new StackPane();
        sw.setPrefSize(44, 24);
        sw.setStyle("-fx-background-color:" + (on ? GREEN : "#33404f") + ";-fx-background-radius:20;");
        Circle knob = new Circle(9, Color.WHITE);
        StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        StackPane.setMargin(knob, new Insets(0, 4, 0, 4));
        sw.getChildren().add(knob);
        h.getChildren().addAll(l, g, sw);
        return h;
    }

    /* ---------------- booking drawer ---------------- */
    private void openDrawer() {
        if (drawerLayer != null) return;
        drawerLayer = new StackPane();
        drawerLayer.setStyle("-fx-background-color:rgba(4,6,10,0.55);");
        VBox drawer = new VBox(16);
        drawer.setPrefWidth(420);
        drawer.setMaxWidth(420);
        drawer.setPadding(new Insets(28));
        drawer.setStyle("-fx-background-color:" + CARD + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 0 1;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, -6, 0);");
        StackPane.setAlignment(drawer, Pos.CENTER_RIGHT);
        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("New booking");
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:20px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label x = new Label("✕");
        x.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:16px;-fx-cursor:hand;-fx-padding:2 8;");
        x.setOnMouseClicked(e -> closeDrawer());
        head.getChildren().addAll(t, g, x);
        drawer.getChildren().addAll(head,
            field("Park", "Caesarea National Park"),
            field("Visit date", "25 June 2026"),
            field("Time slot", "09:00"),
            field("Visitors", "4"),
            field("Subscriber ID", "20517"));
        HBox note = new HBox(10);
        note.setAlignment(Pos.CENTER_LEFT);
        note.setPadding(new Insets(12, 14, 12, 14));
        note.setStyle("-fx-background-color:" + GREEN + "1a;-fx-background-radius:10;-fx-border-color:" + GREEN + "44;-fx-border-radius:10;");
        Label ni = new Label("✓  Capacity OK · total ₪243.00 (subscriber −10%)");
        ni.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        ni.setWrapText(true);
        note.getChildren().add(ni);
        Label submit = new Label("Create booking");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setAlignment(Pos.CENTER);
        submit.setPadding(new Insets(14));
        submit.setStyle("-fx-background-color:" + ACCENT + ";-fx-text-fill:#0a1124;-fx-background-radius:10;-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;");
        submit.setOnMouseClicked(e -> closeDrawer());
        Region sgrow = new Region(); VBox.setVgrow(sgrow, Priority.ALWAYS);
        drawer.getChildren().addAll(note, sgrow, submit);
        drawerLayer.getChildren().add(drawer);
        drawerLayer.setOnMouseClicked(e -> { if (e.getTarget() == drawerLayer) closeDrawer(); });
        rootStack.getChildren().add(drawerLayer);
    }
    private void closeDrawer() { rootStack.getChildren().remove(drawerLayer); drawerLayer = null; }

    /* ---------------- helpers ---------------- */
    private VBox pageHead(String title, String sub) {
        VBox v = new VBox(4);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:25px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13.5px;");
        v.getChildren().addAll(a, b);
        return v;
    }
    private Label sectionLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:17px;-fx-font-weight:bold;");
        return l;
    }
    private VBox statCard(String label, String value, String delta, String color) {
        VBox v = new VBox(6);
        v.setPadding(new Insets(18));
        v.setStyle(cardCss());
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:27px;-fx-font-weight:bold;");
        Label d = new Label(delta);
        d.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11.5px;-fx-font-weight:bold;");
        v.getChildren().addAll(l, val, d);
        return v;
    }
    private Label filterChip(String text, boolean active) {
        Label l = new Label(text);
        l.setPadding(new Insets(8, 16, 8, 16));
        l.setStyle("-fx-background-color:" + (active ? ACCENT + "22" : SOFT) + ";-fx-text-fill:" + (active ? ACCENT : MUTED)
                + ";-fx-background-radius:20;-fx-font-size:12.5px;-fx-cursor:hand;-fx-border-color:" + (active ? ACCENT + "55" : STROKE) + ";-fx-border-radius:20;" + (active ? "-fx-font-weight:bold;" : ""));
        return l;
    }
    private VBox field(String label, String value) {
        VBox v = new VBox(5);
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label box = new Label(value);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(11, 14, 11, 14));
        box.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:9;-fx-border-color:" + STROKE + ";-fx-border-radius:9;-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;");
        v.getChildren().addAll(l, box);
        return v;
    }
    private String cardCss() {
        return "-fx-background-color:" + CARD + ";-fx-background-radius:14;-fx-border-color:" + STROKE
             + ";-fx-border-radius:14;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.3), 14, 0, 0, 5);";
    }

    public static void main(String[] args) { launch(args); }
}
