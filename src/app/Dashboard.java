package app;

import farm.FarmComponent;
import farm.Item;
import farm.ItemContainer;
import farm.Property;
import farm.SampleFarm;
import farm.commands.AddCommand;
import farm.commands.ChangePropertyCommand;
import farm.commands.Command;
import farm.commands.DeleteCommand;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.AddDialog;
import ui.FarmView;

/**
 * Singleton: the one and only dashboard window. The constructor is private,
 * so the only way to get the dashboard is getInstance(), which always
 * returns the same object; show() only ever opens one window.
 */
public final class Dashboard {

    private static Dashboard instance;

    public static synchronized Dashboard getInstance() {
        if (instance == null) {
            instance = new Dashboard();
        }
        return instance;
    }

    /** The root item-container: the whole 800 x 600 farm. */
    private final ItemContainer root =
            new ItemContainer("Farm", 0, 0, 0, FarmView.WIDTH, FarmView.HEIGHT, 1);

    private final Label status = new Label("Select the Farm, then pick a command to add items.");
    private final FarmView farmView = new FarmView(status::setText);
    private final TreeView<FarmComponent> tree = new TreeView<>();
    private final GridPane details = new GridPane();
    private final ComboBox<String> commandPicker = new ComboBox<>();
    private final TextField valueField = new TextField();
    private final Button executeButton = new Button("Execute");

    private Stage stage;

    private Dashboard() { }

    /** Opens the dashboard window. A second call just brings the existing window to the front. */
    public void show(Stage primaryStage) {
        if (stage != null) {
            stage.toFront();
            return;
        }
        stage = primaryStage;

        BorderPane layout = new BorderPane();
        layout.setTop(buildTopBar());
        layout.setLeft(buildManagePanel());
        layout.setCenter(buildVisualization());
        layout.setBottom(buildStatusBar());

        Scene scene = new Scene(layout);
        java.net.URL css = getClass().getResource("application.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        refresh(root);
        stage.setTitle("Drone Dashboard");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ------------------------------------------------------------ layout

    private HBox buildTopBar() {
        Label title = new Label("Drone Dashboard - Farm Setup");
        title.getStyleClass().add("product-title");

        Button loadScenario = new Button("Load Test Scenario");
        loadScenario.setOnAction(e -> {
            SampleFarm.load(root);
            refresh(root);
            status.setText("Loaded the test scenario from the project description.");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(10, title, spacer, loadScenario);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    /** Manage farm items: hierarchy, details of the selection, and its commands. */
    private VBox buildManagePanel() {
        Label treeTitle = new Label("Items and item-containers");
        treeTitle.getStyleClass().add("section-title");
        tree.setPrefHeight(250);
        tree.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, now) -> showSelection());

        Label detailsTitle = new Label("Details");
        detailsTitle.getStyleClass().add("section-title");
        details.setHgap(12);
        details.setVgap(3);

        Label commandsTitle = new Label("Commands");
        commandsTitle.getStyleClass().add("section-title");
        commandPicker.setPromptText("Pick a command");
        commandPicker.setMaxWidth(Double.MAX_VALUE);
        commandPicker.setOnAction(e -> commandChosen());
        valueField.setPromptText("New value");
        valueField.setOnAction(e -> runCommand());
        executeButton.setMaxWidth(Double.MAX_VALUE);
        executeButton.setOnAction(e -> runCommand());

        VBox panel = new VBox(8, treeTitle, tree, detailsTitle, details,
                commandsTitle, commandPicker, valueField, executeButton);
        panel.setPadding(new Insets(10));
        panel.setPrefWidth(320);
        panel.getStyleClass().add("side-panel");
        return panel;
    }

    /** Visualization section plus the drone flight buttons. */
    private VBox buildVisualization() {
        Label title = new Label("Visualization (800 x 600)");
        title.getStyleClass().add("section-title");

        Button visit = new Button("Visit Item/Container");
        visit.setOnAction(e -> farmView.visit(selected()));
        Button scan = new Button("Scan Farm");
        scan.setOnAction(e -> farmView.scan());
        HBox droneControls = new HBox(10, new Label("Drone:"), visit, scan);
        droneControls.setAlignment(Pos.CENTER_LEFT);

        StackPane frame = new StackPane(farmView.getPane());
        frame.getStyleClass().add("farm-frame");

        VBox box = new VBox(8, title, frame, droneControls);
        box.setPadding(new Insets(10));
        return box;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(status);
        bar.setPadding(new Insets(6, 10, 6, 10));
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    // ------------------------------------------------------------ behaviour

    private FarmComponent selected() {
        TreeItem<FarmComponent> item = tree.getSelectionModel().getSelectedItem();
        return item == null ? null : item.getValue();
    }

    /** Rebuilds the tree and the plot from the composite, then selects toSelect. */
    private void refresh(FarmComponent toSelect) {
        tree.setRoot(treeItemFor(root));
        farmView.render(root);
        TreeItem<FarmComponent> match = find(tree.getRoot(), toSelect);
        tree.getSelectionModel().select(match != null ? match : tree.getRoot());
        showSelection();
    }

    private static TreeItem<FarmComponent> treeItemFor(FarmComponent c) {
        TreeItem<FarmComponent> node = new TreeItem<>(c);
        node.setExpanded(true);
        for (FarmComponent child : c.getChildren()) {
            node.getChildren().add(treeItemFor(child));
        }
        return node;
    }

    private static TreeItem<FarmComponent> find(TreeItem<FarmComponent> node, FarmComponent target) {
        if (node.getValue() == target) return node;
        for (TreeItem<FarmComponent> child : node.getChildren()) {
            TreeItem<FarmComponent> hit = find(child, target);
            if (hit != null) return hit;
        }
        return null;
    }

    /** Fills the details and the command list for the selected object. */
    private void showSelection() {
        FarmComponent c = selected();
        details.getChildren().clear();
        commandPicker.setValue(null);
        commandPicker.getItems().clear();
        farmView.highlight(c);
        if (c == null) {
            commandChosen();
            return;
        }
        int row = 0;
        details.addRow(row++, new Label("type"), new Label(c.isContainer() ? "item-container" : "item"));
        for (Property p : Property.values()) {
            details.addRow(row++, new Label(p.getLabel()), new Label(p.read(c)));
        }
        if (c.isContainer()) {
            details.addRow(row, new Label("total price"), new Label(Property.format(c.getTotalPrice())));
        }
        commandPicker.getItems().addAll(c.getCommands());
        commandChosen();
    }

    /** "Change ..." commands need a value (pre-filled with the current one); add/delete do not. */
    private void commandChosen() {
        String command = commandPicker.getValue();
        FarmComponent c = selected();
        boolean needsValue = command != null && command.startsWith("Change ");
        valueField.setDisable(!needsValue);
        valueField.setText(needsValue && c != null ? Property.fromCommand(command).read(c) : "");
        executeButton.setDisable(command == null || c == null);
    }

    private void runCommand() {
        FarmComponent target = selected();
        String name = commandPicker.getValue();
        if (target == null || name == null) return;

        Command command;
        FarmComponent selectAfter = target;
        switch (name) {
            case Item.DELETE, ItemContainer.DELETE -> {
                command = new DeleteCommand(target);
                selectAfter = target.getParent();
            }
            case ItemContainer.ADD_ITEM, ItemContainer.ADD_CONTAINER -> {
                boolean container = name.equals(ItemContainer.ADD_CONTAINER);
                FarmComponent child = AddDialog.ask(stage, (ItemContainer) target, container).orElse(null);
                if (child == null) return;   // cancelled
                command = new AddCommand((ItemContainer) target, child);
                selectAfter = child;
            }
            default -> command = new ChangePropertyCommand(
                    target, Property.fromCommand(name), valueField.getText());
        }

        try {
            command.execute();
        } catch (IllegalArgumentException ex) {
            status.setText("Not changed: " + ex.getMessage());
            return;
        }
        refresh(selectAfter);
        status.setText(command.describe());
    }
}
