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
 * GoNature UI Demo 05 — CLEAN GOVERNMENT SERVICE
 * ------------------------------------------------------------
 * A deliberately light, official, accessibility-first public-service portal —
 * the contrast option in the set. High contrast, clear labels, simple forms,
 * service tiles, a status tracker, and a report-download view.
 *
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_05_Clean_Government_Service extends Application {

    static final String BG      = "#eef2f6";
    static final String CARD    = "#ffffff";
    static final String INK     = "#152536";
    static final String BODY    = "#41566b";
    static final String MUTED   = "#7286a0";
    static final String NAVY    = "#15487a";
    static final String GREEN   = "#1f8a5b";
    static final String AMBER   = "#b9791b";
    static final String STROKE  = "#d6dee8";

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(new VBox(govBanner(), serviceNav()));

        VBox content = new VBox(22);
        content.setPadding(new Insets(26, 40, 40, 40));
        content.setMaxWidth(1180);
        content.getChildren().addAll(hero(), serviceTiles(), formAndTracker(), reportRow());

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");
        root.setCenter(sp);

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — National Parks Service Portal (Demo 05)");
        stage.show();
    }

    private HBox govBanner() {
        HBox h = new HBox(14);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 40, 14, 40));
        h.setStyle("-fx-background-color:" + NAVY + ";");
        StackPane emblem = new StackPane(new Circle(17, Color.web("#ffffff", 0.15)), new Label("🌿"){{ setStyle("-fx-font-size:16px;"); }});
        VBox t = new VBox(-2);
        Label a = new Label("GoNature — National Parks Authority");
        a.setStyle("-fx-text-fill:white;-fx-font-size:16px;-fx-font-weight:bold;");
        Label b = new Label("Official reservation & visitor services portal");
        b.setStyle("-fx-text-fill:#bcd4ee;-fx-font-size:12px;");
        t.getChildren().addAll(a, b);
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label lang = new Label("EN ▾");
        lang.setStyle("-fx-text-fill:white;-fx-font-size:12.5px;-fx-cursor:hand;");
        Label acc = new Label("⛶  Accessibility");
        acc.setStyle("-fx-text-fill:white;-fx-font-size:12.5px;-fx-cursor:hand;-fx-padding:0 0 0 18;");
        Label signin = new Label("Sign in");
        signin.setPadding(new Insets(8, 18, 8, 18));
        signin.setStyle("-fx-background-color:white;-fx-text-fill:" + NAVY + ";-fx-background-radius:6;-fx-font-weight:bold;-fx-font-size:12.5px;-fx-cursor:hand;");
        HBox.setMargin(signin, new Insets(0, 0, 0, 18));
        h.getChildren().addAll(emblem, t, g, lang, acc, signin);
        return h;
    }

    private HBox serviceNav() {
        HBox h = new HBox(4);
        h.setPadding(new Insets(0, 40, 0, 40));
        h.setStyle("-fx-background-color:" + CARD + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        String[] items = {"Home", "Book a Visit", "My Reservations", "Membership", "Reports", "Help"};
        for (int i = 0; i < items.length; i++) {
            Label l = new Label(items[i]);
            boolean active = i == 1;
            l.setPadding(new Insets(15, 18, 15, 18));
            l.setStyle("-fx-font-size:13.5px;-fx-cursor:hand;-fx-text-fill:" + (active ? NAVY : BODY) + ";"
                    + (active ? "-fx-font-weight:bold;-fx-border-color:" + NAVY + ";-fx-border-width:0 0 3 0;" : ""));
            h.getChildren().add(l);
        }
        return h;
    }

    private VBox hero() {
        VBox v = new VBox(8);
        v.setPadding(new Insets(28, 30, 28, 30));
        v.setStyle("-fx-background-color:linear-gradient(to right,#eaf2fb,#eef7f1);-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;");
        Label a = new Label("Book your visit to Israel's national parks");
        a.setStyle("-fx-text-fill:" + INK + ";-fx-font-size:25px;-fx-font-weight:bold;");
        Label b = new Label("Reserve entry, manage your subscription, and track your bookings — all in one official place.");
        b.setStyle("-fx-text-fill:" + BODY + ";-fx-font-size:14px;");
        HBox cta = new HBox(12);
        Label primary = pill("Start a new booking", true);
        Label secondary = pill("Find my reservation", false);
        cta.getChildren().addAll(primary, secondary);
        VBox.setMargin(cta, new Insets(8, 0, 0, 0));
        v.getChildren().addAll(a, b, cta);
        return v;
    }

    private GridPane serviceTiles() {
        GridPane g = new GridPane();
        g.setHgap(18); g.setVgap(18);
        String[][] tiles = {
            {"📅", "Book a Visit", "Reserve entry for an individual, family, or group."},
            {"🎟", "Manage Membership", "Update your subscriber details and family size."},
            {"🔍", "Track a Reservation", "Check status, edit, or cancel an existing booking."},
            {"📄", "Download Reports", "Access visit and usage reports (staff)."},
        };
        for (int i = 0; i < tiles.length; i++) {
            VBox tile = new VBox(10);
            tile.setPadding(new Insets(22));
            tile.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;-fx-cursor:hand;");
            StackPane ic = new StackPane(new Circle(22, Color.web("#e7eff8")), new Label(tiles[i][0]){{ setStyle("-fx-font-size:20px;"); }});
            Label t = new Label(tiles[i][1]);
            t.setStyle("-fx-text-fill:" + INK + ";-fx-font-size:15.5px;-fx-font-weight:bold;");
            Label d = new Label(tiles[i][2]);
            d.setWrapText(true);
            d.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
            Label link = new Label("Open  →");
            link.setStyle("-fx-text-fill:" + NAVY + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
            tile.getChildren().addAll(ic, t, d, link);
            GridPane.setHgrow(tile, Priority.ALWAYS);
            tile.setMaxWidth(Double.MAX_VALUE);
            g.add(tile, i, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            g.getColumnConstraints().add(cc);
        }
        return g;
    }

    private HBox formAndTracker() {
        HBox row = new HBox(18);
        // booking form
        VBox form = sectionCard("Book a Visit", "All fields marked * are required.");
        HBox.setHgrow(form, Priority.ALWAYS);
        form.getChildren().add(field("Select park *", "Caesarea National Park"));
        HBox two = new HBox(14, field("Visit date *", "25/06/2026"), field("Time *", "09:00"));
        for (var n : two.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        form.getChildren().add(two);
        HBox two2 = new HBox(14, field("Number of visitors *", "4"), field("Subscriber ID", "20517"));
        for (var n : two2.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        form.getChildren().add(two2);
        HBox note = new HBox(10);
        note.setAlignment(Pos.CENTER_LEFT);
        note.setPadding(new Insets(12, 14, 12, 14));
        note.setStyle("-fx-background-color:#eef7f1;-fx-background-radius:8;-fx-border-color:#bfe3cf;-fx-border-radius:8;");
        Label ni = new Label("✓  Capacity available. Estimated total: ₪243.00 (subscriber rate).");
        ni.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        note.getChildren().add(ni);
        form.getChildren().add(note);
        Label submit = pill("Submit booking", true);
        VBox.setMargin(submit, new Insets(4, 0, 0, 0));
        form.getChildren().add(submit);

        // status tracker
        VBox track = sectionCard("Reservation Status", "Booking #10429 · Tamar Shapira");
        track.setPrefWidth(360);
        track.getChildren().add(trackStep("Submitted", "Booking received", true, true));
        track.getChildren().add(trackStep("Booked", "Editable until confirmed", true, true));
        track.getChildren().add(trackStep("Pending Confirm", "Confirmation link sent to email", true, false));
        track.getChildren().add(trackStep("Confirmed", "Awaiting your confirmation", false, false));
        track.getChildren().add(trackStep("Visit complete", "After your park visit", false, false));
        row.getChildren().setAll(form, track);
        return row;
    }

    private HBox trackStep(String title, String sub, boolean done, boolean line) {
        HBox h = new HBox(14);
        h.setPadding(new Insets(2, 0, 2, 0));
        VBox marker = new VBox(0);
        marker.setAlignment(Pos.TOP_CENTER);
        StackPane c = new StackPane(new Circle(11, done ? Color.web(GREEN) : Color.web("#ffffff")),
                new Label(done ? "✓" : "○"){{ setStyle("-fx-text-fill:" + (done ? "white" : MUTED) + ";-fx-font-size:11px;-fx-font-weight:bold;"); }});
        if (!done) c.getChildren().get(0).setStyle("-fx-stroke:" + STROKE + ";-fx-fill:white;");
        Region conn = new Region();
        conn.setPrefSize(2, 26);
        conn.setStyle("-fx-background-color:" + (line ? GREEN : STROKE) + ";");
        marker.getChildren().addAll(c, conn);
        VBox t = new VBox(0);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + (done ? INK : MUTED) + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        b.setWrapText(true);
        t.getChildren().addAll(a, b);
        h.getChildren().addAll(marker, t);
        return h;
    }

    private HBox reportRow() {
        VBox c = sectionCard("Available Reports", "Generated monthly · downloadable as PDF");
        HBox.setHgrow(c, Priority.ALWAYS);
        c.getChildren().add(reportLine("Visit Report — May 2026", "Stay times & entry distribution", GREEN));
        c.getChildren().add(reportLine("Cancellations Report — May 2026", "Cancellations & no-shows by day", AMBER));
        c.getChildren().add(reportLine("Usage Report — May 2026", "Daily capacity utilisation", NAVY));
        HBox row = new HBox(c);
        return row;
    }
    private HBox reportLine(String title, String sub, String color) {
        HBox h = new HBox(14);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 8, 14, 8));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        StackPane ic = new StackPane(new Circle(16, Color.web(color, 0.14)), new Label("📄"){{ setStyle("-fx-font-size:14px;"); }});
        VBox t = new VBox(-1);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + INK + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        t.getChildren().addAll(a, b);
        HBox.setHgrow(t, Priority.ALWAYS);
        Label dl = new Label("⬇  Download");
        dl.setPadding(new Insets(8, 16, 8, 16));
        dl.setStyle("-fx-background-color:#eef3f9;-fx-text-fill:" + NAVY + ";-fx-background-radius:7;-fx-font-size:12.5px;-fx-font-weight:bold;-fx-cursor:hand;");
        h.getChildren().addAll(ic, t, dl);
        return h;
    }

    /* ---- helpers ---- */
    private VBox sectionCard(String title, String sub) {
        VBox v = new VBox(13);
        v.setPadding(new Insets(22));
        v.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;");
        VBox head = new VBox(2);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + INK + ";-fx-font-size:16px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        head.getChildren().addAll(a, b);
        v.getChildren().add(head);
        return v;
    }
    private VBox field(String label, String value) {
        VBox v = new VBox(5);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + BODY + ";-fx-font-size:12px;-fx-font-weight:bold;");
        Label box = new Label(value);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(11, 14, 11, 14));
        box.setStyle("-fx-background-color:#f7f9fc;-fx-background-radius:8;-fx-border-color:" + STROKE + ";-fx-border-radius:8;-fx-text-fill:" + INK + ";-fx-font-size:13.5px;");
        v.getChildren().addAll(l, box);
        return v;
    }
    private Label pill(String text, boolean primary) {
        Label l = new Label(text);
        l.setPadding(new Insets(12, 24, 12, 24));
        if (primary) l.setStyle("-fx-background-color:" + NAVY + ";-fx-text-fill:white;-fx-background-radius:8;-fx-font-weight:bold;-fx-font-size:13.5px;-fx-cursor:hand;");
        else l.setStyle("-fx-background-color:white;-fx-text-fill:" + NAVY + ";-fx-background-radius:8;-fx-border-color:" + NAVY + ";-fx-border-radius:8;-fx-font-weight:bold;-fx-font-size:13.5px;-fx-cursor:hand;");
        return l;
    }

    public static void main(String[] args) { launch(args); }
}
