package app;

import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Actor;
import screens.*;

/** AgriDrone Farm Dashboard: top bar, role-filtered sidebar, current screen in the center. */
public class DroneDashboardApp extends Application {

    private final List<BaseScreen> screens = List.of(
            new HomeScreen(),
            new FarmFieldsScreen(),
            new FlightPlanningScreen(),
            new SoilMoistureScreen(),
            new CropGrowthScreen(),
            new HarvestReadinessScreen(),
            new EquipmentScreen(),
            new AlertsScreen(),
            new ReportsScreen(),
            new UsersRolesScreen());

    private final BorderPane root = new BorderPane();
    private final VBox sidebar = new VBox(6);

    @Override
    public void start(Stage stage) {
        // ---- Top bar: product name + role picker (primary actors only) ----
        Label product = new Label("AgriDrone Farm Dashboard");
        product.getStyleClass().add("product-title");

        ComboBox<Actor> rolePicker = new ComboBox<>();
        for (Actor a : Actor.values()) {
            if (a.isPrimary()) rolePicker.getItems().add(a);
        }
        rolePicker.setValue(Actor.FARM_MANAGER);
        rolePicker.setOnAction(e -> showNavigation(rolePicker.getValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox topBar = new HBox(10, product, spacer, new Label("Role:"), rolePicker);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10));
        topBar.getStyleClass().add("top-bar");

        // ---- Sidebar ----
        sidebar.setPadding(new Insets(10));
        sidebar.setPrefWidth(200);
        sidebar.getStyleClass().add("sidebar");

        root.setTop(topBar);
        root.setLeft(sidebar);
        showNavigation(rolePicker.getValue());

        Scene scene = new Scene(root, 1100, 700);
        java.net.URL css = getClass().getResource("application.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        stage.setTitle("AgriDrone Farm Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    /** Rebuilds the sidebar for the role and opens S1. */
    private void showNavigation(Actor role) {
        sidebar.getChildren().clear();
        for (BaseScreen s : screens) {
            if (!s.getScreen().isVisibleTo(role)) continue;
            Button nav = new Button(s.getScreen().toString());
            nav.setMaxWidth(Double.MAX_VALUE);
            nav.getStyleClass().add("nav-button");
            nav.setOnAction(e -> root.setCenter(s.build()));
            sidebar.getChildren().add(nav);
        }
        root.setCenter(screens.get(0).build());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
