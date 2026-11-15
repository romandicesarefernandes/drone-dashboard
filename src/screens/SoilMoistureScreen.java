package screens;

import javafx.scene.Node;
import model.Screen;

public class SoilMoistureScreen extends BaseScreen {

    public SoilMoistureScreen() {
        super(Screen.S4);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-04 Monitor Soil Moisture: moisture readings per field from Drone flights.");
    }
}
