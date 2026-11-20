package control;

import entity.CropGrowthRecord;
import entity.Field;
import entity.Mission;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Control for UC-06 Determine Harvest Readiness: readiness percent,
 * recommended harvest window and estimated yield per field, worked out from
 * the latest crop-growth flight (UC-05, through CropGrowthController).
 */
public final class HarvestReadinessController {

    /** Crop data older than this many days is flagged as possibly outdated. */
    public static final int STALE_DATA_DAYS = 7;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MMM d", Locale.US);

    /** Readiness percent for each growth stage in the sample data. */
    private static final Map<String, Integer> STAGE_READINESS = Map.ofEntries(
            Map.entry("Germination", 5), Map.entry("Tillering", 12),
            Map.entry("Reproductive", 35), Map.entry("Pod Fill", 35), Map.entry("Fruit Development", 40),
            Map.entry("Grain Fill", 65), Map.entry("Seed Fill", 65), Map.entry("Ripening", 75),
            Map.entry("Mature", 92), Map.entry("Harvest Ready", 98));

    /** Typical yield per crop at a peak NDVI of 0.8, and its unit. */
    private static final Map<String, Object[]> TYPICAL_YIELD = Map.of(
            "Corn", new Object[] { 185.0, "bu/ac" }, "Soybean", new Object[] { 52.0, "bu/ac" },
            "Wheat", new Object[] { 71.0, "bu/ac" }, "Apples", new Object[] { 12.0, "t/ac" });

    public record Estimate(Field field, String stage, int readiness, String window, String yield,
                           LocalDate lastFlight, long dataAgeDays, String status) {
        public boolean stale() { return dataAgeDays > STALE_DATA_DAYS; }
    }

    private final CropGrowthController growth;
    private final Set<Integer> markedReady = new HashSet<>();

    public HarvestReadinessController(CropGrowthController growth) {
        this.growth = growth;
    }

    /** Estimates for every field, most ready first. */
    public List<Estimate> getReadiness() {
        List<Estimate> list = new ArrayList<>();
        for (Field f : growth.fields()) {
            Estimate e = calculateReadiness(f);
            if (e != null) list.add(e);
        }
        list.sort(Comparator.comparingInt(Estimate::readiness).reversed());
        return list;
    }

    public Estimate calculateReadiness(Field f) {
        CropGrowthRecord r = growth.latestRecord(f.fieldId());
        if (r == null) return null;
        int readiness = STAGE_READINESS.getOrDefault(r.growthStage(), 50);
        LocalDate last = r.recordedAt().toLocalDate();
        LocalDate today = LocalDate.now();
        long age = ChronoUnit.DAYS.between(last, today);

        String window;
        if (readiness >= 90) window = range(max(today, last.plusDays(1)), 6);
        else if (readiness >= 60) window = range(max(today, last.plusDays(10)), 10);
        else if (f.cropType().equals("Wheat")) window = "Jun - Jul " + (last.getYear() + 1) + " (winter wheat)";
        else window = range(max(today, last.plusDays(30)), 30);

        Object[] typical = TYPICAL_YIELD.getOrDefault(f.cropType(), new Object[] { 0.0, "" });
        double yield = (double) typical[0] * growth.peakNdvi(f.fieldId()) / 0.8;

        String status = markedReady.contains(f.fieldId()) ? "Ready to harvest"
                : readiness >= 90 ? "Ready" : readiness >= 60 ? "Approaching" : "Growing";
        return new Estimate(f, r.growthStage(), readiness, window,
                Math.round(yield) + " " + typical[1], last, age, status);
    }

    /** Mark Ready to Harvest: only for fields that are at least approaching readiness. */
    public void markReadyToHarvest(Field f) {
        Estimate e = calculateReadiness(f);
        if (e == null || e.readiness() < 60) {
            throw new IllegalArgumentException(f.name() + " is still growing ("
                    + (e == null ? 0 : e.readiness()) + "% ready).");
        }
        markedReady.add(f.fieldId());
    }

    /** Schedule Harvest Scan: books a fresh crop-growth flight to update the estimate. */
    public Mission scheduleHarvestScan(Field f) {
        return growth.requestScan(f.fieldId());
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static String range(LocalDate start, int days) {
        return start.format(DAY) + " - " + start.plusDays(days).format(DAY);
    }
}
