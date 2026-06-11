package github.dimazbtw.displays.render;

import github.dimazbtw.displays.compat.VersionSupport;

/**
 * Entity-metadata index map for Display and Interaction entities.
 *
 * <p>Indices follow the modern (1.20.2+) layout. On 1.19.4 - 1.20.1 every Display
 * field is shifted down by one because the position/rotation interpolation field
 * did not exist yet, so we apply {@code offset = -1} there.</p>
 *
 * <p>The shared entity base occupies indices 0-7. Display fields start at 8.
 * Interaction fields are NOT affected by the Display split (Interaction extends
 * Entity, not Display), so they are constant across the whole range.</p>
 */
public final class MetaIndex {

    private final int off;

    private MetaIndex(boolean modernLayout) {
        this.off = modernLayout ? 0 : -1;
    }

    /** Builds the index map for the running server. */
    public static MetaIndex current() {
        return new MetaIndex(VersionSupport.isModernDisplayLayout());
    }

    // ---- Display base (Vector3f / Byte / Int / Float) ----
    public int translation() { return 11 + off; }
    public int scale()       { return 12 + off; }
    public int billboard()   { return 15 + off; }
    public int brightness()  { return 16 + off; }
    public int viewRange()   { return 17 + off; }
    public int width()       { return 20 + off; }
    public int height()      { return 21 + off; }
    public int glowColor()   { return 22 + off; }

    // ---- Text Display specific ----
    public int text()         { return 23 + off; }
    public int lineWidth()    { return 24 + off; }
    public int background()   { return 25 + off; }
    public int textOpacity()  { return 26 + off; }
    public int styleFlags()   { return 27 + off; }

    // ---- Interaction entity (constant across versions) ----
    public int interactionWidth()      { return 8; }
    public int interactionHeight()     { return 9; }
    public int interactionResponsive() { return 10; }
}
