package github.dimazbtw.displays.display;

/**
 * Horizontal alignment of multi-line text, encoded into the Text Display style flags.
 * CENTER is the absence of both the left (0x08) and right (0x10) bits.
 */
public enum TextAlignment {

    CENTER(0),
    LEFT(0x08),
    RIGHT(0x10);

    private final int flagBits;

    TextAlignment(int flagBits) {
        this.flagBits = flagBits;
    }

    public int flagBits() {
        return flagBits;
    }

    public static TextAlignment from(String name, TextAlignment def) {
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
