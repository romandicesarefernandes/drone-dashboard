package farm.commands;

import farm.FarmComponent;
import farm.ItemContainer;

/** "Add item" / "Add item-container": puts a new child inside a container. */
public class AddCommand implements Command {
    private final ItemContainer parent;
    private final FarmComponent child;

    public AddCommand(ItemContainer parent, FarmComponent child) {
        this.parent = parent;
        this.child = child;
    }

    @Override
    public void execute() {
        parent.add(child);
    }

    @Override
    public String describe() {
        return "Added " + child.getName() + (child.isContainer() ? " (item-container)" : " (item)")
                + " to " + parent.getName() + ".";
    }
}
