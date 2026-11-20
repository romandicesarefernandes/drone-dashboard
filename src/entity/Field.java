package entity;

/** Entity: a mapped field of the farm. */
public record Field(int fieldId, int farmId, String name, String cropType, double areaAcres) {

    @Override
    public String toString() { return name; }
}
