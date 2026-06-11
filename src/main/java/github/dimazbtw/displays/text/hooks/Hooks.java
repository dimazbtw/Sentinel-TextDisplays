package github.dimazbtw.displays.text.hooks;

/**
 * Aggregates the optional integration hooks and detects which are present.
 */
public final class Hooks {

    private final PlaceholderHook placeholders = new PlaceholderHook();
    private final ItemsAdderHook itemsAdder = new ItemsAdderHook();
    private final NexoHook nexo = new NexoHook();

    public void init() {
        placeholders.init();
        itemsAdder.init();
        nexo.init();
    }

    public PlaceholderHook placeholders() {
        return placeholders;
    }

    public ItemsAdderHook itemsAdder() {
        return itemsAdder;
    }

    public NexoHook nexo() {
        return nexo;
    }

    /** Short human-readable summary for the startup banner. */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("PlaceholderAPI=").append(placeholders.isEnabled() ? "yes" : "no");
        sb.append(", ItemsAdder=").append(itemsAdder.isEnabled() ? "yes" : "no");
        sb.append(", Nexo=").append(nexo.isEnabled() ? "yes" : "no");
        return sb.toString();
    }
}
