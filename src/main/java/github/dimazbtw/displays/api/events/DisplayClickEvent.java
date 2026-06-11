package github.dimazbtw.displays.api.events;

import github.dimazbtw.displays.display.ClickType;
import github.dimazbtw.displays.display.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Fired on the main thread when a player clicks a text display (before the configured
 * click actions run). Cancelling the event prevents the configured actions from running.
 */
public class DisplayClickEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Display display;
    private final ClickType clickType;
    private final int pageIndex;
    private boolean cancelled;

    public DisplayClickEvent(Player player, Display display, ClickType clickType, int pageIndex) {
        super(player);
        this.display = display;
        this.clickType = clickType;
        this.pageIndex = pageIndex;
    }

    public Display getDisplay() {
        return display;
    }

    public ClickType getClickType() {
        return clickType;
    }

    /** Zero-based index of the page the player was viewing when they clicked. */
    public int getPageIndex() {
        return pageIndex;
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
