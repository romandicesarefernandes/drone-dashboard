package screens;

import javafx.scene.Node;
import model.Screen;

public class CropGrowthScreen extends BaseScreen {

    public CropGrowthScreen() {
        super(Screen.S5);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-05 Monitor Crop Growth: crop images and growth stage per field.");
    }
}
