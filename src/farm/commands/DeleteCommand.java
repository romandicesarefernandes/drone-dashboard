package farm.commands;

import farm.FarmComponent;
import farm.ItemContainer;

/** "Delete item" / "Delete item-container": removes the object (and everything inside it) from its parent. */
public class DeleteCommand implements Command {
    private final FarmComponent target;

    public DeleteCommand(FarmComponent target) {
        this.target = target;
    }

    @Override
    public void execute() {
        ItemContainer parent = target.getParent();
        if (parent == null) {
            throw new IllegalArgumentException("The root farm cannot be deleted.");
        }
        parent.remove(target);
    }

    @Override
    public String describe() {
        return "Deleted " + target.getName() + ".";
    }
}
