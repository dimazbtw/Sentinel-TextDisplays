package github.dimazbtw.displays.api.events;

import github.dimazbtw.displays.display.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player's current page of a display is about to change
 * (via click actions or the API). Cancelling keeps the old page.
 */
public class DisplayPageChangeEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Display display;
    private final int oldPage;
    private final int newPage;
    private boolean cancelled;

    public DisplayPageChangeEvent(Player player, Display display, int oldPage, int newPage) {
        super(player);
        this.display = display;
        this.oldPage = oldPage;
        this.newPage = newPage;
    }

    public Display getDisplay() {
        return display;
    }

    /** Zero-based index of the page the player is leaving. */
    public int getOldPage() {
        return oldPage;
    }

    /** Zero-based index of the page the player is switching to. */
    public int getNewPage() {
        return newPage;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
