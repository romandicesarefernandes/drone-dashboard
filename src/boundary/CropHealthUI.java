package boundary;

import control.CropGrowthController;
import control.CropGrowthController.Cell;
import control.CropGrowthController.ZoneGrowth;
import entity.CropGrowthRecord;
import entity.Field;
import entity.Mission;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

/**
 * Crop Health page (boundary for UC-05 Monitor Crop Growth): NDVI map of one
 * drone flight, actual versus expected growth, and growth by zone.
 */
public final class CropHealthUI implements Page {

    private static final double CELL = 24;

    private final CropGrowthController controller;
    private final Consumer<String> status;

    private final ComboBox<Field> field = new ComboBox<>();
    private final ComboBox<LocalDate> flight = new ComboBox<>();
    private final GridPane ndviMap = new GridPane();
    private final Label stage = new Label();
    private final VBox chartBox = new VBox();
    private final Label banner = new Label();
    private final TableView<ZoneGrowth> table = new TableView<>();
    private final BorderPane view = new BorderPane();

    public CropHealthUI(CropGrowthController controller, Consumer<String> status) {
        this.controller = controller;
        this.status = status;

        Button scan = new Button("Request Scan");
        scan.setOnAction(e -> requestScan());
        view.setTop(Page.header("Crop Health", "UC-05 Monitor Crop Growth: NDVI compared with the expected growth curve",
                scan));

        field.getItems().addAll(controller.fields());
        field.setOnAction(e -> fieldChanged());
        flight.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d == null ? "" : d.format(SoilMoistureUI.DATE); }
            @Override public LocalDate fromString(String s) { return null; }
        });
        flight.setOnAction(e -> refresh());
        HBox filters = new HBox(10, new Label("Field"), field, new Label("Flight"), flight);
        filters.setAlignment(Pos.CENTER_LEFT);

        ndviMap.setHgap(1);
        ndviMap.setVgap(1);
        VBox mapCard = Page.card("NDVI map", ndviMap,
                SoilMoistureUI.legend("Stressed 0.1", "Healthy 0.9", CropHealthUI::ndviColor, 0.1, 0.9));

        stage.setStyle("-fx-font-weight: bold;");
        banner.getStyleClass().add("warning-banner");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setWrapText(true);
        VBox growthCard = Page.card("Growth vs expected", stage, chartBox, banner);
        HBox.setHgrow(growthCard, Priority.ALWAYS);
        HBox top = new HBox(12, mapCard, growthCard);

        table.getColumns().add(column("Zone", ZoneGrowth::zone));
        table.getColumns().add(column("Avg NDVI", z -> String.format(Locale.US, "%.2f", z.ndvi())));
        table.getColumns().add(column("vs expected", z -> (z.vsExpectedPct() > 0 ? "+" : "")
                + SoilMoistureUI.fmt(z.vsExpectedPct()) + "%"));
        table.getColumns().add(column("Last flight", z -> flight.getValue().format(SoilMoistureUI.DATE)));
        TableColumn<ZoneGrowth, String> statusCol = column("Status", ZoneGrowth::status);
        statusCol.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                getStyleClass().removeAll("status-active", "status-warning", "status-missing");
                if (!empty && s != null) getStyleClass().add(switch (s) {
                    case "Below expected" -> "status-missing";
                    case "Watch" -> "status-warning";
                    default -> "status-active";
                });
            }
        });
        table.getColumns().add(statusCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(26);
        table.setPrefHeight(4 * 26 + 32);
        table.setMinHeight(4 * 26 + 32);
        VBox tableCard = Page.card("Growth by zone", table);

        VBox body = new VBox(12, filters, top, tableCard);
        body.setPadding(new Insets(4, 16, 16, 16));
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        view.setCenter(scroll);

        field.setValue(controller.fields().get(1));   // East Orchard: lagging NE corner in the scenario
        fieldChanged();
    }

    @Override public String getNavLabel() { return "Crop Health"; }
    @Override public Node getView() { return view; }

    @Override
    public void onShow() {
        status.accept("Pick a field and flight. Hover over the NDVI map to see a cell's value.");
    }

    private void fieldChanged() {
        Field f = field.getValue();
        if (f == null) return;
        flight.getItems().setAll(controller.flightDates(f.fieldId()));
        flight.setValue(flight.getItems().isEmpty() ? null : flight.getItems().get(0));
        refresh();
    }

    private void refresh() {
        Field f = field.getValue();
        LocalDate d = flight.getValue();
        ndviMap.getChildren().clear();
        chartBox.getChildren().clear();
        if (f == null || d == null) return;

        for (Cell c : controller.ndviMap(f.fieldId(), d)) {
            Rectangle r = new Rectangle(CELL, CELL, ndviColor(c.ndvi()));
            Tooltip.install(r, new Tooltip(c.zone() + " zone: NDVI " + String.format(Locale.US, "%.2f", c.ndvi())));
            ndviMap.add(r, c.col(), c.row());
        }

        CropGrowthRecord rec = controller.record(f.fieldId(), d);
        stage.setText(f.cropType() + "  ·  Stage: " + rec.growthStage() + "  ·  Field NDVI "
                + String.format(Locale.US, "%.2f", rec.ndvi()) + "  ·  Expected "
                + String.format(Locale.US, "%.2f", controller.expectedNdvi(f.fieldId(), d)));
        chartBox.getChildren().add(growthChart(f, d));

        List<ZoneGrowth> zones = controller.growthByZone(f.fieldId(), d);
        table.setItems(FXCollections.observableArrayList(zones));
        List<ZoneGrowth> lagging = zones.stream().filter(z -> z.status().equals("Below expected")).toList();
        if (lagging.isEmpty()) {
            banner.setVisible(false);
            banner.setManaged(false);
        } else {
            banner.setText("⚠ " + lagging.stream().map(z -> z.zone() + " (" + SoilMoistureUI.fmt(z.vsExpectedPct())
                    + "%)").collect(Collectors.joining(", ")) + " growing more than "
                    + CropGrowthController.LAG_THRESHOLD_PCT + "% below expected.");
            banner.setVisible(true);
            banner.setManaged(true);
        }
    }

    private Node growthChart(Field f, LocalDate d) {
        List<double[]> points = controller.growthSeries(f.fieldId(), d);
        NumberAxis x = new NumberAxis();
        x.setForceZeroInRange(false);
        x.setAutoRanging(false);
        x.setLowerBound(points.get(0)[0]);
        x.setUpperBound(Math.max(points.get(0)[0] + 7, d.toEpochDay()));
        x.setTickUnit(14);
        x.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number n) {
                return LocalDate.ofEpochDay(n.longValue()).format(DateTimeFormatter.ofPattern("MMM d", Locale.US));
            }
            @Override public Number fromString(String s) { return 0; }
        });
        NumberAxis y = new NumberAxis(0, 1, 0.2);
        y.setLabel("NDVI");
        LineChart<Number, Number> chart = new LineChart<>(x, y);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setPrefHeight(200);
        chart.setMinHeight(200);
        XYChart.Series<Number, Number> expected = new XYChart.Series<>();
        expected.setName("Expected");
        XYChart.Series<Number, Number> actual = new XYChart.Series<>();
        actual.setName("Actual");
        for (double[] p : points) {
            expected.getData().add(new XYChart.Data<>(p[0], p[2]));
            actual.getData().add(new XYChart.Data<>(p[0], p[1]));
        }
        chart.getData().add(expected);
        chart.getData().add(actual);
        return chart;
    }

    private void requestScan() {
        Field f = field.getValue();
        Mission m = controller.requestScan(f.fieldId());
        status.accept("Crop-growth scan of " + f.name() + " scheduled as mission #" + m.missionId()
                + " (drone " + m.droneId() + ", " + m.scheduledTime().format(SoilMoistureUI.DATE) + " 10:30).");
    }

    /** Red (stressed) through yellow to green (healthy). */
    static Color ndviColor(double v) {
        double t = Math.max(0, Math.min(1, (v - 0.1) / 0.8));
        return t < 0.5 ? Color.web("#d73027").interpolate(Color.web("#fee08b"), t * 2)
                : Color.web("#fee08b").interpolate(Color.web("#1a9850"), (t - 0.5) * 2);
    }

    private static TableColumn<ZoneGrowth, String> column(String name,
                                                          java.util.function.Function<ZoneGrowth, String> value) {
        TableColumn<ZoneGrowth, String> col = new TableColumn<>(name);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        col.setSortable(false);
        return col;
    }
}
