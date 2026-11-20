package boundary;

import control.HarvestReadinessController;
import control.HarvestReadinessController.Estimate;
import entity.Mission;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Harvest page (boundary for UC-06 Determine Harvest Readiness): readiness by
 * field, the harvest plan with window and yield estimate, and actions for the
 * selected field.
 */
public final class HarvestReadinessUI implements Page {

    private final HarvestReadinessController controller;
    private final Consumer<String> status;

    private final VBox chartBox = new VBox();
    private final GridPane details = new GridPane();
    private final TableView<Estimate> table = new TableView<>();
    private final Label banner = new Label();
    private final Button scan = new Button("Schedule Harvest Scan");
    private final Button markReady = new Button("Mark Ready to Harvest");
    private final BorderPane view = new BorderPane();

    public HarvestReadinessUI(HarvestReadinessController controller, Consumer<String> status) {
        this.controller = controller;
        this.status = status;

        view.setTop(Page.header("Harvest Readiness",
                "UC-06 Determine Harvest Readiness: readiness, best harvest window and yield estimate per field"));

        VBox chartCard = Page.card("Readiness by field", chartBox);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        details.setHgap(16);
        details.setVgap(8);
        VBox detailCard = Page.card("Selected field", details);
        detailCard.setPrefWidth(420);
        detailCard.setMinWidth(420);
        HBox top = new HBox(12, chartCard, detailCard);

        table.getColumns().add(column("Field", e -> e.field().name()));
        table.getColumns().add(column("Crop", e -> e.field().cropType()));
        table.getColumns().add(column("Readiness", e -> e.readiness() + "%"));
        table.getColumns().add(column("Harvest window", Estimate::window));
        table.getColumns().add(column("Est. yield", Estimate::yield));
        table.getColumns().add(column("Data age", e -> e.dataAgeDays() + " days"));
        table.getColumns().add(column("Status", Estimate::status));
        styleCells();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(26);
        table.setPrefHeight(4 * 26 + 32);
        table.setMinHeight(4 * 26 + 32);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> showDetails(now));

        banner.getStyleClass().add("warning-banner");
        banner.setMaxWidth(Double.MAX_VALUE);
        banner.setWrapText(true);

        scan.setOnAction(e -> scheduleScan());
        markReady.getStyleClass().add("primary-button");
        markReady.setOnAction(e -> markReady());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, markReady, scan, spacer, new Label("Actions apply to the field selected in the plan."));
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox planCard = Page.card("Harvest plan", table, banner, actions);

        VBox body = new VBox(12, top, planCard);
        body.setPadding(new Insets(4, 16, 16, 16));
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        view.setCenter(scroll);

        refresh(null);
    }

    @Override public String getNavLabel() { return "Harvest"; }
    @Override public Node getView() { return view; }

    @Override
    public void onShow() {
        refresh(table.getSelectionModel().getSelectedItem());
        status.accept("Select a field in the harvest plan to see its details and actions.");
    }

    private void refresh(Estimate keep) {
        List<Estimate> estimates = controller.getReadiness();
        table.setItems(FXCollections.observableArrayList(estimates));
        int index = 0;
        for (int i = 0; i < estimates.size(); i++) {
            if (keep != null && estimates.get(i).field().fieldId() == keep.field().fieldId()) index = i;
        }
        table.getSelectionModel().select(index);

        chartBox.getChildren().setAll(chart(estimates));

        List<Estimate> stale = estimates.stream().filter(Estimate::stale).toList();
        if (stale.isEmpty()) {
            banner.setVisible(false);
            banner.setManaged(false);
        } else {
            banner.setText("⚠ " + stale.stream().map(e -> e.field().name()).collect(Collectors.joining(", "))
                    + (stale.size() == 1 ? ": crop data is " : ": crop data is up to ")
                    + stale.stream().mapToLong(Estimate::dataAgeDays).max().getAsLong()
                    + " days old, so the estimate may be outdated. Schedule a harvest scan to refresh it.");
            banner.setVisible(true);
            banner.setManaged(true);
        }
    }

    private Node chart(List<Estimate> estimates) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 20);
        y.setLabel("Readiness %");
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(50);
        chart.setPrefHeight(250);
        chart.setMinHeight(250);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (Estimate e : estimates) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(e.field().name(), e.readiness());
            String color = e.readiness() >= 90 ? "#2e7d32" : e.readiness() >= 60 ? "#7cb342" : "#f9a825";
            d.nodeProperty().addListener((obs, old, node) -> {
                if (node != null) node.setStyle("-fx-bar-fill: " + color + ";");
            });
            s.getData().add(d);
        }
        chart.getData().add(s);
        return chart;
    }

    private void showDetails(Estimate e) {
        details.getChildren().clear();
        boolean none = e == null;
        scan.setDisable(none);
        markReady.setDisable(none || e.readiness() < 60 || e.status().equals("Ready to harvest"));
        if (none) return;
        String[][] rows = {
            { "Field", e.field().name() + " (" + e.field().cropType() + ")" },
            { "Growth stage", e.stage() },
            { "Readiness", e.readiness() + "%" },
            { "Harvest window", e.window() },
            { "Estimated yield", e.yield() },
            { "Last crop flight", e.lastFlight().format(SoilMoistureUI.DATE) + " (" + e.dataAgeDays() + " days ago)" },
            { "Status", e.status() },
        };
        for (int i = 0; i < rows.length; i++) {
            Label n = new Label(rows[i][0]);
            n.getStyleClass().add("detail-label");
            details.addRow(i, n, new Label(rows[i][1]));
        }
        Label inputs = new Label("Estimated from the latest drone NDVI flight (UC-05): growth stage, "
                + "NDVI peak for the yield, and data age.");
        inputs.getStyleClass().add("detail-label");
        inputs.setWrapText(true);
        inputs.setMaxWidth(390);
        details.add(inputs, 0, rows.length, 2, 1);
    }

    private void scheduleScan() {
        Estimate e = table.getSelectionModel().getSelectedItem();
        if (e == null) return;
        Mission m = controller.scheduleHarvestScan(e.field());
        status.accept("Harvest scan of " + e.field().name() + " scheduled as mission #" + m.missionId()
                + " (drone " + m.droneId() + ", " + m.scheduledTime().format(SoilMoistureUI.DATE) + " 10:30).");
    }

    private void markReady() {
        Estimate e = table.getSelectionModel().getSelectedItem();
        if (e == null) return;
        try {
            controller.markReadyToHarvest(e.field());
            refresh(e);
            status.accept(e.field().name() + " marked Ready to harvest (window " + e.window() + ").");
        } catch (IllegalArgumentException ex) {
            status.accept("Not marked: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ table helpers

    /** Colors the Readiness, Data age and Status cells. */
    private void styleCells() {
        for (TableColumn<Estimate, ?> raw : table.getColumns()) {
            @SuppressWarnings("unchecked")
            TableColumn<Estimate, String> col = (TableColumn<Estimate, String>) raw;
            String name = col.getText();
            if (!List.of("Readiness", "Data age", "Status").contains(name)) continue;
            col.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(String text, boolean empty) {
                    super.updateItem(text, empty);
                    setText(empty ? null : text);
                    getStyleClass().removeAll("status-active", "status-warning", "status-missing");
                    Estimate e = empty || getTableRow() == null ? null : getTableRow().getItem();
                    if (e == null) return;
                    String style = switch (name) {
                        case "Readiness" -> e.readiness() >= 90 ? "status-active" : e.readiness() >= 60 ? null : "status-warning";
                        case "Data age" -> e.stale() ? "status-missing" : null;
                        default -> e.status().startsWith("Ready") ? "status-active" : null;
                    };
                    if (style != null) getStyleClass().add(style);
                }
            });
        }
    }

    private static TableColumn<Estimate, String> column(String name, Function<Estimate, String> value) {
        TableColumn<Estimate, String> col = new TableColumn<>(name);
        col.setCellValueFactory(c -> new ReadOnlyStringWrapper(value.apply(c.getValue())));
        col.setSortable(false);
        return col;
    }
}
