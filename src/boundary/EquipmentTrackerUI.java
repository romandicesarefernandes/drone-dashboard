package boundary;

import control.EquipmentController;
import entity.Alert;
import entity.Equipment;
import entity.Field;
import entity.FieldBoundary;
import entity.Mission;
import entity.SampleData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Equipment page (boundary for UC-07 Track Equipment Location and Inventory):
 * map of last known locations, counts by type, search and filters, item
 * details with location history, Add Equipment and Locate with Drone.
 */
public final class EquipmentTrackerUI implements Page {

    /** Farm map units (1000 x 600) are drawn at this scale. */
    private static final double SCALE = 0.6;
    private static final double MAP_W = 1000 * SCALE, MAP_H = 600 * SCALE;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a", Locale.US);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("MM/dd h:mm a", Locale.US);

    private final EquipmentController controller;
    private final Supplier<Window> owner;
    private final Consumer<String> status;

    private final TextField search = new TextField();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> fieldFilter = new ComboBox<>();
    private final Pane map = new Pane();
    private final GridPane counts = new GridPane();
    private final TableView<Equipment> table = new TableView<>();
    private final GridPane details = new GridPane();
    private final ListView<String> history = new ListView<>();
    private final Button locate = new Button("Locate with Drone");
    private final BorderPane view = new BorderPane();

    private Animation flight;
    private boolean updatingTable;

    public EquipmentTrackerUI(EquipmentController controller, Supplier<Window> owner, Consumer<String> status) {
        this.controller = controller;
        this.owner = owner;
        this.status = status;

        view.setTop(new VBox(Page.header("Equipment Tracker", "UC-07 Track Equipment Location and Inventory"),
                buildFilters()));
        HBox center = new HBox(14, buildLeftColumn(), buildRightColumn());
        center.setPadding(new Insets(4, 16, 16, 16));
        view.setCenter(center);
        refresh(null);
    }

    @Override public String getNavLabel() { return "Equipment"; }
    @Override public Node getView() { return view; }

    @Override
    public void onShow() {
        LocalDateTime now = LocalDateTime.now();
        int silent = controller.checkSilentTags(now);
        List<Alert> raised = controller.checkGeofences(now);
        refresh(selected());
        List<String> notes = new ArrayList<>();
        notes.add(controller.all().size() + " items tracked, " + controller.missingCount() + " missing");
        if (silent > 0) notes.add(silent + " tag(s) stopped reporting and were marked Missing");
        for (Alert a : raised) notes.add("Alert raised: " + a.message());
        status.accept(String.join(". ", notes) + ".");
    }

    // ------------------------------------------------------------ layout

    private HBox buildFilters() {
        search.setPromptText("Search name, tag or operator");
        search.setPrefWidth(240);
        typeFilter.getItems().add(EquipmentController.ALL);
        typeFilter.getItems().addAll(EquipmentController.TYPES);
        statusFilter.getItems().add(EquipmentController.ALL);
        statusFilter.getItems().addAll(EquipmentController.STATUSES);
        fieldFilter.getItems().add(EquipmentController.ALL_FIELDS);
        for (Field f : controller.fields()) fieldFilter.getItems().add(f.name());
        typeFilter.setValue(EquipmentController.ALL);
        statusFilter.setValue(EquipmentController.ALL);
        fieldFilter.setValue(EquipmentController.ALL_FIELDS);

        search.textProperty().addListener((obs, old, now) -> refresh(selected()));
        typeFilter.setOnAction(e -> refresh(selected()));
        statusFilter.setOnAction(e -> refresh(selected()));
        fieldFilter.setOnAction(e -> refresh(selected()));

        HBox bar = new HBox(10, new Label("Search"), search, new Label("Type"), typeFilter,
                new Label("Status"), statusFilter, new Label("Field"), fieldFilter);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 8, 16));
        return bar;
    }

    private VBox buildLeftColumn() {
        map.setPrefSize(MAP_W, MAP_H);
        map.setMinSize(MAP_W, MAP_H);
        map.setMaxSize(MAP_W, MAP_H);
        map.setClip(new Rectangle(MAP_W, MAP_H));
        map.getStyleClass().add("equipment-map");

        HBox legend = new HBox(14, legendItem(Color.web("#2e7d32"), "Active"), legendItem(Color.web("#1e6fb8"), "Idle"),
                legendItem(Color.web("#c62828"), "Missing (X)"), legendItem(Color.web("#ef8a00"), "Outside geofence (ring)"));
        legend.setAlignment(Pos.CENTER_LEFT);
        VBox mapCard = Page.card("Equipment Map", map, legend);

        Button add = new Button("Add Equipment");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> addEquipment());
        locate.setOnAction(e -> locateWithDrone());
        locate.setTooltip(new Tooltip("Sends a drone to scan the last known area of a missing item."));
        HBox buttons = new HBox(12, add, locate);
        VBox controls = Page.card("Equipment Controls", buttons);

        VBox column = new VBox(12, mapCard, controls);
        column.setMinWidth(MAP_W + 26);
        return column;
    }

    private VBox buildRightColumn() {
        counts.setHgap(18);
        counts.setVgap(4);
        buildTable();
        table.setPrefHeight(210);
        VBox inventory = Page.card("Inventory", counts, table);

        details.setHgap(12);
        details.setVgap(5);
        history.setPrefHeight(96);
        history.setPlaceholder(new Label("No location reports."));
        Label historyTitle = new Label("Location History");
        historyTitle.getStyleClass().add("detail-label");
        VBox detailsCard = Page.card("Selected Equipment", details, historyTitle, history);

        VBox column = new VBox(12, inventory, detailsCard);
        HBox.setHgrow(column, Priority.ALWAYS);
        VBox.setVgrow(detailsCard, Priority.ALWAYS);
        return column;
    }

    private void buildTable() {
        table.getColumns().add(column("Name", 130, Equipment::getName));
        table.getColumns().add(column("Type", 110, Equipment::getType));
        TableColumn<Equipment, String> statusCol = column("Status", 75, Equipment::getStatus);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("status-active", "status-idle", "status-missing");
                setText(empty ? null : value);
                if (!empty && value != null) getStyleClass().add("status-" + value.toLowerCase(Locale.ROOT));
            }
        });
        table.getColumns().add(statusCol);
        table.getColumns().add(column("Field", 105, e -> controller.fieldName(e.getLastKnownFieldId())));
        table.getColumns().add(column("Last Seen", 90,
                e -> EquipmentController.ago(e.getLastSeen(), LocalDateTime.now())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No equipment matches the filters."));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> {
            if (!updatingTable) {
                showDetails();
                drawMap();
            }
        });
    }

    private static TableColumn<Equipment, String> column(String title, double width,
                                                         java.util.function.Function<Equipment, String> value) {
        TableColumn<Equipment, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return c;
    }

    // ------------------------------------------------------------ refresh

    private Equipment selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private List<Equipment> visible() {
        return controller.search(search.getText(), typeFilter.getValue(), statusFilter.getValue(), fieldFilter.getValue());
    }

    private void refresh(Equipment select) {
        List<Equipment> items = visible();
        updatingTable = true;
        table.setItems(FXCollections.observableArrayList(items));
        if (select != null && items.contains(select)) table.getSelectionModel().select(select);
        else table.getSelectionModel().selectFirst();
        table.refresh();
        updatingTable = false;
        showCounts();
        showDetails();
        drawMap();
    }

    private void showCounts() {
        counts.getChildren().clear();
        int col = 0, row = 0;
        for (Map.Entry<String, Integer> c : controller.countsByType().entrySet()) {
            Label l = new Label(plural(c.getKey()) + ":");
            l.getStyleClass().add("detail-label");
            counts.add(l, col, row);
            counts.add(new Label(String.valueOf(c.getValue())), col + 1, row);
            if (++row == 3) { row = 0; col += 2; }
        }
        Label missing = new Label("Missing/Offline:");
        missing.getStyleClass().add("detail-label");
        Label n = new Label(String.valueOf(controller.missingCount()));
        if (controller.missingCount() > 0) n.getStyleClass().add("status-missing");
        counts.add(missing, col, row);
        counts.add(n, col + 1, row);
    }

    private void showDetails() {
        Equipment e = selected();
        details.getChildren().clear();
        history.getItems().clear();
        locate.setDisable(e == null || !e.getStatus().equals(Equipment.MISSING) || isFlying());
        if (e == null) {
            details.add(new Label("Select equipment in the list or on the map."), 0, 0);
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        detail(0, 0, "Name", e.getName());
        detail(0, 1, "Type", e.getType());
        detail(0, 2, "GPS Tag", e.getTagId());
        detail(0, 3, "Description", e.getDescription().isEmpty() ? "-" : e.getDescription());
        Label statusValue = detail(2, 0, "Status", e.getStatus());
        statusValue.getStyleClass().add("status-" + e.getStatus().toLowerCase(Locale.ROOT));
        detail(2, 1, "Operator", e.getOperator().isEmpty() ? "Unassigned" : e.getOperator());
        detail(2, 2, "Last Known", controller.fieldName(e.getLastKnownFieldId()));
        detail(2, 3, "Last Seen", e.getLastSeen().format(STAMP) + " (" + EquipmentController.ago(e.getLastSeen(), now) + ")");
        String fence = e.getGeofenceFieldId() == 0 ? "None"
                : controller.fieldName(e.getGeofenceFieldId()) + (controller.isOutsideGeofence(e) ? " - OUTSIDE" : " - inside");
        Label fenceValue = detail(0, 4, "Geofence", fence);
        if (controller.isOutsideGeofence(e)) fenceValue.getStyleClass().add("status-warning");

        List<Equipment.LocationRecord> h = new ArrayList<>(e.getHistory());
        java.util.Collections.reverse(h);
        for (Equipment.LocationRecord r : h) {
            history.getItems().add(r.time().format(SHORT) + "   " + controller.fieldName(r.fieldId())
                    + "   (" + r.source() + ")");
        }
    }

    private Label detail(int col, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("detail-label");
        l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label v = new Label(value);
        v.setWrapText(true);
        v.setMaxWidth(col == 0 && row == 4 ? 360 : 160);
        details.add(l, col, row);
        if (col == 0 && row == 4) details.add(v, 1, row, 3, 1);
        else details.add(v, col + 1, row);
        return v;
    }

    // ------------------------------------------------------------ map

    private void drawMap() {
        if (isFlying()) return;   // keep the drone on screen until its flight ends
        map.getChildren().clear();
        Rectangle bg = new Rectangle(MAP_W, MAP_H, Color.web("#eef0e6"));
        map.getChildren().add(bg);

        Equipment sel = selected();
        for (FieldBoundary b : controller.boundaries()) {
            Field f = controller.fields().stream().filter(x -> x.fieldId() == b.fieldId()).findFirst().orElse(null);
            Rectangle r = new Rectangle(b.x() * SCALE, b.y() * SCALE, b.width() * SCALE, b.height() * SCALE);
            r.setFill(cropColor(f == null ? "" : f.cropType()));
            r.setStroke(Color.web("#8d6e63"));
            boolean fence = sel != null && sel.getGeofenceFieldId() == b.fieldId();
            if (fence) {
                r.setStroke(Color.web("#ef8a00"));
                r.setStrokeWidth(2.5);
                r.getStrokeDashArray().addAll(8.0, 5.0);
            }
            Text t = new Text(b.x() * SCALE + 6, b.y() * SCALE + 15,
                    (f == null ? "" : f.name() + " - " + f.cropType()) + (fence ? "   (geofence)" : ""));
            t.setFill(Color.web("#5d4037"));
            t.setFont(Font.font(11));
            map.getChildren().addAll(r, t);
        }

        Rectangle pad = new Rectangle(SampleData.BASE_X * SCALE - 7, SampleData.BASE_Y * SCALE - 7, 14, 14);
        pad.setFill(Color.web("#37474f"));
        Text padLabel = new Text(SampleData.BASE_X * SCALE + 10, SampleData.BASE_Y * SCALE + 4, "Drone base");
        padLabel.setFont(Font.font(10));
        map.getChildren().addAll(pad, padLabel);

        for (Equipment e : visible()) {
            map.getChildren().add(marker(e, e == sel));
        }
    }

    private Node marker(Equipment e, boolean isSelected) {
        double x = e.getX() * SCALE, y = e.getY() * SCALE;
        Pane g = new Pane();
        g.setPickOnBounds(false);
        if (e.getStatus().equals(Equipment.MISSING)) {
            Line a = new Line(x - 6, y - 6, x + 6, y + 6);
            Line b = new Line(x - 6, y + 6, x + 6, y - 6);
            for (Line l : List.of(a, b)) {
                l.setStroke(Color.web("#c62828"));
                l.setStrokeWidth(3);
            }
            Circle hit = new Circle(x, y, 9, Color.TRANSPARENT);
            g.getChildren().addAll(hit, a, b);
        } else {
            Circle dot = new Circle(x, y, 6, e.getStatus().equals(Equipment.ACTIVE)
                    ? Color.web("#2e7d32") : Color.web("#1e6fb8"));
            dot.setStroke(Color.WHITE);
            g.getChildren().add(dot);
        }
        if (controller.isOutsideGeofence(e)) {
            Circle ring = new Circle(x, y, 11, Color.TRANSPARENT);
            ring.setStroke(Color.web("#ef8a00"));
            ring.setStrokeWidth(2.5);
            g.getChildren().add(ring);
        }
        if (isSelected) {
            Circle sel = new Circle(x, y, 15, Color.TRANSPARENT);
            sel.setStroke(Color.web("#212121"));
            sel.setStrokeWidth(1.5);
            sel.getStrokeDashArray().addAll(4.0, 3.0);
            g.getChildren().add(sel);
        }
        Text label = new Text(x + 9, y - 7, e.getName());
        label.setFont(Font.font(isSelected ? 11 : 9.5));
        label.setFill(Color.web("#263238"));
        g.getChildren().add(label);
        g.setOnMouseClicked(ev -> {
            if (table.getItems().contains(e)) table.getSelectionModel().select(e);
            table.scrollTo(e);
        });
        Tooltip.install(g, new Tooltip(e.getName() + " - " + e.getStatus() + "\n"
                + controller.fieldName(e.getLastKnownFieldId())));
        g.setStyle("-fx-cursor: hand;");
        return g;
    }

    private static Color cropColor(String crop) {
        return switch (crop) {
            case "Corn" -> Color.web("#f3e7b3");
            case "Apples" -> Color.web("#d8ebc8");
            case "Wheat" -> Color.web("#efe2bd");
            case "Soybean" -> Color.web("#dce9c4");
            default -> Color.web("#e8e8e0");
        };
    }

    private static HBox legendItem(Color c, String text) {
        Circle dot = new Circle(5, c);
        Label l = new Label(text);
        l.getStyleClass().add("detail-label");
        HBox box = new HBox(5, dot, l);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ------------------------------------------------------------ Locate with Drone

    private boolean isFlying() {
        return flight != null && flight.getStatus() == Animation.Status.RUNNING;
    }

    private void locateWithDrone() {
        Equipment e = selected();
        if (e == null) return;
        Mission mission;
        try {
            mission = controller.startDroneSearch(e);
        } catch (IllegalArgumentException ex) {
            status.accept(ex.getMessage());
            return;
        }
        status.accept("Drone " + mission.droneId() + " is scanning " + controller.fieldName(e.getLastKnownFieldId())
                + " for " + e.getName() + "'s tag...");

        double bx = SampleData.BASE_X * SCALE, by = SampleData.BASE_Y * SCALE;
        double tx = e.getX() * SCALE, ty = e.getY() * SCALE;
        Circle drone = new Circle(bx, by, 8, Color.web("#1e88e5"));
        drone.setStroke(Color.WHITE);
        drone.setStrokeWidth(2);
        map.getChildren().add(drone);

        double seconds = Math.max(0.6, Math.hypot(tx - bx, ty - by) / 220);
        TranslateTransition out = new TranslateTransition(Duration.seconds(seconds), drone);
        out.setToX(tx - bx);
        out.setToY(ty - by);
        PauseTransition scan = new PauseTransition(Duration.seconds(0.8));
        scan.setOnFinished(ev -> {
            controller.droneFound(e, mission);
            status.accept("Tag " + e.getTagId() + " found by the drone: " + e.getName() + " is in "
                    + controller.fieldName(e.getLastKnownFieldId()) + ". Status set to Idle. Drone returning to base...");
        });
        TranslateTransition back = new TranslateTransition(Duration.seconds(seconds), drone);
        back.setToX(0);
        back.setToY(0);
        SequentialTransition trip = new SequentialTransition(out, scan, back);
        trip.setOnFinished(ev -> {
            flight = null;
            refresh(e);
            status.accept(e.getName() + " located by drone " + mission.droneId() + " in "
                    + controller.fieldName(e.getLastKnownFieldId()) + ". Mission #" + mission.missionId() + " completed.");
        });
        flight = trip;
        locate.setDisable(true);
        trip.play();
    }

    // ------------------------------------------------------------ Add Equipment

    private void addEquipment() {
        TextField name = new TextField();
        name.setPromptText("e.g. Tractor 6");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(EquipmentController.TYPES));
        type.setValue("Tractor");
        TextField description = new TextField();
        description.setPromptText("Optional");
        TextField tag = new TextField(suggestTag());
        ComboBox<Field> field = new ComboBox<>(FXCollections.observableArrayList(controller.fields()));
        field.setValue(controller.fields().get(0));
        TextField operator = new TextField();
        operator.setPromptText("Optional");
        ComboBox<String> geofence = new ComboBox<>();
        geofence.getItems().add(EquipmentController.NO_GEOFENCE);
        for (Field f : controller.fields()) geofence.getItems().add(f.name());
        geofence.setValue(field.getValue().name());
        field.setOnAction(ev -> geofence.setValue(field.getValue().name()));

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("Type"), type);
        form.addRow(2, new Label("Description"), description);
        form.addRow(3, new Label("GPS Tag ID"), tag);
        form.addRow(4, new Label("Current Field"), field);
        form.addRow(5, new Label("Operator"), operator);
        form.addRow(6, new Label("Geofence"), geofence);
        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        form.add(error, 0, 7, 2, 1);

        ButtonType save = new ButtonType("Add Equipment", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner.get());
        dialog.setTitle("Add Equipment");
        dialog.setHeaderText("Register new equipment and its GPS tag");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(440);

        Equipment[] created = new Equipment[1];
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, ev -> {
            try {
                Field fence = controller.fields().stream()
                        .filter(f -> f.name().equals(geofence.getValue())).findFirst().orElse(null);
                created[0] = controller.addEquipment(name.getText(), type.getValue(), description.getText(),
                        tag.getText(), field.getValue(), operator.getText(), fence);
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                ev.consume();
            }
        });
        if (dialog.showAndWait().filter(b -> b == save).isPresent() && created[0] != null) {
            Equipment e = created[0];
            // make sure the new item is visible
            search.clear();
            typeFilter.setValue(EquipmentController.ALL);
            statusFilter.setValue(EquipmentController.ALL);
            fieldFilter.setValue(EquipmentController.ALL_FIELDS);
            refresh(e);
            status.accept("Added " + e.getName() + " with tag " + e.getTagId() + " in "
                    + controller.fieldName(e.getLastKnownFieldId()) + ". The tag is registered and will report its location.");
        }
    }

    private String suggestTag() {
        int max = 1000;
        for (Equipment e : controller.all()) {
            String digits = e.getTagId().replaceAll("\\D", "");
            if (!digits.isEmpty()) max = Math.max(max, Integer.parseInt(digits));
        }
        return String.format("TAG-%04d", max + 1);
    }

    private static String plural(String type) {
        return switch (type) {
            case "Irrigation Unit" -> "Irrigation";
            case "Other" -> "Other";
            default -> type + "s";
        };
    }
}
