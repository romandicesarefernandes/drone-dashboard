package farm;

/** The "Scenario for testing" from the project description, placed on the 800 x 600 farm. */
public final class SampleFarm {

    private SampleFarm() { }

    /** Clears the root and adds the test scenario to it. */
    public static void load(ItemContainer root) {
        root.clear();

        // Barn > Livestock Area > Cow, and Milk Storage
        ItemContainer barn = new ItemContainer("Barn", 20000, 20, 40, 360, 300, 30);
        ItemContainer livestock = new ItemContainer("Livestock Area", 3000, 40, 70, 200, 140, 10);
        livestock.add(new Item("Cow", 1500, 70, 120, 50, 30, 5));
        barn.add(livestock);
        barn.add(new Item("Milk Storage", 4000, 260, 70, 100, 60, 12));
        root.add(barn);

        // Storage Building > Tractor, Tiller
        ItemContainer storage = new ItemContainer("Storage Building", 8000, 420, 40, 360, 160, 25);
        storage.add(new Item("Tractor", 45000, 450, 90, 100, 50, 10));
        storage.add(new Item("Tiller", 2500, 600, 90, 80, 50, 4));
        root.add(storage);

        // Command Center > Drone (the drone's start position for every flight)
        ItemContainer command = new ItemContainer("Command Center", 10000, 20, 380, 260, 180, 15);
        command.add(new Item("Drone", 1200, 120, 440, 60, 60, 1));
        root.add(command);

        root.add(new Item("Soy Crop", 6000, 420, 240, 360, 320, 3));
    }
}
