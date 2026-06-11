package github.dimazbtw.displays.display;

import java.util.Locale;

/**
 * Click types, matching DecentHolograms: LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT.
 */
public enum ClickType {

    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT;

    /** attack = left click, otherwise right click; combined with the sneaking state. */
    public static ClickType of(boolean attack, boolean sneaking) {
        if (attack) {
            return sneaking ? SHIFT_LEFT : LEFT;
        }
        return sneaking ? SHIFT_RIGHT : RIGHT;
    }

    public static ClickType from(String name, ClickType def) {
        if (name == null) {
            return def;
        }
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
