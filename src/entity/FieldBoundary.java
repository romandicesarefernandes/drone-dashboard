package entity;

/**
 * Where a field sits on the farm map (UC-01 field boundary, simplified to a
 * rectangle). Farm-map units: 1000 x 600, origin top-left.
 */
public record FieldBoundary(int fieldId, double x, double y, double width, double height) {

    public boolean contains(double px, double py) {
        return px >= x && px <= x + width && py >= y && py <= y + height;
    }

    public double centerX() { return x + width / 2; }
    public double centerY() { return y + height / 2; }
}
