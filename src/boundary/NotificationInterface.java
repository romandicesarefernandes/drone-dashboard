package boundary;

import entity.ReportSchedule;
import entity.Alert;
import entity.User;

/** Boundary to the email/SMS notification service (UC-08, UC-09, UC-10). */
public interface NotificationInterface {

    /** Sends the account-activation invitation to a new user (UC-10 step 4). */
    void sendInvitation(User user, String fieldAccess);

    /** Confirms a scheduled report; the report itself is emailed on each run (UC-09 alternate flow 5a). */
    void sendReportScheduled(ReportSchedule schedule);
    void sendAlert(Alert alert);
}
