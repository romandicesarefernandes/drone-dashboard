package boundary;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/** A blank page for a screen that is not implemented in this part of the project. */
public final class PlaceholderUI implements Page {
    private final String navLabel;
    private final BorderPane view = new BorderPane();

    public PlaceholderUI(String navLabel, String title, String useCases) {
        this.navLabel = navLabel;
        Label note = new Label("This page is blank for now.");
        note.getStyleClass().add("placeholder-note");
        VBox center = new VBox(note);
        center.setAlignment(Pos.CENTER);
        view.setTop(Page.header(title, useCases));
        view.setCenter(center);
    }

    @Override public String getNavLabel() { return navLabel; }
    @Override public Node getView() { return view; }
}
