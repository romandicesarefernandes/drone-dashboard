package boundary;

import control.AlertController;
import entity.Alert;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextArea;
import entity.User;
import javafx.scene.control.ChoiceDialog;
import java.util.Optional;

public class AlertsUI implements Page {

    private final AlertController controller;
    private final BorderPane view = new BorderPane();
    private final TableView<Alert> alertTable = new TableView<>();
    private final ComboBox<String> severityFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public AlertsUI(AlertController controller) {
        this.controller = controller;
        buildView();
    }

    private void buildView() {

        // Page header
        view.setTop(Page.header(
            "Alerts & Notifications",
            "UC-08 Receive Alerts and Notifications"
        ));

        // Create table columns
        TableColumn<Alert, Integer> idColumn =
            new TableColumn<>("Alert ID");

        TableColumn<Alert, String> severityColumn =
            new TableColumn<>("Severity");

        TableColumn<Alert, String> messageColumn =
            new TableColumn<>("Message");

        TableColumn<Alert, String> statusColumn =
            new TableColumn<>("Status");
        TableColumn<Alert, String> fieldColumn =
        	    new TableColumn<>("Field");
        TableColumn<Alert, String> timeColumn =
        	    new TableColumn<>("Raised At");

        	timeColumn.setCellValueFactory(data ->
        	    new javafx.beans.property.SimpleStringProperty(
        	        data.getValue().raisedAt() == null
        	            ? ""
        	            : data.getValue().raisedAt().format(
        	                java.time.format.DateTimeFormatter.ofPattern(
        	                    "MM/dd/yyyy HH:mm"
        	                )
        	            )
        	    )
        	);

        	fieldColumn.setCellValueFactory(data ->
        	    new javafx.beans.property.SimpleStringProperty(
        	        controller.getFieldName(data.getValue().fieldId())
        	    )
        	);

        // Connect columns to Alert record methods
        idColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleIntegerProperty(
                data.getValue().alertId()
            ).asObject()
        );

        severityColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().severity()
            )
        );

        messageColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().message()
            )
        );

        statusColumn.setCellValueFactory(data ->
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().status()
            )
        );

        alertTable.getColumns().addAll(
        	    idColumn,
        	    fieldColumn,
        	    severityColumn,
        	    messageColumn,
        	    statusColumn,
        	    timeColumn
        	);

        alertTable.setColumnResizePolicy(
            TableView.CONSTRAINED_RESIZE_POLICY
        );

        // Set up severity filter
        severityFilter.getItems().addAll(
            "All", "Critical", "High", "Medium", "Low"
        );
        severityFilter.setValue("All");

        // Set up status filter
        statusFilter.getItems().addAll(
            "All", "Active", "Acknowledged"
        );
        statusFilter.setValue("All");

        // Refresh the table whenever a filter changes
        severityFilter.setOnAction(event -> refreshAlerts());
        statusFilter.setOnAction(event -> refreshAlerts());

        // Place both filters above the table
        HBox filters = new HBox(10,
            new Label("Severity:"),
            severityFilter,
            new Label("Status:"),
            statusFilter
        );

        filters.setAlignment(javafx.geometry.Pos.CENTER_LEFT);


        // Display alerts
        refreshAlerts();
        
        Button detailsButton = new Button("View Details");

        detailsButton.setOnAction(event -> {
            Alert selectedAlert = alertTable.getSelectionModel().getSelectedItem();

            if (selectedAlert != null) {
                showAlertDetails(selectedAlert);
            }
        });
        
        Button acknowledgeButton = new Button("Acknowledge Selected Alert");


acknowledgeButton.setOnAction(event -> {

    Alert selectedAlert =
            alertTable.getSelectionModel().getSelectedItem();

    if (selectedAlert == null ||
            selectedAlert.status().equalsIgnoreCase("Acknowledged")) {
        return;
    }

    if (controller.getUsers().isEmpty()) {
        return;
    }

    ChoiceDialog<User> dialog = new ChoiceDialog<>(
            null,
            controller.getUsers()
    );

    dialog.setTitle("Acknowledge Alert");
    dialog.setHeaderText(
            "Acknowledge Alert #" + selectedAlert.alertId());
    dialog.setContentText("Select your user account:");

    Optional<User> result = dialog.showAndWait();

    if (result.isPresent()) {
        boolean updated = controller.acknowledgeAlert(
                selectedAlert.alertId(),
                result.get()
        );

        if (updated) {
            refreshAlerts();
        }
    }
});


        HBox actions = new HBox(10, detailsButton, acknowledgeButton);

        view.setCenter(Page.card(
            "Farm Alerts",
            filters,
            alertTable,
            actions
        ));
        BorderPane.setMargin(view.getCenter(), new Insets(16));
    }


private void showAlertDetails(Alert alert) {

    Dialog<Void> dialog = new Dialog<>();
    dialog.setTitle("Alert Details");
    dialog.setHeaderText("Alert #" + alert.alertId());

    dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

    VBox details = new VBox(10);
    details.setPadding(new Insets(15));

    Label field = new Label(
            "Field: " + controller.getFieldName(alert.fieldId()));

    Label severity = new Label(
            "Severity: " + alert.severity());

    Label status = new Label(
            "Status: " + alert.status());

    Label raisedAt = new Label(
            "Raised At: " + alert.raisedAt());

    Label messageLabel = new Label("Alert Message:");

    TextArea message = new TextArea(alert.message());
    message.setEditable(false);
    message.setWrapText(true);
    message.setPrefRowCount(3);

    details.getChildren().addAll(
            field,
            severity,
            status,
            raisedAt,
            messageLabel,
            message
    );

Label historyHeader = new Label("Acknowledgment History");

historyHeader.setStyle("-fx-font-weight: bold;");

details.getChildren().add(historyHeader);

var history = controller.getAcknowledgmentHistory(alert.alertId());

if (history.isEmpty()) {
    details.getChildren().add(
            new Label("No acknowledgment history recorded.")
    );
} else {
    for (var entry : history) {
        Label record = new Label(
                "Acknowledged by: " + entry.userName()
                + "\nUser ID: " + entry.userId()
                + "\nAcknowledged at: "
                + entry.acknowledgedAt().format(
                        java.time.format.DateTimeFormatter.ofPattern(
                                "MM/dd/yyyy HH:mm"
                        )
                )
        );

        details.getChildren().add(record);
    }
}


    dialog.getDialogPane().setContent(details);
    dialog.showAndWait();
}

private void refreshAlerts() {

    FilteredList<Alert> filteredAlerts = new FilteredList<>(
        FXCollections.observableArrayList(controller.getAlerts())
    );

    filteredAlerts.setPredicate(alert -> {

        boolean matchesSeverity =
            severityFilter.getValue() == null ||
            severityFilter.getValue().equals("All") ||
            alert.severity().equalsIgnoreCase(
                severityFilter.getValue()
            );

        boolean matchesStatus =
            statusFilter.getValue() == null ||
            statusFilter.getValue().equals("All") ||
            alert.status().equalsIgnoreCase(
                statusFilter.getValue()
            );

        return matchesSeverity && matchesStatus;
    });

    alertTable.setItems(filteredAlerts);
}


    @Override
    public String getNavLabel() {
        return "Alerts";
    }

    @Override
    public Node getView() {
        return view;
    }

    @Override
    public void onShow() {
        refreshAlerts();
    }
}