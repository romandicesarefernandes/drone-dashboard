package farm;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/** Leaf: a single item (cow, tractor, drone, crop, ...). */
public class Item extends AbstractFarmComponent {

    public static final String DELETE = "Delete item";

    public Item(String name, double price, double x, double y,
                double length, double width, double height) {
        super(name, price, x, y, length, width, height);
    }

    @Override public boolean isContainer() { return false; }
    @Override public double getTotalPrice() { return getPrice(); }
    @Override public List<FarmComponent> getChildren() { return List.of(); }

    @Override
    public List<String> getCommands() {
        List<String> commands = new ArrayList<>();
        commands.add(DELETE);
        for (Property p : Property.values()) {
            commands.add(p.commandName());
        }
        return commands;
    }

    @Override
    public void draw(Pane pane) {
        Rectangle box = new Rectangle(getX(), getY(), getLength(), getWidth());
        box.setFill(Color.LIGHTGREEN);
        box.setStroke(Color.DARKGREEN);
        Text label = new Text(getX() + 4, getY() + 14, getName());
        label.setFill(Color.DARKGREEN.darker());
        pane.getChildren().addAll(box, label);
    }

    @Override
    public void collect(List<FarmComponent> out) {
        out.add(this);
    }
}
