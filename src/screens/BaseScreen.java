package screens;

import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import model.Screen;
import model.UseCase;

/**
 * Common frame for every screen: a header with the screen id and title, the
 * use cases it covers (with their actors and relationships), then the
 * screen-specific content from buildContent().
 */
public abstract class BaseScreen {
    private final Screen screen;

    protected BaseScreen(Screen screen) {
        this.screen = screen;
    }

    public Screen getScreen() { return screen; }

    /** Builds a fresh view each time the screen is opened. */
    public Node build() {
        Label title = new Label(screen.getId() + " - " + screen.getTitle());
        title.getStyleClass().add("screen-title");

        VBox useCases = new VBox(4);
        for (UseCase uc : screen.getUseCases()) {
            String secondary = uc.getSecondary().isEmpty() ? "-"
                    : uc.getSecondary().stream().map(a -> a.getDisplayName())
                        .collect(Collectors.joining(", "));
            Label line = new Label(uc + "   |   Primary: " + uc.getPrimary()
                    + "   |   Secondary: " + secondary
                    + "   |   " + uc.getRelationship());
            line.getStyleClass().add("use-case-line");
            useCases.getChildren().add(line);
        }

        VBox root = new VBox(12, title, useCases, buildContent());
        root.setPadding(new Insets(20));
        return root;
    }

    /** Screen-specific widgets. Replace the placeholder in each subclass. */
    protected abstract Node buildContent();

    /** Placeholder used until the wireframe for this screen is implemented. */
    protected Node placeholder(String todo) {
        Label label = new Label("TODO: " + todo);
        label.getStyleClass().add("placeholder");
        label.setWrapText(true);
        return label;
    }
}
