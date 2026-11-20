package farm;

/** The editable variables of items and item-containers, and their "Change ..." commands. */
public enum Property {
    NAME("name"),
    PRICE("price"),
    LOCATION_X("location-x"),
    LOCATION_Y("location-y"),
    LENGTH("length"),
    WIDTH("width"),
    HEIGHT("height");

    private final String label;

    Property(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }

    public String commandName() { return "Change " + label; }

    public static Property fromCommand(String command) {
        for (Property p : values()) {
            if (p.commandName().equals(command)) return p;
        }
        throw new IllegalArgumentException("Unknown command: " + command);
    }

    /** Current value as text, for showing in the dashboard. */
    public String read(FarmComponent c) {
        return switch (this) {
            case NAME -> c.getName();
            case PRICE -> format(c.getPrice());
            case LOCATION_X -> format(c.getX());
            case LOCATION_Y -> format(c.getY());
            case LENGTH -> format(c.getLength());
            case WIDTH -> format(c.getWidth());
            case HEIGHT -> format(c.getHeight());
        };
    }

    /** Parses the text and sets the value; throws IllegalArgumentException on bad input. */
    public void apply(FarmComponent c, String value) {
        switch (this) {
            case NAME -> c.setName(value);
            case PRICE -> c.setPrice(number(value));
            case LOCATION_X -> c.setX(number(value));
            case LOCATION_Y -> c.setY(number(value));
            case LENGTH -> c.setLength(number(value));
            case WIDTH -> c.setWidth(number(value));
            case HEIGHT -> c.setHeight(number(value));
        }
    }

    private double number(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new IllegalArgumentException(label + " must be a number.");
        }
    }

    /** 1500.0 -> "1500", 12.5 -> "12.5". */
    public static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
