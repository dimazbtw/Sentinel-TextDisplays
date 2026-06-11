package github.dimazbtw.displays.text;

import github.dimazbtw.displays.text.hooks.Hooks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns raw configured text into a per-player Adventure {@link Component}.
 *
 * <p>Resolution order (each stage is a no-op when its plugin is absent):
 * PlaceholderAPI -&gt; ItemsAdder -&gt; Nexo -&gt; {@link ColorTranslator}.</p>
 *
 * <p>Lines are resolved once via {@link #resolveLines}; the caller then derives both
 * the component ({@link #componentOf}) and the change-detection key ({@link #join})
 * from that single result, so PlaceholderAPI is never called twice for the same render.</p>
 */
public final class TextPipeline {

    private final Hooks hooks;

    public TextPipeline(Hooks hooks) {
        this.hooks = hooks;
    }

    /** Runs the placeholder/glyph stages for a single line (no color parsing). */
    public String resolve(Player viewer, String raw) {
        if (raw == null) {
            return "";
        }
        String s = hooks.placeholders().apply(viewer, raw);
        s = hooks.itemsAdder().apply(s);
        s = hooks.nexo().apply(s);
        return s;
    }

    /** Resolves every line for a viewer (placeholders + glyphs, no color parsing yet). */
    public List<String> resolveLines(Player viewer, List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(resolve(viewer, line));
        }
        return out;
    }

    /** Builds a multi-line component from already-resolved lines. */
    public static Component componentOf(List<String> resolvedLines) {
        if (resolvedLines == null || resolvedLines.isEmpty()) {
            return Component.empty();
        }
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < resolvedLines.size(); i++) {
            if (i > 0) {
                builder.append(Component.newline());
            }
            builder.append(ColorTranslator.toComponent(resolvedLines.get(i)));
        }
        return builder.build();
    }

    /** Change-detection key for already-resolved lines. */
    public static String join(List<String> resolvedLines) {
        return String.join("\n", resolvedLines);
    }

    /** Convenience: resolve + build component in one call. */
    public Component toComponent(Player viewer, List<String> lines) {
        return componentOf(resolveLines(viewer, lines));
    }
}
