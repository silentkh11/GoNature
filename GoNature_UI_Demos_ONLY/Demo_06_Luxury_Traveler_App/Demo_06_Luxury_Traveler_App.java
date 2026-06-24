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
 * GoNature UI Demo 06 — LUXURY TRAVELER APP
 * ------------------------------------------------------------
 * Cinematic, high-end tourism feel for the traveler side. A full-bleed featured
 * hero, premium park cards with gradient imagery, membership status, an upcoming-
 * visit timeline, and a confirmation modal (click "Reserve" to open / close it).
 *
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_06_Luxury_Traveler_App extends Application {

    static final String BG    = "#0b0c0f";
    static final String CARD  = "#15171c";
    static final String STROKE= "#23262e";
    static final String TEXT  = "#f5f3ee";
    static final String MUTED = "#9a9ba4";
    static final String GOLD  = "#d9b46a";
    static final String GREEN = "#5fd39a";

    private StackPane rootStack;
    private StackPane modalLayer;

    @Override
    public void start(Stage stage) {
        rootStack = new StackPane();
        rootStack.setStyle("-fx-background-color:" + BG + ";");

        ScrollPane sp = new ScrollPane(content());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");
        rootStack.getChildren().add(sp);

        Scene scene = new Scene(rootStack, 1280, 760);
        stage.setScene(scene);
        stage.setTitle("GoNature — Luxury Traveler (Demo 06)");
        stage.show();
    }

    private VBox content() {
        VBox col = new VBox(28);
        col.setPadding(new Insets(24, 44, 48, 44));
        col.getChildren().add(nav());
        col.getChildren().add(hero());
        col.getChildren().add(rowHeader("Featured this season", "Curated for desert & coast lovers"));
        HBox cards = new HBox(20);
        cards.getChildren().addAll(
            featured("Masada", "Sunrise above the Dead Sea", "#3a2a16", "#8a5a22", "₪85"),
            featured("Caesarea", "Roman ruins by the sea", "#10303f", "#1f6f8a", "₪75"),
            featured("Timna", "Crimson canyons of the Arava", "#3a1717", "#8a3631", "₪70")
        );
        for (var n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        col.getChildren().add(cards);

        HBox lower = new HBox(20);
        lower.getChildren().addAll(timeline(), membershipPanel());
        col.getChildren().add(lower);
        return col;
    }

    private HBox nav() {
        HBox h = new HBox(13);
        h.setAlignment(Pos.CENTER_LEFT);
        StackPane logo = new StackPane(new Circle(15, Color.web(GOLD)), new Label("✦"){{ setStyle("-fx-text-fill:#0b0c0f;-fx-font-size:13px;-fx-font-weight:bold;"); }});
        Label brand = new Label("GoNature");
        brand.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:19px;-fx-font-weight:bold;-fx-letter-spacing:1px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        HBox links = new HBox(26,
            navlink("Discover", true), navlink("My Journeys", false), navlink("Membership", false));
        links.setAlignment(Pos.CENTER);
        StackPane avatar = new StackPane(new Circle(19, Color.web("#2a2417")), new Label("TS"){{ setStyle("-fx-text-fill:" + GOLD + ";-fx-font-weight:bold;-fx-font-size:12.5px;"); }});
        h.getChildren().addAll(logo, brand, g, links, new Region(){{ setPrefWidth(26);}}, avatar);
        return h;
    }
    private Label navlink(String t, boolean active) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:" + (active ? TEXT : MUTED) + ";-fx-font-size:13.5px;-fx-cursor:hand;" + (active ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    private StackPane hero() {
        StackPane s = new StackPane();
        s.setPrefHeight(310);
        Region img = new Region();
        img.setStyle("-fx-background-color:linear-gradient(to bottom right,#221608,#3a2a14 40%,#0e1a1f);-fx-background-radius:22;");
        Region shade = new Region();
        shade.setStyle("-fx-background-color:linear-gradient(to right, rgba(8,8,10,0.85), rgba(8,8,10,0.15));-fx-background-radius:22;");
        Label peak = new Label("⛰");
        peak.setStyle("-fx-font-size:150px;-fx-opacity:0.10;");
        StackPane.setAlignment(peak, Pos.CENTER_RIGHT);
        StackPane.setMargin(peak, new Insets(0, 60, 0, 0));

        VBox txt = new VBox(14);
        txt.setAlignment(Pos.CENTER_LEFT);
        txt.setMaxWidth(560);
        txt.setPadding(new Insets(0, 0, 0, 44));
        StackPane.setAlignment(txt, Pos.CENTER_LEFT);
        Label tag = new Label("✦  EXCLUSIVE · SUBSCRIBER EXPERIENCE");
        tag.setStyle("-fx-text-fill:" + GOLD + ";-fx-font-size:12px;-fx-font-weight:bold;-fx-letter-spacing:1.5px;");
        Label big = new Label("Masada at Sunrise");
        big.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:44px;-fx-font-weight:bold;");
        Label sub = new Label("A guided ascent before dawn, then the desert turns gold. Limited daily capacity — reserved for members first.");
        sub.setWrapText(true);
        sub.setStyle("-fx-text-fill:#d9d6cf;-fx-font-size:14.5px;");
        HBox cta = new HBox(14);
        Label reserve = new Label("Reserve your visit  →");
        reserve.setPadding(new Insets(14, 28, 14, 28));
        reserve.setStyle("-fx-background-color:" + GOLD + ";-fx-text-fill:#231a08;-fx-background-radius:30;-fx-font-weight:bold;-fx-font-size:14px;-fx-cursor:hand;");
        reserve.setOnMouseClicked(e -> openModal());
        Label more = new Label("View details");
        more.setPadding(new Insets(14, 24, 14, 24));
        more.setStyle("-fx-background-color:rgba(255,255,255,0.08);-fx-text-fill:" + TEXT + ";-fx-background-radius:30;-fx-font-size:14px;-fx-cursor:hand;-fx-border-color:rgba(255,255,255,0.15);-fx-border-radius:30;");
        cta.getChildren().addAll(reserve, more);
        txt.getChildren().addAll(tag, big, sub, cta);

        s.getChildren().addAll(img, shade, peak, txt);
        return s;
    }

    private VBox rowHeader(String title, String sub) {
        VBox v = new VBox(2);
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:21px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        v.getChildren().addAll(a, b);
        return v;
    }

    private VBox featured(String name, String tagline, String c1, String c2, String price) {
        VBox c = new VBox(0);
        c.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:18;-fx-border-color:" + STROKE + ";-fx-border-radius:18;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.45), 20, 0, 0, 8);");
        StackPane img = new StackPane();
        img.setPrefHeight(190);
        Region g = new Region();
        g.setStyle("-fx-background-color:linear-gradient(to bottom right," + c1 + "," + c2 + ");-fx-background-radius:18 18 0 0;");
        Region sh = new Region();
        sh.setStyle("-fx-background-color:linear-gradient(to top, rgba(11,12,15,0.92), transparent);-fx-background-radius:18 18 0 0;");
        Label fav = new Label("♡");
        fav.setStyle("-fx-text-fill:white;-fx-font-size:18px;-fx-background-color:rgba(0,0,0,0.3);-fx-background-radius:30;-fx-padding:6 10;-fx-cursor:hand;");
        StackPane.setAlignment(fav, Pos.TOP_RIGHT);
        StackPane.setMargin(fav, new Insets(14));
        VBox cap = new VBox(-2);
        Label nm = new Label(name);
        nm.setStyle("-fx-text-fill:white;-fx-font-size:23px;-fx-font-weight:bold;");
        Label tg = new Label(tagline);
        tg.setStyle("-fx-text-fill:#d9d6cf;-fx-font-size:12px;");
        cap.getChildren().addAll(nm, tg);
        StackPane.setAlignment(cap, Pos.BOTTOM_LEFT);
        StackPane.setMargin(cap, new Insets(18));
        img.getChildren().addAll(g, sh, fav, cap);

        HBox foot = new HBox();
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.setPadding(new Insets(16, 18, 16, 18));
        VBox pr = new VBox(-2);
        HBox star = new HBox(4, new Label("★ 4.9"){{ setStyle("-fx-text-fill:" + GOLD + ";-fx-font-size:12.5px;-fx-font-weight:bold;"); }},
                new Label("· 2.4k visits"){{ setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;"); }});
        Label p = new Label(price + "  ·  per adult");
        p.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13px;-fx-font-weight:bold;");
        pr.getChildren().addAll(star, p);
        Region grow = new Region(); HBox.setHgrow(grow, Priority.ALWAYS);
        Label book = new Label("Reserve");
        book.setPadding(new Insets(10, 20, 10, 20));
        book.setStyle("-fx-background-color:rgba(217,180,106,0.15);-fx-text-fill:" + GOLD + ";-fx-background-radius:24;-fx-font-weight:bold;-fx-font-size:13px;-fx-cursor:hand;-fx-border-color:rgba(217,180,106,0.4);-fx-border-radius:24;");
        book.setOnMouseClicked(e -> openModal());
        foot.getChildren().addAll(pr, grow, book);

        c.getChildren().addAll(img, foot);
        return c;
    }

    private VBox timeline() {
        VBox c = new VBox(16);
        c.setPadding(new Insets(22));
        c.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:18;-fx-border-color:" + STROKE + ";-fx-border-radius:18;");
        HBox.setHgrow(c, Priority.ALWAYS);
        c.getChildren().add(rowHeader("Your upcoming journeys", "3 visits planned"));
        c.getChildren().add(tl("27", "JUN", "Masada National Park", "08:00 · 4 visitors · Confirmed", GREEN));
        c.getChildren().add(tl("02", "JUL", "Caesarea National Park", "10:30 · 2 visitors · Pending Confirm", GOLD));
        c.getChildren().add(tl("14", "JUL", "Timna Valley Park", "09:00 · 6 visitors · Booked", MUTED));
        return c;
    }
    private HBox tl(String day, String mon, String name, String meta, String color) {
        HBox h = new HBox(16);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(12, 6, 12, 6));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        VBox date = new VBox(-2);
        date.setAlignment(Pos.CENTER);
        date.setMinWidth(50);
        date.setPadding(new Insets(8, 0, 8, 0));
        date.setStyle("-fx-background-color:#1c1f26;-fx-background-radius:10;");
        Label d = new Label(day);
        d.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:18px;-fx-font-weight:bold;");
        Label m = new Label(mon);
        m.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10px;-fx-font-weight:bold;");
        date.getChildren().addAll(d, m);
        VBox t = new VBox(2);
        Label a = new Label(name);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:14px;-fx-font-weight:bold;");
        Label b = new Label(meta);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        t.getChildren().addAll(a, b);
        HBox.setHgrow(t, Priority.ALWAYS);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;");
        h.getChildren().addAll(date, t, dot);
        return h;
    }

    private VBox membershipPanel() {
        VBox c = new VBox(16);
        c.setPrefWidth(340);
        c.setPadding(new Insets(22));
        c.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:18;-fx-border-color:" + STROKE + ";-fx-border-radius:18;");
        c.getChildren().add(rowHeader("Membership", ""));
        VBox card = new VBox(16);
        card.setPadding(new Insets(22));
        card.setStyle("-fx-background-color:linear-gradient(to bottom right,#2a2310,#1a1407);-fx-background-radius:16;-fx-border-color:rgba(217,180,106,0.4);-fx-border-radius:16;");
        HBox top = new HBox();
        Label g = new Label("GoNature");
        g.setStyle("-fx-text-fill:" + GOLD + ";-fx-font-size:15px;-fx-font-weight:bold;");
        Region gr = new Region(); HBox.setHgrow(gr, Priority.ALWAYS);
        Label tier = new Label("✦ GOLD GUIDE");
        tier.setStyle("-fx-text-fill:" + GOLD + ";-fx-font-size:11px;-fx-font-weight:bold;");
        top.getChildren().addAll(g, gr, tier);
        Label num = new Label("MEMBER · 20419");
        num.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:13px;-fx-letter-spacing:2px;");
        Label nm = new Label("Tamar Shapira");
        nm.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:17px;-fx-font-weight:bold;");
        Label perk = new Label("Priority capacity · 10% off every visit · family of 6");
        perk.setWrapText(true);
        perk.setStyle("-fx-text-fill:#cabf9f;-fx-font-size:11.5px;");
        card.getChildren().addAll(top, num, nm, perk);
        c.getChildren().add(card);
        Label manage = new Label("Manage membership  →");
        manage.setStyle("-fx-text-fill:" + GOLD + ";-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;");
        c.getChildren().add(manage);
        return c;
    }

    /* ------------- confirmation modal ------------- */
    private void openModal() {
        if (modalLayer != null) return;
        modalLayer = new StackPane();
        modalLayer.setStyle("-fx-background-color:rgba(5,5,7,0.72);");

        VBox modal = new VBox(16);
        modal.setMaxSize(440, 430);
        modal.setPadding(new Insets(30));
        modal.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:18;-fx-border-color:" + STROKE + ";-fx-border-radius:18;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.6), 36, 0, 0, 14);");
        StackPane check = new StackPane(new Circle(28, Color.web(GREEN, 0.16)), new Label("✓"){{ setStyle("-fx-text-fill:" + GREEN + ";-fx-font-size:26px;-fx-font-weight:bold;"); }});
        Label t = new Label("Confirm your reservation");
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:21px;-fx-font-weight:bold;");
        Label s = new Label("Masada National Park · Sunrise experience");
        s.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        VBox sum = new VBox(0);
        sum.setStyle("-fx-background-color:#1b1e24;-fx-background-radius:12;");
        sum.getChildren().addAll(mrow("Date", "Fri, 27 Jun 2026 · 05:30", false), mrow("Visitors", "4 (subscriber rate)", false), mrow("Total", "₪306.00", true));
        HBox btns = new HBox(12);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Label cancel = new Label("Not now");
        cancel.setPadding(new Insets(12, 22, 12, 22));
        cancel.setStyle("-fx-background-color:transparent;-fx-border-color:" + STROKE + ";-fx-border-radius:10;-fx-text-fill:" + MUTED + ";-fx-font-size:13.5px;-fx-cursor:hand;");
        cancel.setOnMouseClicked(e -> closeModal());
        Label confirm = new Label("Confirm & pay");
        confirm.setPadding(new Insets(12, 26, 12, 26));
        confirm.setStyle("-fx-background-color:" + GOLD + ";-fx-text-fill:#231a08;-fx-background-radius:10;-fx-font-weight:bold;-fx-font-size:13.5px;-fx-cursor:hand;");
        confirm.setOnMouseClicked(e -> closeModal());
        btns.getChildren().addAll(cancel, confirm);
        VBox.setMargin(btns, new Insets(6, 0, 0, 0));
        modal.getChildren().addAll(check, t, s, sum, btns);
        modal.setAlignment(Pos.TOP_LEFT);

        modalLayer.getChildren().add(modal);
        modalLayer.setOnMouseClicked(e -> { if (e.getTarget() == modalLayer) closeModal(); });
        rootStack.getChildren().add(modalLayer);
    }
    private void closeModal() {
        rootStack.getChildren().remove(modalLayer);
        modalLayer = null;
    }
    private HBox mrow(String k, String v, boolean total) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(13, 16, 13, 16));
        if (!total) h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        Label a = new Label(k);
        a.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:13px;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label b = new Label(v);
        b.setStyle("-fx-text-fill:" + (total ? GOLD : TEXT) + ";-fx-font-size:" + (total ? "17px" : "13px") + ";-fx-font-weight:bold;");
        h.getChildren().addAll(a, g, b);
        return h;
    }

    public static void main(String[] args) { launch(args); }
}
