package github.dimazbtw.displays.api.events;

import github.dimazbtw.displays.display.Display;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a display has been created (by command, GUI or API).
 * Not fired for displays loaded from disk on startup/reload.
 */
public class DisplayCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Display display;

    public DisplayCreateEvent(Display display) {
        this.display = display;
    }

    public Display getDisplay() {
        return display;
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
