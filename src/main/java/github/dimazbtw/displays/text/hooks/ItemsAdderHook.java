package github.dimazbtw.displays.text.hooks;

import org.bukkit.Bukkit;

import java.lang.reflect.Method;

/**
 * Soft hook for ItemsAdder, bound reflectively. Replaces {@code :emoji:} style font
 * image placeholders with their glyph characters via
 * {@code FontImageWrapper.replaceFontImages(String)}.
 */
public final class ItemsAdderHook {

    private boolean enabled;
    private Method replaceFontImages;

    public void init() {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return;
        }
        try {
            Class<?> wrapper = Class.forName("dev.lone.itemsadder.api.FontImages.FontImageWrapper");
            replaceFontImages = wrapper.getMethod("replaceFontImages", String.class);
            enabled = true;
        } catch (Throwable ignored) {
            enabled = false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Replaces ItemsAdder font images. Returns the input unchanged on any failure. */
    public String apply(String text) {
        if (!enabled || text == null) {
            return text;
        }
        try {
            Object result = replaceFontImages.invoke(null, text);
            return result != null ? result.toString() : text;
        } catch (Throwable ignored) {
            return text;
        }
    }
}
