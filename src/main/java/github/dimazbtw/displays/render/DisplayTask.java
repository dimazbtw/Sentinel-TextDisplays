package github.dimazbtw.displays.render;

import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Per-tick heartbeat: re-resolves dynamic (placeholder) text on each display's
 * update-interval and periodically re-checks visibility (range / edits).
 */
public class DisplayTask extends BukkitRunnable {

    private static final int VISIBILITY_SWEEP_TICKS = 40;

    private final DisplayManager manager;
    private final ViewerTracker tracker;
    private long tick;

    public DisplayTask(DisplayManager manager, ViewerTracker tracker) {
        this.manager = manager;
        this.tracker = tracker;
    }

    @Override
    public void run() {
        tick++;

        if (tick % VISIBILITY_SWEEP_TICKS == 0) {
            tracker.updateAllPlayers();
        }

        for (Display display : manager.all()) {
            if (display.isDynamic() && tick % display.getUpdateInterval() == 0) {
                tracker.refreshDisplay(display);
            }
        }
    }
}
