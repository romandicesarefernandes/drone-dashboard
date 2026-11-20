package control;

import boundary.NotificationInterface;
import entity.Alert;
import entity.CropGrowthRecord;
import entity.DataStore;
import entity.Equipment;
import entity.Field;
import entity.Mission;
import entity.Report;
import entity.Report.Chart;
import entity.Report.Point;
import entity.Report.Series;
import entity.ReportSchedule;
import entity.SoilMoistureReading;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * Control for UC-09 Generate Farm Reports: compiles stored drone data for a
 * report type, period and fields into a Report, keeps the report history and
 * scheduled reports.
 */
public class ReportController {

    public static final String FARM_SUMMARY = "Farm Summary";
    public static final String SOIL_MOISTURE = "Soil Moisture";
    public static final String CROP_GROWTH = "Crop Growth";
    public static final String HARVEST_READINESS = "Harvest Readiness";
    public static final String EQUIPMENT_USAGE = "Equipment Usage";
    public static final String DRONE_MISSIONS = "Drone Missions";
    public static final List<String> REPORT_TYPES = List.of(
            FARM_SUMMARY, SOIL_MOISTURE, CROP_GROWTH, HARVEST_READINESS, EQUIPMENT_USAGE, DRONE_MISSIONS);

    public static final String ALL_FIELDS = "All Fields";
    public static final List<String> FREQUENCIES = List.of("Weekly", "Monthly");

    /** Moisture thresholds from the Soil Moisture wireframe (low 40 %, high 70 %). */
    public static final double LOW_MOISTURE = 40;
    public static final double HIGH_MOISTURE = 70;

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM/dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

    private final DataStore store;
    private final NotificationInterface notifier;

    public ReportController(DataStore store, NotificationInterface notifier) {
        this.store = store;
        this.notifier = notifier;
    }

    /** "All Fields" followed by every field name. */
    public List<String> fieldChoices() {
        List<String> out = new ArrayList<>();
        out.add(ALL_FIELDS);
        for (Field f : store.fields()) out.add(f.name());
        return out;
    }

    public String farmLabel() {
        return store.getFarm().name() + " (" + store.getFarm().location() + ")";
    }

    public List<Report> history() {
        List<Report> out = new ArrayList<>(store.reports());
        out.sort(Comparator.comparing(Report::getReportId).reversed());
        return out;
    }

    public List<ReportSchedule> schedules() {
        return List.copyOf(store.schedules());
    }

    /**
     * Generate Report (UC-09 steps 2-4). Throws IllegalArgumentException with a
     * message for the user when the input is incomplete or there is no data.
     */
    public Report generate(String type, String fieldChoice, LocalDate start, LocalDate end) {
        if (type == null || !REPORT_TYPES.contains(type)) throw new IllegalArgumentException("Select a report type.");
        if (fieldChoice == null) throw new IllegalArgumentException("Select a field.");
        if (start == null || end == null) throw new IllegalArgumentException("Select a start and an end date.");
        if (start.isAfter(end)) throw new IllegalArgumentException("The start date must be on or before the end date.");

        List<Field> fields = selectedFields(fieldChoice);
        Builder b = new Builder();
        switch (type) {
            case FARM_SUMMARY -> farmSummary(b, fields, start, end);
            case SOIL_MOISTURE -> soilMoisture(b, fields, start, end);
            case CROP_GROWTH -> cropGrowth(b, fields, start, end);
            case HARVEST_READINESS -> harvestReadiness(b, fields, start, end);
            case EQUIPMENT_USAGE -> equipmentUsage(b, fields, start, end);
            default -> droneMissions(b, fields, start, end);
        }
        Report report = new Report(store.nextReportId(), type, LocalDateTime.now(), start, end,
                fieldChoice, b.summary, b.columns, b.rows, b.chart);
        store.reports().add(report);   // postcondition: saved to the report history
        return report;
    }

    /** Export PDF / Export CSV record the format on the report in the history. */
    public void markExported(Report report, String format) {
        report.addFormat(format);
    }

    /** Schedule Report (UC-09 alternate flow 5a). */
    public ReportSchedule schedule(String type, String fieldChoice, String frequency, String deliveryDay) {
        if (type == null || !REPORT_TYPES.contains(type)) throw new IllegalArgumentException("Select a report type.");
        if (fieldChoice == null) throw new IllegalArgumentException("Select a field.");
        if (frequency == null || !FREQUENCIES.contains(frequency)) throw new IllegalArgumentException("Select a frequency.");
        if (deliveryDay == null || deliveryDay.isBlank()) throw new IllegalArgumentException("Select a delivery day.");
        ReportSchedule s = new ReportSchedule(store.nextScheduleId(), type, fieldChoice, frequency, deliveryDay,
                LocalDateTime.now());
        store.schedules().add(s);
        notifier.sendReportScheduled(s);
        return s;
    }

    // ------------------------------------------------------------ report types

    private void farmSummary(Builder b, List<Field> fields, LocalDate start, LocalDate end) {
        List<SoilMoistureReading> soil = soil(fields, start, end);
        List<CropGrowthRecord> crop = crop(fields, start, end);
        if (soil.isEmpty() && crop.isEmpty()) throw noData(start, end);

        List<CropGrowthRecord> latest = latestPerField(fields, crop);
        double avgNdvi = latest.stream().mapToDouble(CropGrowthRecord::ndvi).average().orElse(Double.NaN);
        double avgReady = latest.stream().mapToInt(r -> readiness(r.growthStage())).average().orElse(Double.NaN);
        List<Alert> alerts = alerts(fields, start, end);
        long activeAlerts = alerts.stream().filter(a -> a.status().equals("Active")).count();
        int units = 0, missing = 0;
        for (Equipment e : equipment(fields)) {
            units += e.getQuantity();
            if (e.getStatus().equals(Equipment.MISSING)) missing += e.getQuantity();
        }

        b.summary("Soil moisture (average)", soil.isEmpty() ? "No readings" : pct(avg(soil)));
        b.summary("Crop health", Double.isNaN(avgNdvi) ? "No records"
                : healthLabel(avgNdvi) + " (NDVI " + two(avgNdvi) + ")");
        b.summary("Harvest readiness (average)", Double.isNaN(avgReady) ? "No records" : Math.round(avgReady) + "%");
        b.summary("Equipment", units + " units / " + missing + " missing");
        b.summary("Alerts", alerts.size() + " (" + activeAlerts + " active)");
        b.summary("Drone missions completed", String.valueOf(missions(fields, start, end).stream()
                .filter(m -> m.status().equals("Completed")).count()));

        b.columns("Field", "Crop", "Acres", "Avg moisture", "Min moisture", "Latest NDVI", "Growth stage",
                "Readiness", "Alerts");
        for (Field f : fields) {
            List<SoilMoistureReading> fs = soil.stream().filter(r -> r.fieldId() == f.fieldId()).toList();
            CropGrowthRecord last = latest.stream().filter(r -> r.fieldId() == f.fieldId()).findFirst().orElse(null);
            long fieldAlerts = alerts.stream().filter(a -> a.fieldId() == f.fieldId()).count();
            b.row(f.name(), f.cropType(), one(f.areaAcres()),
                    fs.isEmpty() ? "-" : pct(avg(fs)),
                    fs.isEmpty() ? "-" : pct(fs.stream().mapToDouble(SoilMoistureReading::moistureLevel).min().orElse(0)),
                    last == null ? "-" : two(last.ndvi()),
                    last == null ? "-" : last.growthStage(),
                    last == null ? "-" : readiness(last.growthStage()) + "%",
                    String.valueOf(fieldAlerts));
        }
        if (!soil.isEmpty()) b.chart = moistureChart(fields, soil);
    }

    private void soilMoisture(Builder b, List<Field> fields, LocalDate start, LocalDate end) {
        List<SoilMoistureReading> soil = soil(fields, start, end);
        if (soil.isEmpty()) throw noData(start, end);
        SoilMoistureReading min = soil.stream().min(Comparator.comparingDouble(SoilMoistureReading::moistureLevel)).get();
        SoilMoistureReading max = soil.stream().max(Comparator.comparingDouble(SoilMoistureReading::moistureLevel)).get();
        long low = soil.stream().filter(r -> r.moistureLevel() < LOW_MOISTURE).count();
        long high = soil.stream().filter(r -> r.moistureLevel() > HIGH_MOISTURE).count();

        b.summary("Average moisture", pct(avg(soil)));
        b.summary("Lowest reading", pct(min.moistureLevel()) + " - " + store.fieldName(min.fieldId())
                + " on " + min.timestamp().format(DATE));
        b.summary("Highest reading", pct(max.moistureLevel()) + " - " + store.fieldName(max.fieldId())
                + " on " + max.timestamp().format(DATE));
        b.summary("Readings below " + pct(LOW_MOISTURE), String.valueOf(low));
        b.summary("Readings above " + pct(HIGH_MOISTURE), String.valueOf(high));
        b.summary("Readings in period", String.valueOf(soil.size()));

        List<String> cols = new ArrayList<>();
        cols.add("Date");
        for (Field f : fields) cols.add(f.name());
        b.columns(cols.toArray(String[]::new));
        for (LocalDate d : new TreeSet<>(soil.stream().map(r -> r.timestamp().toLocalDate()).toList())) {
            List<String> row = new ArrayList<>();
            row.add(d.format(DATE));
            for (Field f : fields) {
                row.add(soil.stream().filter(r -> r.fieldId() == f.fieldId() && r.timestamp().toLocalDate().equals(d))
                        .findFirst().map(r -> pct(r.moistureLevel())).orElse("-"));
            }
            b.row(row.toArray(String[]::new));
        }
        b.chart = moistureChart(fields, soil);
    }

    private void cropGrowth(Builder b, List<Field> fields, LocalDate start, LocalDate end) {
        List<CropGrowthRecord> crop = crop(fields, start, end);
        if (crop.isEmpty()) throw noData(start, end);
        List<CropGrowthRecord> latest = latestPerField(fields, crop);
        double avgNdvi = latest.stream().mapToDouble(CropGrowthRecord::ndvi).average().orElse(0);
        b.summary("Crop health (latest)", healthLabel(avgNdvi) + " (average NDVI " + two(avgNdvi) + ")");
        for (CropGrowthRecord r : latest) {
            b.summary(store.fieldName(r.fieldId()), r.growthStage() + ", NDVI " + two(r.ndvi()));
        }
        b.summary("Records in period", String.valueOf(crop.size()));

        b.columns("Date", "Field", "Crop", "Growth stage", "NDVI", "Health");
        for (CropGrowthRecord r : crop) {
            Field f = store.findField(r.fieldId());
            b.row(r.recordedAt().format(DATE), f.name(), f.cropType(), r.growthStage(), two(r.ndvi()),
                    healthLabel(r.ndvi()));
        }
        b.chart = new Chart("NDVI by field", "NDVI", false, seriesByField(fields,
                crop.stream().map(r -> new Object[] { r.fieldId(), r.recordedAt().toLocalDate(), r.ndvi() }).toList()));
    }

    private void harvestReadiness(Builder b, List<Field> fields, LocalDate start, LocalDate end) {
        List<CropGrowthRecord> latest = latestPerField(fields, crop(fields, start, end));
        if (latest.isEmpty()) throw noData(start, end);
        long ready = latest.stream().filter(r -> readiness(r.growthStage()) >= 90).count();
        b.summary("Average readiness", Math.round(latest.stream().mapToInt(r -> readiness(r.growthStage()))
                .average().orElse(0)) + "%");
        b.summary("Fields ready to harvest", ready + " of " + latest.size());

        b.columns("Field", "Crop", "Acres", "Growth stage", "Readiness", "Status", "Last scan");
        List<Point> bars = new ArrayList<>();
        for (CropGrowthRecord r : latest) {
            Field f = store.findField(r.fieldId());
            int pct = readiness(r.growthStage());
            b.row(f.name(), f.cropType(), one(f.areaAcres()), r.growthStage(), pct + "%", readinessStatus(pct),
                    r.recordedAt().format(DATE));
            bars.add(new Point(f.name(), pct));
        }
        b.chart = new Chart("Harvest readiness by field", "Readiness %", true, List.of(new Series("Readiness", bars)));
    }

    private void equipmentUsage(Builder b, List<Field> fields, LocalDate start, LocalDate end) {
        List<Equipment> items = equipment(fields);
        if (items.isEmpty()) throw new IllegalArgumentException("No equipment is recorded for the selected fields.");
        int units = items.stream().mapToInt(Equipment::getQuantity).sum();
        int missing = items.stream().filter(e -> e.getStatus().equals(Equipment.MISSING))
                .mapToInt(Equipment::getQuantity).sum();
        long equipmentAlerts = alerts(fields, start, end).stream()
                .filter(a -> a.message().toLowerCase().contains("equipment")).count();
        long scans = missions(fields, start, end).stream()
                .filter(m -> m.missionType().equals("Equipment Scan") && m.status().equals("Completed")).count();
        b.summary("Equipment units", String.valueOf(units));
        b.summary("Missing / offline", String.valueOf(missing));
        b.summary("Equipment alerts in period", String.valueOf(equipmentAlerts));
        b.summary("Drone equipment scans in period", String.valueOf(scans));

        b.columns("Equipment", "Type", "GPS tag", "Operator", "Status", "Last known field", "Last seen");
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (Equipment e : items) {
            b.row(e.getName(), e.getType(), e.getTagId(), e.getOperator().isEmpty() ? "-" : e.getOperator(),
                    e.getStatus(), store.fieldName(e.getLastKnownFieldId()), e.getLastSeen().format(DATE));
            byType.merge(e.getType(), e.getQuantity(), Integer::sum);
        }
        List<Point> bars = new ArrayList<>();
        byType.forEach((t, n) -> bars.add(new Point(t, n)));
        b.chart = new Chart("Equipment units by type", "Units", true, List.of(new Series("Units", bars)));
    }

    private void droneMissions(Builder b, List<Field> fields, LocalDate start, LocalDate end) {
        List<Mission> ms = missions(fields, start, end);
        if (ms.isEmpty()) throw noData(start, end);
        Map<String, Integer> byType = new LinkedHashMap<>();
        for (Mission m : ms) byType.merge(m.missionType(), 1, Integer::sum);
        b.summary("Missions", String.valueOf(ms.size()));
        b.summary("Completed", String.valueOf(ms.stream().filter(m -> m.status().equals("Completed")).count()));
        b.summary("Postponed", String.valueOf(ms.stream().filter(m -> m.status().startsWith("Postponed")).count()));
        b.summary("Scheduled", String.valueOf(ms.stream().filter(m -> m.status().equals("Scheduled")).count()));

        b.columns("Date", "Time", "Mission type", "Fields", "Drone", "Status");
        for (Mission m : ms) {
            b.row(m.scheduledTime().format(DATE), m.scheduledTime().format(TIME), m.missionType(),
                    store.fieldName(m.fieldId()), m.droneId(), m.status());
        }
        List<Point> bars = new ArrayList<>();
        byType.forEach((t, n) -> bars.add(new Point(t, n)));
        b.chart = new Chart("Missions by type", "Missions", true, List.of(new Series("Missions", bars)));
    }

    // ------------------------------------------------------------ data selection

    private List<Field> selectedFields(String choice) {
        if (choice.equals(ALL_FIELDS)) return List.copyOf(store.fields());
        for (Field f : store.fields()) {
            if (f.name().equals(choice)) return List.of(f);
        }
        throw new IllegalArgumentException("Unknown field: " + choice);
    }

    private static boolean inPeriod(LocalDateTime t, LocalDate start, LocalDate end) {
        LocalDate d = t.toLocalDate();
        return !d.isBefore(start) && !d.isAfter(end);
    }

    private static boolean covers(List<Field> fields, int fieldId) {
        return fields.stream().anyMatch(f -> f.fieldId() == fieldId);
    }

    private List<SoilMoistureReading> soil(List<Field> fields, LocalDate start, LocalDate end) {
        return store.soilReadings().stream()
                .filter(r -> covers(fields, r.fieldId()) && inPeriod(r.timestamp(), start, end)).toList();
    }

    private List<CropGrowthRecord> crop(List<Field> fields, LocalDate start, LocalDate end) {
        return store.cropRecords().stream()
                .filter(r -> covers(fields, r.fieldId()) && inPeriod(r.recordedAt(), start, end)).toList();
    }

    private List<Alert> alerts(List<Field> fields, LocalDate start, LocalDate end) {
        return store.alerts().stream()
                .filter(a -> covers(fields, a.fieldId()) && inPeriod(a.raisedAt(), start, end)).toList();
    }

    private List<Mission> missions(List<Field> fields, LocalDate start, LocalDate end) {
        return store.missions().stream()
                .filter(m -> (m.fieldId() == 0 || covers(fields, m.fieldId())) && inPeriod(m.scheduledTime(), start, end))
                .sorted(Comparator.comparing(Mission::scheduledTime)).toList();
    }

    private List<Equipment> equipment(List<Field> fields) {
        return store.equipment().stream().filter(e -> covers(fields, e.getLastKnownFieldId())).toList();
    }

    /** The most recent crop record of each field, in field order. */
    private static List<CropGrowthRecord> latestPerField(List<Field> fields, List<CropGrowthRecord> records) {
        List<CropGrowthRecord> out = new ArrayList<>();
        for (Field f : fields) {
            records.stream().filter(r -> r.fieldId() == f.fieldId())
                    .max(Comparator.comparing(CropGrowthRecord::recordedAt)).ifPresent(out::add);
        }
        return out;
    }

    // ------------------------------------------------------------ calculations

    /** Harvest readiness % estimated from the crop's growth stage. */
    public static int readiness(String growthStage) {
        return switch (growthStage) {
            case "Germination" -> 5;
            case "Tillering" -> 15;
            case "Reproductive" -> 30;
            case "Pod Fill" -> 40;
            case "Fruit Development" -> 45;
            case "Grain Fill" -> 65;
            case "Seed Fill" -> 70;
            case "Ripening" -> 75;
            case "Mature" -> 92;
            case "Harvest Ready" -> 95;
            default -> 0;
        };
    }

    public static String readinessStatus(int pct) {
        if (pct >= 90) return "Ready";
        if (pct >= 70) return "Nearly ready";
        return "Not ready";
    }

    public static String healthLabel(double ndvi) {
        if (ndvi >= 0.6) return "Good";
        if (ndvi >= 0.4) return "Fair";
        return "Poor";
    }

    private Chart moistureChart(List<Field> fields, List<SoilMoistureReading> soil) {
        return new Chart("Soil moisture by field", "Moisture %", false, seriesByField(fields,
                soil.stream().map(r -> new Object[] { r.fieldId(), r.timestamp().toLocalDate(), r.moistureLevel() }).toList()));
    }

    /** values: {fieldId, LocalDate, Double}; one series per field, x = MM/dd. */
    private static List<Series> seriesByField(List<Field> fields, List<Object[]> values) {
        List<Series> out = new ArrayList<>();
        for (Field f : fields) {
            List<Point> points = new ArrayList<>();
            for (Object[] v : values) {
                if ((int) v[0] == f.fieldId()) points.add(new Point(((LocalDate) v[1]).format(DAY), (double) v[2]));
            }
            if (!points.isEmpty()) out.add(new Series(f.name(), points));
        }
        return out;
    }

    private static double avg(List<SoilMoistureReading> readings) {
        return readings.stream().mapToDouble(SoilMoistureReading::moistureLevel).average().orElse(0);
    }

    private static IllegalArgumentException noData(LocalDate start, LocalDate end) {
        return new IllegalArgumentException("No data exists for the selected fields between "
                + start.format(DATE) + " and " + end.format(DATE) + ".");
    }

    static String pct(double v) { return String.format(Locale.US, "%.1f%%", v); }
    static String one(double v) { return String.format(Locale.US, "%.1f", v); }
    static String two(double v) { return String.format(Locale.US, "%.2f", v); }

    /** Collects the parts of a report while it is being compiled. */
    private static final class Builder {
        final List<String[]> summary = new ArrayList<>();
        String[] columns = new String[0];
        final List<String[]> rows = new ArrayList<>();
        Chart chart;

        void summary(String label, String value) { summary.add(new String[] { label, value }); }
        void columns(String... names) { columns = names; }
        void row(String... cells) { rows.add(cells); }
    }
}
