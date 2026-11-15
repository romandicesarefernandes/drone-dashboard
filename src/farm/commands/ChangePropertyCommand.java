package farm.commands;

import farm.FarmComponent;
import farm.Property;

/** "Change name", "Change price", "Change location-x", ... : sets one variable from text input. */
public class ChangePropertyCommand implements Command {
    private final FarmComponent target;
    private final Property property;
    private final String value;

    public ChangePropertyCommand(FarmComponent target, Property property, String value) {
        this.target = target;
        this.property = property;
        this.value = value;
    }

    @Override
    public void execute() {
        property.apply(target, value);
    }

    @Override
    public String describe() {
        return "Changed " + property.getLabel() + " of " + target.getName()
                + " to " + property.read(target) + ".";
    }
}
