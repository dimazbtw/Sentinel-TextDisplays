package github.dimazbtw.displays.display;

/**
 * Display billboard constraint - how the entity rotates to face the player.
 * The id matches the vanilla metadata byte value.
 */
public enum BillboardMode {

    FIXED(0),
    VERTICAL(1),
    HORIZONTAL(2),
    CENTER(3);

    private final int id;

    BillboardMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static BillboardMode from(String name, BillboardMode def) {
        if (name == null) {
            return def;
        }
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
