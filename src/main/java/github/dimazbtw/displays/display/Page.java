package github.dimazbtw.displays.display;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A hologram page (DecentHolograms style): a list of text lines plus click actions
 * keyed by {@link ClickType}. Each action is a raw {@code TYPE:data} string.
 */
public class Page {

    private final List<String> lines = new ArrayList<>();
    private final Map<ClickType, List<String>> actions = new EnumMap<>(ClickType.class);

    public List<String> getLines() {
        return lines;
    }

    public Map<ClickType, List<String>> getActions() {
        return actions;
    }

    public List<String> getActions(ClickType type) {
        return actions.getOrDefault(type, java.util.Collections.emptyList());
    }

    public void addAction(ClickType type, String action) {
        actions.computeIfAbsent(type, k -> new ArrayList<>()).add(action);
    }

    public boolean hasAnyAction() {
        for (List<String> list : actions.values()) {
            if (!list.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
