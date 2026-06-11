package github.dimazbtw.displays.text.hooks;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Soft hook for PlaceholderAPI, bound reflectively so the plugin compiles and runs
 * without PlaceholderAPI on the classpath.
 */
public final class PlaceholderHook {

    private boolean enabled;
    private Method setPlaceholders;

    public void init() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholders = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            enabled = true;
        } catch (Throwable ignored) {
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Resolves placeholders for the given viewer. Returns the input unchanged on any failure. */
    public String apply(Player viewer, String text) {
        if (!enabled || viewer == null || text == null || text.indexOf('%') < 0) {
            return text;
        }
        try {
            Object result = setPlaceholders.invoke(null, viewer, text);
            return result != null ? result.toString() : text;
        } catch (Throwable ignored) {
            return text;
        }
    }
}
