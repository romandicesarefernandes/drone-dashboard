package screens;

import javafx.scene.Node;
import model.Screen;

public class ReportsScreen extends BaseScreen {

    public ReportsScreen() {
        super(Screen.S9);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-09 Generate Farm Reports: choose report contents and generate a PDF.");
    }
}
