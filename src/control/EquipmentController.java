package control;

import boundary.EquipmentGPSTagInterface;
import entity.Alert;
import entity.DataStore;
import entity.Equipment;
import entity.Field;
import entity.FieldBoundary;
import entity.Mission;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Control for UC-07 Track Equipment Location and Inventory: inventory counts,
 * search and filters, adding tagged equipment, geofence checks, and sending a
 * drone to find equipment whose tag has gone silent.
 */
public class EquipmentController {

    public static final List<String> TYPES = List.of("Tractor", "Sprayer", "Irrigation Unit", "Harvester", "Trailer", "Other");
    public static final List<String> STATUSES = List.of(Equipment.ACTIVE, Equipment.IDLE, Equipment.MISSING);
    public static final String ALL = "All";
    public static final String ALL_FIELDS = "All Fields";
    public static final String NO_GEOFENCE = "No geofence";

    /** A tag that has not reported for this long is treated as missing (UC-07 alternate flow 5a). */
    public static final Duration SILENT_LIMIT = Duration.ofHours(24);

    private static final Pattern TAG = Pattern.compile("^[A-Za-z0-9-]{4,20}$");

    private final DataStore store;
    private final EquipmentGPSTagInterface tags;

    public EquipmentController(DataStore store, EquipmentGPSTagInterface tags) {
        this.store = store;
        this.tags = tags;
    }

    public List<Equipment> all() {
        return List.copyOf(store.equipment());
    }

    public List<Field> fields() {
        return List.copyOf(store.fields());
    }

    public List<FieldBoundary> boundaries() {
        return List.copyOf(store.boundaries());
    }

    public String fieldName(int fieldId) {
        return fieldId == 0 ? "Farmyard" : store.fieldName(fieldId);
    }

    /** Search by name, tag or operator, and filter by type, status and field (UC-07 step 4). */
    public List<Equipment> search(String text, String type, String status, String field) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<Equipment> out = new ArrayList<>();
        for (Equipment e : store.equipment()) {
            if (!q.isEmpty() && !(e.getName().toLowerCase(Locale.ROOT).contains(q)
                    || e.getTagId().toLowerCase(Locale.ROOT).contains(q)
                    || e.getOperator().toLowerCase(Locale.ROOT).contains(q))) continue;
            if (type != null && !type.equals(ALL) && !e.getType().equals(type)) continue;
            if (status != null && !status.equals(ALL) && !e.getStatus().equals(status)) continue;
            if (field != null && !field.equals(ALL_FIELDS) && !fieldName(e.getLastKnownFieldId()).equals(field)) continue;
            out.add(e);
        }
        return out;
    }

    /** Units per type, in TYPES order; types with no units are left out. */
    public Map<String, Integer> countsByType() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String t : TYPES) {
            int n = 0;
            for (Equipment e : store.equipment()) {
                if (e.getType().equals(t)) n += e.getQuantity();
            }
            if (n > 0) counts.put(t, n);
        }
        return counts;
    }

    public int missingCount() {
        return (int) store.equipment().stream().filter(e -> e.getStatus().equals(Equipment.MISSING)).count();
    }

    /** Marks equipment whose tag has not reported within SILENT_LIMIT as Missing. Returns how many changed. */
    public int checkSilentTags(LocalDateTime now) {
        int changed = 0;
        for (Equipment e : store.equipment()) {
            if (!e.getStatus().equals(Equipment.MISSING) && e.getLastSeen().isBefore(now.minus(SILENT_LIMIT))) {
                e.setStatus(Equipment.MISSING);
                changed++;
            }
        }
        return changed;
    }

    public boolean isOutsideGeofence(Equipment e) {
        return e.getGeofenceFieldId() != 0 && e.getLastKnownFieldId() != e.getGeofenceFieldId();
    }

    /**
     * Raises a geofence alert (UC-08 extends UC-07, alternate flow 3a) for each
     * item outside its geofence that has no active alert yet. Returns the new alerts.
     */
    public List<Alert> checkGeofences(LocalDateTime now) {
        List<Alert> raised = new ArrayList<>();
        for (Equipment e : store.equipment()) {
            if (!isOutsideGeofence(e)) continue;
            String message = "Equipment outside geofence - " + e.getName();
            boolean exists = store.alerts().stream()
                    .anyMatch(a -> a.message().equals(message) && a.status().equals("Active"));
            if (!exists) {
                Alert a = new Alert(store.alerts().size() + 1, e.getLastKnownFieldId(), "High", message, "Active", now);
                store.alerts().add(a);
                raised.add(a);
            }
        }
        return raised;
    }

    /**
     * Add Equipment (UC-07 step 6): registers a new item and its tag, placed at
     * the centre of the chosen field. Throws IllegalArgumentException on bad input.
     */
    public Equipment addEquipment(String name, String type, String description, String tagId,
                                  Field field, String operator, Field geofence) {
        String n = name == null ? "" : name.trim();
        String t = tagId == null ? "" : tagId.trim().toUpperCase(Locale.ROOT);
        if (n.isEmpty()) throw new IllegalArgumentException("Enter the equipment name.");
        if (type == null || !TYPES.contains(type)) throw new IllegalArgumentException("Select the equipment type.");
        if (t.isEmpty()) throw new IllegalArgumentException("Enter the GPS tag ID.");
        if (!TAG.matcher(t).matches()) {
            throw new IllegalArgumentException("The tag ID must be 4-20 letters, digits or dashes (e.g. TAG-1015).");
        }
        if (field == null) throw new IllegalArgumentException("Select the field where the equipment is now.");
        for (Equipment e : store.equipment()) {
            if (e.getName().equalsIgnoreCase(n)) throw new IllegalArgumentException("Equipment named " + n + " already exists.");
            if (e.getTagId().equalsIgnoreCase(t)) {
                throw new IllegalArgumentException("Tag " + t + " is already registered to " + e.getName() + ".");
            }
        }

        int id = store.equipment().stream().mapToInt(Equipment::getEquipmentId).max().orElse(0) + 1;
        Equipment e = new Equipment(id, n, type, description == null ? "" : description.trim(), t,
                Equipment.IDLE, operator == null ? "" : operator.trim(), geofence == null ? 0 : geofence.fieldId());
        double[] spot = freeSpot(store.boundaryOf(field.fieldId()));
        e.recordLocation(LocalDateTime.now().withNano(0), spot[0], spot[1], field.fieldId(), "Registered");
        store.equipment().add(e);
        tags.registerTag(t, n);
        return e;
    }

    /** A point inside the field, as far as possible from the equipment already there (checked on a grid). */
    private double[] freeSpot(FieldBoundary b) {
        if (b == null) return new double[] { 500, 300 };
        double[] best = { b.centerX(), b.centerY() };
        double bestDistance = -1;
        for (double y = b.y() + 40; y <= b.y() + b.height() - 20; y += 20) {
            for (double x = b.x() + 30; x <= b.x() + b.width() - 70; x += 20) {
                double nearest = Double.MAX_VALUE;
                for (Equipment e : store.equipment()) {
                    // map labels sit to the right of each marker, so horizontal space counts half
                    nearest = Math.min(nearest, Math.hypot((e.getX() - x) / 2.2, e.getY() - y));
                }
                // prefer spots near the centre when they are equally free (capped distance)
                double score = Math.min(nearest, 90) - Math.hypot(x - b.centerX(), y - b.centerY()) / 100;
                if (score > bestDistance) {
                    bestDistance = score;
                    best = new double[] { x, y };
                }
            }
        }
        return best;
    }

    /**
     * Locate with Drone (UC-07 alternate flow 5a): only for Missing equipment.
     * Creates an equipment-scan mission over the last known field.
     */
    public Mission startDroneSearch(Equipment e) {
        if (!e.getStatus().equals(Equipment.MISSING)) {
            throw new IllegalArgumentException(e.getName() + " is reporting normally, so no drone search is needed.");
        }
        int id = store.missions().stream().mapToInt(Mission::missionId).max().orElse(0) + 1;
        Mission m = new Mission(id, LocalDateTime.now().withNano(0), "In Progress", id, "Equipment Scan",
                e.getLastKnownFieldId(), "D1");
        store.missions().add(m);
        return m;
    }

    /** The drone read the tag over the last known area: record the sighting and close the mission. */
    public void droneFound(Equipment e, Mission m) {
        e.recordLocation(LocalDateTime.now().withNano(0), e.getX(), e.getY(), e.getLastKnownFieldId(), "Drone scan");
        e.setStatus(Equipment.IDLE);
        int i = store.missions().indexOf(m);
        Mission done = new Mission(m.missionId(), m.scheduledTime(), "Completed", m.flightPathId(),
                m.missionType(), m.fieldId(), m.droneId());
        if (i >= 0) store.missions().set(i, done);
        else store.missions().add(done);
    }

    /** "12 min ago", "3 h ago", "2 days ago". */
    public static String ago(LocalDateTime time, LocalDateTime now) {
        long minutes = Math.max(0, Duration.between(time, now).toMinutes());
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + " h ago";
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
