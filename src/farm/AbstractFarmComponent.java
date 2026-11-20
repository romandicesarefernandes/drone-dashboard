package farm;

/** Variables and setters shared by items and item-containers. Setters validate their input. */
public abstract class AbstractFarmComponent implements FarmComponent {
    private String name;
    private double price, x, y, length, width, height;
    private ItemContainer parent;

    protected AbstractFarmComponent(String name, double price, double x, double y,
                                    double length, double width, double height) {
        setName(name);
        setPrice(price);
        setX(x);
        setY(y);
        setLength(length);
        setWidth(width);
        setHeight(height);
    }

    @Override public String getName() { return name; }
    @Override public double getPrice() { return price; }
    @Override public double getX() { return x; }
    @Override public double getY() { return y; }
    @Override public double getLength() { return length; }
    @Override public double getWidth() { return width; }
    @Override public double getHeight() { return height; }
    @Override public ItemContainer getParent() { return parent; }

    @Override
    public final void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        this.name = name.trim();
    }

    @Override
    public final void setPrice(double price) {
        if (price < 0) throw new IllegalArgumentException("Price cannot be negative.");
        this.price = price;
    }

    @Override public final void setX(double x) { this.x = x; }
    @Override public final void setY(double y) { this.y = y; }

    @Override
    public final void setLength(double length) {
        this.length = positive("Length", length);
    }

    @Override
    public final void setWidth(double width) {
        this.width = positive("Width", width);
    }

    @Override
    public final void setHeight(double height) {
        this.height = positive("Height", height);
    }

    @Override public final void setParent(ItemContainer parent) { this.parent = parent; }

    private static double positive(String what, double value) {
        if (value <= 0) throw new IllegalArgumentException(what + " must be greater than 0.");
        return value;
    }

    @Override
    public String toString() {
        return name + (isContainer() ? "  (item-container)" : "  (item)");
    }
}
