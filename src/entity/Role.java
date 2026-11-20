package entity;

/** The two dashboard roles (Requirements, section 2). */
public enum Role {
    FARM_MANAGER("Farm Manager"),
    SYSTEM_ADMINISTRATOR("System Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
