package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for all menus. Implements {@link InventoryHolder} so a single shared
 * {@link GuiListener} can route clicks to the right menu and slot handler.
 */
public abstract class Menu implements InventoryHolder {

    protected final Main plugin;
    protected final Player player;
    private Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> handlers = new HashMap<>();

    protected Menu(Main plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract String title();

    protected abstract int size();

    protected abstract void build();

    public void open() {
        inventory = Bukkit.createInventory(this, size(), title());
        handlers.clear();
        build();
        player.openInventory(inventory);
    }

    /** Rebuilds the contents in place (keeps the same open window). */
    public void refresh() {
        if (inventory == null) {
            open();
            return;
        }
        handlers.clear();
        inventory.clear();
        build();
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> handler) {
        inventory.setItem(slot, item);
        if (handler != null) {
            handlers.put(slot, handler);
        }
    }

    public void click(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> handler = handlers.get(event.getRawSlot());
        if (handler != null) {
            handler.accept(event);
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected void fillEmpty() {
        ItemStack filler = icon(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    protected String color(String text) {
        return plugin.getConfigManager().color(text);
    }

    /** Save a display and re-render it for online players. */
    protected void apply(github.dimazbtw.displays.display.Display display) {
        plugin.getDisplayManager().save(display);
        plugin.getViewerTracker().reloadDisplay(display);
    }

    /** Left-click adds, right-click subtracts; shift uses the bigger step. Clamped to [min, max]. */
    protected static double adjust(InventoryClickEvent event, double current, double step, double bigStep,
                                   double min, double max) {
        double delta = event.isShiftClick() ? bigStep : step;
        double value = event.isLeftClick() ? current + delta : current - delta;
        return Math.max(min, Math.min(max, value));
    }

    protected ItemStack numberItem(Material material, String name, String value, double step, double bigStep) {
        return icon(material, name,
                color("&7Current: &f" + value),
                "",
                color("&eLeft-click &7+" + trim(step) + "   &eRight-click &7-" + trim(step)),
                color("&eShift-click &7for ±" + trim(bigStep)));
    }

    protected static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    // ---- item builders ----

    protected static ItemStack icon(Material material, String name, String... lore) {
        return icon(material, name, new ArrayList<>(Arrays.asList(lore)));
    }

    protected static ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
