package ui;

import entity.DataStore;
import entity.Field;
import entity.FieldBoundary;
import entity.FlightPlan;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lightweight JavaFX farm map using the same coordinates as EquipmentTrackerUI. */
public final class MissionMapView extends Pane {
    private static final double SCALE = 0.55;
    private final DataStore store;
    private Set<Integer> selected = Set.of();
    private FlightPlan plan;

    public MissionMapView(DataStore store) {
        this.store = store;
        setPrefSize(560, 350);
        setMinSize(420, 280);
        setStyle("-fx-background-color: #fbfaf3; -fx-border-color: #b7c7b5;");
        widthProperty().addListener((o,a,b) -> draw());
        heightProperty().addListener((o,a,b) -> draw());
        draw();
    }

    public void setSelectedFields(List<Field> fields) {
        Set<Integer> ids = new HashSet<>();
        if (fields != null) for (Field f : fields) ids.add(f.fieldId());
        selected = Set.copyOf(ids);
        draw();
    }

    public void showPlan(FlightPlan plan) { this.plan = plan; draw(); }

    private void draw() {
        getChildren().clear();
        double scale = Math.min(SCALE, Math.min((getWidth() > 0 ? getWidth() - 25 : 535) / 1000.0,
                (getHeight() > 0 ? getHeight() - 25 : 325) / 600.0));
        double ox = 10, oy = 10;
        for (FieldBoundary b : store.boundaries()) {
            Field f = store.findField(b.fieldId());
            Rectangle r = new Rectangle(ox + b.x()*scale, oy + b.y()*scale,
                    b.width()*scale, b.height()*scale);
            r.setFill(selected.contains(b.fieldId()) ? Color.web("#c5e1a5") : Color.web("#e8eddf"));
            r.setStroke(selected.contains(b.fieldId()) ? Color.web("#2e7d32") : Color.web("#9aa89a"));
            r.setStrokeWidth(selected.contains(b.fieldId()) ? 2.4 : 1.2);
            Text label = new Text(ox + b.x()*scale + 6, oy + b.y()*scale + 16,
                    f == null ? "Field" : f.name());
            label.setFont(Font.font(11));
            label.setFill(Color.web("#304b32"));
            getChildren().addAll(r, label);
        }
        double bx = ox + 70*scale, by = oy + 540*scale;
        Rectangle base = new Rectangle(bx-5, by-5, 10, 10);
        base.setFill(Color.web("#37474f"));
        Text baseText = new Text(bx+8, by+4, "Base");
        baseText.setFont(Font.font(10));
        getChildren().addAll(base, baseText);
        if (plan != null && plan.waypoints().size() > 1) {
            Polyline route = new Polyline();
            route.setStroke(Color.web("#1565c0"));
            route.setStrokeWidth(2.5);
            route.setFill(Color.TRANSPARENT);
            for (FlightPlan.RoutePoint p : plan.waypoints()) {
                route.getPoints().addAll(ox + p.x()*scale, oy + p.y()*scale);
            }
            getChildren().add(route);
            for (int i=1; i<plan.waypoints().size()-1; i++) {
                FlightPlan.RoutePoint p = plan.waypoints().get(i);
                Circle waypoint = new Circle(ox+p.x()*scale, oy+p.y()*scale, 2.5, Color.web("#1565c0"));
                getChildren().add(waypoint);
            }
        }
    }
}
