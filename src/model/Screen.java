package model;

import java.util.List;

/** Screens S1..S10. Every use case lives on exactly one screen (see UseCase.getScreen()). */
public enum Screen {
    S1("S1", "Home"),
    S2("S2", "Farm and Fields"),
    S3("S3", "Flight Planning"),
    S4("S4", "Soil Moisture"),
    S5("S5", "Crop Growth"),
    S6("S6", "Harvest Readiness"),
    S7("S7", "Equipment"),
    S8("S8", "Alerts"),
    S9("S9", "Reports"),
    S10("S10", "Users and Roles");

    private final String id;
    private final String title;

    Screen(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }

    /** Use cases covered by this screen, in UC order. */
    public List<UseCase> getUseCases() {
        return UseCase.forScreen(this);
    }

    /** S1 is shared; every other screen is shown to the primary actor of its use cases. */
    public boolean isVisibleTo(Actor role) {
        if (this == S1) return true;
        for (UseCase uc : getUseCases()) {
            if (uc.getPrimary() == role) return true;
        }
        return false;
    }

    @Override
    public String toString() { return id + "  " + title; }
}
