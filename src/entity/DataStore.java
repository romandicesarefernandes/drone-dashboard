package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory store for all entities (stands in for a database in this part).
 * Singleton so every page and controller sees the same data. Loaded with the
 * team's scenario data (Green Valley Farms) on first use.
 */
public final class DataStore {

    private static DataStore instance;

    public static synchronized DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
            SampleData.load(instance);
        }
        return instance;
    }

    private Farm farm;
    private final List<Field> fields = new ArrayList<>();
    private final List<User> users = new ArrayList<>();
    private final List<SoilMoistureReading> soilReadings = new ArrayList<>();
    private final List<CropGrowthRecord> cropRecords = new ArrayList<>();
    private final List<Alert> alerts = new ArrayList<>();
    private final List<Equipment> equipment = new ArrayList<>();
    private final List<FieldBoundary> boundaries = new ArrayList<>();
    private final List<Mission> missions = new ArrayList<>();
    private final List<Report> reports = new ArrayList<>();
    private final List<ReportSchedule> schedules = new ArrayList<>();

    private int lastUserId, lastReportId, lastScheduleId;

    /** Empty store; use getInstance() in the app. Public so tests can build their own data. */
    public DataStore() { }

    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }

    public List<Field> fields() { return fields; }
    public List<User> users() { return users; }
    public List<SoilMoistureReading> soilReadings() { return soilReadings; }
    public List<CropGrowthRecord> cropRecords() { return cropRecords; }
    public List<Alert> alerts() { return alerts; }
    public List<Equipment> equipment() { return equipment; }
    public List<FieldBoundary> boundaries() { return boundaries; }
    public List<Mission> missions() { return missions; }
    public List<Report> reports() { return reports; }
    public List<ReportSchedule> schedules() { return schedules; }

    public int nextUserId() { return ++lastUserId; }
    public int nextReportId() { return ++lastReportId; }
    public int nextScheduleId() { return ++lastScheduleId; }

    public Field findField(int fieldId) {
        for (Field f : fields) {
            if (f.fieldId() == fieldId) return f;
        }
        return null;
    }

    /** Field whose map boundary contains the point, or 0 if it is outside every field. */
    public int fieldAt(double x, double y) {
        for (FieldBoundary b : boundaries) {
            if (b.contains(x, y)) return b.fieldId();
        }
        return 0;
    }

    public FieldBoundary boundaryOf(int fieldId) {
        for (FieldBoundary b : boundaries) {
            if (b.fieldId() == fieldId) return b;
        }
        return null;
    }

    public String fieldName(int fieldId) {
        if (fieldId == 0) return "All fields";
        Field f = findField(fieldId);
        return f == null ? "Unknown field" : f.name();
    }
}
