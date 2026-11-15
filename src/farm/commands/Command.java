package farm.commands;

/** One edit the dashboard can run on an item or item-container. */
public interface Command {

    /** Runs the edit. Throws IllegalArgumentException if the input is not valid. */
    void execute();

    /** Short message for the dashboard's status bar after a successful run. */
    String describe();
}
