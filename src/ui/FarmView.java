package ui;

import farm.FarmComponent;
import farm.ItemContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.Animation;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Visualization section: an 800 x 600 plot of the farm, drawn in screen
 * coordinates (origin top-left, x grows right, y grows down). 1 unit of
 * location/length/width = 1 pixel. Also animates the drone's flights.
 */
public class FarmView {
    public static final double WIDTH = 800;
    public static final double HEIGHT = 600;

    private static final double SPEED = 400;      // drone speed, pixels per second
    private static final double SCAN_MARGIN = 30;  // distance kept from the farm edge while scanning
    private static final double SCAN_ROW_GAP = 80; // distance between scan passes

    private final Pane pane = new Pane();
    private final Rectangle highlight = new Rectangle();
    private final Consumer<String> status;

    private Circle drone;
    private double homeX, homeY;   // drone item's center: start and end of every flight
    private Animation running;

    public FarmView(Consumer<String> status) {
        this.status = status;
        pane.setPrefSize(WIDTH, HEIGHT);
        pane.setMinSize(WIDTH, HEIGHT);
        pane.setMaxSize(WIDTH, HEIGHT);
        pane.setClip(new Rectangle(WIDTH, HEIGHT));
        pane.getStyleClass().add("farm-view");

        highlight.setFill(Color.TRANSPARENT);
        highlight.setStroke(Color.ORANGERED);
        highlight.setStrokeWidth(3);
        highlight.getStrokeDashArray().addAll(8.0, 6.0);
        highlight.setMouseTransparent(true);
        highlight.setVisible(false);
    }

    public Pane getPane() { return pane; }

    /** Redraws every item and item-container, then places the drone on its item. */
    public void render(ItemContainer root) {
        stop();
        pane.getChildren().clear();
        root.draw(pane);
        pane.getChildren().add(highlight);

        FarmComponent droneItem = findDrone(root);
        if (droneItem == null) {
            drone = null;
            return;
        }
        homeX = droneItem.getCenterX();
        homeY = droneItem.getCenterY();
        drone = new Circle(homeX, homeY, 10, Color.DODGERBLUE);
        drone.setStroke(Color.WHITE);
        drone.setStrokeWidth(2);
        pane.getChildren().add(drone);   // added last so it flies above everything
    }

    /** Outlines the selected object on the plot (null clears it). */
    public void highlight(FarmComponent c) {
        if (c == null) {
            highlight.setVisible(false);
            return;
        }
        highlight.setX(c.getX());
        highlight.setY(c.getY());
        highlight.setWidth(c.getLength());
        highlight.setHeight(c.getWidth());
        highlight.setVisible(true);
    }

    /** "Visit item": fly from the command center to the target's center and back. */
    public void visit(FarmComponent target) {
        if (!readyToFly()) return;
        if (target == null) {
            status.accept("Select an item or item-container for the drone to visit.");
            return;
        }
        List<double[]> path = List.of(
                new double[] { target.getCenterX(), target.getCenterY() },
                new double[] { homeX, homeY });
        status.accept("Drone flying to " + target.getName() + "...");
        fly(path, "Drone visited " + target.getName() + " and returned to the command center.");
    }

    /** "Scan farm": sweep back and forth over the whole farm, row by row, then return. */
    public void scan() {
        if (!readyToFly()) return;
        List<double[]> path = new ArrayList<>();
        boolean leftToRight = true;
        for (double y = SCAN_MARGIN; y <= HEIGHT - SCAN_MARGIN; y += SCAN_ROW_GAP) {
            double startX = leftToRight ? SCAN_MARGIN : WIDTH - SCAN_MARGIN;
            double endX = leftToRight ? WIDTH - SCAN_MARGIN : SCAN_MARGIN;
            path.add(new double[] { startX, y });
            path.add(new double[] { endX, y });
            leftToRight = !leftToRight;
        }
        path.add(new double[] { homeX, homeY });
        status.accept("Drone scanning the farm...");
        fly(path, "Farm scan complete. Drone returned to the command center.");
    }

    private boolean readyToFly() {
        if (drone == null) {
            status.accept("No drone on the farm. Add a \"Command Center\" item-container to the root"
                    + " and a \"Drone\" item to it.");
            return false;
        }
        if (running != null && running.getStatus() == Animation.Status.RUNNING) {
            status.accept("The drone is already flying.");
            return false;
        }
        return true;
    }

    /** Flies through the points in order. Speed is constant, so longer legs take longer. */
    private void fly(List<double[]> points, String doneMessage) {
        SequentialTransition flight = new SequentialTransition();
        double fromX = homeX, fromY = homeY;
        for (double[] p : points) {
            double distance = Math.hypot(p[0] - fromX, p[1] - fromY);
            TranslateTransition leg = new TranslateTransition(
                    Duration.seconds(Math.max(0.15, distance / SPEED)), drone);
            leg.setToX(p[0] - homeX);    // translate is relative to the drone's start
            leg.setToY(p[1] - homeY);
            flight.getChildren().add(leg);
            fromX = p[0];
            fromY = p[1];
        }
        flight.setOnFinished(e -> status.accept(doneMessage));
        running = flight;
        flight.play();
    }

    private void stop() {
        if (running != null) {
            running.stop();
            running = null;
        }
    }

    /** The first item named "Drone" (any case) is the drone. */
    private static FarmComponent findDrone(ItemContainer root) {
        List<FarmComponent> all = new ArrayList<>();
        root.collect(all);
        for (FarmComponent c : all) {
            if (!c.isContainer() && c.getName().equalsIgnoreCase("drone")) return c;
        }
        return null;
    }
}
