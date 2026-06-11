package github.dimazbtw.displays.action;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import github.dimazbtw.displays.render.ViewerTracker;
import github.dimazbtw.displays.text.ColorTranslator;
import github.dimazbtw.displays.text.hooks.PlaceholderHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses and runs click actions in the DecentHolograms {@code TYPE:data} format.
 * Must be called on the main thread.
 *
 * <p>Supported types: MESSAGE, COMMAND, CONSOLE, CONNECT, TELEPORT, SOUND, PERMISSION,
 * NEXT_PAGE, PREV_PAGE, PAGE. PERMISSION stops the remaining actions if the player lacks the
 * node. Page numbers are 1-based (as shown to users).</p>
 */
public class ActionExecutor {

    private final Plugin plugin;
    private final PlaceholderHook placeholders;
    private final ViewerTracker tracker;
    private final DisplayManager manager;
    private long cooldownMillis;
    private final Map<String, Long> cooldowns = new HashMap<>();

    public ActionExecutor(Plugin plugin, PlaceholderHook placeholders, ViewerTracker tracker,
                          DisplayManager manager, long cooldownMillis) {
        this.plugin = plugin;
        this.placeholders = placeholders;
        this.tracker = tracker;
        this.manager = manager;
        this.cooldownMillis = cooldownMillis;
    }

    public void setCooldownMillis(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public void execute(Player player, Display display, List<String> actions) {
        if (player == null || actions == null || actions.isEmpty()) {
            return;
        }
        if (cooldownMillis > 0) {
            String key = player.getUniqueId() + ":" + display.getId();
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(key);
            if (last != null && now - last < cooldownMillis) {
                return;
            }
            cooldowns.put(key, now);
        }
        for (String action : actions) {
            try {
                if (!run(player, display, action)) {
                    return; // a PERMISSION check failed - stop the chain
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to run action '" + action + "': " + ex.getMessage());
            }
        }
    }

    /** Returns false to stop the action chain (failed PERMISSION gate). */
    private boolean run(Player player, Display display, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return true;
        }
        String trimmed = raw.trim();
        int colon = trimmed.indexOf(':');
        String type = (colon >= 0 ? trimmed.substring(0, colon) : trimmed).trim().toUpperCase(Locale.ROOT);
        String data = colon >= 0 ? trimmed.substring(colon + 1) : "";
        data = placeholders.apply(player, data);

        switch (type) {
            case "MESSAGE":
                player.sendMessage(ColorTranslator.toLegacy(data));
                return true;
            case "COMMAND":
                player.performCommand(stripSlash(data));
                return true;
            case "CONSOLE":
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(data));
                return true;
            case "CONNECT":
                connect(player, data.trim());
                return true;
            case "TELEPORT":
                teleport(player, data);
                return true;
            case "SOUND":
                playSound(player, data);
                return true;
            case "PERMISSION":
                return data.isEmpty() || player.hasPermission(data.trim());
            case "NEXT_PAGE":
                tracker.nextPage(player, target(data, display));
                return true;
            case "PREV_PAGE":
            case "PREVIOUS_PAGE":
                tracker.prevPage(player, target(data, display));
                return true;
            case "PAGE":
                page(player, display, data);
                return true;
            default:
                player.sendMessage(ColorTranslator.toLegacy(raw));
                return true;
        }
    }

    private Display target(String data, Display fallback) {
        if (data == null || data.trim().isEmpty()) {
            return fallback;
        }
        Display d = manager.get(data.trim());
        return d != null ? d : fallback;
    }

    private void page(Player player, Display display, String data) {
        String[] parts = data.split(":");
        Display targetDisplay = display;
        String pageStr;
        if (parts.length >= 2) {
            Display d = manager.get(parts[0].trim());
            if (d != null) {
                targetDisplay = d;
            }
            pageStr = parts[1];
        } else {
            pageStr = parts.length == 1 ? parts[0] : "1";
        }
        try {
            tracker.setPage(player, targetDisplay, Integer.parseInt(pageStr.trim()) - 1);
        } catch (NumberFormatException ignored) {
            // bad page number - do nothing
        }
    }

    private void teleport(Player player, String data) {
        String[] t = data.split(":");
        int i = 0;
        World world = player.getWorld();
        if (t.length > 0 && !isNumber(t[0])) {
            World w = Bukkit.getWorld(t[0].trim());
            if (w != null) {
                world = w;
            }
            i = 1;
        }
        if (t.length < i + 3) {
            return;
        }
        try {
            double x = Double.parseDouble(t[i]);
            double y = Double.parseDouble(t[i + 1]);
            double z = Double.parseDouble(t[i + 2]);
            float yaw = t.length > i + 3 ? Float.parseFloat(t[i + 3]) : player.getLocation().getYaw();
            float pitch = t.length > i + 4 ? Float.parseFloat(t[i + 4]) : player.getLocation().getPitch();
            player.teleport(new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException ignored) {
            // bad coordinates
        }
    }

    private void playSound(Player player, String data) {
        String[] parts = data.split(":");
        float volume = parts.length > 1 ? parseFloat(parts[1], 1f) : 1f;
        float pitch = parts.length > 2 ? parseFloat(parts[2], 1f) : 1f;
        try {
            Sound sound = Sound.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown sound in action: " + parts[0]);
        }
    }

    private void connect(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s.trim());
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
