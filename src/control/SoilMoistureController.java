package control;

import entity.DataStore;
import entity.Field;
import entity.Mission;
import entity.SoilMoistureReading;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Control for UC-04 Monitor Soil Moisture: splits each field scan into a grid
 * of zones, gives the moisture map and trend for a field, and keeps the
 * low / high alert thresholds.
 *
 * The sample data has one reading per field per scan, so the zone values are
 * that reading plus a fixed spatial pattern per field (the same on every run).
 */
public final class SoilMoistureController {

    public static final int ROWS = 8, COLS = 12;
    public static final double DEFAULT_LOW = 20, DEFAULT_HIGH = 45;

    /** One zone of a scan: name like "B4" (row letter, column number). */
    public record Zone(String name, int row, int col, double moisture) { }

    private final DataStore store;
    private final Map<Integer, double[]> thresholds = new HashMap<>();   // fieldId -> {low, high}

    public SoilMoistureController(DataStore store) {
        this.store = store;
    }

    public List<Field> fields() { return store.fields(); }

    /** Scan dates for the field, newest first. */
    public List<LocalDate> scanDates(int fieldId) {
        return readings(fieldId).stream().map(r -> r.timestamp().toLocalDate())
                .sorted(Comparator.reverseOrder()).toList();
    }

    /** Zones of the field's scan on that date (empty if there was no scan). */
    public List<Zone> getMoistureMap(int fieldId, LocalDate date) {
        SoilMoistureReading r = reading(fieldId, date);
        List<Zone> zones = new ArrayList<>();
        if (r == null) return zones;
        double[][] pattern = pattern(fieldId);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double v = Math.max(5, Math.min(95, r.moistureLevel() + pattern[row][col]));
                zones.add(new Zone(zoneName(row, col), row, col, Math.round(v * 10) / 10.0));
            }
        }
        return zones;
    }

    public static double average(List<Zone> zones) {
        return zones.stream().mapToDouble(Zone::moisture).average().orElse(0);
    }

    public static Zone driest(List<Zone> zones) {
        return zones.stream().min(Comparator.comparingDouble(Zone::moisture)).orElse(null);
    }

    public static Zone wettest(List<Zone> zones) {
        return zones.stream().max(Comparator.comparingDouble(Zone::moisture)).orElse(null);
    }

    public List<Zone> zonesBelowLow(int fieldId, List<Zone> zones) {
        double low = getThresholds(fieldId)[0];
        return zones.stream().filter(z -> z.moisture() < low).toList();
    }

    /** Field average and driest zone for each scan in the 30 days up to the date. */
    public List<double[]> trend(int fieldId, LocalDate upTo) {
        List<double[]> points = new ArrayList<>();   // {epochDay, average, driest}
        for (SoilMoistureReading r : readings(fieldId)) {
            LocalDate d = r.timestamp().toLocalDate();
            if (d.isAfter(upTo) || d.isBefore(upTo.minusDays(30))) continue;
            List<Zone> zones = getMoistureMap(fieldId, d);
            points.add(new double[] { d.toEpochDay(), average(zones), driest(zones).moisture() });
        }
        points.sort(Comparator.comparingDouble(p -> p[0]));
        return points;
    }

    public double[] getThresholds(int fieldId) {
        return thresholds.getOrDefault(fieldId, new double[] { DEFAULT_LOW, DEFAULT_HIGH });
    }

    /** Stores the field's thresholds. Low must be below high, both 0..100. */
    public void saveThresholds(int fieldId, double low, double high) {
        if (low < 0 || high > 100 || low >= high) {
            throw new IllegalArgumentException("Low must be below high, and both between 0 and 100 %.");
        }
        thresholds.put(fieldId, new double[] { low, high });
    }

    /** Request Scan: schedules a soil-moisture mission over the field for tomorrow morning. */
    public Mission requestScan(int fieldId) {
        return scheduleMission(store, "Soil Moisture", fieldId, LocalDate.now().plusDays(1).atTime(7, 30));
    }

    /** Adds a Scheduled mission to the store (shared by the monitoring pages). */
    static Mission scheduleMission(DataStore store, String type, int fieldId, LocalDateTime when) {
        int id = store.missions().stream().mapToInt(Mission::missionId).max().orElse(0) + 1;
        Mission m = new Mission(id, when, "Scheduled", id, type, fieldId, "D" + (id % 3 + 1));
        store.missions().add(m);
        return m;
    }

    /** CSV of the zones of one scan. */
    public String toCsv(int fieldId, LocalDate date) {
        StringBuilder sb = new StringBuilder("Field,Date,Zone,Moisture %\n");
        for (Zone z : getMoistureMap(fieldId, date)) {
            sb.append(store.fieldName(fieldId)).append(',').append(date).append(',')
              .append(z.name()).append(',').append(z.moisture()).append('\n');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ helpers

    public static String zoneName(int row, int col) {
        return (char) ('A' + row) + String.valueOf(col + 1);
    }

    private List<SoilMoistureReading> readings(int fieldId) {
        return store.soilReadings().stream().filter(r -> r.fieldId() == fieldId).toList();
    }

    private SoilMoistureReading reading(int fieldId, LocalDate date) {
        for (SoilMoistureReading r : readings(fieldId)) {
            if (r.timestamp().toLocalDate().equals(date)) return r;
        }
        return null;
    }

    /** Fixed offset per zone: a smooth wet-to-dry slope plus a little noise. */
    private static double[][] pattern(int fieldId) {
        Random rnd = new Random(100 + fieldId);
        double[][] p = new double[ROWS][COLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                p[row][col] = 9 * Math.sin(col / 2.5 + fieldId) + 6 * Math.cos(row / 2.0 + fieldId * 0.7)
                        + (rnd.nextDouble() * 4 - 2);
            }
        }
        return p;
    }
}
