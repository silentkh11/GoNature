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
 * GoNature UI Demo 02 — GLASS ECO EXPLORER
 * ------------------------------------------------------------
 * Dark glassmorphism for travelers: discovery hero, frosted park cards with
 * availability, a booking stepper, a confirmation panel, and a membership card.
 *
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_02_Glass_Eco_Explorer extends Application {

    static final String TEXT  = "#eaf3ee";
    static final String MUTED = "#a9c0b4";
    static final String GREEN = "#5ef0a8";
    static final String CYAN  = "#5cd2ff";
    static final String AMBER = "#ffcf6b";
    static final String GLASS = "rgba(255,255,255,0.07)";
    static final String GLASS_BORDER = "rgba(255,255,255,0.14)";

    @Override
    public void start(Stage stage) {
        // layered nature gradient background (forest → deep teal night)
        StackPane root = new StackPane();
        Region bg = new Region();
        bg.setStyle("-fx-background-color:linear-gradient(to bottom right,#0b1f1a,#0a2c2a 45%,#071a24);");
        Region glow1 = blob(520, "rgba(94,240,168,0.18)", Pos.TOP_LEFT, -120, -120);
        Region glow2 = blob(480, "rgba(92,210,255,0.14)", Pos.BOTTOM_RIGHT, 120, 80);

        ScrollPane sp = new ScrollPane(buildContent());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");

        root.getChildren().addAll(bg, glow1, glow2, sp);

        Scene scene = new Scene(root, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Glass Eco Explorer (Demo 02)");
        stage.show();
    }

    private Region blob(double size, String color, Pos pos, double tx, double ty) {
        Region r = new Region();
        r.setMaxSize(size, size);
        r.setStyle("-fx-background-color:radial-gradient(center 50% 50%, radius 50%," + color + ", transparent);");
        StackPane.setAlignment(r, pos);
        r.setTranslateX(tx); r.setTranslateY(ty);
        r.setMouseTransparent(true);
        return r;
    }

    private VBox buildContent() {
        VBox col = new VBox(26);
        col.setPadding(new Insets(28, 40, 48, 40));

        col.getChildren().add(topNav());
        col.getChildren().add(hero());

        Label sec = section("Featured Reserves", "Live availability for Tue, 24 June");
        col.getChildren().add(sec);

        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            parkCard("Masada National Park", "Judean Desert", "#7a4a1e", "#c98a3c", 0.80, "₪85", AMBER),
            parkCard("Caesarea National Park", "Mediterranean Coast", "#15485f", "#3aa6c9", 0.86, "₪75", AMBER),
            parkCard("Timna Valley Park", "Arava · Eilat", "#6e2a2a", "#cf5b4a", 0.30, "₪70", GREEN)
        );
        for (var n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        col.getChildren().add(cards);

        // booking + membership row
        HBox row = new HBox(20);
        row.getChildren().addAll(bookingStepper(), membershipCard());
        col.getChildren().add(row);

        return col;
    }

    private HBox topNav() {
        HBox nav = new HBox(14);
        nav.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(16, Color.web(GREEN));
        Label leaf = new Label("❋");
        leaf.setStyle("-fx-text-fill:#08231b;-fx-font-size:15px;-fx-font-weight:bold;");
        StackPane logo = new StackPane(dot, leaf);
        Label brand = new Label("GoNature");
        brand.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:19px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        HBox links = new HBox(8, navlink("Explore", true), navlink("My Visits", false), navlink("Membership", false));
        HBox account = new HBox(10, glassPill("🔔"), avatar("TS"));
        account.setAlignment(Pos.CENTER);
        nav.getChildren().addAll(logo, brand, g, links, new Region(){{ setPrefWidth(18);}}, account);
        return nav;
    }

    private Label navlink(String t, boolean active) {
        Label l = new Label(t);
        l.setPadding(new Insets(8, 16, 8, 16));
        l.setStyle("-fx-background-radius:20;-fx-cursor:hand;-fx-font-size:13px;"
                + (active ? "-fx-background-color:" + GLASS + ";-fx-text-fill:" + TEXT + ";-fx-font-weight:bold;-fx-border-color:" + GLASS_BORDER + ";-fx-border-radius:20;"
                          : "-fx-text-fill:" + MUTED + ";"));
        return l;
    }

    private VBox hero() {
        VBox h = new VBox(14);
        h.setPadding(new Insets(34, 36, 34, 36));
        h.setStyle(glassCss(20) + "-fx-background-color:linear-gradient(to right, rgba(94,240,168,0.10), rgba(92,210,255,0.05));");
        Label tag = new Label("◍  EXPLORE ISRAEL'S NATURE RESERVES");
        tag.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:12px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
        Label big = new Label("Good evening, Tamar.");
        big.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:34px;-fx-font-weight:bold;");
        Label sub = new Label("Where will your next visit take you? Book in seconds — your subscriber discount is applied automatically.");
        sub.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:14px;");
        sub.setWrapText(true);

        HBox searchBar = new HBox(0);
        searchBar.setMaxWidth(640);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle(glassCss(30) + "-fx-padding:6;");
        Label field = new Label("🔍   Search a park, date or region…");
        field.setPadding(new Insets(12, 18, 12, 18));
        field.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13.5px;");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
        Label go = new Label("Find availability");
        go.setPadding(new Insets(12, 24, 12, 24));
        go.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#06241a;-fx-background-radius:24;-fx-font-weight:bold;-fx-font-size:13.5px;-fx-cursor:hand;");
        searchBar.getChildren().addAll(field, go);

        h.getChildren().addAll(tag, big, sub, searchBar);
        return h;
    }

    private VBox parkCard(String name, String region, String c1, String c2, double load, String price, String badgeColor) {
        VBox c = new VBox(0);
        c.setStyle(glassCss(18));
        c.setPrefWidth(360);

        Region photo = new Region();
        photo.setPrefHeight(150);
        photo.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");
        Label sun = new Label("⛰");
        sun.setStyle("-fx-font-size:46px;-fx-opacity:0.45;");
        String avail = load >= 0.85 ? "Few spots left" : load >= 0.6 ? "Filling up" : "Wide open";
        Label badge = new Label("●  " + avail);
        badge.setPadding(new Insets(5, 12, 5, 12));
        badge.setStyle("-fx-background-color:rgba(8,20,16,0.6);-fx-text-fill:" + badgeColor + ";-fx-background-radius:20;-fx-font-size:11.5px;-fx-font-weight:bold;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(12));
        StackPane photoWrap = new StackPane(photo, sun, badge);

        VBox body = new VBox(10);
        body.setPadding(new Insets(18));
        Label n = new Label(name);
        n.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:17px;-fx-font-weight:bold;");
        Label rg = new Label("📍 " + region);
        rg.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");

        // availability bar
        StackPane track = new StackPane();
        track.setAlignment(Pos.CENTER_LEFT);
        track.setPrefHeight(7);
        track.setStyle("-fx-background-color:rgba(255,255,255,0.10);-fx-background-radius:20;");
        Region fill = new Region();
        fill.setPrefHeight(7);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(load));
        String fc = load >= 0.85 ? AMBER : GREEN;
        fill.setStyle("-fx-background-color:" + fc + ";-fx-background-radius:20;");
        track.getChildren().add(fill);
        Label cap = new Label(Math.round(load * 100) + "% booked today");
        cap.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11px;");

        HBox foot = new HBox();
        foot.setAlignment(Pos.CENTER_LEFT);
        VBox pr = new VBox(-2);
        Label p = new Label(price);
        p.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:19px;-fx-font-weight:bold;");
        Label pp = new Label("per adult · incl. tax");
        pp.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;");
        pr.getChildren().addAll(p, pp);
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label book = new Label("Book visit  →");
        book.setPadding(new Insets(10, 18, 10, 18));
        book.setStyle("-fx-background-color:" + GLASS + ";-fx-border-color:" + GLASS_BORDER + ";-fx-border-radius:22;-fx-background-radius:22;-fx-text-fill:" + GREEN + ";-fx-font-weight:bold;-fx-font-size:13px;-fx-cursor:hand;");
        book.setOnMouseEntered(e -> book.setStyle("-fx-background-color:" + GREEN + ";-fx-background-radius:22;-fx-text-fill:#06241a;-fx-font-weight:bold;-fx-font-size:13px;-fx-cursor:hand;"));
        book.setOnMouseExited(e -> book.setStyle("-fx-background-color:" + GLASS + ";-fx-border-color:" + GLASS_BORDER + ";-fx-border-radius:22;-fx-background-radius:22;-fx-text-fill:" + GREEN + ";-fx-font-weight:bold;-fx-font-size:13px;-fx-cursor:hand;"));
        foot.getChildren().addAll(pr, g, book);

        body.getChildren().addAll(n, rg, track, cap, new Region(){{ setPrefHeight(4);}}, foot);
        c.getChildren().addAll(photoWrap, body);
        return c;
    }

    private VBox bookingStepper() {
        VBox c = glassPanel();
        HBox.setHgrow(c, Priority.ALWAYS);
        c.getChildren().add(section("Quick Booking", "Caesarea National Park · subscriber rate"));
        HBox steps = new HBox(0);
        steps.getChildren().addAll(step("1", "Date", true), conn(), step("2", "Group", true), conn(), step("3", "Details", false), conn(), step("4", "Confirm", false));
        steps.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(steps, new Insets(8, 0, 16, 0));

        VBox form = new VBox(12);
        form.getChildren().addAll(
            field2("Visit date", "Wed, 25 June 2026", CYAN),
            field2("Time slot", "09:00 — group of 4", CYAN),
            field2("Group size", "4 visitors  ·  within capacity ✓", GREEN)
        );

        HBox summary = new HBox();
        summary.setAlignment(Pos.CENTER_LEFT);
        summary.setPadding(new Insets(14, 16, 14, 16));
        summary.setStyle("-fx-background-color:rgba(94,240,168,0.08);-fx-background-radius:12;-fx-border-color:rgba(94,240,168,0.25);-fx-border-radius:12;");
        VBox st = new VBox(-2);
        Label sl = new Label("Estimated total");
        sl.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        Label sv = new Label("₪243.00");
        sv.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:22px;-fx-font-weight:bold;");
        st.getChildren().addAll(sl, sv);
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label disc = new Label("Subscriber −10%  applied");
        disc.setPadding(new Insets(6, 12, 6, 12));
        disc.setStyle("-fx-background-color:rgba(94,240,168,0.18);-fx-text-fill:" + GREEN + ";-fx-background-radius:20;-fx-font-size:11.5px;-fx-font-weight:bold;");
        summary.getChildren().addAll(st, g, disc);
        VBox.setMargin(summary, new Insets(8, 0, 0, 0));

        Label cta = new Label("Confirm reservation");
        cta.setMaxWidth(Double.MAX_VALUE);
        cta.setAlignment(Pos.CENTER);
        cta.setPadding(new Insets(14));
        cta.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#06241a;-fx-background-radius:12;-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;");
        VBox.setMargin(cta, new Insets(8, 0, 0, 0));

        c.getChildren().addAll(steps, form, summary, cta);
        return c;
    }

    private VBox membershipCard() {
        VBox wrap = glassPanel();
        wrap.setPrefWidth(360);
        wrap.getChildren().add(section("Membership", "Subscriber since 2023"));
        VBox mc = new VBox(14);
        mc.setPadding(new Insets(22));
        mc.setStyle("-fx-background-color:linear-gradient(to bottom right,#0f3b2e,#0a2740);-fx-background-radius:16;-fx-border-color:rgba(94,240,168,0.3);-fx-border-radius:16;");
        HBox top = new HBox();
        Label gn = new Label("GoNature");
        gn.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:15px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label tier = new Label("GUIDE");
        tier.setPadding(new Insets(3, 10, 3, 10));
        tier.setStyle("-fx-background-color:rgba(255,207,107,0.2);-fx-text-fill:" + AMBER + ";-fx-background-radius:20;-fx-font-size:10.5px;-fx-font-weight:bold;");
        top.getChildren().addAll(gn, g, tier);
        Label num = new Label("•••• •••• ••  20419");
        num.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-family:'Consolas';-fx-letter-spacing:2px;");
        HBox bottom = new HBox();
        VBox nm = new VBox(-2);
        Label h = new Label("HOLDER");
        h.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:9.5px;-fx-font-weight:bold;");
        Label nmv = new Label("Tamar Shapira");
        nmv.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14px;-fx-font-weight:bold;");
        nm.getChildren().addAll(h, nmv);
        Region g2 = new Region(); HBox.setHgrow(g2, Priority.ALWAYS);
        VBox fam = new VBox(-2);
        Label fh = new Label("FAMILY");
        fh.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:9.5px;-fx-font-weight:bold;");
        Label fv = new Label("6 members");
        fv.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14px;-fx-font-weight:bold;");
        fam.getChildren().addAll(fh, fv);
        bottom.getChildren().addAll(nm, g2, fam);
        mc.getChildren().addAll(top, num, bottom);

        VBox upcoming = new VBox(8);
        VBox.setMargin(upcoming, new Insets(14, 0, 0, 0));
        Label ut = new Label("Next visit");
        ut.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;-fx-font-weight:bold;");
        HBox nv = new HBox(12);
        nv.setAlignment(Pos.CENTER_LEFT);
        nv.setPadding(new Insets(12, 14, 12, 14));
        nv.setStyle(glassCss(12));
        Label cal = new Label("📅");
        cal.setStyle("-fx-font-size:20px;");
        VBox nvt = new VBox(-1);
        Label nv1 = new Label("Masada National Park");
        nv1.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13px;-fx-font-weight:bold;");
        Label nv2 = new Label("Fri, 27 Jun · 08:00 · 4 visitors");
        nv2.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        nvt.getChildren().addAll(nv1, nv2);
        nv.getChildren().addAll(cal, nvt);
        upcoming.getChildren().addAll(ut, nv);

        wrap.getChildren().addAll(mc, upcoming);
        return wrap;
    }

    /* ---------- helpers ---------- */
    private VBox glassPanel() {
        VBox c = new VBox(12);
        c.setPadding(new Insets(22));
        c.setStyle(glassCss(18));
        return c;
    }
    private Label section(String title, String sub) {
        Label l = new Label(title + "\n");
        l.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-weight:bold;");
        VBox v = new VBox(1);
        Label t = new Label(title);
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-weight:bold;");
        Label s = new Label(sub);
        s.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12.5px;");
        v.getChildren().addAll(t, s);
        // wrap in a label-compatible return: we actually return a Label-less VBox via cast trick? -> just return t with tooltip
        Label holder = new Label();
        holder.setGraphic(v);
        return holder;
    }
    private HBox step(String n, String label, boolean done) {
        HBox s = new HBox(8);
        s.setAlignment(Pos.CENTER_LEFT);
        StackPane c = new StackPane();
        Circle circ = new Circle(14, done ? Color.web(GREEN) : Color.web("#ffffff", 0.10));
        Label num = new Label(done ? "✓" : n);
        num.setStyle("-fx-text-fill:" + (done ? "#06241a" : MUTED) + ";-fx-font-weight:bold;-fx-font-size:12px;");
        c.getChildren().addAll(circ, num);
        Label l = new Label(label);
        l.setStyle("-fx-text-fill:" + (done ? TEXT : MUTED) + ";-fx-font-size:12.5px;" + (done ? "-fx-font-weight:bold;" : ""));
        s.getChildren().addAll(c, l);
        return s;
    }
    private Region conn() {
        Region r = new Region();
        r.setPrefWidth(26); r.setPrefHeight(2);
        r.setStyle("-fx-background-color:rgba(255,255,255,0.14);");
        HBox.setMargin(r, new Insets(0, 8, 0, 8));
        return r;
    }
    private VBox field2(String label, String value, String accent) {
        VBox v = new VBox(5);
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label box = new Label(value);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(11, 14, 11, 14));
        box.setStyle("-fx-background-color:rgba(255,255,255,0.05);-fx-background-radius:10;-fx-border-color:" + GLASS_BORDER + ";-fx-border-radius:10;-fx-text-fill:" + TEXT + ";-fx-font-size:13px;");
        v.getChildren().addAll(l, box);
        return v;
    }
    private StackPane glassPill(String glyph) {
        StackPane p = new StackPane();
        p.setPrefSize(40, 40);
        p.setStyle(glassCss(20) + "-fx-cursor:hand;");
        Label g = new Label(glyph);
        g.setStyle("-fx-font-size:15px;");
        p.getChildren().add(g);
        return p;
    }
    private StackPane avatar(String initials) {
        StackPane s = new StackPane();
        Circle c = new Circle(20, Color.web("#1b4d3e"));
        Label l = new Label(initials);
        l.setStyle("-fx-text-fill:" + GREEN + ";-fx-font-weight:bold;-fx-font-size:13px;");
        s.getChildren().addAll(c, l);
        return s;
    }
    private String glassCss(int radius) {
        return "-fx-background-color:" + GLASS + ";-fx-background-radius:" + radius + ";-fx-border-color:" + GLASS_BORDER
             + ";-fx-border-radius:" + radius + ";-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 22, 0, 0, 8);";
    }

    public static void main(String[] args) { launch(args); }
}
