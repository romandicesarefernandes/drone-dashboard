package control;

import entity.CropGrowthRecord;
import entity.DataStore;
import entity.Field;
import entity.Mission;
import entity.SampleData;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Control for UC-05 Monitor Crop Growth: NDVI map of a flight, growth by zone
 * and actual versus expected NDVI. Also used by harvest readiness (UC-06
 * «include» UC-05).
 *
 * The sample data has one NDVI value per field per flight, so the map cells are
 * that value plus a fixed pattern per field. East Orchard's NE corner and North
 * 40's SW corner lag behind, so the demo has zones below expected.
 */
public final class CropGrowthController {

    public static final int ROWS = 9, COLS = 14;
    /** A zone this far below expected growth (percent) is flagged. */
    public static final int LAG_THRESHOLD_PCT = 15;
    public static final String[] ZONES = { "NW", "NE", "SW", "SE" };

    public record Cell(int row, int col, String zone, double ndvi) { }

    public record ZoneGrowth(String zone, double ndvi, double vsExpectedPct, String status) { }

    private final DataStore store;

    public CropGrowthController(DataStore store) {
        this.store = store;
    }

    public List<Field> fields() { return store.fields(); }

    /** Flight dates for the field, newest first. */
    public List<LocalDate> flightDates(int fieldId) {
        return records(fieldId).stream().map(r -> r.recordedAt().toLocalDate())
                .sorted(Comparator.reverseOrder()).toList();
    }

    public CropGrowthRecord record(int fieldId, LocalDate date) {
        for (CropGrowthRecord r : records(fieldId)) {
            if (r.recordedAt().toLocalDate().equals(date)) return r;
        }
        return null;
    }

    public CropGrowthRecord latestRecord(int fieldId) {
        return records(fieldId).stream().max(Comparator.comparing(CropGrowthRecord::recordedAt)).orElse(null);
    }

    public double peakNdvi(int fieldId) {
        return records(fieldId).stream().mapToDouble(CropGrowthRecord::ndvi).max().orElse(0);
    }

    /** NDVI of every map cell for the flight on that date. */
    public List<Cell> ndviMap(int fieldId, LocalDate date) {
        CropGrowthRecord r = record(fieldId, date);
        List<Cell> cells = new ArrayList<>();
        if (r == null) return cells;
        Random rnd = new Random(200 + fieldId);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                String zone = zoneOf(row, col);
                double v = r.ndvi() + lag(fieldId, zone) + 0.05 * Math.sin(col / 2.0 + row / 3.0 + fieldId)
                        + (rnd.nextDouble() * 0.06 - 0.03);
                cells.add(new Cell(row, col, zone, round2(Math.max(0.05, Math.min(0.95, v)))));
            }
        }
        return cells;
    }

    /** Average NDVI of each zone compared with the expected curve. */
    public List<ZoneGrowth> growthByZone(int fieldId, LocalDate date) {
        List<Cell> cells = ndviMap(fieldId, date);
        double expected = expectedNdvi(fieldId, date);
        List<ZoneGrowth> zones = new ArrayList<>();
        for (String z : ZONES) {
            double avg = cells.stream().filter(c -> c.zone().equals(z)).mapToDouble(Cell::ndvi).average().orElse(0);
            double pct = Math.round((avg - expected) / expected * 100);
            String status = pct <= -LAG_THRESHOLD_PCT ? "Below expected" : pct <= -7 ? "Watch" : "On track";
            zones.add(new ZoneGrowth(z, round2(avg), pct, status));
        }
        return zones;
    }

    /** Expected NDVI on the date: straight line between the field's first and last flight values. */
    public double expectedNdvi(int fieldId, LocalDate date) {
        List<LocalDate> dates = flightDates(fieldId);
        if (dates.isEmpty()) return 0;
        double first = dates.get(dates.size() - 1).toEpochDay(), last = dates.get(0).toEpochDay();
        double t = last == first ? 0 : (date.toEpochDay() - first) / (last - first);
        double[] curve = SampleData.NDVI_CURVE[fieldId];
        return curve[0] + (curve[1] - curve[0]) * t;
    }

    /** {epochDay, actual field NDVI, expected NDVI} for each flight up to the date. */
    public List<double[]> growthSeries(int fieldId, LocalDate upTo) {
        List<double[]> points = new ArrayList<>();
        for (CropGrowthRecord r : records(fieldId)) {
            LocalDate d = r.recordedAt().toLocalDate();
            if (d.isAfter(upTo)) continue;
            points.add(new double[] { d.toEpochDay(), r.ndvi(), expectedNdvi(fieldId, d) });
        }
        points.sort(Comparator.comparingDouble(p -> p[0]));
        return points;
    }

    /** Request Scan: schedules a crop-growth (multispectral) mission over the field for tomorrow. */
    public Mission requestScan(int fieldId) {
        return SoilMoistureController.scheduleMission(store, "Crop Growth", fieldId,
                LocalDate.now().plusDays(1).atTime(10, 30));
    }

    // ------------------------------------------------------------ helpers

    private List<CropGrowthRecord> records(int fieldId) {
        return store.cropRecords().stream().filter(r -> r.fieldId() == fieldId).toList();
    }

    private static String zoneOf(int row, int col) {
        return (row < ROWS / 2 ? "N" : "S") + (col < COLS / 2 ? "W" : "E");
    }

    private static double lag(int fieldId, String zone) {
        if (fieldId == 2 && zone.equals("NE")) return -0.14;
        if (fieldId == 1 && zone.equals("SW")) return -0.06;
        return 0.01;
    }

    private static double round2(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
