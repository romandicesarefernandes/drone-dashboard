package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entity: one tracked piece of equipment with its GPS/BLE tag (UC-07).
 * Keeps the UML attributes (equipmentId, name, description, quantity,
 * imagePath) plus what tracking needs: type, tag, status, operator,
 * geofence, last known position and the location history.
 *
 * Positions are in farm-map units (1000 x 600, origin top-left).
 */
public class Equipment {

    /** One location report, from the GPS tag, a drone scan, or registration. */
    public record LocationRecord(LocalDateTime time, double x, double y, int fieldId, String source) { }

    public static final String ACTIVE = "Active";
    public static final String IDLE = "Idle";
    public static final String MISSING = "Missing";

    private final int equipmentId;
    private final String name;
    private final String description;
    private final int quantity;
    private final String imagePath;
    private final String type;
    private final String tagId;
    private String status;
    private String operator;
    private int geofenceFieldId;   // 0 = no geofence
    private double x, y;
    private int lastKnownFieldId;
    private LocalDateTime lastSeen;
    private final List<LocationRecord> history = new ArrayList<>();

    public Equipment(int equipmentId, String name, String type, String description, String tagId,
                     String status, String operator, int geofenceFieldId) {
        this.equipmentId = equipmentId;
        this.name = name;
        this.type = type;
        this.description = description;
        this.tagId = tagId;
        this.status = status;
        this.operator = operator;
        this.geofenceFieldId = geofenceFieldId;
        this.quantity = 1;
        this.imagePath = "";
    }

    public int getEquipmentId() { return equipmentId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public String getImagePath() { return imagePath; }
    public String getTagId() { return tagId; }
    public String getStatus() { return status; }
    public String getOperator() { return operator; }
    public int getGeofenceFieldId() { return geofenceFieldId; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getLastKnownFieldId() { return lastKnownFieldId; }
    public LocalDateTime getLastSeen() { return lastSeen; }

    /** Oldest first. */
    public List<LocationRecord> getHistory() { return Collections.unmodifiableList(history); }

    public void setStatus(String status) { this.status = status; }
    public void setOperator(String operator) { this.operator = operator; }
    public void setGeofenceFieldId(int fieldId) { this.geofenceFieldId = fieldId; }

    /** Stores a new location report and makes it the last known position. */
    public void recordLocation(LocalDateTime time, double x, double y, int fieldId, String source) {
        this.x = x;
        this.y = y;
        this.lastKnownFieldId = fieldId;
        this.lastSeen = time;
        history.add(new LocationRecord(time, x, y, fieldId, source));
    }

    @Override
    public String toString() { return name; }
}
