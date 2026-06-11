package github.dimazbtw.displays.api;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.ClickType;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Public developer API for Sentinel-TextDisplays.
 *
 * <p>All methods must be called from the <b>main server thread</b>, after the plugin has
 * enabled (declare {@code depend} or {@code softdepend: [Sentinel-TextDisplays]} in your
 * plugin.yml).</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * Display d = TextDisplaysAPI.createDisplay("shop", location, "&bShop", "&7Right-click me!");
 * d.setScale(1.5f);
 * d.page(0).addAction(ClickType.RIGHT, "MESSAGE:&aWelcome!");
 * TextDisplaysAPI.update(d);
 * }</pre>
 *
 * <p>For advanced edits, mutate the {@link Display} object directly (lines, pages, actions,
 * appearance) and call {@link #update(Display)} to persist and re-render it.</p>
 */
public final class TextDisplaysAPI {

    private TextDisplaysAPI() {
    }

    /** True once the plugin is enabled and the API can be used. */
    public static boolean isAvailable() {
        Main plugin = Main.getInstance();
        return plugin != null && plugin.isEnabled();
    }

    // ===================== Lookup =====================

    /** The display with this name, or null. Names are case-insensitive. */
    public static Display getDisplay(String name) {
        return plugin().getDisplayManager().get(name);
    }

    public static boolean exists(String name) {
        return plugin().getDisplayManager().exists(name);
    }

    public static List<String> getDisplayNames() {
        return plugin().getDisplayManager().ids();
    }

    public static Collection<Display> getDisplays() {
        return Collections.unmodifiableCollection(plugin().getDisplayManager().all());
    }

    // ===================== Create / delete =====================

    /**
     * Creates a persistent display (saved to its own file in the displays folder) with the
     * plugin's configured defaults applied.
     *
     * @param name  unique name (letters, numbers, '-', '_'; max 32 chars)
     * @param lines initial text lines (color codes, hex, gradients and placeholders allowed)
     * @throws IllegalArgumentException if the name is invalid or already taken
     */
    public static Display createDisplay(String name, Location location, String... lines) {
        return create(name, location, true, lines);
    }

    /**
     * Creates a <b>temporary</b> display: identical to a normal one, but never written to
     * disk. It survives {@code /td reload} but disappears on server restart. Ideal for
     * displays your plugin spawns and manages itself.
     */
    public static Display createTemporaryDisplay(String name, Location location, String... lines) {
        return create(name, location, false, lines);
    }

    private static Display create(String name, Location location, boolean persistent, String... lines) {
        Main plugin = plugin();
        if (!DisplayManager.isValidId(name)) {
            throw new IllegalArgumentException("Invalid display name: " + name);
        }
        if (plugin.getDisplayManager().exists(name)) {
            throw new IllegalArgumentException("A display named '" + name + "' already exists");
        }
        Display d = plugin.createDisplay(name.toLowerCase(java.util.Locale.ROOT), location);
        d.setPersistent(persistent);
        if (lines != null && lines.length > 0) {
            d.getLines().clear();
            d.getLines().addAll(Arrays.asList(lines));
        }
        update(d);
        return d;
    }

    /** Deletes a display (and its file, if persistent). Returns false if it did not exist. */
    public static boolean deleteDisplay(String name) {
        Main plugin = plugin();
        Display d = plugin.getDisplayManager().get(name);
        if (d == null) {
            return false;
        }
        plugin.getViewerTracker().removeDisplay(d);
        return plugin.getDisplayManager().delete(name);
    }

    // ===================== Editing =====================

    /** Replaces all lines of the first page. */
    public static void setLines(Display display, List<String> lines) {
        display.getLines().clear();
        display.getLines().addAll(new ArrayList<>(lines));
        update(display);
    }

    public static void addLine(Display display, String line) {
        display.getLines().add(line);
        update(display);
    }

    /** Sets one line of the first page (zero-based index). */
    public static void setLine(Display display, int index, String line) {
        display.getLines().set(index, line);
        update(display);
    }

    public static void removeLine(Display display, int index) {
        display.getLines().remove(index);
        update(display);
    }

    /** Adds a click action ({@code TYPE:data} format) to a page, enabling the hitbox if needed. */
    public static void addClickAction(Display display, int pageIndex, ClickType clickType, String action) {
        display.page(pageIndex).addAction(clickType, action);
        if (display.getClickWidth() <= 0f || display.getClickHeight() <= 0f) {
            Main plugin = plugin();
            display.setClickBox((float) plugin.getConfigManager().getDefaultClickWidth(),
                    (float) plugin.getConfigManager().getDefaultClickHeight());
        }
        update(display);
    }

    public static void moveDisplay(Display display, Location location) {
        display.setLocation(location);
        update(display);
    }

    public static void setEnabled(Display display, boolean enabled) {
        display.setEnabled(enabled);
        update(display);
    }

    /**
     * Saves the display (if persistent) and re-renders it for every online viewer.
     * Call this after mutating a {@link Display} directly.
     */
    public static void update(Display display) {
        Main plugin = plugin();
        plugin.getDisplayManager().save(display);
        plugin.getViewerTracker().reloadDisplay(display);
    }

    // ===================== Per-player pages =====================

    /** The zero-based page the player currently sees on this display. */
    public static int getPage(Player player, Display display) {
        return plugin().getViewerTracker().getPage(player.getUniqueId(), display);
    }

    /** Shows a specific page (zero-based) of the display to one player. */
    public static void setPage(Player player, Display display, int pageIndex) {
        plugin().getViewerTracker().setPage(player, display, pageIndex);
    }

    public static void nextPage(Player player, Display display) {
        plugin().getViewerTracker().nextPage(player, display);
    }

    public static void previousPage(Player player, Display display) {
        plugin().getViewerTracker().prevPage(player, display);
    }

    // ===================== Internal =====================

    private static Main plugin() {
        Main plugin = Main.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException(
                    "Sentinel-TextDisplays is not enabled yet. Add it to depend/softdepend in your plugin.yml.");
        }
        return plugin;
    }
}
