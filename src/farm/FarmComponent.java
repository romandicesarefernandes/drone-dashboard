package farm;

import java.util.List;
import javafx.scene.layout.Pane;

/**
 * Composite pattern: the common interface for items (leaves) and
 * item-containers (composites). The dashboard only talks to this type.
 */
public interface FarmComponent {

    String getName();
    void setName(String name);

    /** Price of this object alone. */
    double getPrice();
    void setPrice(double price);

    /** Price of this object plus everything inside it. */
    double getTotalPrice();

    /** location-x: pixels from the left edge of the farm (screen coordinates, x grows right). */
    double getX();
    void setX(double x);

    /** location-y: pixels from the top edge of the farm (screen coordinates, y grows down). */
    double getY();
    void setY(double y);

    /** Horizontal size on the farm plot. */
    double getLength();
    void setLength(double length);

    /** Vertical size on the farm plot. */
    double getWidth();
    void setWidth(double width);

    /** Height of the real object; stored, not drawn on the 2D plot. */
    double getHeight();
    void setHeight(double height);

    /** The container this object is in, or null for the root farm. */
    ItemContainer getParent();
    void setParent(ItemContainer parent);

    boolean isContainer();

    /** Direct children (always empty for an item). */
    List<FarmComponent> getChildren();

    /** Names of the commands this object supports, as shown in the dashboard. */
    List<String> getCommands();

    /** Adds this object's shapes, and those of everything inside it, to the pane. */
    void draw(Pane pane);

    /** Adds this object, then everything inside it, to out, in tree order. */
    void collect(List<FarmComponent> out);

    default double getCenterX() { return getX() + getLength() / 2; }
    default double getCenterY() { return getY() + getWidth() / 2; }
}
