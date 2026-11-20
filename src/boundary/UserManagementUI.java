package boundary;

import control.UserManagementController;
import entity.AccountStatus;
import entity.Field;
import entity.Role;
import entity.User;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Users & Settings page (boundary for UC-10 Manage Users and Roles): lists all
 * users with role, status and last login; Invite User, Edit Role, Deactivate.
 */
public final class UserManagementUI implements Page {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("MM/dd/yyyy h:mm a", Locale.US);

    private final UserManagementController controller;
    private final Supplier<Window> owner;
    private final Consumer<String> status;

    private final TableView<User> table = new TableView<>();
    private final GridPane details = new GridPane();
    private final Button editRole = new Button("Edit Role");
    private final Button deactivate = new Button("Deactivate");
    private final BorderPane view = new BorderPane();

    public UserManagementUI(UserManagementController controller, Supplier<Window> owner, Consumer<String> status) {
        this.controller = controller;
        this.owner = owner;
        this.status = status;

        buildTable();
        details.setHgap(14);
        details.setVgap(8);

        Button invite = new Button("Invite User");
        invite.getStyleClass().add("primary-button");
        invite.setOnAction(e -> inviteUser());
        editRole.setOnAction(e -> editRole());
        deactivate.setOnAction(e -> deactivate());
        HBox controls = new HBox(12, invite, editRole, deactivate);
        controls.setAlignment(Pos.CENTER_LEFT);

        VBox usersCard = Page.card("Users", table);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(usersCard, Priority.ALWAYS);
        VBox detailsCard = Page.card("User Details", details);
        detailsCard.setPrefWidth(340);
        detailsCard.setMinWidth(340);
        HBox center = new HBox(14, usersCard, detailsCard);
        center.setPadding(new Insets(4, 16, 10, 16));

        VBox controlsCard = Page.card("User Controls", controls);
        VBox bottom = new VBox(controlsCard);
        bottom.setPadding(new Insets(0, 16, 16, 16));

        view.setTop(Page.header("Users & Settings", "UC-10 Manage Users and Roles"));
        view.setCenter(center);
        view.setBottom(bottom);

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, now) -> showDetails());
        reload(null);
    }

    @Override public String getNavLabel() { return "Users & Settings"; }
    @Override public Node getView() { return view; }

    @Override
    public void onShow() {
        reload(table.getSelectionModel().getSelectedItem());
        status.accept(controller.loadUsers().size() + " users. Select a user to see details, or click Invite User.");
    }

    // ------------------------------------------------------------ users list and details

    private void buildTable() {
        table.getColumns().add(column("Name", 170, User::getName));
        table.getColumns().add(column("Email", 250, User::getEmail));
        table.getColumns().add(column("Role", 160, u -> u.getRole().getLabel()));
        TableColumn<User, String> statusCol = column("Status", 90, u -> u.getAccountStatus().getLabel());
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeAll("status-active", "status-pending", "status-inactive");
                setText(empty ? null : value);
                if (!empty && value != null) getStyleClass().add("status-" + value.toLowerCase());
            }
        });
        table.getColumns().add(statusCol);
        table.getColumns().add(column("Last Login", 160, u -> lastLogin(u)));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No users yet."));
    }

    private static TableColumn<User, String> column(String title, double width,
                                                    java.util.function.Function<User, String> value) {
        TableColumn<User, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
        return c;
    }

    private void reload(User select) {
        table.setItems(FXCollections.observableArrayList(controller.loadUsers()));
        if (select != null) table.getSelectionModel().select(select);
        else table.getSelectionModel().selectFirst();
        table.refresh();
        showDetails();
    }

    private void showDetails() {
        User u = table.getSelectionModel().getSelectedItem();
        details.getChildren().clear();
        editRole.setDisable(u == null);
        deactivate.setDisable(u == null || u.getAccountStatus() == AccountStatus.INACTIVE);
        if (u == null) {
            details.add(new Label("Select a user."), 0, 0);
            return;
        }
        int row = 0;
        row = detail(row, "Name", u.getName());
        row = detail(row, "Email", u.getEmail());
        row = detail(row, "Phone", u.getPhone().isEmpty() ? "-" : u.getPhone());
        row = detail(row, "Role", u.getRole().getLabel());
        row = detail(row, "Field Access", controller.fieldAccessLabel(u));
        row = detail(row, "Status", u.getAccountStatus().getLabel());
        row = detail(row, "Last Login", lastLogin(u));
        detail(row, "Member Since", u.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
    }

    private int detail(int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("detail-label");
        Label v = new Label(value);
        v.setWrapText(true);
        v.setMaxWidth(200);
        details.addRow(row, l, v);
        return row + 1;
    }

    private static String lastLogin(User u) {
        return u.getLastLogin() == null ? "Never" : u.getLastLogin().format(STAMP);
    }

    // ------------------------------------------------------------ Invite User

    private void inviteUser() {
        TextField name = new TextField();
        name.setPromptText("Full name");
        TextField email = new TextField();
        email.setPromptText("name@example.com");
        TextField phone = new TextField();
        phone.setPromptText("Optional");
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.setValue(Role.FARM_MANAGER);
        List<CheckBox> fieldBoxes = fieldCheckBoxes(List.of());

        GridPane form = form();
        form.addRow(0, new Label("Name"), name);
        form.addRow(1, new Label("Email"), email);
        form.addRow(2, new Label("Phone"), phone);
        form.addRow(3, new Label("Role"), role);
        form.addRow(4, new Label("Field Access"), fieldPane(fieldBoxes));
        Label error = errorLabel();
        form.add(error, 0, 5, 2, 1);

        ButtonType send = new ButtonType("Send Invitation", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = dialog("Invite User", "Invite a new user to the dashboard", form, send);
        User[] created = new User[1];
        dialog.getDialogPane().lookupButton(send).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                created[0] = controller.inviteUser(name.getText(), email.getText(), phone.getText(),
                        role.getValue(), selectedFieldIds(fieldBoxes));
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                event.consume();   // keep the form open
            }
        });
        if (dialog.showAndWait().filter(b -> b == send).isPresent() && created[0] != null) {
            reload(created[0]);
            status.accept("Invitation sent to " + created[0].getEmail() + ". "
                    + created[0].getName() + " is listed as Pending until they activate the account.");
        }
    }

    // ------------------------------------------------------------ Edit Role

    private void editRole() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) return;
        ComboBox<Role> role = new ComboBox<>(FXCollections.observableArrayList(Role.values()));
        role.setValue(u.getRole());
        List<CheckBox> fieldBoxes = fieldCheckBoxes(u.getFieldAccess());

        GridPane form = form();
        form.addRow(0, new Label("User"), new Label(u.getName() + "  (" + u.getEmail() + ")"));
        form.addRow(1, new Label("Role"), role);
        form.addRow(2, new Label("Field Access"), fieldPane(fieldBoxes));
        Label error = errorLabel();
        form.add(error, 0, 3, 2, 1);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = dialog("Edit Role", "Change the role or field access of " + u.getName(), form, save);
        dialog.getDialogPane().lookupButton(save).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                controller.editRole(u, role.getValue(), selectedFieldIds(fieldBoxes));
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                event.consume();
            }
        });
        if (dialog.showAndWait().filter(b -> b == save).isPresent()) {
            reload(u);
            status.accept("Saved: " + u.getName() + " is now " + u.getRole().getLabel()
                    + " with access to " + controller.fieldAccessLabel(u)
                    + ". The change applies at their next action.");
        }
    }

    // ------------------------------------------------------------ Deactivate

    private void deactivate() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(owner.get());
        confirm.setTitle("Deactivate");
        confirm.setHeaderText("Deactivate " + u.getName() + "?");
        confirm.setContentText("They will no longer be able to log in. Their account and history are kept.");
        if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;
        try {
            controller.deactivate(u);
            reload(u);
            status.accept(u.getName() + " was deactivated. Their history is kept.");
        } catch (IllegalArgumentException ex) {
            status.accept("Not deactivated: " + ex.getMessage());
            Alert warn = new Alert(Alert.AlertType.WARNING, ex.getMessage());
            warn.initOwner(owner.get());
            warn.setHeaderText("Cannot deactivate " + u.getName());
            warn.showAndWait();
        }
    }

    // ------------------------------------------------------------ form helpers

    private List<CheckBox> fieldCheckBoxes(List<Integer> checked) {
        List<CheckBox> boxes = new ArrayList<>();
        for (Field f : controller.getFields()) {
            CheckBox cb = new CheckBox(f.name());
            cb.setUserData(f.fieldId());
            cb.setSelected(checked.contains(f.fieldId()));
            boxes.add(cb);
        }
        return boxes;
    }

    private static FlowPane fieldPane(List<CheckBox> boxes) {
        FlowPane pane = new FlowPane(14, 6);
        pane.setPrefWrapLength(260);
        pane.getChildren().addAll(boxes);
        return pane;
    }

    private static List<Integer> selectedFieldIds(List<CheckBox> boxes) {
        List<Integer> ids = new ArrayList<>();
        for (CheckBox cb : boxes) {
            if (cb.isSelected()) ids.add((Integer) cb.getUserData());
        }
        return ids;
    }

    private static GridPane form() {
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        return form;
    }

    private static Label errorLabel() {
        Label error = new Label();
        error.getStyleClass().add("error-text");
        error.setWrapText(true);
        return error;
    }

    private Dialog<ButtonType> dialog(String title, String header, Node content, ButtonType confirm) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(owner.get());
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(confirm, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(460);
        return dialog;
    }
}
