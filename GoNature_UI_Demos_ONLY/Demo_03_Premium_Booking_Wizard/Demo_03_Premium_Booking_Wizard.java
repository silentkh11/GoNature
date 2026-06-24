import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/**
 * GoNature UI Demo 03 — PREMIUM BOOKING WIZARD
 * ------------------------------------------------------------
 * The safest, clearest reservation flow. A centered 5-step wizard
 * (Park → Date & Time → Group → Details → Confirm) with a live step rail,
 * inline validation, capacity feedback, and a final summary.
 *
 * Clicking Back / Continue actually moves between the 5 steps.
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_03_Premium_Booking_Wizard extends Application {

    static final String BG     = "#0f1216";
    static final String CARD   = "#171c22";
    static final String SOFT   = "#1f262e";
    static final String STROKE = "#2b333d";
    static final String TEXT   = "#f1f5f9";
    static final String MUTED  = "#94a3b8";
    static final String ACCENT = "#4ea1ff";
    static final String GREEN  = "#41d6a3";
    static final String AMBER  = "#f6b860";

    private final String[] titles = {"Choose Park", "Date & Time", "Group Size", "Your Details", "Confirm"};
    private int step = 0;
    private final StackPane stepArea = new StackPane();
    private HBox rail;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:linear-gradient(to bottom right,#0c0f13,#11161d);");

        VBox shell = new VBox(0);
        shell.setMaxWidth(880);
        shell.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:18;-fx-border-color:" + STROKE
                + ";-fx-border-radius:18;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 12);");

        shell.getChildren().add(header());
        rail = stepRail();
        VBox railWrap = new VBox(rail);
        railWrap.setPadding(new Insets(20, 34, 4, 34));
        shell.getChildren().add(railWrap);

        stepArea.setPadding(new Insets(20, 34, 10, 34));
        stepArea.setPrefHeight(330);
        stepArea.setAlignment(Pos.TOP_LEFT);
        shell.getChildren().add(stepArea);

        shell.getChildren().add(footer());

        StackPane center = new StackPane(shell);
        center.setPadding(new Insets(30));
        root.setCenter(center);
        render();

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Premium Booking Wizard (Demo 03)");
        stage.show();
    }

    private HBox header() {
        HBox h = new HBox(13);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(24, 34, 20, 34));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Circle dot = new Circle(15, Color.web(GREEN));
        Label leaf = new Label("▲");
        leaf.setStyle("-fx-text-fill:#06241a;-fx-font-size:12px;-fx-font-weight:bold;");
        StackPane logo = new StackPane(dot, leaf);
        VBox t = new VBox(-2);
        Label a = new Label("New Reservation");
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-weight:bold;");
        Label b = new Label("Guided booking · takes about a minute");
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        t.getChildren().addAll(a, b);
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label close = new Label("✕");
        close.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:15px;-fx-cursor:hand;-fx-padding:4 8;");
        h.getChildren().addAll(logo, t, g, close);
        return h;
    }

    private HBox stepRail() {
        HBox r = new HBox(0);
        r.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < titles.length; i++) {
            r.getChildren().add(railNode(i));
            if (i < titles.length - 1) {
                Region line = new Region();
                line.setPrefHeight(2);
                HBox.setHgrow(line, Priority.ALWAYS);
                line.setMaxWidth(Double.MAX_VALUE);
                line.setStyle("-fx-background-color:" + STROKE + ";");
                HBox.setMargin(line, new Insets(0, 6, 0, 6));
                r.getChildren().add(line);
            }
        }
        return r;
    }
    private VBox railNode(int i) {
        VBox v = new VBox(7);
        v.setAlignment(Pos.CENTER);
        StackPane c = new StackPane();
        boolean done = i < step, cur = i == step;
        Circle circ = new Circle(16);
        circ.setFill(done ? Color.web(GREEN) : cur ? Color.web(ACCENT) : Color.web(SOFT));
        if (!done && !cur) { circ.setStroke(Color.web(STROKE)); }
        Label n = new Label(done ? "✓" : String.valueOf(i + 1));
        n.setStyle("-fx-text-fill:" + (done || cur ? "#091016" : MUTED) + ";-fx-font-weight:bold;-fx-font-size:13px;");
        c.getChildren().addAll(circ, n);
        Label l = new Label(titles[i]);
        l.setStyle("-fx-text-fill:" + (cur ? TEXT : MUTED) + ";-fx-font-size:11.5px;" + (cur ? "-fx-font-weight:bold;" : ""));
        v.getChildren().addAll(c, l);
        v.setMinWidth(96);
        return v;
    }

    private HBox footer() {
        HBox f = new HBox(12);
        f.setAlignment(Pos.CENTER_LEFT);
        f.setPadding(new Insets(18, 34, 24, 34));
        f.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:1 0 0 0;");
        Label back = btn("←  Back", false);
        back.setOnMouseClicked(e -> { if (step > 0) { step--; render(); } });
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label hint = new Label("Step " + (step + 1) + " of 5");
        hint.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        Label next = btn(step == 4 ? "Confirm booking  ✓" : "Continue  →", true);
        next.setOnMouseClicked(e -> { if (step < 4) { step++; render(); } });
        f.getChildren().addAll(back, g, hint, next);
        // store hint reference via id for refresh
        f.setUserData(hint);
        this.footerRef = f;
        return f;
    }
    private HBox footerRef;

    private Label btn(String text, boolean primary) {
        Label l = new Label(text);
        l.setPadding(new Insets(12, 26, 12, 26));
        if (primary) l.setStyle("-fx-background-color:" + ACCENT + ";-fx-text-fill:#06121f;-fx-background-radius:10;-fx-font-weight:bold;-fx-font-size:13.5px;-fx-cursor:hand;");
        else l.setStyle("-fx-background-color:transparent;-fx-border-color:" + STROKE + ";-fx-border-radius:10;-fx-text-fill:" + MUTED + ";-fx-font-size:13.5px;-fx-cursor:hand;-fx-padding:12 26;");
        return l;
    }

    /* ---------------- step rendering ---------------- */
    private void render() {
        // rebuild rail + footer to reflect state
        rail.getChildren().clear();
        HBox fresh = stepRail();
        rail.getChildren().setAll(fresh.getChildren());
        // refresh footer
        VBox shell = (VBox) footerRef.getParent();
        shell.getChildren().remove(footerRef);
        shell.getChildren().add(footer());

        stepArea.getChildren().setAll(stepContent(step));
    }

    private VBox stepContent(int s) {
        switch (s) {
            case 0:  return parkStep();
            case 1:  return dateStep();
            case 2:  return groupStep();
            case 3:  return detailsStep();
            default: return confirmStep();
        }
    }

    private VBox heading(String t, String sub) {
        VBox v = new VBox(4);
        Label a = new Label(t);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:21px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        v.getChildren().addAll(a, b);
        VBox.setMargin(v, new Insets(0, 0, 16, 0));
        return v;
    }

    private VBox parkStep() {
        VBox v = new VBox(12);
        v.getChildren().add(heading("Which park would you like to visit?", "Choose a destination to see live availability."));
        v.getChildren().add(choice("Masada National Park", "Judean Desert · 642/800 today", true));
        v.getChildren().add(choice("Caesarea National Park", "Mediterranean Coast · 518/600 today", false));
        v.getChildren().add(choice("Timna Valley Park", "Arava · 121/400 today", false));
        v.getChildren().add(choice("Nahal Ayun Nature Reserve", "Upper Galilee · 196/200 — nearly full", false));
        return v;
    }
    private HBox choice(String name, String sub, boolean selected) {
        HBox h = new HBox(14);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(15, 18, 15, 18));
        h.setStyle("-fx-background-color:" + (selected ? "rgba(78,161,255,0.10)" : SOFT) + ";-fx-background-radius:12;-fx-border-color:"
                + (selected ? ACCENT : STROKE) + ";-fx-border-radius:12;-fx-border-width:" + (selected ? 1.6 : 1) + ";-fx-cursor:hand;");
        Label ic = new Label("⛰");
        ic.setStyle("-fx-font-size:22px;");
        VBox t = new VBox(-1);
        Label a = new Label(name);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14.5px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        t.getChildren().addAll(a, b);
        HBox.setHgrow(t, Priority.ALWAYS);
        Label radio = new Label(selected ? "◉" : "○");
        radio.setStyle("-fx-text-fill:" + (selected ? ACCENT : MUTED) + ";-fx-font-size:18px;");
        h.getChildren().addAll(ic, t, radio);
        return h;
    }

    private VBox dateStep() {
        VBox v = new VBox(14);
        v.getChildren().add(heading("Pick a date and time", "Masada National Park · slots are 30 minutes apart."));
        v.getChildren().add(labeled("VISIT DATE", "Wed, 25 June 2026"));
        Label slotLabel = new Label("TIME SLOT");
        slotLabel.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        FlowPane slots = new FlowPane(10, 10);
        String[] times = {"08:00", "08:30", "09:00", "09:30", "10:00", "11:00", "13:00", "14:30"};
        for (int i = 0; i < times.length; i++) {
            boolean sel = i == 2, full = i == 4;
            Label t = new Label(times[i]);
            t.setPadding(new Insets(10, 18, 10, 18));
            if (full) t.setStyle("-fx-background-color:" + SOFT + ";-fx-text-fill:#4b5563;-fx-background-radius:9;-fx-strikethrough:true;-fx-font-size:13px;");
            else t.setStyle("-fx-background-color:" + (sel ? ACCENT : SOFT) + ";-fx-text-fill:" + (sel ? "#06121f" : TEXT) + ";-fx-background-radius:9;-fx-font-size:13px;-fx-cursor:hand;" + (sel ? "-fx-font-weight:bold;" : "-fx-border-color:" + STROKE + ";-fx-border-radius:9;"));
            slots.getChildren().add(t);
        }
        v.getChildren().addAll(slotLabel, slots);
        v.getChildren().add(infoBar(GREEN, "✓  09:00 selected — capacity available for groups up to 38."));
        return v;
    }

    private VBox groupStep() {
        VBox v = new VBox(14);
        v.getChildren().add(heading("How many visitors?", "Enter the total group size for this reservation."));
        HBox stepper = new HBox(0);
        stepper.setAlignment(Pos.CENTER_LEFT);
        stepper.setMaxWidth(220);
        stepper.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;");
        Label minus = stepBtn("−");
        Label count = new Label("4");
        count.setMaxWidth(Double.MAX_VALUE);
        count.setAlignment(Pos.CENTER);
        HBox.setHgrow(count, Priority.ALWAYS);
        count.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:26px;-fx-font-weight:bold;");
        Label plus = stepBtn("+");
        stepper.getChildren().addAll(minus, count, plus);
        v.getChildren().add(stepper);
        v.getChildren().add(infoBar(GREEN, "✓  Within park capacity. 4 visitors · subscriber rate applies."));
        v.getChildren().add(infoBar(AMBER, "ℹ  Groups of 15+ are billed at the guided-group rate and may require a certified guide."));
        return v;
    }
    private Label stepBtn(String s) {
        Label l = new Label(s);
        l.setPrefSize(58, 58);
        l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:24px;-fx-font-weight:bold;-fx-cursor:hand;");
        return l;
    }

    private VBox detailsStep() {
        VBox v = new VBox(13);
        v.getChildren().add(heading("Your contact details", "We'll send confirmation and reminders here."));
        HBox r1 = new HBox(13, labeled("FULL NAME", "Yossi Avraham"), labeled("SUBSCRIBER ID", "20517"));
        HBox r2 = new HBox(13, labeled("EMAIL", "yossi.a@example.com"), labeled("PHONE", "054-781-2290"));
        for (var n : r1.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        for (var n : r2.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        v.getChildren().addAll(r1, r2);
        v.getChildren().add(infoBar(ACCENT, "ℹ  A one-click confirmation link will be emailed. Booking stays editable until you confirm."));
        return v;
    }

    private VBox confirmStep() {
        VBox v = new VBox(12);
        v.getChildren().add(heading("Review & confirm", "Please check everything before you book."));
        VBox sum = new VBox(0);
        sum.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:14;-fx-border-color:" + STROKE + ";-fx-border-radius:14;");
        sum.getChildren().add(sumRow("Park", "Masada National Park", false));
        sum.getChildren().add(sumRow("Date & time", "Wed, 25 June 2026 · 09:00", false));
        sum.getChildren().add(sumRow("Visitors", "4 (subscriber rate)", false));
        sum.getChildren().add(sumRow("Booked by", "Yossi Avraham · #20517", false));
        sum.getChildren().add(sumRow("Total", "₪243.00", true));
        v.getChildren().add(sum);
        v.getChildren().add(infoBar(GREEN, "✓  All checks passed. Status will be set to \"Booked\" and remain editable until confirmed."));
        return v;
    }
    private HBox sumRow(String k, String val, boolean total) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 18, 14, 18));
        if (!total) h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Label k1 = new Label(k);
        k1.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label v1 = new Label(val);
        v1.setStyle("-fx-text-fill:" + (total ? GREEN : TEXT) + ";-fx-font-size:" + (total ? "18px" : "13.5px") + ";-fx-font-weight:bold;");
        h.getChildren().addAll(k1, g, v1);
        return h;
    }

    private VBox labeled(String label, String value) {
        VBox v = new VBox(5);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label box = new Label(value);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(12, 15, 12, 15));
        box.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:10;-fx-border-color:" + STROKE + ";-fx-border-radius:10;-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;");
        v.getChildren().addAll(l, box);
        return v;
    }
    private HBox infoBar(String color, String text) {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(12, 15, 12, 15));
        h.setStyle("-fx-background-color:" + color + "1a;-fx-background-radius:10;-fx-border-color:" + color + "55;-fx-border-radius:10;");
        Label t = new Label(text);
        t.setWrapText(true);
        t.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        h.getChildren().add(t);
        return h;
    }

    public static void main(String[] args) { launch(args); }
}
