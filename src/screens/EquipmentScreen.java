package screens;

import javafx.scene.Node;
import model.Screen;

public class EquipmentScreen extends BaseScreen {

    public EquipmentScreen() {
        super(Screen.S7);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-07 Track Equipment Location and Inventory: Equipment GPS Tag positions and inventory list.");
    }
}
