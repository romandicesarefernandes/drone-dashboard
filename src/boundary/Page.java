package boundary;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** One screen of the dashboard, opened from the navigation sidebar. */
public interface Page {

    /** Text of this page's sidebar button (as in the wireframes). */
    String getNavLabel();

    /** The page's content. Built once and reused, so the page keeps its state. */
    Node getView();

    /** Called each time the page is opened. */
    default void onShow() { }

    /** Standard page header: title, the use case it covers, and optional controls on the right. */
    static HBox header(String title, String subtitle, Node... right) {
        Label t = new Label(title);
        t.getStyleClass().add("page-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("page-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, new VBox(2, t, s), spacer);
        box.getChildren().addAll(right);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 16, 10, 16));
        box.getStyleClass().add("page-header");
        return box;
    }

    /** A titled white panel ("card") around some content. */
    static VBox card(String title, Node... content) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        VBox box = new VBox(8, t);
        box.getChildren().addAll(content);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("card");
        return box;
    }
}
