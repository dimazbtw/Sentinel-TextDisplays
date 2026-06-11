package github.dimazbtw.displays.display;

import github.dimazbtw.displays.api.events.DisplayCreateEvent;
import github.dimazbtw.displays.api.events.DisplayDeleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * Loads, stores and persists {@link Display} definitions (one YAML file each, under
 * {@code <dataFolder>/displays/}), and allocates the fake packet entity ids.
 */
public class DisplayManager {

    private static final Pattern VALID_ID = Pattern.compile("[a-z0-9_-]{1,32}");

    private final Plugin plugin;
    private final File folder;
    private final Map<String, Display> displays = new LinkedHashMap<>();
    private final Map<Integer, Display> byInteractionId = new LinkedHashMap<>();

    // Fake entity ids live in a high range to avoid colliding with real server entities.
    private final AtomicInteger idCounter = new AtomicInteger(2_000_000_000);

    public DisplayManager(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "displays");
    }

    public void loadAll() {
        // API-created (non-persistent) displays survive /td reload; only file-backed ones reload.
        List<Display> apiDisplays = new ArrayList<>();
        for (Display d : displays.values()) {
            if (!d.isPersistent()) {
                apiDisplays.add(d);
            }
        }
        displays.clear();
        byInteractionId.clear();
        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                plugin.getLogger().warning("Could not create displays folder.");
                return;
            }
            // First run: ship a fully commented example display as a template.
            try {
                plugin.saveResource("displays/example.yml", false);
            } catch (Throwable ignored) {
                // resource missing from the jar - not fatal
            }
        }
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                try {
                    YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                    Display display = Display.fromConfig(id, cfg);
                    allocateIds(display);
                    displays.put(id, display);
                } catch (Exception ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to load display '" + id + "'", ex);
                }
            }
        }
        // Re-attach API displays (they keep their entity ids; on a name clash the API one wins).
        for (Display d : apiDisplays) {
            displays.put(d.getId(), d);
            byInteractionId.put(d.getInteractionEntityId(), d);
        }
    }

    private void allocateIds(Display display) {
        display.setTextEntityId(idCounter.incrementAndGet());
        display.setInteractionEntityId(idCounter.incrementAndGet());
        byInteractionId.put(display.getInteractionEntityId(), display);
    }

    public Display create(String id, Location location) {
        Display display = new Display(id);
        display.setLocation(location);
        display.getLines().add("&fNew display");
        register(display);
        return display;
    }

    /** Registers an already-built display (allocates ids, stores and saves it). Used by clone. */
    public void register(Display display) {
        allocateIds(display);
        displays.put(display.getId(), display);
        save(display);
        Bukkit.getPluginManager().callEvent(new DisplayCreateEvent(display));
    }

    public void save(Display display) {
        if (!display.isPersistent()) {
            return; // API-created, memory-only display
        }
        File file = new File(folder, display.getId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        display.toConfig(cfg);
        try {
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save display '" + display.getId() + "'", ex);
        }
    }

    public boolean delete(String id) {
        Display display = displays.remove(id.toLowerCase(Locale.ROOT));
        if (display == null) {
            return false;
        }
        byInteractionId.remove(display.getInteractionEntityId());
        if (display.isPersistent()) {
            File file = new File(folder, display.getId() + ".yml");
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("Could not delete file for display '" + id + "'");
            }
        }
        Bukkit.getPluginManager().callEvent(new DisplayDeleteEvent(display));
        return true;
    }

    public Display get(String id) {
        return id == null ? null : displays.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean exists(String id) {
        return id != null && displays.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Display> all() {
        return displays.values();
    }

    public List<String> ids() {
        return new ArrayList<>(displays.keySet());
    }

    public Display byInteractionId(int entityId) {
        return byInteractionId.get(entityId);
    }

    public static boolean isValidId(String id) {
        return id != null && VALID_ID.matcher(id.toLowerCase(Locale.ROOT)).matches();
    }
}
