package screens;

import javafx.scene.Node;
import model.Screen;

public class AlertsScreen extends BaseScreen {

    public AlertsScreen() {
        super(Screen.S8);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-08 Receive Alerts and Notifications: email alert settings and alert list (extends UC-04, UC-05, UC-07).");
    }
}
