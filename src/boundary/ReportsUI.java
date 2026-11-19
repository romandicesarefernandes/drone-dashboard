package boundary;

import control.ReportController;
import control.ReportExporter;
import entity.Report;
import entity.ReportSchedule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.StringConverter;

/**
 * Reports page (boundary for UC-09 Generate Farm Reports): choose report
 * type, field and period, generate a preview, export it as PDF or CSV,
 * schedule it, and reopen earlier reports from the history.
 */
public final class ReportsUI implements Page {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MM/dd h:mm a", Locale.US);

    private final ReportController controller;
    private final Supplier<Window> owner;
    private final Consumer<String> status;

    private final ComboBox<String> type = new ComboBox<>();
    private final ComboBox<String> field = new ComboBox<>();
    private final DatePicker start = datePicker(LocalDate.of(2026, 10, 1));
    private final DatePicker end = datePicker(LocalDate.of(2026, 10, 31));
    private final ListView<Report> history = new ListView<>();
    private final ListView<ReportSchedule> scheduled = new ListView<>();
    private final VBox preview = new VBox(12);
    private final Button exportPdf = new Button("Export PDF");
    private final Button exportCsv = new Button("Export CSV");
    private final ComboBox<String> frequency = new ComboBox<>(FXCollections.observableArrayList(ReportController.FREQUENCIES));
    private final ComboBox<String> deliveryDay = new ComboBox<>();
    private final BorderPane view = new BorderPane();

    private Report current;
    private boolean showingHistory;   // avoids a loop between history selection and preview

    public ReportsUI(ReportController controller, Supplier<Window> owner, Consumer<String> status) {
        this.controller = controller;
        this.owner = owner;
        this.status = status;

        view.setTop(Page.header("Reports", "UC-09 Generate Farm Reports"));
        view.setLeft(buildLeftColumn());
        view.setCenter(buildPreview());
        view.setBottom(buildActions());
        showReport(null);
    }

    @Override public String getNavLabel() { return "Reports"; }
    @Override public Node getView() { return view; }

    @Override
    public void onShow() {
        status.accept("Choose the report type, field and period, then click Generate Report.");
    }

    // ------------------------------------------------------------ layout

    private VBox buildLeftColumn() {
        type.getItems().addAll(ReportController.REPORT_TYPES);
        type.setValue(ReportController.FARM_SUMMARY);
        field.getItems().addAll(controller.fieldChoices());
        field.setValue(ReportController.ALL_FIELDS);
        for (Region r : List.of(type, field, start, end)) r.setMaxWidth(Double.MAX_VALUE);

        Button generate = new Button("Generate Report");
        generate.getStyleClass().add("primary-button");
        generate.setDefaultButton(true);
        generate.setMaxWidth(Double.MAX_VALUE);
        generate.setOnAction(e -> generate());

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Report Type"), type);
        form.addRow(1, new Label("Field"), field);
        form.addRow(2, new Label("Start"), start);
        form.addRow(3, new Label("End"), end);
        GridPane.setHgrow(type, Priority.ALWAYS);
        VBox settings = Page.card("Report Settings", form, generate);

        history.setPlaceholder(new Label("No reports generated yet."));
        history.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Report r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : "#" + r.getReportId() + "  " + r.getReportType() + " - "
                        + r.getFields() + "\n" + r.getStartDate().format(ReportController.DATE) + " - "
                        + r.getEndDate().format(ReportController.DATE) + "  |  " + r.getFormat());
            }
        });
        history.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (now != null && !showingHistory) {
                showReport(now);
                status.accept("Showing report #" + now.getReportId() + " from the history.");
            }
        });
        VBox.setVgrow(history, Priority.ALWAYS);
        VBox historyCard = Page.card("Report History", history);
        VBox.setVgrow(historyCard, Priority.ALWAYS);

        scheduled.setPlaceholder(new Label("No scheduled reports."));
        scheduled.setPrefHeight(90);
        scheduled.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ReportSchedule s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : s.reportType() + " - " + s.fields() + "\n"
                        + s.frequency() + " on " + s.deliveryDay());
            }
        });
        VBox scheduledCard = Page.card("Scheduled Reports", scheduled);

        VBox column = new VBox(12, settings, historyCard, scheduledCard);
        column.setPadding(new Insets(4, 0, 12, 16));
        column.setPrefWidth(310);
        column.setMinWidth(310);
        return column;
    }

    private VBox buildPreview() {
        preview.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(preview);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("preview-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox card = Page.card("Report Preview", scroll);
        VBox box = new VBox(card);
        VBox.setVgrow(card, Priority.ALWAYS);
        box.setPadding(new Insets(4, 16, 12, 14));
        return box;
    }

    private VBox buildActions() {
        exportPdf.setOnAction(e -> export("PDF"));
        exportCsv.setOnAction(e -> export("CSV"));

        frequency.setValue("Weekly");
        frequency.setOnAction(e -> fillDeliveryDays());
        fillDeliveryDays();
        Button schedule = new Button("Schedule Report");
        schedule.setOnAction(e -> schedule());

        HBox row = new HBox(10, exportPdf, exportCsv, new Separator(javafx.geometry.Orientation.VERTICAL),
                new Label("Frequency"), frequency, new Label("Delivery Day"), deliveryDay, schedule);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox card = Page.card("Report Actions", row);
        VBox box = new VBox(card);
        box.setPadding(new Insets(0, 16, 16, 16));
        return box;
    }

    private void fillDeliveryDays() {
        List<String> days = new ArrayList<>();
        if ("Monthly".equals(frequency.getValue())) {
            for (int d = 1; d <= 28; d++) days.add(ordinal(d) + " of the month");
        } else {
            for (DayOfWeek d : DayOfWeek.values()) days.add(d.getDisplayName(TextStyle.FULL, Locale.US));
        }
        deliveryDay.setItems(FXCollections.observableArrayList(days));
        deliveryDay.setValue(days.get(0));
    }

    // ------------------------------------------------------------ actions

    private void generate() {
        try {
            Report r = controller.generate(type.getValue(), field.getValue(), start.getValue(), end.getValue());
            refreshHistory(r);
            showReport(r);
            status.accept("Generated " + r.getTitle() + " #" + r.getReportId()
                    + ". Review the preview, then export it as PDF or CSV.");
        } catch (IllegalArgumentException ex) {
            status.accept("Report not generated: " + ex.getMessage());
            Alert warn = new Alert(Alert.AlertType.WARNING, ex.getMessage());
            warn.initOwner(owner.get());
            warn.setHeaderText("Report not generated");
            warn.showAndWait();
        }
    }

    private void export(String format) {
        if (current == null) return;
        String ext = format.toLowerCase(Locale.ROOT);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + format);
        chooser.setInitialFileName(ReportExporter.fileName(current, ext));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(format + " file", "*." + ext));
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (downloads.isDirectory()) chooser.setInitialDirectory(downloads);
        File file = chooser.showSaveDialog(owner.get());
        if (file == null) return;   // cancelled
        exportTo(format, file.toPath());
    }

    /** Writes the current report to the file; public so tests can export without the file dialog. */
    public void exportTo(String format, Path file) {
        try {
            if (format.equals("PDF")) ReportExporter.writePdf(current, controller.farmLabel(), file);
            else ReportExporter.writeCsv(current, controller.farmLabel(), file);
            controller.markExported(current, format);
            refreshHistory(current);
            status.accept("Exported " + format + ": " + file);
        } catch (IOException ex) {
            status.accept("Export failed: " + ex.getMessage());
        }
    }

    private void schedule() {
        try {
            ReportSchedule s = controller.schedule(type.getValue(), field.getValue(),
                    frequency.getValue(), deliveryDay.getValue());
            scheduled.setItems(FXCollections.observableArrayList(controller.schedules()));
            status.accept(s.reportType() + " (" + s.fields() + ") scheduled " + s.frequency().toLowerCase()
                    + " on " + s.deliveryDay() + ". It will be emailed to the Farm Manager automatically.");
        } catch (IllegalArgumentException ex) {
            status.accept("Not scheduled: " + ex.getMessage());
        }
    }

    private void refreshHistory(Report select) {
        showingHistory = true;
        history.setItems(FXCollections.observableArrayList(controller.history()));
        if (select != null) history.getSelectionModel().select(select);
        showingHistory = false;
    }

    // ------------------------------------------------------------ preview

    private void showReport(Report r) {
        current = r;
        exportPdf.setDisable(r == null);
        exportCsv.setDisable(r == null);
        preview.getChildren().clear();
        if (r == null) {
            Label hint = new Label("Choose the report settings and click Generate Report to see a preview here.");
            hint.getStyleClass().add("placeholder-note");
            hint.setWrapText(true);
            preview.getChildren().add(hint);
            return;
        }

        Label title = new Label(r.getTitle());
        title.getStyleClass().add("report-title");
        Label info = new Label(controller.farmLabel() + "   |   " + r.getStartDate().format(ReportController.DATE)
                + " - " + r.getEndDate().format(ReportController.DATE) + "   |   " + r.getFields()
                + "   |   generated " + r.getDateGenerated().format(STAMP) + "  (#" + r.getReportId() + ")");
        info.getStyleClass().add("page-subtitle");
        info.setWrapText(true);

        GridPane summary = new GridPane();
        summary.setHgap(16);
        summary.setVgap(4);
        int row = 0;
        for (String[] s : r.getSummary()) {
            Label l = new Label(s[0]);
            l.getStyleClass().add("detail-label");
            summary.addRow(row++, l, new Label(s[1]));
        }

        preview.getChildren().addAll(title, info, summary);
        if (r.getChart() != null) preview.getChildren().add(chart(r.getChart()));
        preview.getChildren().add(table(r));
    }

    private static Node chart(Report.Chart c) {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setLabel(c.yLabel());
        XYChart<String, Number> chart = c.bar() ? new BarChart<>(x, y) : new LineChart<>(x, y);
        chart.setTitle(c.title());
        chart.setAnimated(false);
        chart.setPrefHeight(250);
        chart.setMinHeight(250);
        if (chart instanceof LineChart<String, Number> line) line.setCreateSymbols(false);
        if (chart instanceof BarChart<String, Number> bar) bar.setCategoryGap(40);
        chart.setLegendVisible(c.series().size() > 1);
        for (Report.Series s : c.series()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(s.name());
            for (Report.Point p : s.points()) series.getData().add(new XYChart.Data<>(p.x(), p.y()));
            chart.getData().add(series);
        }
        return chart;
    }

    private static Node table(Report r) {
        TableView<String[]> table = new TableView<>();
        String[] cols = r.getColumns();
        for (int i = 0; i < cols.length; i++) {
            int index = i;
            TableColumn<String[], String> col = new TableColumn<>(cols[i]);
            col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue()[index]));
            col.setSortable(false);
            table.getColumns().add(col);
        }
        table.setItems(FXCollections.observableArrayList(r.getRows()));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(24);
        table.setPrefHeight(Math.min(10, r.getRows().size()) * 24 + 30);
        table.setMinHeight(Math.min(10, r.getRows().size()) * 24 + 30);
        return table;
    }

    // ------------------------------------------------------------ helpers

    private static DatePicker datePicker(LocalDate value) {
        DatePicker p = new DatePicker(value);
        p.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return d == null ? "" : d.format(ReportController.DATE);
            }

            @Override
            public LocalDate fromString(String s) {
                try {
                    return s == null || s.isBlank() ? null : LocalDate.parse(s.trim(), ReportController.DATE);
                } catch (java.time.format.DateTimeParseException e) {
                    return null;
                }
            }
        });
        p.setPromptText("MM/DD/YYYY");
        return p;
    }

    private static String ordinal(int n) {
        if (n >= 11 && n <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
