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

        Button acknowledgeButton = new Button("Acknowledge Selected Alert");

        acknowledgeButton.setOnAction(event -> {
            Alert selectedAlert = alertTable.getSelectionModel().getSelectedItem();

            if (selectedAlert != null) {
                boolean updated = controller.acknowledgeAlert(
                    selectedAlert.alertId()
                );

                if (updated) {
                    refreshAlerts();
                }
            }
        });

        view.setCenter(Page.card(
            "Farm Alerts",
            filters,
            alertTable,
            acknowledgeButton
        ));
        BorderPane.setMargin(view.getCenter(), new Insets(16));
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