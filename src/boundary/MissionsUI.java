package boundary;

import control.MissionController;
import entity.DataStore;
import entity.Field;
import entity.FlightPlan;
import entity.Mission;
import entity.MissionSchedule;
import ui.MissionMapView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** UC-02 Mission Planner and UC-03 Schedule Drone Mission. */
public final class MissionsUI implements Page {
    private final DataStore store;
    private final MissionController controller;
    private final BorderPane view = new BorderPane();
    private final ListView<Field> fieldList = new ListView<>();
    private final ComboBox<String> missionType = new ComboBox<>();
    private final ComboBox<MissionController.DroneInfo> dronePicker = new ComboBox<>();
    private final TextField planName = new TextField("South+River moisture");
    private final DatePicker datePicker = new DatePicker(LocalDate.now().plusDays(1));
    private final ComboBox<Integer> hourPicker = new ComboBox<>();
    private final ComboBox<Integer> minutePicker = new ComboBox<>();
    private final ComboBox<String> repeatPicker = new ComboBox<>();
    private final ComboBox<String> notifications = new ComboBox<>();
    private final ComboBox<FlightPlan> savedPlanPicker = new ComboBox<>();
    private final Map<DayOfWeek, CheckBox> dayChecks = new EnumMap<>(DayOfWeek.class);
    private final VBox weekdayBox = new VBox(5);
    private final MissionMapView map;
    private final Label flightTimeValue = valueLabel("—");
    private final Label batteryValue = valueLabel("—");
    private final Label coverageValue = valueLabel("—");
    private final Label waypointValue = valueLabel("—");
    private final Label weatherValue = valueLabel("Not checked");
    private final Label feedback = new Label("Generate a flight path to begin planning.");
    private final TableView<MissionRow> upcomingTable = new TableView<>();
    private FlightPlan currentPlan;

    public MissionsUI(DataStore store) {
        this.store = store;
        this.controller = new MissionController(store);
        this.map = new MissionMapView(store);
        build();
    }

    @Override public String getNavLabel() { return "Missions"; }
    @Override public Node getView() { return view; }
    @Override public void onShow() { refreshUpcoming(); }

    private void build() {
        view.setTop(Page.header("Mission Planner", "UC-02 Plan Automated Flight Path · UC-03 Schedule Drone Mission"));
        fieldList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        fieldList.setItems(FXCollections.observableArrayList(controller.getAvailableFields()));
        fieldList.setPrefHeight(112);
        fieldList.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Field>) c -> {
            map.setSelectedFields(List.copyOf(fieldList.getSelectionModel().getSelectedItems()));
        });
        missionType.getItems().setAll(MissionController.MISSION_TYPES);
        missionType.setValue("Soil Moisture Scan");
        dronePicker.getItems().setAll(controller.getAvailableDrones());
        dronePicker.setValue(controller.getAvailableDrones().get(1));

        VBox settings = buildSettings();
        VBox schedule = buildSchedule();
        VBox status = buildFlightStatus();
        VBox mapCard = Page.card("Mission Map · Flight Path Preview", map,
                new Label("Green = selected fields · Blue = generated flight route · Square = drone base"));
        HBox topCards = new HBox(12, settings, schedule);
        HBox.setHgrow(settings, Priority.ALWAYS);
        HBox.setHgrow(schedule, Priority.ALWAYS);
        settings.setPrefWidth(440);
        schedule.setPrefWidth(440);
        HBox lower = new HBox(12, mapCard, status);
        HBox.setHgrow(mapCard, Priority.ALWAYS);
        status.setPrefWidth(310);
        VBox content = new VBox(12, topCards, lower, buildUpcoming());
        content.setPadding(new Insets(12, 16, 16, 16));
        view.setCenter(new javafx.scene.control.ScrollPane(content));
        ((javafx.scene.control.ScrollPane)view.getCenter()).setFitToWidth(true);
        feedback.setWrapText(true);
        feedback.setPadding(new Insets(8, 16, 8, 16));
        feedback.getStyleClass().add("detail-label");
        view.setBottom(feedback);
    }

    private VBox buildSettings() {
        Label fieldHint = new Label("Select one or more mapped fields (Ctrl/Shift-click for multiple).");
        fieldHint.getStyleClass().add("detail-label");
        Label defaults = new Label("Soil moisture defaults: 120 ft altitude · 18 mph · 75% image overlap (simulated)");
        defaults.getStyleClass().add("detail-label");
        Button generate = new Button("Generate Flight Path");
        generate.getStyleClass().add("primary-button");
        generate.setOnAction(e -> generatePlan());
        Button save = new Button("Save Mission Plan");
        save.setOnAction(e -> savePlan());
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(8);
        form.addRow(0, new Label("Field(s):"), fieldList);
        form.addRow(1, new Label("Mission Type:"), missionType);
        form.addRow(2, new Label("Drone:"), dronePicker);
        form.addRow(3, new Label("Plan Name:"), planName);
        HBox actions = new HBox(8, generate, save);
        return Page.card("1. Mission Settings", fieldHint, form, defaults, actions);
    }

    private VBox buildSchedule() {
        for (int h=0; h<24; h++) hourPicker.getItems().add(h);
        for (int m=0; m<60; m+=5) minutePicker.getItems().add(m);
        hourPicker.setValue(9); minutePicker.setValue(0);
        hourPicker.setPrefWidth(90); minutePicker.setPrefWidth(90);
        repeatPicker.getItems().addAll("Does not repeat", "Daily", "Weekly", "Monthly");
        repeatPicker.setValue("Does not repeat");
        notifications.getItems().addAll("No notifications", "Email", "SMS", "SMS + Email");
        notifications.setValue("SMS + Email");
        HBox time = new HBox(6, hourPicker, new Label(":"), minutePicker);
        time.setAlignment(Pos.CENTER_LEFT);
        for (DayOfWeek day : DayOfWeek.values()) {
            CheckBox cb = new CheckBox(day.toString().substring(0,3));
            dayChecks.put(day, cb);
        }
        HBox days = new HBox(8, dayChecks.get(DayOfWeek.MONDAY), dayChecks.get(DayOfWeek.TUESDAY),
                dayChecks.get(DayOfWeek.WEDNESDAY), dayChecks.get(DayOfWeek.THURSDAY),
                dayChecks.get(DayOfWeek.FRIDAY), dayChecks.get(DayOfWeek.SATURDAY), dayChecks.get(DayOfWeek.SUNDAY));
        weekdayBox.getChildren().add(days);
        weekdayBox.setVisible(false); weekdayBox.setManaged(false);
        repeatPicker.setOnAction(e -> {
            boolean weekly = "Weekly".equals(repeatPicker.getValue());
            weekdayBox.setVisible(weekly); weekdayBox.setManaged(weekly);
        });
        Button confirm = new Button("Confirm Schedule");
        confirm.getStyleClass().add("primary-button");
        confirm.setOnAction(e -> confirmSchedule());
        savedPlanPicker.setMaxWidth(Double.MAX_VALUE);
        savedPlanPicker.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(FlightPlan p) { return p == null ? "Select a saved flight plan" : p.name() + " · " + p.droneId(); }
            @Override public FlightPlan fromString(String text) { return null; }
        });
        refreshSavedPlans();
        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(9);
        form.addRow(0, new Label("Saved Plan:"), savedPlanPicker);
        form.addRow(1, new Label("Mission Date:"), datePicker);
        form.addRow(2, new Label("Start Time:"), time);
        form.addRow(3, new Label("Repeat:"), repeatPicker);
        form.add(weekdayBox, 1, 4);
        form.addRow(5, new Label("Notify:"), notifications);
        return Page.card("2. Schedule Mission", new Label("Save a flight plan before confirming its schedule."), form, confirm);
    }

    private VBox buildFlightStatus() {
        VBox metrics = new VBox(9,
                metric("Estimated flight time", flightTimeValue),
                metric("Battery usage", batteryValue),
                metric("Coverage area", coverageValue),
                metric("Waypoints", waypointValue),
                metric("Weather / flight safety", weatherValue));
        return Page.card("3. Flight Status", metrics,
                new Label("Weather values are simulated for this project. A live forecast service is not connected."));
    }

    private VBox buildUpcoming() {
        TableColumn<MissionRow, String> date = new TableColumn<>("Date"); date.setCellValueFactory(new PropertyValueFactory<>("date"));
        TableColumn<MissionRow, String> time = new TableColumn<>("Time"); time.setCellValueFactory(new PropertyValueFactory<>("time"));
        TableColumn<MissionRow, String> fields = new TableColumn<>("Field(s)"); fields.setCellValueFactory(new PropertyValueFactory<>("fields"));
        TableColumn<MissionRow, String> type = new TableColumn<>("Mission"); type.setCellValueFactory(new PropertyValueFactory<>("missionType"));
        TableColumn<MissionRow, String> drone = new TableColumn<>("Drone"); drone.setCellValueFactory(new PropertyValueFactory<>("drone"));
        TableColumn<MissionRow, String> status = new TableColumn<>("Status"); status.setCellValueFactory(new PropertyValueFactory<>("status"));
        upcomingTable.getColumns().setAll(date,time,fields,type,drone,status);
        upcomingTable.setPrefHeight(170);
        refreshUpcoming();
        return Page.card("4. Upcoming Missions", upcomingTable);
    }

    private void generatePlan() {
        try {
            List<Field> selected = List.copyOf(fieldList.getSelectionModel().getSelectedItems());
            currentPlan = controller.generateFlightPath(selected, missionType.getValue(), dronePicker.getValue());
            map.setSelectedFields(selected);
            map.showPlan(currentPlan);
            flightTimeValue.setText(currentPlan.estimatedMinutes() + " minutes");
            batteryValue.setText(String.format("%.0f%%", currentPlan.batteryUsagePercent()));
            coverageValue.setText(String.format("%.1f acres", currentPlan.coverageAcres()));
            waypointValue.setText(String.valueOf(currentPlan.waypoints().size()));
            MissionController.WeatherInfo weather = controller.checkWeather(datePicker.getValue(), selectedTime(), currentPlan.estimatedMinutes());
            weatherValue.setText(weather.safe() ? "✓ " + weather.message() : "⚠ " + weather.message());
            weatherValue.setTextFill(weather.safe() ? Color.web("#2e7d32") : Color.web("#c62828"));
            feedback.setText("Flight path generated. Review the map and estimates, then save the plan before scheduling.");
        } catch (IllegalArgumentException ex) { feedback.setText(ex.getMessage()); }
    }

    private void savePlan() {
        try {
            if (currentPlan == null) throw new IllegalArgumentException("Generate a flight path first.");
            currentPlan = controller.saveFlightPlan(currentPlan, planName.getText());
            refreshSavedPlans();
            savedPlanPicker.setValue(currentPlan);
            feedback.setText("Saved flight plan " + currentPlan.name() + " (FP-" + String.format("%04d", currentPlan.flightPathId()) + "). You can now schedule it.");
        } catch (IllegalArgumentException ex) { feedback.setText(ex.getMessage()); }
    }

    private void confirmSchedule() {
        try {
            Set<DayOfWeek> days = new HashSet<>();
            dayChecks.forEach((day, cb) -> { if (cb.isSelected()) days.add(day); });
            FlightPlan planToSchedule = savedPlanPicker.getValue() != null ? savedPlanPicker.getValue() : currentPlan;
            MissionSchedule schedule = controller.scheduleMission(planToSchedule, datePicker.getValue(), selectedTime(),
                    repeatPicker.getValue(), days, notifications.getValue());
            refreshUpcoming();
            feedback.setText("Schedule confirmed (#" + schedule.scheduleId() + "). Mission occurrences were added to Upcoming Missions.");
        } catch (IllegalArgumentException ex) { feedback.setText(ex.getMessage()); }
    }

    private LocalTime selectedTime() {
        return LocalTime.of(hourPicker.getValue() == null ? 9 : hourPicker.getValue(),
                minutePicker.getValue() == null ? 0 : minutePicker.getValue());
    }

    private void refreshSavedPlans() {
        if (savedPlanPicker == null) return;
        FlightPlan selected = savedPlanPicker.getValue();
        savedPlanPicker.getItems().setAll(store.flightPlans());
        if (selected != null) savedPlanPicker.setValue(store.flightPlans().stream()
                .filter(p -> p.flightPathId() == selected.flightPathId()).findFirst().orElse(null));
    }

    private void refreshUpcoming() {
        if (upcomingTable == null) return;
        ObservableList<MissionRow> rows = FXCollections.observableArrayList();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("h:mm a");
        for (Mission m : controller.upcomingMissions()) {
            String fieldText = m.fieldId() == 0 ? "Multiple / All fields" : store.fieldName(m.fieldId());
            FlightPlan plan = store.flightPlans().stream().filter(p -> p.flightPathId() == m.flightPathId()).findFirst().orElse(null);
            if (plan != null) fieldText = controller.fieldsLabel(plan);
            rows.add(new MissionRow(m.scheduledTime().format(dateFmt), m.scheduledTime().format(timeFmt),
                    fieldText, m.missionType(), m.droneId(), m.status()));
        }
        upcomingTable.setItems(rows);
    }

    private static VBox metric(String name, Label value) {
        Label label = new Label(name); label.getStyleClass().add("detail-label");
        value.setStyle("-fx-font-weight: bold;");
        return new VBox(2, label, value);
    }
    private static Label valueLabel(String text) { return new Label(text); }

    /** JavaFX bean-style getters are used by TableView's PropertyValueFactory. */
    public static final class MissionRow {
        private final String date, time, fields, missionType, drone, status;
        public MissionRow(String date, String time, String fields, String missionType, String drone, String status) {
            this.date=date; this.time=time; this.fields=fields; this.missionType=missionType; this.drone=drone; this.status=status;
        }
        public String getDate(){return date;} public String getTime(){return time;} public String getFields(){return fields;}
        public String getMissionType(){return missionType;} public String getDrone(){return drone;} public String getStatus(){return status;}
    }
}
