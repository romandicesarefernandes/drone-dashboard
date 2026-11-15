package screens;

import javafx.scene.Node;
import model.Screen;

public class UsersRolesScreen extends BaseScreen {

    public UsersRolesScreen() {
        super(Screen.S10);
    }

    @Override
    protected Node buildContent() {
        return placeholder("UC-10 Manage Users and Roles: create or modify accounts and roles.");
    }
}
