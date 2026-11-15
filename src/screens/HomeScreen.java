package screens;

import javafx.scene.Node;
import model.Screen;

public class HomeScreen extends BaseScreen {

    public HomeScreen() {
        super(Screen.S1);
    }

    @Override
    protected Node buildContent() {
        return placeholder("Overview for the logged-in role; links to the other screens.");
    }
}
