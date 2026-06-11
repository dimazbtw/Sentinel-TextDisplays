package github.dimazbtw.displays.render;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import github.dimazbtw.displays.text.TextPipeline;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Decides which displays each player can see and keeps the client in sync by sending
 * spawn / text-update / destroy packets. Tracks the current page per player (for multi-page
 * holograms). All methods run on the main server thread.
 *
 * <p>display-range controls visibility; update-range controls how close a player must be for
 * placeholders to keep refreshing (DecentHolograms semantics).</p>
 */
public class ViewerTracker {

    private final DisplayManager manager;
    private final TextPipeline pipeline;
    private final MetaIndex idx;

    private final Map<UUID, Set<String>> visibleByPlayer = new HashMap<>();
    private final Map<String, Set<UUID>> viewersByDisplay = new HashMap<>();
    private final Map<String, Map<UUID, String>> lastText = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> pageByPlayer = new HashMap<>();

    public ViewerTracker(DisplayManager manager, TextPipeline pipeline) {
        this.manager = manager;
        this.pipeline = pipeline;
        this.idx = MetaIndex.current();
    }

    // ===================== Visibility =====================

    public void updatePlayer(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Set<String> visible = visibleByPlayer.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        for (Display display : manager.all()) {
            boolean should = shouldSee(player, display);
            boolean has = visible.contains(display.getId());
            if (should && !has) {
                spawn(player, display);
            } else if (!should && has) {
                despawn(player, display);
            }
        }
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private boolean shouldSee(Player player, Display display) {
        if (!display.isEnabled() || !display.canSee(player)) {
            return false;
        }
        Location loc = display.getBukkitLocation();
        if (loc == null || loc.getWorld() == null || !player.getWorld().equals(loc.getWorld())) {
            return false;
        }
        double range = display.getDisplayRange();
        return player.getLocation().distanceSquared(loc) <= range * range;
    }

    // ===================== Spawn / refresh / despawn =====================

    private void spawn(Player player, Display display) {
        UUID uuid = player.getUniqueId();
        int pageIndex = getPage(uuid, display);
        List<String> resolved = pipeline.resolveLines(player, display.page(pageIndex).getLines());
        Component text = TextPipeline.componentOf(resolved);

        PacketSender.spawn(player, display.getTextEntityId(), EntityTypes.TEXT_DISPLAY,
                display.getX(), display.getY(), display.getZ(), display.getFacing(), 0f);
        PacketSender.metadata(player, display.getTextEntityId(),
                DisplayMeta.full(display, text, resolved.size(), idx));

        if (display.isClickable()) {
            double centerY = display.getY() + DisplayMeta.verticalOffset(display, resolved.size());
            double interactionY = centerY - display.getClickHeight() / 2.0;
            PacketSender.spawn(player, display.getInteractionEntityId(), EntityTypes.INTERACTION,
                    display.getX(), interactionY, display.getZ(), 0f, 0f);
            PacketSender.metadata(player, display.getInteractionEntityId(), InteractionMeta.build(display, idx));
        }

        visible(uuid).add(display.getId());
        viewers(display.getId()).add(uuid);
        text(display.getId()).put(uuid, TextPipeline.join(resolved));
    }

    private void despawn(Player player, Display display) {
        UUID uuid = player.getUniqueId();
        PacketSender.destroy(player, display.getTextEntityId(), display.getInteractionEntityId());
        Set<String> vis = visibleByPlayer.get(uuid);
        if (vis != null) {
            vis.remove(display.getId());
        }
        Set<UUID> v = viewersByDisplay.get(display.getId());
        if (v != null) {
            v.remove(uuid);
        }
        Map<UUID, String> t = lastText.get(display.getId());
        if (t != null) {
            t.remove(uuid);
        }
    }

    /** Re-resolves text for every viewer within update-range, sending updates only when changed. */
    public void refreshDisplay(Display display) {
        Set<UUID> v = viewersByDisplay.get(display.getId());
        if (v == null || v.isEmpty()) {
            return;
        }
        Location loc = display.getBukkitLocation();
        if (loc == null) {
            return;
        }
        double updateRange = display.getUpdateRange();
        double maxSq = updateRange * updateRange;
        Map<UUID, String> sent = text(display.getId());
        for (UUID uuid : new ArrayList<>(v)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline() || !player.getWorld().equals(loc.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(loc) > maxSq) {
                continue; // within display-range (visible) but beyond update-range
            }
            List<String> resolved = pipeline.resolveLines(player, display.page(getPage(uuid, display)).getLines());
            String key = TextPipeline.join(resolved);
            if (!key.equals(sent.get(uuid))) {
                PacketSender.metadata(player, display.getTextEntityId(),
                        DisplayMeta.textOnly(TextPipeline.componentOf(resolved), idx));
                sent.put(uuid, key);
            }
        }
    }

    /** Despawns a display from everyone then re-evaluates visibility (used after edits/teleports). */
    public void reloadDisplay(Display display) {
        destroyEverywhere(display);
        updateAllPlayers();
    }

    /** Removes a deleted display from everyone. */
    public void removeDisplay(Display display) {
        destroyEverywhere(display);
    }

    private void destroyEverywhere(Display display) {
        Set<UUID> v = viewersByDisplay.get(display.getId());
        if (v != null) {
            for (UUID uuid : new ArrayList<>(v)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    despawn(player, display);
                }
            }
        }
        viewersByDisplay.remove(display.getId());
        lastText.remove(display.getId());
        for (Set<String> set : visibleByPlayer.values()) {
            set.remove(display.getId());
        }
    }

    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();
        pageByPlayer.remove(uuid);
        Set<String> vis = visibleByPlayer.remove(uuid);
        if (vis != null) {
            for (String id : vis) {
                Set<UUID> v = viewersByDisplay.get(id);
                if (v != null) {
                    v.remove(uuid);
                }
                Map<UUID, String> t = lastText.get(id);
                if (t != null) {
                    t.remove(uuid);
                }
            }
        }
    }

    /** Destroys every visible display for every online player (used on plugin disable). */
    public void clearAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Set<String> vis = visibleByPlayer.get(player.getUniqueId());
            if (vis == null) {
                continue;
            }
            for (String id : new ArrayList<>(vis)) {
                Display display = manager.get(id);
                if (display != null) {
                    PacketSender.destroy(player, display.getTextEntityId(), display.getInteractionEntityId());
                }
            }
        }
        visibleByPlayer.clear();
        viewersByDisplay.clear();
        lastText.clear();
        pageByPlayer.clear();
    }

    // ===================== Pages =====================

    public int getPage(UUID uuid, Display display) {
        int page = pageByPlayer.getOrDefault(uuid, java.util.Collections.emptyMap())
                .getOrDefault(display.getId(), 0);
        return Math.max(0, Math.min(display.pageCount() - 1, page));
    }

    public void setPage(Player player, Display display, int index) {
        int clamped = Math.max(0, Math.min(display.pageCount() - 1, index));
        int current = getPage(player.getUniqueId(), display);
        if (clamped == current) {
            return;
        }
        github.dimazbtw.displays.api.events.DisplayPageChangeEvent event =
                new github.dimazbtw.displays.api.events.DisplayPageChangeEvent(player, display, current, clamped);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        pageByPlayer.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(display.getId(), clamped);
        rerender(player, display);
    }

    public void nextPage(Player player, Display display) {
        int next = getPage(player.getUniqueId(), display) + 1;
        if (next >= display.pageCount()) {
            next = 0;
        }
        setPage(player, display, next);
    }

    public void prevPage(Player player, Display display) {
        int prev = getPage(player.getUniqueId(), display) - 1;
        if (prev < 0) {
            prev = display.pageCount() - 1;
        }
        setPage(player, display, prev);
    }

    private void rerender(Player player, Display display) {
        UUID uuid = player.getUniqueId();
        Set<UUID> v = viewersByDisplay.get(display.getId());
        if (v == null || !v.contains(uuid)) {
            return; // not currently viewing
        }
        List<String> resolved = pipeline.resolveLines(player, display.page(getPage(uuid, display)).getLines());
        PacketSender.metadata(player, display.getTextEntityId(),
                DisplayMeta.full(display, TextPipeline.componentOf(resolved), resolved.size(), idx));
        text(display.getId()).put(uuid, TextPipeline.join(resolved));
    }

    private Set<String> visible(UUID uuid) {
        return visibleByPlayer.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    private Set<UUID> viewers(String displayId) {
        return viewersByDisplay.computeIfAbsent(displayId, k -> new HashSet<>());
    }

    private Map<UUID, String> text(String displayId) {
        return lastText.computeIfAbsent(displayId, k -> new HashMap<>());
    }
}
