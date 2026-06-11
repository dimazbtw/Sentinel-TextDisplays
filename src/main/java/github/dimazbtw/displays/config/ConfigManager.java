package github.dimazbtw.displays.config;

import github.dimazbtw.displays.text.ColorTranslator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads config.yml, exposes settings and resolves user-facing messages.
 * Colors are translated through {@link ColorTranslator#toLegacy} so both {@code &} codes
 * and {@code &#RRGGBB} hex work in messages.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private String prefix;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.prefix = color(config.getString("settings.prefix", "&8[&bDisplays&8]&r "));
    }

    public String color(String text) {
        return ColorTranslator.toLegacy(text);
    }

    public String getPrefix() {
        return prefix;
    }

    /** Prefixed, colored message from the messages section. */
    public String getMessage(String path) {
        return prefix + color(config.getString("messages." + path, "&c<missing: " + path + ">"));
    }

    /** Colored message without the prefix. */
    public String getRaw(String path) {
        return color(config.getString("messages." + path, path));
    }

    // ---- Settings ----

    public boolean isDebug() {
        return config.getBoolean("settings.debug", false);
    }

    public double getDefaultDisplayRange() {
        return config.getDouble("settings.default-display-range", 48d);
    }

    public double getDefaultUpdateRange() {
        return config.getDouble("settings.default-update-range", 48d);
    }

    public int getDefaultUpdateInterval() {
        return Math.max(1, config.getInt("settings.default-update-interval", 20));
    }

    public long getClickCooldownMillis() {
        return Math.max(0L, config.getLong("settings.click-cooldown-ms", 500L));
    }

    public double getDefaultScale() {
        return config.getDouble("settings.default-scale", 1.0d);
    }

    public String getDefaultBillboard() {
        return config.getString("settings.default-billboard", "CENTER");
    }

    public String getDefaultBackground() {
        return config.getString("settings.default-background", "default");
    }

    public double getDefaultClickWidth() {
        return config.getDouble("settings.default-click-width", 2.0d);
    }

    public double getDefaultClickHeight() {
        return config.getDouble("settings.default-click-height", 1.0d);
    }

    public FileConfiguration raw() {
        return config;
    }
}
