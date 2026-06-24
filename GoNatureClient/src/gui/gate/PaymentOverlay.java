package gui.gate;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.*;
import javafx.util.Duration;

/**
 * Full-window payment simulation overlay for the Gate Terminal.
 * Plays: NFC tap prompt → iOS-style ring + checkmark animation → "Payment Accepted".
 * Covers the owner stage exactly and auto-closes after ~4 seconds.
 */
public final class PaymentOverlay {

    private PaymentOverlay() {}

    /**
     * Opens a modal payment overlay covering {@code owner}.
     * @param owner      the Gate Terminal stage
     * @param onComplete called on the JavaFX thread after the overlay closes (may be null)
     */
    public static void show(Stage owner, Runnable onComplete) {
        Stage overlay = new Stage();
        overlay.initOwner(owner);
        overlay.initModality(Modality.WINDOW_MODAL);
        overlay.initStyle(StageStyle.TRANSPARENT);
        overlay.setResizable(false);

        // ── DARK BACKDROP ─────────────────────────────────────
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.78);");

        // ── CARD ──────────────────────────────────────────────
        VBox card = new VBox();
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(300);
        card.setMinWidth(300);
        card.setMaxWidth(300);
        card.setOpacity(0);
        card.setStyle(
            "-fx-background-color: #0D1F35;" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 48, 0.12, 0, 12);"
        );

        // Header
        Label header = new Label("💳  Payment Terminal");
        header.setStyle(
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 15px;" +
            "-fx-font-weight: bold; -fx-text-fill: #1DC98A;"
        );
        VBox.setMargin(header, new Insets(22, 20, 0, 20));

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxWidth(Double.MAX_VALUE);
        divider.setStyle("-fx-background-color: rgba(29,201,138,0.18);");
        VBox.setMargin(divider, new Insets(14, 0, 18, 0));

        // ── NFC PHASE ─────────────────────────────────────────
        Label nfcEmoji = new Label("🛜");
        nfcEmoji.setStyle("-fx-font-size: 50px;");

        Label nfcTitle = new Label("Tap Card or Phone");
        nfcTitle.setStyle(
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 14px;" +
            "-fx-font-weight: bold; -fx-text-fill: #D5E3EF;"
        );

        Label nfcSub = new Label("Hold your payment device near the terminal");
        nfcSub.setStyle(
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 11.5px; -fx-text-fill: #7A98B2;"
        );
        nfcSub.setWrapText(true);
        nfcSub.setMaxWidth(220);
        nfcSub.setTextAlignment(TextAlignment.CENTER);

        VBox nfcPane = new VBox(10, nfcEmoji, nfcTitle, nfcSub);
        nfcPane.setAlignment(Pos.CENTER);
        nfcPane.setOpacity(0);

        // ── ANIMATION PHASE ───────────────────────────────────
        // Pane with absolute coords; circle center at (65, 65), radius 48
        final double CX = 65, CY = 65, R = 48;

        // Faint background disc
        Circle bgDisc = new Circle(CX, CY, R);
        bgDisc.setFill(Color.web("#1DC98A", 0.10));

        // Ring: animates from 0 → -360 degrees (clockwise from 12 o'clock)
        Arc ring = new Arc(CX, CY, R, R, 90, 0);
        ring.setType(ArcType.OPEN);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#1DC98A"));
        ring.setStrokeWidth(4.0);
        ring.setStrokeLineCap(StrokeLineCap.ROUND);

        // Solid disc that springs in once ring is complete
        Circle solidDisc = new Circle(CX, CY, R);
        solidDisc.setFill(Color.web("#1DC98A"));
        solidDisc.setScaleX(0);
        solidDisc.setScaleY(0);

        // Checkmark path: left(-21,-1) → corner(-3,+17) → right(+24,-14) relative to (CX,CY)
        // Drawn via stroke-dash-offset animation (path length ≈ 67px; use 75 with [75,75] pattern)
        final double TICK_LEN = 75.0;
        Path tick = new Path(
            new MoveTo(CX - 21, CY - 1),
            new LineTo(CX - 3,  CY + 17),
            new LineTo(CX + 24, CY - 14)
        );
        tick.setFill(Color.TRANSPARENT);
        tick.setStroke(Color.WHITE);
        tick.setStrokeWidth(5.5);
        tick.setStrokeLineCap(StrokeLineCap.ROUND);
        tick.setStrokeLineJoin(StrokeLineJoin.ROUND);
        tick.setOpacity(0);
        tick.getStrokeDashArray().addAll(TICK_LEN, TICK_LEN);
        tick.setStrokeDashOffset(TICK_LEN); // starts fully hidden

        Pane animPane = new Pane(bgDisc, ring, solidDisc, tick);
        animPane.setPrefSize(130, 130);
        animPane.setMinSize(130, 130);
        animPane.setMaxSize(130, 130);
        animPane.setOpacity(0);

        // Center area: NFC pane and animation pane layered (cross-fade between them)
        StackPane centerArea = new StackPane(nfcPane, animPane);
        centerArea.setAlignment(Pos.CENTER);
        centerArea.setPrefHeight(170);
        centerArea.setMinHeight(170);
        VBox.setMargin(centerArea, new Insets(0, 10, 0, 10));

        // ── STATUS LABELS ─────────────────────────────────────
        Label lblProcessing = new Label("Processing payment…");
        lblProcessing.setStyle(
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 13px; -fx-text-fill: #7A98B2;"
        );
        lblProcessing.setOpacity(0);

        Label lblAccepted = new Label("Payment Accepted");
        lblAccepted.setStyle(
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 15px;" +
            "-fx-font-weight: bold; -fx-text-fill: #1DC98A;"
        );
        lblAccepted.setOpacity(0);

        Label lblDone = new Label("Returning to terminal…");
        lblDone.setStyle(
            "-fx-font-family: 'Segoe UI'; -fx-font-size: 11px; -fx-text-fill: #4A6680;"
        );
        lblDone.setOpacity(0);

        VBox bottomArea = new VBox(5, lblProcessing, lblAccepted, lblDone);
        bottomArea.setAlignment(Pos.CENTER);
        VBox.setMargin(bottomArea, new Insets(14, 0, 26, 0));

        card.getChildren().addAll(header, divider, centerArea, bottomArea);
        backdrop.getChildren().add(card);

        // ── SCENE ─────────────────────────────────────────────
        double w = owner.getWidth();
        double h = owner.getHeight();
        Scene scene = new Scene(backdrop, w, h, Color.TRANSPARENT);
        overlay.setScene(scene);
        overlay.setWidth(w);
        overlay.setHeight(h);
        overlay.setX(owner.getX());
        overlay.setY(owner.getY());
        overlay.show();

        // ── ANIMATION SEQUENCE ────────────────────────────────
        // T=0-350    card fades in
        // T=350-650  nfcPane + "Processing…" fade in (300ms)
        // T=650-1450 nfc icon pulses up and back (800ms)
        // T=1450-1700 nfcPane out / animPane in (250ms cross-fade)
        // T=1700-2400 ring draws clockwise 0 → -360° (700ms)
        // T=2400     solid disc springs in + ring fades + tick draws
        //   disc:    2400-2680 spring up (280ms) → 2680-2810 settle (130ms)
        //   ring:    2400-2600 fade out (200ms)
        //   tick:    2400-3000 stroke-dash draws (600ms)
        // T=3000-3280 "Processing…" out / "Payment Accepted" in (280ms)
        // T=3280-3530 "Returning…" in (250ms)
        // T=3530-3980 hold 450ms → close

        FadeTransition cardIn   = ft(card,         350, 0, 1);
        FadeTransition nfcIn    = ft(nfcPane,       300, 0, 1);
        FadeTransition procIn   = ft(lblProcessing, 300, 0, 1);

        ScaleTransition nfcPulse = new ScaleTransition(Duration.millis(400), nfcEmoji);
        nfcPulse.setByX(0.16);
        nfcPulse.setByY(0.16);
        nfcPulse.setAutoReverse(true);
        nfcPulse.setCycleCount(2); // forward 400ms + reverse 400ms = 800ms
        nfcPulse.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition nfcOut  = ft(nfcPane,  250, 1, 0);
        FadeTransition animIn  = ft(animPane, 250, 0, 1);

        Timeline ringDraw = new Timeline(
            new KeyFrame(Duration.ZERO,        new KeyValue(ring.lengthProperty(), 0.0)),
            new KeyFrame(Duration.millis(700), new KeyValue(ring.lengthProperty(), -360.0, Interpolator.EASE_BOTH))
        );

        ScaleTransition discSpring = new ScaleTransition(Duration.millis(280), solidDisc);
        discSpring.setFromX(0);   discSpring.setToX(1.12);
        discSpring.setFromY(0);   discSpring.setToY(1.12);
        discSpring.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition discSettle = new ScaleTransition(Duration.millis(130), solidDisc);
        discSettle.setFromX(1.12); discSettle.setToX(1.0);
        discSettle.setFromY(1.12); discSettle.setToY(1.0);
        discSettle.setInterpolator(Interpolator.EASE_IN);

        FadeTransition ringFade = ft(ring, 200, 1, 0);

        Timeline tickDraw = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(tick.opacityProperty(), 0.0),
                new KeyValue(tick.strokeDashOffsetProperty(), TICK_LEN)),
            new KeyFrame(Duration.millis(60),
                new KeyValue(tick.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(600),
                new KeyValue(tick.strokeDashOffsetProperty(), 0.0, Interpolator.EASE_OUT))
        );

        FadeTransition procOut  = ft(lblProcessing, 200, 1, 0);
        FadeTransition acceptIn = ft(lblAccepted,   280, 0, 1);
        FadeTransition doneIn   = ft(lblDone,       250, 0, 1);

        PauseTransition closeWait = new PauseTransition(Duration.millis(450));
        closeWait.setOnFinished(e -> {
            overlay.close();
            if (onComplete != null) onComplete.run();
        });

        new SequentialTransition(
            cardIn,
            new ParallelTransition(nfcIn, procIn),
            nfcPulse,
            new ParallelTransition(nfcOut, animIn),
            ringDraw,
            new ParallelTransition(
                new SequentialTransition(discSpring, discSettle),
                ringFade,
                tickDraw
            ),
            new ParallelTransition(procOut, acceptIn),
            doneIn,
            closeWait
        ).play();
    }

    private static FadeTransition ft(Node node, int ms, double from, double to) {
        FadeTransition f = new FadeTransition(Duration.millis(ms), node);
        f.setFromValue(from);
        f.setToValue(to);
        return f;
    }
}
