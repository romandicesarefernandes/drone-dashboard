package boundary;

import control.SoilMoistureController;
import control.SoilMoistureController.Zone;
import entity.Field;
import entity.Mission;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Soil Moisture page (boundary for UC-04 Monitor Soil Moisture): moisture heat
 * map of one field scan, summary, alert thresholds and the 30-day trend.
 */
public final class SoilMoistureUI implements Page {

    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US);
    private static final double CELL = 30;

    private final SoilMoistureController controller;
    private final Supplier<Window> owner;
    private final Consumer<String> status;

    private final ComboBox<Field> field = new ComboBox<>();
    private final ComboBox<LocalDate> date = new ComboBox<>();
    private final GridPane heatMap = new GridPane();
    private final GridPane summary = new GridPane();
    private final TextField low = new TextField();
    private final TextField high = new TextField();
    private final Label banner = new Label();
    private final VBox trendBox = new VBox();
    private final BorderPane view = new BorderPane();

    public SoilMoistureUI(SoilMoistureController controller, Supplier<Window> owner, Consumer<String> status) {
        this.controller = controller;
        this.owner = owner;
        this.status = status;

        Button scan = new Button("Request Scan");
        scan.setOnAction(e -> requestScan());
        Button export = new Button("Export");
        export.setOnAction(e -> export());
        view.setTop(Page.header("Soil Moisture", "UC-04 Monitor Soil Moisture: moisture by zone from drone scans",
                scan, export));

        field.getItems().addAll(controller.fields());
        field.setOnAction(e -> fieldChanged());
        date.setConverter(dateConverter());
        date.setOnAction(e -> refresh());
        HBox filters = new HBox(10, new Label("Field"), field, new Label("Scan date"), date);
        filters.setAlignment(Pos.CENTER_LEFT);

        heatMap.setHgap(1);
        heatMap.setVgap(1);
        VBox mapCard = Page.card("Moisture heat map (click a zone)", heatMap, legend("Dry 5%", "Wet 50%",
                SoilMoistureUI::moistureColor, 5, 50));

        summary.setHgap(20);
        summary.setVgap(8);
        VBox summaryCard = Page.card("Summary", summary);

        low.setPrefColumnCount(5);
        high.setPrefColumnCount(5);
        Button save = new Button("Save Thresholds");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> saveThresholds());
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Low (irrigate below) %"), low);
        form.addRow(1, new Label("High (waterlogged above) %"), high);
        VBox thresholdCard = Page.card("Alert thresholds", form, save);

        VBox right = new VBox(12, summaryCard, thresholdCard);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox top = new HBox(12, mapCard, right);

        banner.getStyleClass().add("warning-banner");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setWrapText(true);

        VBox trendCard = Page.card("Moisture trend, last 30 days", trendBox);

        VBox body = new VBox(12, filters, top, banner, trendCard);
        body.setPadding(new Insets(4, 16, 16, 16));
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        view.setCenter(scroll);

        field.setValue(controller.fields().get(2));   // South Field: the scenario's dry spell
        fieldChanged();
        date.setValue(LocalDate.of(2026, 10, 23));   // scan nearest the 22 Oct low
        refresh();
    }

    @Override public String getNavLabel() { return "Soil Moisture"; }
    @Override public Node getView() { return view; }

    @Override
    public void onShow() {
        status.accept("Pick a field and scan date. Click a zone on the heat map for its reading.");
    }

    // ------------------------------------------------------------ display

    private void fieldChanged() {
        Field f = field.getValue();
        if (f == null) return;
        double[] t = controller.getThresholds(f.fieldId());
        low.setText(fmt(t[0]));
        high.setText(fmt(t[1]));
        date.getItems().setAll(controller.scanDates(f.fieldId()));
        date.setValue(date.getItems().isEmpty() ? null : date.getItems().get(0));
        refresh();
    }

    private void refresh() {
        Field f = field.getValue();
        LocalDate d = date.getValue();
        heatMap.getChildren().clear();
        summary.getChildren().clear();
        trendBox.getChildren().clear();
        if (f == null || d == null) return;

        List<Zone> zones = controller.getMoistureMap(f.fieldId(), d);
        if (zones.isEmpty()) return;
        for (Zone z : zones) {
            Rectangle r = new Rectangle(CELL, CELL, moistureColor(z.moisture()));
            Tooltip.install(r, new Tooltip("Zone " + z.name() + ": " + fmt(z.moisture()) + "%"));
            r.setOnMouseClicked(e -> status.accept("Zone " + z.name() + " of " + f.name() + ": "
                    + fmt(z.moisture()) + "% moisture (scan " + d.format(DATE) + ", 08:00)."));
            heatMap.add(r, z.col(), z.row());
        }

        List<Zone> dry = controller.zonesBelowLow(f.fieldId(), zones);
        Zone driest = SoilMoistureController.driest(zones);
        Zone wettest = SoilMoistureController.wettest(zones);
        summaryRow(0, "Field average", fmt(SoilMoistureController.average(zones)) + "%", null);
        summaryRow(1, "Driest zone", fmt(driest.moisture()) + "% (" + driest.name() + ")", "status-missing");
        summaryRow(2, "Wettest zone", fmt(wettest.moisture()) + "% (" + wettest.name() + ")", "status-idle");
        summaryRow(3, "Zones below threshold", dry.size() + " of " + zones.size(),
                dry.isEmpty() ? "status-active" : "status-warning");

        if (dry.isEmpty()) {
            banner.setVisible(false);
            banner.setManaged(false);
        } else {
            String names = dry.stream().limit(6).map(Zone::name).collect(Collectors.joining(", "))
                    + (dry.size() > 6 ? " and " + (dry.size() - 6) + " more" : "");
            banner.setText("⚠ " + f.name() + " zones " + names + " are below "
                    + fmt(controller.getThresholds(f.fieldId())[0]) + "%. Irrigation recommended within 24 h.");
            banner.setVisible(true);
            banner.setManaged(true);
        }

        trendBox.getChildren().add(trendChart(f, d));
    }

    private void summaryRow(int row, String name, String value, String style) {
        Label n = new Label(name);
        n.getStyleClass().add("detail-label");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        if (style != null) v.getStyleClass().add(style);
        summary.addRow(row, n, v);
    }

    private Node trendChart(Field f, LocalDate d) {
        List<double[]> points = controller.trend(f.fieldId(), d);
        double[] t = controller.getThresholds(f.fieldId());
        NumberAxis x = new NumberAxis();
        x.setForceZeroInRange(false);
        x.setAutoRanging(false);
        x.setLowerBound(d.minusDays(30).toEpochDay());
        x.setUpperBound(d.toEpochDay());
        x.setTickUnit(7);
        x.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) {
                return LocalDate.ofEpochDay(n.longValue()).format(DateTimeFormatter.ofPattern("MMM d", Locale.US));
            }
            @Override public Number fromString(String s) { return 0; }
        });
        NumberAxis y = new NumberAxis(0, 80, 10);
        y.setLabel("Moisture %");
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(230);
        chart.setMinHeight(230);
        chart.getData().add(series("Field average", points, 1));
        chart.getData().add(series("Driest zone", points, 2));
        chart.getData().add(line("Low " + fmt(t[0]) + "%", t[0], d));
        chart.getData().add(line("High " + fmt(t[1]) + "%", t[1], d));
        return chart;
    }

    private static XYChart.Series<Number, Number> series(String name, List<double[]> points, int index) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(name);
        for (double[] p : points) s.getData().add(new XYChart.Data<>(p[0], p[index]));
        return s;
    }

    private static XYChart.Series<Number, Number> line(String name, double value, LocalDate d) {
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(name);
        s.getData().add(new XYChart.Data<>(d.minusDays(30).toEpochDay(), value));
        s.getData().add(new XYChart.Data<>(d.toEpochDay(), value));
        return s;
    }

    // ------------------------------------------------------------ actions

    private void saveThresholds() {
        Field f = field.getValue();
        try {
            controller.saveThresholds(f.fieldId(), Double.parseDouble(low.getText().trim()),
                    Double.parseDouble(high.getText().trim()));
            refresh();
            status.accept("Thresholds saved for " + f.name() + ": alert below " + low.getText().trim()
                    + "% or above " + high.getText().trim() + "%.");
        } catch (NumberFormatException ex) {
            status.accept("Thresholds not saved: enter numbers, e.g. 20 and 45.");
        } catch (IllegalArgumentException ex) {
            status.accept("Thresholds not saved: " + ex.getMessage());
        }
    }

    private void requestScan() {
        Field f = field.getValue();
        Mission m = controller.requestScan(f.fieldId());
        status.accept("Soil-moisture scan of " + f.name() + " scheduled as mission #" + m.missionId()
                + " (drone " + m.droneId() + ", " + m.scheduledTime().format(DATE) + " 07:30).");
    }

    private void export() {
        Field f = field.getValue();
        LocalDate d = date.getValue();
        if (f == null || d == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export zone readings");
        chooser.setInitialFileName("Soil_Moisture_" + f.name().replace(' ', '_') + "_" + d + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.isDirectory()) chooser.setInitialDirectory(downloads);
        File file = chooser.showSaveDialog(owner.get());
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), controller.toCsv(f.fieldId(), d));
            status.accept("Exported zone readings: " + file);
        } catch (IOException ex) {
            status.accept("Export failed: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ helpers

    /** Light orange (dry) to dark blue (wet), 5% to 50%. */
    static Color moistureColor(double v) {
        double t = Math.max(0, Math.min(1, (v - 5) / 45));
        return Color.web("#fdae6b").interpolate(Color.web("#e3eef8"), Math.min(1, t * 3))
                .interpolate(Color.web("#08519c"), Math.max(0, (t - 0.33) * 1.5));
    }

    /** Color bar with a label at each end. */
    static HBox legend(String left, String right, java.util.function.DoubleFunction<Color> color,
                       double from, double to) {
        HBox bar = new HBox();
        for (int i = 0; i < 8; i++) bar.getChildren().add(new Rectangle(24, 10, color.apply(from + (to - from) * i / 7.0)));
        Label l = new Label(left);
        Label r = new Label(right);
        l.getStyleClass().add("detail-label");
        r.getStyleClass().add("detail-label");
        HBox box = new HBox(8, l, bar, r);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static StringConverter<LocalDate> dateConverter() {
        return new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d == null ? "" : d.format(DATE); }
            @Override public LocalDate fromString(String s) { return null; }
        };
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.US, "%.1f", v);
    }
}
