package entity;

/** Account status of a User. Pending = invited but not activated yet. */
public enum AccountStatus {
    ACTIVE("Active"),
    PENDING("Pending"),
    INACTIVE("Inactive");

    private final String label;

    AccountStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    @Override
    public String toString() { return label; }
}
