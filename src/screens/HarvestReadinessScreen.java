package screens;

import javafx.scene.Node;
import model.Screen;

public class HarvestReadinessScreen extends BaseScreen {

    public HarvestReadinessScreen() {
        super(Screen.S6);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-06 Determine Harvest Readiness (includes UC-05 Monitor Crop Growth).");
    }
}
