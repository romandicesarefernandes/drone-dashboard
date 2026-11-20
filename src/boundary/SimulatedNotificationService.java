package boundary;

import entity.ReportSchedule;
import entity.Alert;
import entity.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stand-in for the real email/SMS provider: messages are kept in an outbox
 * and printed to the console instead of being sent.
 */
public class SimulatedNotificationService implements NotificationInterface {

    private final List<String> outbox = new ArrayList<>();

    @Override
    public void sendInvitation(User user, String fieldAccess) {
        send("To: " + user.getEmail() + " | Subject: Your AgriDrone Farm Dashboard invitation | "
                + "Role: " + user.getRole() + ", field access: " + fieldAccess
                + ". Use the activation link to set up your account.");
    }

    @Override
    public void sendReportScheduled(ReportSchedule schedule) {
        send("Scheduled report: " + schedule.reportType() + " (" + schedule.fields() + "), "
                + schedule.frequency().toLowerCase() + " on " + schedule.deliveryDay() + ".");
    }

@Override
public void sendAlert(Alert alert) {
    send("Farm alert | Severity: " + alert.severity()
            + " | Field ID: " + alert.fieldId()
            + " | Message: " + alert.message()
            + " | Raised: " + alert.raisedAt());
}


    public List<String> getOutbox() {
        return Collections.unmodifiableList(outbox);
    }

    private void send(String message) {
        outbox.add(message);
        System.out.println("[notification] " + message);
    }
}
