package control;

import entity.DataStore;
import entity.Equipment;
import entity.Field;
import entity.FieldBoundary;
import entity.FlightPlan;
import entity.Mission;
import entity.MissionSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Business rules for planning and scheduling missions. Weather and drones are simulated. */
public final class MissionController {
    public static final List<String> MISSION_TYPES = List.of(
            "Soil Moisture Scan", "Crop Growth Scan", "Equipment Scan");
    public static final List<String> DRONE_IDS = List.of("D1", "D2", "D3");
    public static final double MAX_BATTERY_USE = 80.0;
    private static final int TURNAROUND_MINUTES = 15;
    private final DataStore store;

    public record DroneInfo(String id, String model, String status, double batteryPercent) {
        @Override public String toString() {
            return id + " · " + model + " (" + status + ", " + Math.round(batteryPercent) + "%)";
        }
    }

    public record WeatherInfo(double windMph, double gustMph, double rainPercent,
                              double visibilityMiles, double temperatureF,
                              LocalTime safeUntil, boolean safe, String message) { }

    public MissionController(DataStore store) { this.store = store; }

    public List<Field> getAvailableFields() { return List.copyOf(store.fields()); }

    /** Demo fleet. Replace with a Drone entity/service when real drone data is integrated. */
    public List<DroneInfo> getAvailableDrones() {
        return List.of(new DroneInfo("D1", "AgriQuad X8", "Ready", 100),
                new DroneInfo("D2", "AgriQuad X8", "Ready", 100),
                new DroneInfo("D3", "AgriQuad X8", "Ready", 92));
    }

    public FlightPlan generateFlightPath(List<Field> selectedFields, String missionType,
                                        DroneInfo drone) {
        if (selectedFields == null || selectedFields.isEmpty())
            throw new IllegalArgumentException("Select at least one mapped field.");
        if (!MISSION_TYPES.contains(missionType))
            throw new IllegalArgumentException("Select a valid mission type.");
        if (drone == null || !"Ready".equals(drone.status()))
            throw new IllegalArgumentException("Select a ready drone.");

        List<Integer> ids = selectedFields.stream().map(Field::fieldId).toList();
        List<FlightPlan.RoutePoint> route = new ArrayList<>();
        // Demo lawn-mower route: parallel horizontal passes through each selected boundary.
        // Coordinates deliberately reuse the project's FieldBoundary coordinate system.
        for (Field field : selectedFields) {
            FieldBoundary b = store.boundaryOf(field.fieldId());
            if (b == null) throw new IllegalArgumentException(field.name() + " has not been mapped.");
            int passes = Math.max(2, Math.min(8, (int) Math.ceil(b.height() / 55.0)));
            double left = b.x() + 12, right = b.x() + b.width() - 12;
            for (int i = 0; i < passes; i++) {
                double y = b.y() + 12 + (b.height() - 24) * i / Math.max(1, passes - 1);
                if (i % 2 == 0) {
                    route.add(new FlightPlan.RoutePoint(left, y));
                    route.add(new FlightPlan.RoutePoint(right, y));
                } else {
                    route.add(new FlightPlan.RoutePoint(right, y));
                    route.add(new FlightPlan.RoutePoint(left, y));
                }
            }
        }
        double acres = selectedFields.stream().mapToDouble(Field::areaAcres).sum();
        int waypoints = route.size() + 2; // base departure and return
        int minutes = Math.max(12, (int) Math.ceil(acres * 0.34));
        double battery = Math.min(78.0, Math.max(25.0, minutes * 2.25));
        if (battery > MAX_BATTERY_USE)
            throw new IllegalArgumentException("This route exceeds the 80% battery-use limit. Select fewer fields or another drone.");
        List<FlightPlan.RoutePoint> withBase = new ArrayList<>();
        withBase.add(new FlightPlan.RoutePoint(70, 540));
        withBase.addAll(route);
        withBase.add(new FlightPlan.RoutePoint(70, 540));
        return new FlightPlan(store.nextFlightPathId(), "Flight Plan FP-" + String.format("%04d", store.flightPlans().size()+1),
                missionType, ids, drone.id(), withBase, minutes, battery, acres, "Generated", LocalDateTime.now());
    }

    public WeatherInfo checkWeather(LocalDate date, LocalTime start, int flightMinutes) {
        // Simulated forecast values. This deliberately mirrors the safe values in UC-02.
        double wind = 8, gusts = 12, rain = 0, visibility = 10, temp = 64;
        LocalTime twilightEnd = LocalTime.of(18, 44); // sample scenario value; replace with location-based service
        boolean inDaylight = start.isAfter(LocalTime.of(6, 0)) && !start.plusMinutes(flightMinutes).isAfter(twilightEnd);
        boolean safe = wind <= 20 && gusts <= 25 && rain <= 30 && visibility >= 3
                && temp >= 32 && temp <= 104 && inDaylight;
        String msg = safe ? "Safe to fly until " + twilightEnd : "Unsafe: weather or daylight limits are not met.";
        return new WeatherInfo(wind, gusts, rain, visibility, temp, twilightEnd, safe, msg);
    }

    public FlightPlan saveFlightPlan(FlightPlan generated, String name) {
        if (generated == null) throw new IllegalArgumentException("Generate a flight path first.");
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Enter a name for the flight plan.");
        FlightPlan saved = new FlightPlan(generated.flightPathId(), clean, generated.missionType(),
                generated.fieldIds(), generated.droneId(), generated.waypoints(), generated.estimatedMinutes(),
                generated.batteryUsagePercent(), generated.coverageAcres(), "Saved", generated.createdAt());
        store.flightPlans().removeIf(p -> p.flightPathId() == saved.flightPathId());
        store.flightPlans().add(saved);
        return saved;
    }

    public MissionSchedule scheduleMission(FlightPlan plan, LocalDate date, LocalTime time,
                                           String repeat, Set<DayOfWeek> days, String notifications) {
        if (plan == null || !"Saved".equals(plan.status()))
            throw new IllegalArgumentException("Save a flight plan before scheduling it.");
        if (date == null || time == null) throw new IllegalArgumentException("Choose a mission date and time.");
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("The mission date cannot be in the past.");
        if (repeat == null) repeat = "Does not repeat";
        if ("Weekly".equals(repeat) && (days == null || days.isEmpty()))
            throw new IllegalArgumentException("Choose at least one weekday for a weekly mission.");
        if (notifications == null || notifications.isBlank()) notifications = "No notifications";

        double required = plan.batteryUsagePercent() + 20.0;
        DroneInfo drone = getAvailableDrones().stream().filter(d -> d.id().equals(plan.droneId())).findFirst().orElse(null);
        if (drone == null || drone.batteryPercent() < required)
            throw new IllegalArgumentException("Drone " + plan.droneId() + " needs at least " + Math.round(required) + "% battery.");

        List<LocalDate> occurrences = upcomingDates(date, repeat, days, 4);
        for (LocalDate occurrence : occurrences) {
            LocalDateTime start = occurrence.atTime(time);
            LocalDateTime end = start.plusMinutes(plan.estimatedMinutes() + TURNAROUND_MINUTES);
            for (Mission existing : store.missions()) {
                if (!existing.droneId().equals(plan.droneId()) || !"Scheduled".equalsIgnoreCase(existing.status())) continue;
                LocalDateTime existingStart = existing.scheduledTime();
                // Existing missions have no duration in the legacy model; reserve a conservative 49 minutes.
                LocalDateTime existingEnd = existingStart.plusMinutes(49);
                if (start.isBefore(existingEnd) && end.isAfter(existingStart))
                    throw new IllegalArgumentException("Drone " + plan.droneId() + " is already booked on " + occurrence + " around " + time + ".");
            }
            WeatherInfo weather = checkWeather(occurrence, time, plan.estimatedMinutes());
            if (!weather.safe()) throw new IllegalArgumentException("Weather/daylight check failed for " + occurrence + ": " + weather.message());
        }

        MissionSchedule schedule = new MissionSchedule(store.nextScheduleId(), plan.flightPathId(), date, time,
                repeat, days == null ? Set.of() : days, notifications, "Scheduled");
        store.missionSchedules().add(schedule);
        for (LocalDate occurrence : occurrences) {
            store.missions().add(new Mission(store.nextMissionId(), occurrence.atTime(time), "Scheduled",
                    plan.flightPathId(), plan.missionType(), plan.fieldIds().size() == 1 ? plan.fieldIds().get(0) : 0,
                    plan.droneId()));
        }
        return schedule;
    }

    private List<LocalDate> upcomingDates(LocalDate first, String repeat, Set<DayOfWeek> days, int count) {
        List<LocalDate> dates = new ArrayList<>();
        if ("Does not repeat".equals(repeat)) return List.of(first);
        LocalDate cursor = first;
        int guard = 0;
        while (dates.size() < count && guard++ < 370) {
            boolean match = switch (repeat) {
                case "Daily" -> true;
                case "Weekly" -> days.contains(cursor.getDayOfWeek());
                case "Monthly" -> cursor.getDayOfMonth() == first.getDayOfMonth();
                default -> false;
            };
            if (match && !cursor.isBefore(first)) dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    public List<Mission> upcomingMissions() {
        return store.missions().stream().filter(m -> "Scheduled".equalsIgnoreCase(m.status()))
                .sorted(Comparator.comparing(Mission::scheduledTime)).toList();
    }

    public String fieldsLabel(FlightPlan plan) {
        return plan.fieldIds().stream().map(store::fieldName).reduce((a,b) -> a + ", " + b).orElse("No fields");
    }
}
