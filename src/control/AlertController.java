package control;

import entity.Alert;
import entity.DataStore;
import java.util.List;
import java.util.ArrayList;

public class AlertController {

    private final DataStore store;

    public AlertController(DataStore store) {
        this.store = store;
    }

    public List<Alert> getAlerts() {
        return new ArrayList<>(store.alerts());
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
    public String getFieldName(int fieldId) {
        return store.fieldName(fieldId);
    }  
}