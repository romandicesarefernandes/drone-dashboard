package farm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/** Composite: an item-container (farm, barn, room, storage building, ...) holding items and other containers. */
public class ItemContainer extends AbstractFarmComponent {

    public static final String ADD_CONTAINER = "Add item-container";
    public static final String ADD_ITEM = "Add item";
    public static final String DELETE = "Delete item-container";

    private final List<FarmComponent> children = new ArrayList<>();

    public ItemContainer(String name, double price, double x, double y,
                         double length, double width, double height) {
        super(name, price, x, y, length, width, height);
    }

    public void add(FarmComponent child) {
        if (child == this) throw new IllegalArgumentException("A container cannot contain itself.");
        children.add(child);
        child.setParent(this);
    }

    public void remove(FarmComponent child) {
        if (children.remove(child)) {
            child.setParent(null);
        }
    }

    /** Removes everything inside this container. */
    public void clear() {
        for (FarmComponent child : children) {
            child.setParent(null);
        }
        children.clear();
    }

    @Override public boolean isContainer() { return true; }

    @Override
    public List<FarmComponent> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public double getTotalPrice() {
        double total = getPrice();
        for (FarmComponent child : children) {
            total += child.getTotalPrice();
        }
        return total;
    }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        commands.add(ADD_CONTAINER);
        commands.add(ADD_ITEM);
        if (getParent() != null) {          // the root farm cannot be deleted
            commands.add(DELETE);
        }
        for (Property p : Property.values()) {
            commands.add(p.commandName());
        }
        return commands;
    }

    @Override
    public void draw(Pane pane) {
        // Draw self first so the outline sits underneath the children.
        Rectangle box = new Rectangle(getX(), getY(), getLength(), getWidth());
        box.setFill(Color.TRANSPARENT);
        box.setStroke(Color.SADDLEBROWN);
        box.setStrokeWidth(2);
        Text label = new Text(getX() + 6, getY() + 16, getName());
        label.setFill(Color.SADDLEBROWN.darker());
        pane.getChildren().addAll(box, label);

        for (FarmComponent child : children) {
            child.draw(pane);
        }
    }

    @Override
    public void collect(List<FarmComponent> out) {
        out.add(this);
        for (FarmComponent child : children) {
            child.collect(out);
        }
    }
}
