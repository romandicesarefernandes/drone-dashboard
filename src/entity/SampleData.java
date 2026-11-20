package entity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Demo data from the team's scenario documents ("Common scenario data"):
 * Green Valley Farms, its four fields, Alex Morgan (System Administrator),
 * Dana Whitfield (Farm Manager), and drone data for September to mid-November 2026.
 * A fixed random seed keeps the numbers the same on every run.
 */
public final class SampleData {

    public static final LocalDate DATA_START = LocalDate.of(2026, 9, 1);
    public static final LocalDate DATA_END = LocalDate.of(2026, 11, 14);

    private SampleData() { }

    public static void load(DataStore s) {
        s.setFarm(new Farm(1, "Green Valley Farms", "Story County, Iowa", 162.3));
        s.fields().add(new Field(1, 1, "North 40", "Corn", 40.2));
        s.fields().add(new Field(2, 1, "East Orchard", "Apples", 22.5));
        s.fields().add(new Field(3, 1, "South Field", "Wheat", 61.0));
        s.fields().add(new Field(4, 1, "River Plot", "Soybean", 38.6));

        loadUsers(s);
        loadSoilMoisture(s);
        loadCropGrowth(s);
        loadEquipment(s);
        loadAlerts(s);
        loadMissions(s);
    }

    private static void loadUsers(DataStore s) {
        List<Integer> all = List.of(1, 2, 3, 4);
        LocalDateTime created = LocalDateTime.of(2026, 8, 3, 9, 0);
        addUser(s, "Alex Morgan", "alex.morgan@greenvalley.example", "555-0100",
                Role.SYSTEM_ADMINISTRATOR, AccountStatus.ACTIVE, all, LocalDateTime.of(2026, 11, 18, 8, 12), created);
        addUser(s, "Dana Whitfield", "dana.whitfield@greenvalley.example", "555-0101",
                Role.FARM_MANAGER, AccountStatus.ACTIVE, all, LocalDateTime.of(2026, 11, 18, 7, 45), created);
        addUser(s, "John Smith", "john.smith@greenvalley.example", "555-0123",
                Role.FARM_MANAGER, AccountStatus.ACTIVE, List.of(1, 2), LocalDateTime.of(2026, 11, 16, 16, 30), created);
        addUser(s, "Sarah Jones", "sarah.jones@greenvalley.example", "555-0145",
                Role.FARM_MANAGER, AccountStatus.ACTIVE, List.of(3, 4), LocalDateTime.of(2026, 11, 17, 10, 5), created);
        addUser(s, "Mike Davis", "mike.davis@greenvalley.example", "555-0167",
                Role.SYSTEM_ADMINISTRATOR, AccountStatus.ACTIVE, all, LocalDateTime.of(2026, 11, 12, 13, 20), created);
        addUser(s, "Tom Wilson", "tom.wilson@greenvalley.example", "555-0189",
                Role.FARM_MANAGER, AccountStatus.INACTIVE, List.of(1), LocalDateTime.of(2026, 9, 30, 17, 0), created);
    }

    private static void addUser(DataStore s, String name, String email, String phone, Role role,
                                AccountStatus status, List<Integer> fields, LocalDateTime lastLogin,
                                LocalDateTime created) {
        s.users().add(new User(s.nextUserId(), name, email, phone, role, status, fields, lastLogin, created));
    }

    /** A soil-moisture scan of every field every 2 days. South Field dries out around 22 October. */
    private static void loadSoilMoisture(DataStore s) {
        Random rnd = new Random(42);
        double[] base = { 0, 49, 56, 46, 53 };
        int id = 0;
        for (LocalDate d = DATA_START; !d.isAfter(DATA_END); d = d.plusDays(2)) {
            long day = d.toEpochDay() - DATA_START.toEpochDay();
            for (int field = 1; field <= 4; field++) {
                double value = base[field] + 6 * Math.sin(day / 9.0 + field) + (rnd.nextDouble() * 6 - 3);
                if (field == 3) {   // dry spell in South Field, worst on 22 Oct (scenario UC-08)
                    long fromWorst = Math.abs(d.toEpochDay() - LocalDate.of(2026, 10, 22).toEpochDay());
                    if (fromWorst <= 8) value -= 22 * (1 - fromWorst / 9.0);
                }
                value = Math.max(5, Math.min(95, value));
                s.soilReadings().add(new SoilMoistureReading(++id, field, round1(value), d.atTime(8, 0)));
            }
        }
    }

    /** A crop-growth scan of every field each Wednesday. East Orchard shows stress on 14 October. */
    private static void loadCropGrowth(DataStore s) {
        Random rnd = new Random(7);
        String[][] stages = {
            {},
            { "Reproductive", "Reproductive", "Reproductive", "Grain Fill", "Grain Fill", "Grain Fill", "Grain Fill", "Mature" },
            { "Fruit Development", "Fruit Development", "Ripening", "Ripening", "Ripening", "Ripening", "Harvest Ready", "Harvest Ready" },
            { "Germination", "Germination", "Tillering", "Tillering", "Tillering", "Tillering", "Tillering", "Tillering" },
            { "Pod Fill", "Pod Fill", "Seed Fill", "Seed Fill", "Seed Fill", "Mature", "Mature", "Mature" },
        };
        double[][] ndvi = { {}, { 0.84, 0.55 }, { 0.79, 0.64 }, { 0.28, 0.63 }, { 0.81, 0.50 } };   // {start, end}
        LocalDate first = DATA_START.with(DayOfWeek.WEDNESDAY).isBefore(DATA_START)
                ? DATA_START.with(DayOfWeek.WEDNESDAY).plusWeeks(1) : DATA_START.with(DayOfWeek.WEDNESDAY);
        int weeks = (int) ((DATA_END.toEpochDay() - first.toEpochDay()) / 7);
        int id = 0;
        for (int w = 0; w <= weeks; w++) {
            LocalDate d = first.plusWeeks(w);
            double t = w / (double) weeks;
            int stageIndex = Math.min(7, w * 8 / (weeks + 1));
            for (int field = 1; field <= 4; field++) {
                double v = ndvi[field][0] + (ndvi[field][1] - ndvi[field][0]) * t + (rnd.nextDouble() * 0.04 - 0.02);
                if (field == 2 && d.equals(LocalDate.of(2026, 10, 14))) v -= 0.14;   // crop stress
                s.cropRecords().add(new CropGrowthRecord(++id, field, stages[field][stageIndex],
                        Math.round(v * 100) / 100.0, d.atTime(11, 0)));
            }
        }
    }

    private static void loadAlerts(DataStore s) {
        s.alerts().add(new Alert(1, 1, "Medium", "Low soil moisture - North 40", "Acknowledged",
                LocalDateTime.of(2026, 9, 18, 10, 5)));
        s.alerts().add(new Alert(2, 2, "Medium", "Crop stress - East Orchard below expected growth", "Acknowledged",
                LocalDateTime.of(2026, 10, 14, 14, 20)));
        s.alerts().add(new Alert(3, 4, "High", "Equipment outside geofence - Tractor 2", "Acknowledged",
                LocalDateTime.of(2026, 10, 18, 16, 45)));
        s.alerts().add(new Alert(4, 3, "Critical", "Low soil moisture - South Field", "Acknowledged",
                LocalDateTime.of(2026, 10, 22, 9, 40)));
        s.alerts().add(new Alert(5, 2, "High", "Equipment not reporting - Trailer 1", "Active",
                LocalDateTime.of(2026, 11, 12, 8, 30)));
    }

    /** Drone base pad: farmyard at the south-west corner of South Field (scenario data). */
    public static final double BASE_X = 70, BASE_Y = 540;

    /**
     * Field positions on the farm map (1000 x 600) and 14 tagged items matching
     * the wireframe: 5 tractors, 3 sprayers, 4 irrigation units, 1 harvester and
     * 1 trailer. Tractor 2 is outside its geofence; Trailer 1 stopped reporting
     * three days ago (Missing). Last-seen times are relative to app start.
     */
    private static void loadEquipment(DataStore s) {
        s.boundaries().add(new FieldBoundary(1, 40, 40, 420, 240));    // North 40
        s.boundaries().add(new FieldBoundary(2, 500, 40, 260, 240));   // East Orchard
        s.boundaries().add(new FieldBoundary(4, 800, 40, 160, 520));   // River Plot
        s.boundaries().add(new FieldBoundary(3, 40, 320, 720, 240));   // South Field

        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Object[][] items = {
            // name, type, description, operator, geofence field, x, y, status, minutes since last report
            { "Tractor 1", "Tractor", "Row-crop tractor, 310 hp", "John Smith", 1, 150, 120, Equipment.ACTIVE, 4 },
            { "Tractor 2", "Tractor", "Row-crop tractor, 310 hp", "Sarah Jones", 3, 880, 300, Equipment.ACTIVE, 7 },
            { "Tractor 3", "Tractor", "Utility tractor, 120 hp", "", 3, 300, 430, Equipment.IDLE, 35 },
            { "Tractor 4", "Tractor", "Row-crop tractor, 250 hp", "John Smith", 1, 340, 210, Equipment.ACTIVE, 2 },
            { "Tractor 5", "Tractor", "Utility tractor, 120 hp", "", 3, 120, 500, Equipment.IDLE, 50 },
            { "Sprayer 1", "Sprayer", "Self-propelled sprayer, 120 ft boom", "Sarah Jones", 3, 520, 390, Equipment.ACTIVE, 6 },
            { "Sprayer 2", "Sprayer", "Orchard air-blast sprayer", "John Smith", 2, 610, 150, Equipment.ACTIVE, 9 },
            { "Sprayer 3", "Sprayer", "Pull-type sprayer", "", 3, 170, 470, Equipment.IDLE, 120 },
            { "Irrigation Unit 1", "Irrigation Unit", "Center-pivot, 1/4 mile", "", 3, 220, 370, Equipment.ACTIVE, 3 },
            { "Irrigation Unit 2", "Irrigation Unit", "Center-pivot, 1/4 mile", "", 3, 420, 370, Equipment.ACTIVE, 3 },
            { "Irrigation Unit 3", "Irrigation Unit", "Linear-move irrigator", "", 3, 640, 500, Equipment.ACTIVE, 5 },
            { "Irrigation Unit 4", "Irrigation Unit", "Drip irrigation controller", "", 1, 250, 250, Equipment.ACTIVE, 8 },
            { "Harvester 1", "Harvester", "Combine harvester, 40 ft header", "Sarah Jones", 4, 880, 460, Equipment.ACTIVE, 11 },
            { "Trailer 1", "Trailer", "Grain trailer, 1,000 bu", "", 2, 700, 240, Equipment.MISSING, 3 * 24 * 60 + 40 },
        };
        int id = 0;
        for (Object[] it : items) {
            id++;
            Equipment e = new Equipment(id, (String) it[0], (String) it[1], (String) it[2],
                    String.format("TAG-%04d", 1000 + id), (String) it[7], (String) it[3], (int) it[4]);
            double x = (int) it[5], y = (int) it[6];
            LocalDateTime last = now.minusMinutes((int) it[8]);
            // Two older GPS reports a few hours apart, then the latest one.
            e.recordLocation(last.minusHours(26), x - 30, y + 12, s.fieldAt(x - 30, y + 12), "GPS tag");
            e.recordLocation(last.minusHours(5), x - 12, y - 8, s.fieldAt(x - 12, y - 8), "Drone scan");
            e.recordLocation(last, x, y, s.fieldAt(x, y), "GPS tag");
            s.equipment().add(e);
        }
    }

    /** One soil-moisture mission per scan day, one crop-growth mission per week, an equipment scan each Friday. */
    private static void loadMissions(DataStore s) {
        String[] drones = { "D1", "D2", "D3" };
        int id = 0;
        for (LocalDate d = DATA_START; !d.isAfter(DATA_END); d = d.plusDays(1)) {
            long day = d.toEpochDay() - DATA_START.toEpochDay();
            if (day % 2 == 0) {
                String status = d.equals(LocalDate.of(2026, 10, 9)) ? "Postponed (weather)" : "Completed";
                s.missions().add(new Mission(++id, d.atTime(7, 30), status, id, "Soil Moisture", 0, drones[id % 3]));
            }
            if (d.getDayOfWeek() == DayOfWeek.WEDNESDAY) {
                s.missions().add(new Mission(++id, d.atTime(10, 30), "Completed", id, "Crop Growth", 0, drones[id % 3]));
            }
            if (d.getDayOfWeek() == DayOfWeek.FRIDAY) {
                s.missions().add(new Mission(++id, d.atTime(15, 0), "Completed", id, "Equipment Scan", 0, drones[id % 3]));
            }
        }
        // Upcoming missions (today is after DATA_END)
        s.missions().add(new Mission(++id, LocalDateTime.of(2026, 11, 20, 7, 30), "Scheduled", id, "Soil Moisture", 0, "D1"));
        s.missions().add(new Mission(++id, LocalDateTime.of(2026, 11, 25, 10, 30), "Scheduled", id, "Crop Growth", 0, "D2"));
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
