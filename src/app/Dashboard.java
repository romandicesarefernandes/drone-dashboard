package app;

import boundary.EquipmentTrackerUI;
import boundary.FarmSetupUI;
import boundary.NotificationInterface;
import boundary.MissionsUI;
import boundary.Page;
import boundary.PlaceholderUI;
import boundary.ReportsUI;
import boundary.SimulatedGpsTagService;
import boundary.SimulatedNotificationService;
import boundary.UserManagementUI;
import control.EquipmentController;
import control.ReportController;
import control.UserManagementController;
import entity.DataStore;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Singleton: the one and only dashboard window. The constructor is private,
 * so the only way to get the dashboard is getInstance(), which always
 * returns the same object; show() only ever opens one window.
 *
 * The window is a shell: top bar, navigation sidebar (the screens from the
 * wireframes), the current page, and a status bar shared by all pages.
 */
public final class Dashboard {

    private static Dashboard instance;

    public static synchronized Dashboard getInstance() {
        if (instance == null) {
            instance = new Dashboard();
        }
        return instance;
    }

    /** Page the window opens on. */
    public static final String START_PAGE = "Fields";

    private final Label status = new Label();
    private final StackPane content = new StackPane();
    private final ToggleGroup nav = new ToggleGroup();
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, ToggleButton> navButtons = new LinkedHashMap<>();
    private Stage stage;

    private Dashboard() { }

    /** Opens the dashboard window. A second call just brings the existing window to the front. */
    public void show(Stage primaryStage) {
        if (stage != null) {
            stage.toFront();
            return;
        }
        stage = primaryStage;
        createPages();

        BorderPane layout = new BorderPane();
        layout.setTop(buildTopBar());
        layout.setLeft(buildSidebar());
        layout.setCenter(content);
        layout.setBottom(buildStatusBar());

        Scene scene = new Scene(layout, 1330, 840);
        java.net.URL css = getClass().getResource("application.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        open(START_PAGE);
        stage.setTitle("AgriDrone Farm Dashboard");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    /** Switches to the page with this sidebar label. */
    public void open(String navLabel) {
        Page page = pages.get(navLabel);
        if (page == null) return;
        navButtons.get(navLabel).setSelected(true);
        content.getChildren().setAll(page.getView());
        status.setText("");
        page.onShow();
    }

    private void createPages() {
        DataStore store = DataStore.getInstance();
        NotificationInterface notifier = new SimulatedNotificationService();

        add(new PlaceholderUI("Dashboard", "Dashboard Home",
                "Overview of alerts, soil moisture, crop health and equipment (UC-04, UC-05, UC-07, UC-08)"));
        add(new FarmSetupUI(() -> stage, status::setText));
        add(new MissionsUI(store));
        add(new PlaceholderUI("Soil Moisture", "Soil Moisture", "UC-04 Monitor Soil Moisture"));
        add(new PlaceholderUI("Crop Health", "Crop Health", "UC-05 Monitor Crop Growth"));
        add(new PlaceholderUI("Harvest", "Harvest Readiness", "UC-06 Determine Harvest Readiness"));
        add(new EquipmentTrackerUI(new EquipmentController(store, new SimulatedGpsTagService()),
                () -> stage, status::setText));
        add(new PlaceholderUI("Alerts", "Alerts", "UC-08 Receive Alerts and Notifications"));
        add(new ReportsUI(new ReportController(store, notifier), () -> stage, status::setText));
        add(new UserManagementUI(new UserManagementController(store, notifier), () -> stage, status::setText));
    }

    private void add(Page page) {
        pages.put(page.getNavLabel(), page);
    }

    // ------------------------------------------------------------ layout

    private HBox buildTopBar() {
        Label title = new Label("AgriDrone Farm Dashboard");
        title.getStyleClass().add("product-title");
        Label farm = new Label(DataStore.getInstance().getFarm().name() + "  |  "
                + DataStore.getInstance().getFarm().location());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(10, title, spacer, farm);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 16, 10, 16));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private VBox buildSidebar() {
        VBox sidebar = new VBox(2);
        sidebar.setPadding(new Insets(10, 8, 10, 8));
        sidebar.setPrefWidth(170);
        sidebar.getStyleClass().add("sidebar");
        for (String label : pages.keySet()) {
            ToggleButton b = new ToggleButton(label);
            b.setToggleGroup(nav);
            b.setMaxWidth(Double.MAX_VALUE);
            b.getStyleClass().add("nav-button");
            b.setOnAction(e -> open(label));
            navButtons.put(label, b);
            sidebar.getChildren().add(b);
        }
        // Clicking the open page's button again must not leave nothing selected.
        nav.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) old.setSelected(true);
        });
        return sidebar;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(status);
        bar.setPadding(new Insets(6, 10, 6, 10));
        bar.getStyleClass().add("status-bar");
        return bar;
    }
}
