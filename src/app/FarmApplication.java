package app;

import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX entry point: hands the stage to the single Dashboard instance. */
public class FarmApplication extends Application {

    @Override
    public void start(Stage stage) {
        Dashboard.getInstance().show(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
