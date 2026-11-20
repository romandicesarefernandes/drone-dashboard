package boundary;

/** Boundary to the GPS/BLE equipment tags (UC-07): registers new tags with the tracking network. */
public interface EquipmentGPSTagInterface {

    /** Registers a tag so its periodic location reports are accepted for this equipment. */
    void registerTag(String tagId, String equipmentName);
}
