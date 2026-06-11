package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Routes inventory clicks/drags to the open {@link Menu} and feeds chat messages to
 * the {@link ChatInput} manager.
 */
public class GuiListener implements Listener {

    private final Main plugin;

    public GuiListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu menu) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null
                    && event.getClickedInventory().getHolder() == menu) {
                menu.click(event);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation") // AsyncPlayerChatEvent: still the portable way to capture chat input
    public void onChat(AsyncPlayerChatEvent event) {
        if (plugin.getChatInput().isPending(event.getPlayer())) {
            event.setCancelled(true);
            plugin.getChatInput().handle(event.getPlayer(), event.getMessage());
        }
    }
}
