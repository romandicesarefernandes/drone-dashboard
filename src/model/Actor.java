package model;

/** The 5 locked actors (CLAUDE.md, "Actors"). Names must match the requirements docs exactly. */
public enum Actor {
    FARM_MANAGER("Farm Manager", "Primary (human)"),
    SYSTEM_ADMINISTRATOR("System Administrator", "Primary (human)"),
    DRONE("Drone", "Secondary (system)"),
    WEATHER_SERVICE("Weather Service", "Secondary (external system)"),
    EQUIPMENT_GPS_TAG("Equipment GPS Tag", "Secondary (device)");

    private final String displayName;
    private final String type;

    Actor(String displayName, String type) {
        this.displayName = displayName;
        this.type = type;
    }

    public String getDisplayName() { return displayName; }
    public String getType() { return type; }

    /** Only primary (human) actors can log in to the dashboard. */
    public boolean isPrimary() { return type.startsWith("Primary"); }

    @Override
    public String toString() { return displayName; }
}
