package model;

import static model.Actor.*;

import java.util.ArrayList;
import java.util.List;

/**
 * The 10 locked use cases (CLAUDE.md, "Use cases (exactly 10)").
 * Single source of truth inside the code: screens read their titles, actors
 * and relationships from here. Do not add use cases without scope approval.
 */
public enum UseCase {
    UC_01("UC-01", "Register Farm and Map Fields", Screen.S2, "-",
            FARM_MANAGER),
    UC_02("UC-02", "Plan Automated Flight Path", Screen.S3, "-",
            FARM_MANAGER, WEATHER_SERVICE),
    UC_03("UC-03", "Schedule Drone Mission", Screen.S3, "«include» UC-02",
            FARM_MANAGER, DRONE),
    UC_04("UC-04", "Monitor Soil Moisture", Screen.S4, "extended by UC-08",
            FARM_MANAGER, DRONE),
    UC_05("UC-05", "Monitor Crop Growth", Screen.S5, "extended by UC-08",
            FARM_MANAGER, DRONE),
    UC_06("UC-06", "Determine Harvest Readiness", Screen.S6, "«include» UC-05",
            FARM_MANAGER),
    UC_07("UC-07", "Track Equipment Location and Inventory", Screen.S7, "extended by UC-08",
            FARM_MANAGER, DRONE, EQUIPMENT_GPS_TAG),
    UC_08("UC-08", "Receive Alerts and Notifications", Screen.S8, "«extend» UC-04, UC-05, UC-07",
            FARM_MANAGER),
    UC_09("UC-09", "Generate Farm Reports", Screen.S9, "-",
            FARM_MANAGER),
    UC_10("UC-10", "Manage Users and Roles", Screen.S10, "-",
            SYSTEM_ADMINISTRATOR);

    private final String id;
    private final String displayName;
    private final Screen screen;
    private final String relationship;
    private final Actor primary;
    private final List<Actor> secondary;

    /** First actor is the primary actor; any others are secondary. */
    UseCase(String id, String displayName, Screen screen, String relationship,
            Actor primary, Actor... secondary) {
        this.id = id;
        this.displayName = displayName;
        this.screen = screen;
        this.relationship = relationship;
        this.primary = primary;
        this.secondary = List.of(secondary);
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Screen getScreen() { return screen; }
    public String getRelationship() { return relationship; }
    public Actor getPrimary() { return primary; }
    public List<Actor> getSecondary() { return secondary; }

    public static List<UseCase> forScreen(Screen screen) {
        List<UseCase> out = new ArrayList<>();
        for (UseCase uc : values()) {
            if (uc.screen == screen) out.add(uc);
        }
        return out;
    }

    @Override
    public String toString() { return id + " " + displayName; }
}
