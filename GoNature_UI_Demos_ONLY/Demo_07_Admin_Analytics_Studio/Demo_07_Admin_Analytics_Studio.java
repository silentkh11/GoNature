import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Base64;

/**
 * GoNature UI Demo 07 — ADMIN ANALYTICS STUDIO
 * ------------------------------------------------------------
 * A dark, SaaS-grade analytics workspace for managers: filter bar, KPI cards,
 * real JavaFX charts (visitor trend, capacity line, visitor-type pie), an export
 * row, and a clean data table. Charts are dark-themed via a self-contained
 * data-URI stylesheet (no external .css file).
 *
 * DEMO-ONLY. Self-contained. No DB / network / original-project imports.
 */
public class Demo_07_Admin_Analytics_Studio extends Application {

    static final String BG     = "#0d1014";
    static final String PANEL  = "#13171d";
    static final String CARD   = "#181d25";
    static final String STROKE = "#262d38";
    static final String TEXT   = "#e9eef5";
    static final String MUTED  = "#8a97a8";
    static final String ACCENT = "#5b8cff";
    static final String GREEN  = "#3ed598";
    static final String PURPLE = "#a78bfa";
    static final String AMBER  = "#f7b955";

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:" + BG + ";");
        root.setTop(header());

        VBox content = new VBox(20);
        content.setPadding(new Insets(22, 26, 32, 26));
        content.getChildren().addAll(filterBar(), kpiRow(), chartsRow(), tableCard());

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + BG + ";-fx-background-color:" + BG + ";");
        root.setCenter(sp);

        Scene scene = new Scene(root, 1280, 760);
        scene.getStylesheets().add(darkChartCss());
        stage.setScene(scene);
        stage.setTitle("GoNature — Admin Analytics Studio (Demo 07)");
        stage.show();
    }

    private HBox header() {
        HBox h = new HBox(14);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(16, 26, 16, 26));
        h.setStyle("-fx-background-color:" + PANEL + ";-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        StackPane logo = new StackPane(new Circle(14, Color.web(ACCENT)), new Label("📊"){{ setStyle("-fx-font-size:12px;"); }});
        VBox t = new VBox(-2);
        Label a = new Label("Analytics Studio");
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:17px;-fx-font-weight:bold;");
        Label b = new Label("Department Manager · all parks");
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:11.5px;");
        t.getChildren().addAll(a, b);
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        h.getChildren().addAll(logo, t, g, exportBtn("⬇  Export PDF", true), exportBtn("⤓  CSV", false), exportBtn("⎙  Print", false));
        return h;
    }
    private Label exportBtn(String text, boolean primary) {
        Label l = new Label(text);
        l.setPadding(new Insets(9, 16, 9, 16));
        HBox.setMargin(l, new Insets(0, 0, 0, 8));
        if (primary) l.setStyle("-fx-background-color:" + ACCENT + ";-fx-text-fill:#08101f;-fx-background-radius:8;-fx-font-weight:bold;-fx-font-size:12.5px;-fx-cursor:hand;");
        else l.setStyle("-fx-background-color:" + CARD + ";-fx-text-fill:" + TEXT + ";-fx-background-radius:8;-fx-border-color:" + STROKE + ";-fx-border-radius:8;-fx-font-size:12.5px;-fx-cursor:hand;");
        return l;
    }

    private HBox filterBar() {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(14, 16, 14, 16));
        h.setStyle("-fx-background-color:" + CARD + ";-fx-background-radius:12;-fx-border-color:" + STROKE + ";-fx-border-radius:12;");
        h.getChildren().add(chip("📅  May 2026", true));
        h.getChildren().add(chip("🏞  All parks", false));
        h.getChildren().add(chip("👥  All visitor types", false));
        h.getChildren().add(chip("📈  Daily", false));
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        h.getChildren().add(g);
        Label apply = new Label("Apply filters");
        apply.setPadding(new Insets(9, 18, 9, 18));
        apply.setStyle("-fx-background-color:" + GREEN + ";-fx-text-fill:#06231a;-fx-background-radius:8;-fx-font-weight:bold;-fx-font-size:12.5px;-fx-cursor:hand;");
        h.getChildren().add(apply);
        return h;
    }
    private Label chip(String text, boolean active) {
        Label l = new Label(text + "   ▾");
        l.setPadding(new Insets(9, 14, 9, 14));
        l.setStyle("-fx-background-color:" + (active ? ACCENT + "22" : PANEL) + ";-fx-text-fill:" + (active ? ACCENT : MUTED)
                + ";-fx-background-radius:8;-fx-border-color:" + (active ? ACCENT + "55" : STROKE) + ";-fx-border-radius:8;-fx-font-size:12.5px;-fx-cursor:hand;" + (active ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    private HBox kpiRow() {
        HBox h = new HBox(16);
        h.getChildren().addAll(
            kpi("Total Visitors", "38,420", "+9.2%", GREEN),
            kpi("Avg. Daily Occupancy", "71%", "+3.1%", ACCENT),
            kpi("Cancellation Rate", "6.4%", "−1.2%", GREEN),
            kpi("Waitlist Conversions", "812", "+14%", PURPLE)
        );
        for (var n : h.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        return h;
    }
    private VBox kpi(String label, String value, String delta, String color) {
        VBox v = new VBox(7);
        v.setPadding(new Insets(18));
        v.setStyle(cardCss());
        Label l = new Label(label.toUpperCase());
        l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:27px;-fx-font-weight:bold;");
        Label d = new Label(delta + "  vs last month");
        d.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11.5px;-fx-font-weight:bold;");
        v.getChildren().addAll(l, val, d);
        return v;
    }

    private HBox chartsRow() {
        HBox h = new HBox(16);
        VBox left = chartCard("Visitor Trend", "Daily visitors across all parks", visitorBarChart());
        HBox.setHgrow(left, Priority.ALWAYS);
        VBox right = chartCard("Visitor Segmentation", "By order type, May 2026", typePie());
        right.setPrefWidth(380);
        h.getChildren().addAll(left, right);
        return h;
    }

    private BarChart<String, Number> visitorBarChart() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        x.setTickLabelFill(Color.web(MUTED));
        y.setTickLabelFill(Color.web(MUTED));
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);
        chart.setBarGap(2);
        chart.setCategoryGap(10);
        chart.setAnimated(false);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        String[] days = {"Wk1", "Wk2", "Wk3", "Wk4", "Wk5"};
        int[] vals = {6200, 7100, 6800, 8300, 9000};
        for (int i = 0; i < days.length; i++) s.getData().add(new XYChart.Data<>(days[i], vals[i]));
        chart.getData().add(s);
        return chart;
    }

    private PieChart typePie() {
        PieChart pie = new PieChart(FXCollections.observableArrayList(
            new PieChart.Data("Subscriber", 44),
            new PieChart.Data("Solo / Family", 31),
            new PieChart.Data("Guided Group", 16),
            new PieChart.Data("Walk-in", 9)
        ));
        pie.setLegendSide(Side.BOTTOM);
        pie.setPrefHeight(260);
        pie.setLabelsVisible(false);
        pie.setAnimated(false);
        return pie;
    }

    private VBox chartCard(String title, String sub, javafx.scene.Node chart) {
        VBox v = new VBox(4);
        v.setPadding(new Insets(20));
        v.setStyle(cardCss());
        Label a = new Label(title);
        a.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:15px;-fx-font-weight:bold;");
        Label b = new Label(sub);
        b.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12px;");
        VBox.setMargin(chart, new Insets(8, 0, 0, 0));
        v.getChildren().addAll(a, b, chart);
        return v;
    }

    private VBox tableCard() {
        VBox v = new VBox(0);
        v.setPadding(new Insets(20));
        v.setStyle(cardCss());
        HBox head = new HBox();
        head.setAlignment(Pos.CENTER_LEFT);
        Label t = new Label("Per-Park Breakdown");
        t.setStyle("-fx-text-fill:" + TEXT + ";-fx-font-size:15px;-fx-font-weight:bold;");
        Region g = new Region(); HBox.setHgrow(g, Priority.ALWAYS);
        Label more = new Label("View full report  →");
        more.setStyle("-fx-text-fill:" + ACCENT + ";-fx-font-size:12.5px;-fx-font-weight:bold;-fx-cursor:hand;");
        head.getChildren().addAll(t, g, more);
        VBox.setMargin(head, new Insets(0, 0, 14, 0));
        v.getChildren().add(head);

        v.getChildren().add(thead());
        v.getChildren().add(trow("Masada National Park", "12,840", "78%", "5.1%", "Healthy", GREEN));
        v.getChildren().add(trow("Caesarea National Park", "10,210", "82%", "7.0%", "Watch", AMBER));
        v.getChildren().add(trow("Timna Valley Park", "8,930", "54%", "4.2%", "Healthy", GREEN));
        v.getChildren().add(trow("Nahal Ayun Nature Reserve", "6,440", "91%", "9.3%", "Near full", AMBER));
        return v;
    }
    private HBox thead() {
        HBox h = new HBox();
        h.setPadding(new Insets(0, 8, 12, 8));
        String[] cols = {"PARK", "VISITORS", "OCCUPANCY", "CANCEL %", "STATUS"};
        double[] w = {280, 130, 130, 120, 140};
        for (int i = 0; i < cols.length; i++) {
            Label l = new Label(cols[i]);
            l.setPrefWidth(w[i]);
            l.setStyle("-fx-text-fill:#5d6776;-fx-font-size:10.5px;-fx-font-weight:bold;-fx-letter-spacing:0.5px;");
            h.getChildren().add(l);
        }
        return h;
    }
    private HBox trow(String park, String visitors, String occ, String cancel, String status, String color) {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(13, 8, 13, 8));
        h.setStyle("-fx-border-color:" + STROKE + ";-fx-border-width:0 0 1 0;");
        h.getChildren().add(tcell(park, 280, TEXT, true));
        h.getChildren().add(tcell(visitors, 130, TEXT, false));
        h.getChildren().add(tcell(occ, 130, MUTED, false));
        h.getChildren().add(tcell(cancel, 120, MUTED, false));
        Label chip = new Label("● " + status);
        chip.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12.5px;-fx-font-weight:bold;");
        HBox cw = new HBox(chip); cw.setPrefWidth(140);
        h.getChildren().add(cw);
        return h;
    }
    private Label tcell(String text, double w, String color, boolean bold) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:13px;" + (bold ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    private String cardCss() {
        return "-fx-background-color:" + CARD + ";-fx-background-radius:14;-fx-border-color:" + STROKE
             + ";-fx-border-radius:14;-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.35), 16, 0, 0, 5);";
    }

    /** Dark theme for JavaFX charts, delivered as a self-contained base64 data-URI stylesheet. */
    private String darkChartCss() {
        String css =
            ".chart{-fx-background-color:transparent;-fx-padding:4;}" +
            ".chart-plot-background{-fx-background-color:#10141a;}" +
            ".chart-vertical-grid-lines{-fx-stroke:#222a35;}" +
            ".chart-horizontal-grid-lines{-fx-stroke:#222a35;}" +
            ".chart-alternative-row-fill{-fx-fill:transparent;-fx-stroke:transparent;}" +
            ".axis{-fx-tick-label-fill:#8a97a8;-fx-font-size:11px;}" +
            ".axis-tick-mark,.axis-minor-tick-mark{-fx-stroke:#2a323d;}" +
            ".chart-legend{-fx-background-color:transparent;-fx-text-fill:#e9eef5;}" +
            ".chart-legend-item{-fx-text-fill:#c5cfdc;}" +
            ".default-color0.chart-bar{-fx-bar-fill:linear-gradient(to top,#3a5fd0,#5b8cff);}" +
            ".default-color0.chart-pie{-fx-pie-color:#5b8cff;}" +
            ".default-color1.chart-pie{-fx-pie-color:#3ed598;}" +
            ".default-color2.chart-pie{-fx-pie-color:#a78bfa;}" +
            ".default-color3.chart-pie{-fx-pie-color:#f7b955;}" +
            ".chart-pie-label{-fx-fill:#c5cfdc;}";
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(css.getBytes());
    }

    public static void main(String[] args) { launch(args); }
}
