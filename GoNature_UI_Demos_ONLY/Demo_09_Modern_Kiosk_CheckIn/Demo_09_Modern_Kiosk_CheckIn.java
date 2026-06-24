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
 * GoNature UI Demo 09 — MODERN KIOSK CHECK-IN
 * ------------------------------------------------------------
 * A large, high-contrast, touch-friendly entrance terminal for the GateWorker
 * or self-service check-in. Working numeric keypad to enter an order code, a
 * reservation preview, a check-in confirmation, a capacity warning, and a
 * live "today's arrivals" rail.
 *
 * Tap the keypad to type a code; "Check in" shows the confirmation overlay.
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_09_Modern_Kiosk_CheckIn extends Application {

    static final String BG     = "#070b10";
    static final String CARD   = "#111923";
    static final String SOFT   = "#18222f";
    static final String STROKE = "#243140";
    static final String TEXT   = "#f1f6fb";
    static final String MUTED  = "#8499ac";
    static final String GREEN  = "#36e0a0";
    static final String CYAN   = "#39c0ff";
    static final String AMBER  = "#ffc15a";

    private final StringBuilder code = new StringBuilder("10429");
    private Label codeDisplay;
    private StackPane rootStack;

    @Override
    public void start(Stage stage) {
        rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color:radial-gradient(center 50% 0%, radius 90%, #0c141d, #070b10);");

        BorderPane main = new BorderPane();
        main.setTop(topBar());

        HBox body = new HBox(22);
        body.setPadding(new Insets(26, 30, 20, 30));
        body.getChildren().addAll(keypadPanel(), reservationPanel());
        main.setCenter(body);
        main.setBottom(arrivalsRail());

        rootStack.getChildren().add(main);

        Scene scene = new Scene(rootStack, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Kiosk Check-In (Demo 09)");
        stage.show();
    }

    private HBox topBar() {
        HBox h = new HBox(16);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(20, 30, 20, 30));
        StackPane logo = new StackPane(new Circle(18, Color.web(GREEN)), new Label("▲"){{ setStyle("-fx-text-fill:#06241a;-fx-font-size:14px;-fx-font-weight:bold;"); }});
        VBox t = new VBox(-2);
        Label a = new Label("Masada National Park");
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:20px;-fx-font-weight:bold;");
        Label b = new Label("Gate A · Self check-in terminal");
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        t.getChildren().addAll(a, b);
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        HBox cap = new HBox(10);
        cap.setAlignment(Pos.CENTER_LEFT);
        cap.setPadding(new Insets(10, 16, 10, 16));
        cap.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;");
        Circle d = new Circle(6, Color.web(AMBER));
        VBox ct = new VBox(-2);
        Label c1 = new Label("642 / 800 inside");
        c1.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14px;-fx-font-weight:bold;");
        Label c2 = new Label("80% capacity · filling up");
        c2.setStyle("-fx-text-fill:" + AMBER + ";-fx-font-size:11.5px;");
        ct.getChildren().addAll(c1, c2);
        cap.getChildren().addAll(d, ct);
        Label clock = new Label("09:14");
        clock.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:20px;-fx-font-weight:bold;");
        HBox.setMargin(clock, new Insets(0, 0, 0, 8));
        h.getChildren().addAll(logo, t, g, cap, clock);
        return h;
    }

    private VBox keypadPanel() {
        VBox v = new VBox(18);
        v.setPrefWidth(440);
        v.setPadding(new Insets(28));
        v.setStyle(cardCss());
        Label title = new Label("Enter your order code");
        title.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:20px;-fx-font-weight:bold;");
        Label sub = new Label("Find it in your confirmation email, or scan the QR code.");
        sub.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        sub.setWrapText(true);

        codeDisplay = new Label(formatted());
        codeDisplay.setMaxWidth(Double.MAX_VALUE);
        codeDisplay.setAlignment(Pos.CENTER);
        codeDisplay.setPadding(new Insets(20));
        codeDisplay.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:14;-fx-border-color:" + CYAN + "55;-fx-border-radius:14;-fx-text-fill:" + TEXT + ";-fx-font-size:34px;-fx-font-weight:bold;-fx-font-family:'Consolas';-fx-letter-spacing:6px;");

        GridPane pad = new GridPane();
        pad.setHgap(12); pad.setVgap(12);
        String[] keys = {"1","2","3","4","5","6","7","8","9","⌫","0","QR"};
        for (int i = 0; i < keys.length; i++) {
            pad.add(key(keys[i]), i % 3, i / 3);
        }
        for (int i = 0; i < 3; i++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(33.3); pad.getColumnConstraints().add(cc); }

        Label scan = new Label("Find reservation  →");
        scan.setMaxWidth(Double.MAX_VALUE);
        scan.setAlignment(Pos.CENTER);
        scan.setPadding(new Insets(16));
        scan.setStyle("-fx-background-color:" + CYAN + ";-fx-text-fill:#04222f;-fx-background-radius:12;-fx-font-size:15px;-fx-font-weight:bold;-fx-cursor:hand;");

        v.getChildren().addAll(title, sub, codeDisplay, pad, scan);
        return v;
    }
    private StackPane key(String label) {
        StackPane s = new StackPane();
        s.setPrefHeight(62);
        boolean special = label.equals("⌫") || label.equals("QR");
        s.setStyle("-fx-background-color:" + (special ? "#0f2330" : SOFT) + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;-fx-cursor:hand;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + (label.equals("QR") ? CYAN : TEXT) + ";-fx-font-size:" + (special ? "18px" : "22px") + ";-fx-font-weight:bold;");
        s.getChildren().add(l);
        s.setOnMouseEntered(e -> s.setStyle("-fx-background-color:#1d2c3b;-fx-background-radius:12;-fx-border-color:" + CYAN + "66;-fx-border-radius:12;-fx-cursor:hand;"));
        s.setOnMouseExited(e -> s.setStyle("-fx-background-color:" + (special ? "#0f2330" : SOFT) + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;-fx-cursor:hand;"));
        s.setOnMouseClicked(e -> press(label));
        return s;
    }
    private void press(String k) {
        if (k.equals("⌫")) { if (code.length() > 0) code.deleteCharAt(code.length() - 1); }
        else if (k.equals("QR")) { code.setLength(0); code.append("10429"); }
        else if (code.length() < 5) code.append(k);
        codeDisplay.setText(formatted());
    }
    private String formatted() {
        String s = code.toString();
        StringBuilder b = new StringBuilder("#");
        for (int i = 0; i < 5; i++) b.append(i < s.length() ? s.charAt(i) : '•');
        return b.toString();
    }

    private VBox reservationPanel() {
        VBox v = new VBox(18);
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setPadding(new Insets(28));
        v.setStyle(cardCss());

        HBox head = new HBox(14);
        head.setAlignment(Pos.CENTER_LEFT);
        StackPane ok = new StackPane(new Circle(26, Color.web(GREEN, 0.16)), new Label("✓"){{ setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:24px;-fx-font-weight:bold;"); }});
        VBox ht = new VBox(-2);
        Label a = new Label("Reservation found");
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:20px;-fx-font-weight:bold;");
        Label b = new Label("Order #10429 · Confirmed");
        b.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        ht.getChildren().addAll(a, b);
        head.getChildren().addAll(ok, ht);

        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.add(info("VISITOR", "Tamar Shapira"), 0, 0);
        grid.add(info("PARTY SIZE", "4 visitors"), 1, 0);
        grid.add(info("TIME SLOT", "09:00 — 12:00"), 0, 1);
        grid.add(info("TYPE", "Subscriber"), 1, 1);
        for (int i = 0; i < 2; i++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(50); grid.getColumnConstraints().add(cc); }

        HBox warn = new HBox(12);
        warn.setAlignment(Pos.CENTER_LEFT);
        warn.setPadding(new Insets(15, 18, 15, 18));
        warn.setStyle("-fx-background-color:" + AMBER + "1a;-fx-background-radius:12;-fx-border-color:" + AMBER + "55;-fx-border-radius:12;");
        Label wi = new Label("⚠");
        wi.setStyle("-fx-font-size:20px;-fx-text-fill:" + AMBER + ";");
        Label wt = new Label("Park is at 80% capacity. Admitting 4 will bring it to 646 / 800. Proceed?");
        wt.setWrapText(true);
        wt.setStyle("-fx-text-fill:" + AMBER + ";-fx-font-size:13px;-fx-font-weight:bold;");
        HBox.setHgrow(wt, Priority.ALWAYS);
        warn.getChildren().addAll(wi, wt);

        HBox btns = new HBox(14);
        Label checkin = new Label("✓  Check in 4 visitors");
        checkin.setMaxWidth(Double.MAX_VALUE);
        checkin.setAlignment(Pos.CENTER);
        checkin.setPadding(new Insets(20));
        checkin.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#06231a;-fx-background-radius:14;-fx-font-size:17px;-fx-font-weight:bold;-fx-cursor:hand;");
        HBox.setHgrow(checkin, Priority.ALWAYS);
        checkin.setOnMouseClicked(e -> showSuccess());
        Label cancel = new Label("Cancel");
        cancel.setAlignment(Pos.CENTER);
        cancel.setPadding(new Insets(20, 34, 20, 34));
        cancel.setStyle("-fx-background-color:transparent;-fx-text-fill:" + MUTED + ";-fx-border-color:" + STROKE + ";-fx-border-radius:14;-fx-font-size:15px;-fx-cursor:hand;");
        btns.getChildren().addAll(checkin, cancel);

        v.getChildren().addAll(head, sep(), grid, warn, btns);
        return v;
    }
    private VBox info(String label, String value) {
        VBox v = new VBox(5);
        v.setPadding(new Insets(15, 16, 15, 16));
        v.setStyle("-fx-background-color:" + SOFT + ";-fx-background-radius:12;");
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-weight:bold;");
        v.getChildren().addAll(l, val);
        return v;
    }
    private Region sep() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setStyle("-fx-background-color:" + STROKE + ";");
        return r;
    }

    private VBox arrivalsRail() {
        VBox wrap = new VBox(10);
        wrap.setPadding(new Insets(6, 30, 24, 30));
        Label t = new Label("TODAY'S ARRIVALS");
        t.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
        HBox rail = new HBox(14);
        rail.getChildren().addAll(
            arrival("08:30", "Hod Tours (group)", "28 pax", "Checked in", GREEN),
            arrival("09:00", "Tamar Shapira", "4 pax", "Now at gate", CYAN),
            arrival("09:30", "Eitan Barak", "2 pax", "Expected", MUTED),
            arrival("10:00", "Rivka Mizrahi", "3 pax", "Expected", MUTED)
        );
        for (var n : rail.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        wrap.getChildren().addAll(t, rail);
        return wrap;
    }
    private VBox arrival(String time, String who, String pax, String status, String color) {
        VBox v = new VBox(5);
        v.setPadding(new Insets(14, 16, 14, 16));
        v.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;");
        HBox top = new HBox();
        Label tm = new Label(time);
        tm.setStyle("-fx-text-fill:" + CYAN + ";-fx-font-size:13px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill:" + color + ";-fx-font-size:10px;");
        top.getChildren().addAll(tm, g, dot);
        Label w = new Label(who);
        w.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13.5px;-fx-font-weight:bold;");
        Label meta = new Label(pax + " · " + status);
        meta.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        v.getChildren().addAll(top, w, meta);
        return v;
    }

    /* ---- success overlay ---- */
    private void showSuccess() {
        StackPane layer = new StackPane();
        layer.setStyle("-fx-background-color:rgba(4,8,12,0.8);");
        VBox modal = new VBox(18);
        modal.setMaxSize(460, 340);
        modal.setAlignment(Pos.CENTER);
        modal.setPadding(new Insets(36));
        modal.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:20;-fx-border-color:" + GREEN + "44;-fx-border-radius:20;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.6), 40, 0, 0, 16);");
        StackPane check = new StackPane(new Circle(44, Color.web(GREEN, 0.16)), new Label("✓"){{ setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:44px;-fx-font-weight:bold;"); }});
        Label t = new Label("Welcome to Masada!");
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:24px;-fx-font-weight:bold;");
        Label s = new Label("4 visitors checked in · enjoy your visit");
        s.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:14px;");
        Label done = new Label("Done");
        done.setPadding(new Insets(14, 50, 14, 50));
        done.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#06231a;-fx-background-radius:12;-fx-font-size:15px;-fx-font-weight:bold;-fx-cursor:hand;");
        done.setOnMouseClicked(e -> rootStack.getChildren().remove(layer));
        modal.getChildren().addAll(check, t, s, done);
        layer.getChildren().add(modal);
        rootStack.getChildren().add(layer);
    }

    private String cardCss() {
        return "-fx-background-color:" + CARD + ";-fx-background-radius:18;-fx-border-color:" + STROKE
             + ";-fx-border-radius:18;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.4), 20, 0, 0, 8);";
    }

    public static void main(String[] args) { launch(args); }
}
