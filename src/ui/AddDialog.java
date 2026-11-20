package ui;

import farm.FarmComponent;
import farm.Item;
import farm.ItemContainer;
import farm.Property;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/** Form for the "Add item" and "Add item-container" commands. */
public final class AddDialog {

    private AddDialog() { }

    /** Shows the form; returns the new object, or empty if the user cancelled. */
    public static Optional<FarmComponent> ask(Window owner, ItemContainer parent, boolean container) {
        String kind = container ? "item-container" : "item";
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(container ? ItemContainer.ADD_CONTAINER : ItemContainer.ADD_ITEM);
        dialog.setHeaderText("Add a new " + kind + " to " + parent.getName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Defaults: just inside the parent's top-left corner.
        Map<Property, TextField> fields = new EnumMap<>(Property.class);
        fields.put(Property.NAME, new TextField());
        fields.put(Property.PRICE, new TextField("0"));
        fields.put(Property.LOCATION_X, new TextField(Property.format(parent.getX() + 10)));
        fields.put(Property.LOCATION_Y, new TextField(Property.format(parent.getY() + 25)));
        fields.put(Property.LENGTH, new TextField(container ? "150" : "60"));
        fields.put(Property.WIDTH, new TextField(container ? "100" : "40"));
        fields.put(Property.HEIGHT, new TextField("10"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        int row = 0;
        for (Map.Entry<Property, TextField> e : fields.entrySet()) {
            grid.addRow(row++, new Label(e.getKey().getLabel()), e.getValue());
        }
        Label error = new Label();
        error.getStyleClass().add("error-text");
        grid.add(error, 0, row, 2, 1);
        dialog.getDialogPane().setContent(grid);
        fields.get(Property.NAME).setPromptText(container ? "e.g. Barn" : "e.g. Cow");

        // Validate before the dialog closes; keep it open and show the problem if invalid.
        FarmComponent[] result = new FarmComponent[1];
        Node ok = dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                FarmComponent c = container
                        ? new ItemContainer("new", 0, 0, 0, 1, 1, 1)
                        : new Item("new", 0, 0, 0, 1, 1, 1);
                for (Map.Entry<Property, TextField> e : fields.entrySet()) {
                    e.getKey().apply(c, e.getValue().getText());
                }
                result[0] = c;
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                event.consume();
            }
        });

        return dialog.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .map(button -> result[0]);
    }
}
