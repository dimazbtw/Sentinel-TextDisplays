package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Lightweight chat-prompt manager: closes the menu, asks the player to type a value,
 * and runs the callback on the main thread with the next chat message they send.
 */
public class ChatInput {

    private final Main plugin;
    private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

    public ChatInput(Main plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, String prompt, Consumer<String> callback) {
        pending.put(player.getUniqueId(), callback);
        player.closeInventory();
        player.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().color(prompt));
        player.sendMessage(plugin.getConfigManager().color("&7Type it in chat, or '&fcancel&7' to abort."));
    }

    public boolean isPending(Player player) {
        return pending.containsKey(player.getUniqueId());
    }

    /** Called from the (async) chat listener. Dispatches the callback on the main thread. */
    public void handle(Player player, String message) {
        Consumer<String> callback = pending.remove(player.getUniqueId());
        if (callback == null) {
            return;
        }
        if (message.equalsIgnoreCase("cancel")) {
            runSync(() -> player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().color("&7Cancelled.")));
            return;
        }
        runSync(() -> callback.accept(message));
    }

    public void clear(Player player) {
        pending.remove(player.getUniqueId());
    }

    private void runSync(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }
}
