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
 * GoNature UI Demo 08 — ROLE-BASED CONTROL HUB
 * ------------------------------------------------------------
 * One screen, four roles. A role switcher at the top re-renders the dashboard
 * zone with role-appropriate widgets and clearly disabled/restricted actions —
 * showing the GoNature actor hierarchy and permission layers.
 *
 * Click the role tabs (Boss / Admin / Employee / Traveler) to switch.
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_08_Role_Based_Control_Hub extends Application {

    static final String BG     = "#0c1016";
    static final String PANEL  = "#12171f";
    static final String CARD   = "#171e28";
    static final String STROKE = "#252e3b";
    static final String TEXT   = "#e7eef7";
    static final String MUTED  = "#8694a6";
    static final String GREEN  = "#3fd69b";
    static final String BLUE   = "#5b8cff";
    static final String PURPLE = "#a78bfa";
    static final String AMBER  = "#f6b454";
    static final String RED    = "#ff6f6f";

    private final String[][] roles = {
        {"Boss", "Park Manager", "⛰", BLUE},
        {"Admin", "Department Manager", "🛡", PURPLE},
        {"Employee", "Service Rep / Gate", "🎟", GREEN},
        {"Traveler", "Subscriber", "🧭", AMBER},
    };
    private int active = 0;
    private HBox tabs;
    private final StackPane stageArea = new StackPane();

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(header());

        stageArea.setPadding(new Insets(24, 28, 32, 28));
        stageArea.setAlignment(Pos.TOP_LEFT);
        ScrollPane sp = new ScrollPane(stageArea);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");
        root.setCenter(sp);
        renderRole();

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Role-Based Control Hub (Demo 08)");
        stage.show();
    }

    private VBox header() {
        VBox v = new VBox(0);
        v.setStyle("-fx-background-color:" + PANEL + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        HBox top = new HBox(13);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(16, 26, 10, 26));
        StackPane logo = new StackPane(new Circle(14, Color.web(GREEN)), new Label("▲"){{ setStyle("-fx-text-fill:#06241a;-fx-font-size:11px;-fx-font-weight:bold;"); }});
        Label brand = new Label("GoNature  ·  Access Control Hub");
        brand.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:16px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label note = new Label("Single sign-on · permissions enforced server-side");
        note.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        top.getChildren().addAll(logo, brand, g, note);

        tabs = new HBox(8);
        tabs.setPadding(new Insets(4, 26, 14, 26));
        for (int i = 0; i < roles.length; i++) tabs.getChildren().add(roleTab(i));
        v.getChildren().addAll(top, tabs);
        return v;
    }

    private HBox roleTab(int i) {
        boolean on = i == active;
        HBox t = new HBox(9);
        t.setAlignment(Pos.CENTER_LEFT);
        t.setPadding(new Insets(10, 18, 10, 14));
        String c = roles[i][3];
        t.setStyle("-fx-background-color:" + (on ? c + "22" : "transparent") + ";-fx-background-radius:10;-fx-border-color:"
                + (on ? c + "77" : STROKE) + ";-fx-border-radius:10;-fx-cursor:hand;");
        Label ic = new Label(roles[i][2]);
        ic.setStyle("-fx-font-size:16px;");
        VBox tx = new VBox(-2);
        Label a = new Label(roles[i][0]);
        a.setStyle("-fx-text-fill:" + (on ? TEXT : MUTED) + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        Label b = new Label(roles[i][1]);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;");
        tx.getChildren().addAll(a, b);
        t.getChildren().addAll(ic, tx);
        final int idx = i;
        t.setOnMouseClicked(e -> { active = idx; refreshTabs(); renderRole(); });
        return t;
    }
    private void refreshTabs() {
        tabs.getChildren().clear();
        for (int i = 0; i < roles.length; i++) tabs.getChildren().add(roleTab(i));
    }

    private void renderRole() {
        switch (active) {
            case 0:  stageArea.getChildren().setAll(bossView()); break;
            case 1:  stageArea.getChildren().setAll(adminView()); break;
            case 2:  stageArea.getChildren().setAll(employeeView()); break;
            default: stageArea.getChildren().setAll(travelerView());
        }
    }

    private VBox shell(String who, String desc, String color) {
        VBox v = new VBox(20);
        VBox head = new VBox(3);
        Label a = new Label(who);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:22px;-fx-font-weight:bold;");
        Label b = new Label(desc);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        head.getChildren().addAll(a, b);
        v.getChildren().add(head);
        return v;
    }

    /* ---------------- BOSS (Park Manager) ---------------- */
    private VBox bossView() {
        VBox v = shell("Park Manager Workspace", "Manage your park's parameters, promotions and reports.", BLUE);
        HBox kpis = new HBox(16);
        kpis.getChildren().addAll(stat("Today's Visitors", "642 / 800", BLUE), stat("Active Discount", "15% (weekend)", GREEN), stat("Pending Requests", "2 awaiting admin", AMBER));
        for (var n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        v.getChildren().add(kpis);
        HBox row = new HBox(16);
        VBox actions = panel("Actions you can take");
        HBox.setHgrow(actions, Priority.ALWAYS);
        actions.getChildren().add(action("Update capacity parameters", "Submit to Department Manager", BLUE, true));
        actions.getChildren().add(action("Request promotional discount", "Needs admin approval", BLUE, true));
        actions.getChildren().add(action("Generate visitor-count report", "Submit to admin", BLUE, true));
        actions.getChildren().add(action("Generate usage report", "View directly", BLUE, true));
        actions.getChildren().add(action("Approve another park's request", "Department Manager only", RED, false));
        VBox info = panel("My park");
        info.setPrefWidth(330);
        info.getChildren().add(kv("Park", "Masada National Park"));
        info.getChildren().add(kv("Max capacity", "800"));
        info.getChildren().add(kv("Casual gap", "80"));
        info.getChildren().add(kv("Est. stay", "3 hours"));
        row.getChildren().addAll(actions, info);
        v.getChildren().add(row);
        return v;
    }

    /* ---------------- ADMIN (Dept Manager) ---------------- */
    private VBox adminView() {
        VBox v = shell("Department Manager Workspace", "Approve requests, view all reports, manage connected users.", PURPLE);
        HBox kpis = new HBox(16);
        kpis.getChildren().addAll(stat("Pending Approvals", "7", AMBER), stat("Connected Users", "14 online", GREEN), stat("Parks Supervised", "4", PURPLE));
        for (var n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        v.getChildren().add(kpis);
        HBox row = new HBox(16);
        VBox approvals = panel("Approval queue");
        HBox.setHgrow(approvals, Priority.ALWAYS);
        approvals.getChildren().add(approval("Masada · capacity 800 → 850", "Requested by D. Cohen", PURPLE));
        approvals.getChildren().add(approval("Timna · 15% weekend discount", "Promotion request", PURPLE));
        approvals.getChildren().add(approval("Caesarea · casual gap 60 → 70", "Parameter request", PURPLE));
        VBox actions = panel("Privileges");
        actions.setPrefWidth(330);
        actions.getChildren().add(action("Approve / deny requests", "Across all parks", PURPLE, true));
        actions.getChildren().add(action("View all 4 report types", "Visit, Cancellations, Count, Usage", PURPLE, true));
        actions.getChildren().add(action("Force-disconnect a user", "Connected users panel", PURPLE, true));
        actions.getChildren().add(action("Edit a park's daily bookings", "Park staff only", RED, false));
        row.getChildren().addAll(approvals, actions);
        v.getChildren().add(row);
        return v;
    }

    /* ---------------- EMPLOYEE (Service / Gate) ---------------- */
    private VBox employeeView() {
        VBox v = shell("Employee Workspace", "Register subscribers, look people up, and run the gate.", GREEN);
        HBox kpis = new HBox(16);
        kpis.getChildren().addAll(stat("Checked in today", "318", GREEN), stat("New subscribers", "12", BLUE), stat("Walk-ins", "44", AMBER));
        for (var n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        v.getChildren().add(kpis);
        HBox row = new HBox(16);
        VBox actions = panel("Daily tasks");
        HBox.setHgrow(actions, Priority.ALWAYS);
        actions.getChildren().add(action("Register a new subscriber", "Service desk", GREEN, true));
        actions.getChildren().add(action("Look up / edit a subscriber", "Service desk", GREEN, true));
        actions.getChildren().add(action("Admit a reservation at the gate", "Scan order ID", GREEN, true));
        actions.getChildren().add(action("Process a walk-in entry", "Gate terminal", GREEN, true));
        actions.getChildren().add(action("Change park capacity", "Park Manager only", RED, false));
        actions.getChildren().add(action("Approve promotions", "Department Manager only", RED, false));
        VBox prof = panel("My profile");
        prof.setPrefWidth(330);
        prof.getChildren().add(kv("Name", "Noa Friedman"));
        prof.getChildren().add(kv("Role", "Service Representative"));
        prof.getChildren().add(kv("Assigned park", "Caesarea"));
        row.getChildren().addAll(actions, prof);
        v.getChildren().add(row);
        return v;
    }

    /* ---------------- TRAVELER (Subscriber) ---------------- */
    private VBox travelerView() {
        VBox v = shell("Traveler Workspace", "Book visits, manage your profile, and track reservations.", AMBER);
        HBox kpis = new HBox(16);
        kpis.getChildren().addAll(stat("Upcoming visits", "3", AMBER), stat("Member discount", "10% active", GREEN), stat("Family size", "6", BLUE));
        for (var n : kpis.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        v.getChildren().add(kpis);
        HBox row = new HBox(16);
        VBox actions = panel("What you can do");
        HBox.setHgrow(actions, Priority.ALWAYS);
        actions.getChildren().add(action("Book a new visit", "Any park", AMBER, true));
        actions.getChildren().add(action("Edit my booking", "While status is 'Booked'", AMBER, true));
        actions.getChildren().add(action("Update my profile", "Name, email, family size", AMBER, true));
        actions.getChildren().add(action("Join a waitlist", "When a park is full", AMBER, true));
        actions.getChildren().add(action("View other visitors' bookings", "Not permitted", RED, false));
        actions.getChildren().add(action("Access staff reports", "Staff only", RED, false));
        VBox prof = panel("My membership");
        prof.setPrefWidth(330);
        prof.getChildren().add(kv("Name", "Tamar Shapira"));
        prof.getChildren().add(kv("Subscriber ID", "20419"));
        prof.getChildren().add(kv("Tier", "Guide ✓"));
        row.getChildren().addAll(actions, prof);
        v.getChildren().add(row);
        return v;
    }

    /* ---------------- shared widgets ---------------- */
    private VBox panel(String title) {
        VBox v = new VBox(10);
        v.setPadding(new Insets(20));
        v.setStyle(cardCss());
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14.5px;-fx-font-weight:bold;");
        VBox.setMargin(t, new Insets(0, 0, 4, 0));
        v.getChildren().add(t);
        return v;
    }
    private VBox stat(String label, String value, String color) {
        VBox v = new VBox(6);
        v.setPadding(new Insets(16, 18, 16, 18));
        v.setStyle(cardCss());
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + color + ";-fx-font-size:21px;-fx-font-weight:bold;");
        v.getChildren().addAll(l, val);
        return v;
    }
    private HBox action(String title, String sub, String color, boolean allowed) {
        HBox h = new HBox(13);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(13, 15, 13, 15));
        h.setStyle("-fx-background-color:" + (allowed ? "#1b2330" : "#221619") + ";-fx-background-radius:11;-fx-border-color:"
                + (allowed ? STROKE : "#5a2a2e") + ";-fx-border-radius:11;" + (allowed ? "-fx-cursor:hand;" : ""));
        StackPane ic = new StackPane(new Circle(15, Color.web(allowed ? color : RED, 0.16)),
                new Label(allowed ? "✓" : "🔒"){{ setStyle("-fx-font-size:12px;-fx-text-fill:" + (allowed ? color : RED) + ";-fx-font-weight:bold;"); }});
        VBox t = new VBox(-1);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + (allowed ? TEXT : "#9b8086") + ";-fx-font-size:13.5px;-fx-font-weight:bold;" + (allowed ? "" : "-fx-strikethrough:true;"));
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        t.getChildren().addAll(a, b);
        HBox.setHgrow(t, Priority.ALWAYS);
        Label tag = new Label(allowed ? "Allowed" : "Restricted");
        tag.setPadding(new Insets(4, 11, 4, 11));
        tag.setStyle("-fx-background-color:" + (allowed ? GREEN : RED) + "22;-fx-text-fill:" + (allowed ? GREEN : RED)
                + ";-fx-background-radius:20;-fx-font-size:10.5px;-fx-font-weight:bold;");
        h.getChildren().addAll(ic, t, tag);
        return h;
    }
    private HBox approval(String title, String sub, String color) {
        HBox h = new HBox(13);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(12, 14, 12, 14));
        h.setStyle("-fx-background-color:#1b2330;-fx-background-radius:11;-fx-border-color:" + STROKE + ";-fx-border-radius:11;");
        VBox t = new VBox(-1);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        t.getChildren().addAll(a, b);
        HBox.setHgrow(t, Priority.ALWAYS);
        Label ok = new Label("Approve");
        ok.setPadding(new Insets(7, 14, 7, 14));
        ok.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#06231a;-fx-background-radius:8;-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;");
        Label no = new Label("Deny");
        no.setPadding(new Insets(7, 14, 7, 14));
        no.setStyle("-fx-background-color:transparent;-fx-text-fill:" + RED + ";-fx-border-color:" + RED + "66;-fx-border-radius:8;-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;");
        HBox.setMargin(no, new Insets(0, 0, 0, 8));
        h.getChildren().addAll(t, ok, no);
        return h;
    }
    private HBox kv(String k, String v) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(9, 2, 9, 2));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Label a = new Label(k);
        a.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label b = new Label(v);
        b.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        h.getChildren().addAll(a, g, b);
        return h;
    }
    private String cardCss() {
        return "-fx-background-color:" + CARD + ";-fx-background-radius:14;-fx-border-color:" + STROKE
             + ";-fx-border-radius:14;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.3), 14, 0, 0, 5);";
    }

    public static void main(String[] args) { launch(args); }
}
