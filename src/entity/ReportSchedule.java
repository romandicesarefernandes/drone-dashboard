package entity;

import java.time.LocalDateTime;

/** Entity: a report set to be generated and emailed on a recurring basis (UC-09 alternate flow 5a). */
public record ReportSchedule(int scheduleId, String reportType, String fields, String frequency,
                             String deliveryDay, LocalDateTime createdAt) { }
