package github.dimazbtw.displays.action;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity.InteractAction;
import github.dimazbtw.displays.api.events.DisplayClickEvent;
import github.dimazbtw.displays.display.ClickType;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import github.dimazbtw.displays.render.ViewerTracker;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * Listens for the client's INTERACT_ENTITY packet. Matches the target entity id against a
 * display's Interaction hitbox, derives the {@link ClickType} (attack/interact + sneaking),
 * and runs that click type's actions for the viewer's current page.
 */
public class ClickListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final DisplayManager manager;
    private final ActionExecutor executor;
    private final ViewerTracker tracker;

    public ClickListener(Plugin plugin, DisplayManager manager, ActionExecutor executor, ViewerTracker tracker) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.manager = manager;
        this.executor = executor;
        this.tracker = tracker;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        Display display = manager.byInteractionId(wrapper.getEntityId());
        if (display == null) {
            return;
        }

        InteractAction action = wrapper.getAction();
        if (action == InteractAction.INTERACT_AT) {
            return; // duplicate of INTERACT for right-clicks
        }

        final Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        boolean sneaking = wrapper.isSneaking().orElse(Boolean.FALSE);
        final ClickType clickType = ClickType.of(action == InteractAction.ATTACK, sneaking);

        // Resolve page/actions and run on the main thread (packet listeners are off-thread).
        Bukkit.getScheduler().runTask(plugin, () -> {
            int pageIndex = tracker.getPage(player.getUniqueId(), display);

            // API event first - listeners may cancel the configured actions.
            DisplayClickEvent apiEvent = new DisplayClickEvent(player, display, clickType, pageIndex);
            Bukkit.getPluginManager().callEvent(apiEvent);
            if (apiEvent.isCancelled()) {
                return;
            }

            List<String> actions = display.page(pageIndex).getActions(clickType);
            if (!actions.isEmpty()) {
                executor.execute(player, display, actions);
            }
        });
    }
}
