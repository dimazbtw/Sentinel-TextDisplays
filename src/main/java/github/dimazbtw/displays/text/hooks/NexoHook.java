package github.dimazbtw.displays.text.hooks;

import org.bukkit.Bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

/**
 * Soft hook for Nexo (the successor to Oraxen).
 *
 * <p>The guaranteed way to render Nexo glyphs is through Nexo's PlaceholderAPI
 * expansion ({@code %nexo_<glyph>%}), which the {@link github.dimazbtw.displays.text.TextPipeline}
 * already resolves in its PlaceholderAPI stage. In addition, this hook tries to bind
 * a direct {@code String -> String} glyph parser by <em>scanning</em> Nexo's font API
 * reflectively, so it keeps working across Nexo's (Kotlin) API changes. If nothing
 * suitable is found it simply no-ops and the PlaceholderAPI route is used.</p>
 */
public final class NexoHook {

    private static final List<String> CANDIDATE_CLASSES = Arrays.asList(
            "com.nexomc.nexo.api.NexoFonts",
            "com.nexomc.nexo.fonts.FontManager"
    );
    private static final List<String> CANDIDATE_METHODS = Arrays.asList(
            "parseGlyphs", "replaceGlyphs", "unparsed", "parseToString"
    );

    private boolean enabled;
    private Method method;
    private Object instance; // null for static methods

    public void init() {
        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) {
            return;
        }
        enabled = true; // Nexo present: PlaceholderAPI route works regardless
        for (String className : CANDIDATE_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                for (Method m : clazz.getMethods()) {
                    if (m.getReturnType() == String.class
                            && m.getParameterCount() == 1
                            && m.getParameterTypes()[0] == String.class
                            && CANDIDATE_METHODS.contains(m.getName())) {
                        this.method = m;
                        if (!Modifier.isStatic(m.getModifiers())) {
                            this.instance = resolveInstance(clazz);
                            if (this.instance == null) {
                                this.method = null;
                                continue;
                            }
                        }
                        return;
                    }
                }
            } catch (Throwable ignored) {
                // try next candidate
            }
        }
    }

    /** Kotlin objects expose a singleton via an {@code INSTANCE} field. */
    private Object resolveInstance(Class<?> clazz) {
        try {
            Field instanceField = clazz.getField("INSTANCE");
            return instanceField.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Best-effort direct glyph parsing. Returns input unchanged when unavailable. */
    public String apply(String text) {
        if (!enabled || method == null || text == null) {
            return text;
        }
        try {
            Object result = method.invoke(instance, text);
            return result != null ? result.toString() : text;
        } catch (Throwable ignored) {
            return text;
        }
    }
}
