package farm;

import entity.DataStore;
import entity.Field;
import entity.FieldBoundary;

/**
 * Test scenario for the Fields page: the Green Valley Farms farm items
 * (crops, livestock, buildings). Equipment is not included; it is managed on
 * the Equipment page.
 *
 *  - each field is an item-container at the same place as on the Equipment
 *    map, holding its crop (e.g. "200 Corn")
 *  - the farmyard below South Field has the Barn (Livestock Area with cows
 *    and chickens, Milk Storage), the Storage Building (feed and seed) and the
 *    Command Center with the Drone
 *
 * Items have no quantity variable, so a group is one item: the count is in its
 * name and the price is the price of the whole group.
 * The farm map (1000 x 600 units) is scaled by 0.8 to fit the 800 x 600 plot,
 * which leaves the bottom strip (y 480-600) for the farmyard.
 */
public final class SampleFarm {

    private static final double SCALE = 0.8;

    private SampleFarm() { }

    /** Clears the root and adds the Green Valley Farms items. */
    public static void load(ItemContainer root, DataStore store) {
        root.clear();

        // Fields with their crops
        for (FieldBoundary b : store.boundaries()) {
            Field f = store.findField(b.fieldId());
            double x = b.x() * SCALE, y = b.y() * SCALE, w = b.width() * SCALE, h = b.height() * SCALE;
            ItemContainer field = new ItemContainer(f == null ? "Field " + b.fieldId() : f.name(), 0, x, y, w, h, 1);
            if (f != null) field.add(crop(f.cropType(), x + 12, y + 26, w - 24, h - 38));
            root.add(field);
        }

        // Barn > Livestock Area > Cows, Chickens; Milk Storage
        ItemContainer barn = new ItemContainer("Barn", 20000, 214, 488, 250, 104, 30);
        ItemContainer livestock = new ItemContainer("Livestock Area", 3000, 224, 512, 156, 72, 10);
        livestock.add(new Item("5 Cows", 7500, 232, 534, 70, 42, 5));
        livestock.add(new Item("3 Chickens", 45, 308, 534, 64, 42, 2));
        barn.add(livestock);
        barn.add(new Item("Milk Storage", 4000, 388, 524, 68, 56, 12));
        root.add(barn);

        // Storage Building > Feed, Seed
        ItemContainer storage = new ItemContainer("Storage Building", 8000, 480, 488, 190, 104, 25);
        storage.add(new Item("40 Feed Bags", 600, 488, 524, 84, 56, 4));
        storage.add(new Item("25 Seed Bags", 1250, 578, 524, 84, 56, 4));
        root.add(storage);

        // Command Center > Drone: the drone's start position for every flight
        ItemContainer command = new ItemContainer("Command Center", 10000, 24, 488, 176, 104, 15);
        command.add(new Item("Drone", 1200, 82, 520, 60, 54, 1));
        root.add(command);
    }

    /** One crop item filling most of its field, with the plant count in its name. */
    private static Item crop(String cropType, double x, double y, double w, double h) {
        return switch (cropType) {
            case "Corn" -> new Item("200 Corn", 4000, x, y, w, h, 8);
            case "Apples" -> new Item("60 Apple Trees", 18000, x, y, w, h, 12);
            case "Wheat" -> new Item("300 Wheat", 6000, x, y, w, h, 3);
            case "Soybean" -> new Item("150 Soybeans", 3000, x, y, w, h, 3);
            default -> new Item(cropType, 1000, x, y, w, h, 3);
        };
    }
}
