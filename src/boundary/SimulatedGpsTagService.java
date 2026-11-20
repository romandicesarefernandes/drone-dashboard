package boundary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stand-in for the real tag network: keeps registered tag IDs and prints each registration. */
public class SimulatedGpsTagService implements EquipmentGPSTagInterface {

    private final List<String> registered = new ArrayList<>();

    @Override
    public void registerTag(String tagId, String equipmentName) {
        registered.add(tagId);
        System.out.println("[gps-tag] registered " + tagId + " for " + equipmentName);
    }

    public List<String> getRegistered() {
        return Collections.unmodifiableList(registered);
    }
}
