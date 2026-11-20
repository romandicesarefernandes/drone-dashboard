package control;

import entity.Alert;
import entity.DataStore;
import java.util.List;
import java.util.ArrayList;
import boundary.NotificationInterface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import entity.User;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class AlertController {

    private final DataStore store;
    private final NotificationInterface notifier;

    public AlertController(DataStore store, NotificationInterface notifier) {
        this.store = store;
        this.notifier = notifier;
    }
    

    public List<Alert> getAlerts() {
        return new ArrayList<>(store.alerts());
    }

    public void raiseAlert(Alert alert) {
    store.alerts().add(alert);
    notifier.sendAlert(alert);
    }

public int checkSoilMoisture(
        SoilMoistureController soilController,
        int fieldId,
        LocalDate date) {

    List<SoilMoistureController.Zone> zones =
            soilController.getMoistureMap(fieldId, date);

    if (zones.isEmpty()) {
        return 0;
    }

    List<SoilMoistureController.Zone> dryZones =
            soilController.zonesBelowLow(fieldId, zones);

    if (dryZones.isEmpty()) {
        return 0;
    }

    // Avoid generating the same alert repeatedly.
    String message = "Low soil moisture detected in "
            + dryZones.size() + " zones on " + date
            + ". Recommended action: Inspect irrigation.";

    boolean alreadyExists = store.alerts().stream()
            .anyMatch(a -> a.fieldId() == fieldId
                    && a.message().equals(message));

    if (alreadyExists) {
        return 0;
    }

    int nextId = store.alerts().stream()
            .mapToInt(Alert::alertId)
            .max()
            .orElse(0) + 1;

    Alert alert = new Alert(
            nextId,
            fieldId,
            "High",
            message,
            "Active",
            LocalDateTime.now()
    );

    raiseAlert(alert);

    return 1;
}


    public boolean acknowledgeAlert(int alertId) {

        List<Alert> alerts = store.alerts();

        for (int i = 0; i < alerts.size(); i++) {
            Alert alert = alerts.get(i);

            if (alert.alertId() == alertId) {

                if (alert.status().equalsIgnoreCase("Acknowledged")) {
                    return false;
                }

                Alert updatedAlert = new Alert(
                    alert.alertId(),
                    alert.fieldId(),
                    alert.severity(),
                    alert.message(),
                    "Acknowledged",
                    alert.raisedAt()
                );

                alerts.set(i, updatedAlert);
                return true;
            }
        }

        return false;
    }

public boolean acknowledgeAlert(int alertId, User user) {

    if (user == null ||
            store.users().stream().noneMatch(
                    u -> u.getUserId() == user.getUserId())) {
        return false;
    }

    boolean updated = acknowledgeAlert(alertId);

    if (updated) {
        acknowledgmentHistory.add(new Acknowledgment(
                alertId,
                user.getUserId(),
                user.getName(),
                LocalDateTime.now()
        ));
    }

    return updated;
}

    public String getFieldName(int fieldId) {
        return store.fieldName(fieldId);
    }
    

public record Acknowledgment(
        int alertId,
        int userId,
        String userName,
        LocalDateTime acknowledgedAt) { }

private final List<Acknowledgment> acknowledgmentHistory =
        new ArrayList<>();

public List<User> getUsers() {
    return store.users();
}

public List<Acknowledgment> getAcknowledgmentHistory(int alertId) {
    return acknowledgmentHistory.stream()
            .filter(a -> a.alertId() == alertId)
            .toList();
}

}