package screens;

import javafx.scene.Node;
import model.Screen;

public class FlightPlanningScreen extends BaseScreen {

    public FlightPlanningScreen() {
        super(Screen.S3);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-02 Plan Automated Flight Path (with Weather Service check) and UC-03 Schedule Drone Mission (includes UC-02).");
    }
}
