package screens;

import javafx.scene.Node;
import model.Screen;

public class FarmFieldsScreen extends BaseScreen {

    public FarmFieldsScreen() {
        super(Screen.S2);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-01 Register Farm and Map Fields: farm details form and field map.");
    }
}
