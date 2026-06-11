package github.dimazbtw.displays.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a formatted string into an Adventure {@link Component} using only
 * adventure-api (which is bundled inside PacketEvents) - no MiniMessage dependency.
 *
 * <p>Supported syntax:</p>
 * <ul>
 *   <li>Legacy codes with {@code &} or {@code §}: {@code &0-&9}, {@code &a-&f},
 *       {@code &k &l &m &n &o} (format), {@code &r} (reset).</li>
 *   <li>HEX colors: {@code &#RRGGBB} and {@code <#RRGGBB>}.</li>
 *   <li>Simple gradients: {@code <gradient:#RRGGBB:#RRGGBB>text</gradient>}.</li>
 * </ul>
 *
 * <p>Unlike vanilla, a color code does NOT clear active formatting - only
 * {@code &r} resets. This is the friendlier, modern behaviour (so {@code &l&cText}
 * renders bold red).</p>
 */
public final class ColorTranslator {

    private static final Pattern GRADIENT =
            Pattern.compile("(?i)<gradient:#([0-9a-f]{6}):#([0-9a-f]{6})>(.*?)</gradient>");
    private static final Pattern HEX_TAG = Pattern.compile("(?i)^<#([0-9a-f]{6})>");
    private static final Pattern HEX_AMP = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern HEX_ANGLE = Pattern.compile("(?i)<#([0-9a-f]{6})>");
    private static final Pattern LEGACY_AMP = Pattern.compile("(?i)&([0-9a-fk-or])");

    private ColorTranslator() {
    }

    public static Component toComponent(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return parse(applyGradients(input));
    }

    /**
     * Translates {@code &}-codes, {@code &#RRGGBB} / {@code <#RRGGBB>} hex and gradients
     * into a legacy {@code §}-string. Used for click-action chat/title/actionbar output,
     * which the Bukkit API consumes as legacy strings rather than components.
     */
    public static String toLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String s = applyGradients(input);
        s = HEX_AMP.matcher(s).replaceAll(m -> sectionHex(m.group(1)));
        s = HEX_ANGLE.matcher(s).replaceAll(m -> sectionHex(m.group(1)));
        s = LEGACY_AMP.matcher(s).replaceAll("§$1");
        return s;
    }

    private static String sectionHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (int i = 0; i < 6; i++) {
            sb.append('§').append(Character.toLowerCase(hex.charAt(i)));
        }
        return sb.toString();
    }

    // ---- gradients -> per-character &#RRGGBB codes ----

    private static String applyGradients(String input) {
        if (input.indexOf('<') < 0) {
            return input;
        }
        Matcher m = GRADIENT.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try {
                int start = Integer.parseInt(m.group(1), 16);
                int end = Integer.parseInt(m.group(2), 16);
                m.appendReplacement(sb, Matcher.quoteReplacement(gradient(m.group(3), start, end)));
            } catch (Exception ex) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(3)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String gradient(String content, int start, int end) {
        int len = content.length();
        if (len == 0) {
            return content;
        }
        int sr = (start >> 16) & 0xFF, sg = (start >> 8) & 0xFF, sb = start & 0xFF;
        int er = (end >> 16) & 0xFF, eg = (end >> 8) & 0xFF, eb = end & 0xFF;
        int n = Math.max(1, len - 1);
        StringBuilder out = new StringBuilder(len * 9);
        for (int i = 0; i < len; i++) {
            double t = (double) i / n;
            int r = (int) Math.round(sr + (er - sr) * t);
            int g = (int) Math.round(sg + (eg - sg) * t);
            int b = (int) Math.round(sb + (eb - sb) * t);
            out.append(String.format("&#%02x%02x%02x", r, g, b)).append(content.charAt(i));
        }
        return out.toString();
    }

    // ---- legacy + hex walker ----

    private static Component parse(String text) {
        TextComponent.Builder root = Component.text();
        StringBuilder buf = new StringBuilder();
        TextColor color = null;
        EnumSet<TextDecoration> decos = EnumSet.noneOf(TextDecoration.class);

        int i = 0;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i);

            if (c == '&' || c == '§') {
                if (i + 1 < len) {
                    char n = Character.toLowerCase(text.charAt(i + 1));
                    if (n == '#' && i + 8 <= len && isHex(text, i + 2, 6)) {
                        color = flushAndColor(root, buf, color, decos, hex(text, i + 2));
                        i += 8;
                        continue;
                    }
                    TextColor named = legacyColor(n);
                    if (named != null) {
                        color = flushAndColor(root, buf, color, decos, named);
                        i += 2;
                        continue;
                    }
                    TextDecoration deco = legacyDeco(n);
                    if (deco != null) {
                        flush(root, buf, color, decos);
                        decos.add(deco);
                        i += 2;
                        continue;
                    }
                    if (n == 'r') {
                        flush(root, buf, color, decos);
                        color = null;
                        decos.clear();
                        i += 2;
                        continue;
                    }
                }
                buf.append(c);
                i++;
            } else if (c == '<') {
                Matcher hm = HEX_TAG.matcher(text.substring(i, Math.min(len, i + 10)));
                if (hm.find()) {
                    color = flushAndColor(root, buf, color, decos, TextColor.color(Integer.parseInt(hm.group(1), 16)));
                    i += hm.end();
                } else {
                    buf.append(c);
                    i++;
                }
            } else {
                buf.append(c);
                i++;
            }
        }
        flush(root, buf, color, decos);
        return root.build();
    }

    private static TextColor flushAndColor(TextComponent.Builder root, StringBuilder buf, TextColor current,
                                           EnumSet<TextDecoration> decos, TextColor next) {
        flush(root, buf, current, decos);
        return next;
    }

    private static void flush(TextComponent.Builder root, StringBuilder buf, TextColor color,
                              EnumSet<TextDecoration> decos) {
        if (buf.length() == 0) {
            return;
        }
        Style.Builder s = Style.style();
        if (color != null) {
            s.color(color);
        }
        for (TextDecoration d : decos) {
            s.decorate(d);
        }
        root.append(Component.text(buf.toString(), s.build()));
        buf.setLength(0);
    }

    private static boolean isHex(String s, int start, int count) {
        for (int i = start; i < start + count; i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    private static TextColor hex(String s, int start) {
        return TextColor.color(Integer.parseInt(s.substring(start, start + 6), 16));
    }

    private static TextColor legacyColor(char c) {
        switch (c) {
            case '0': return NamedTextColor.BLACK;
            case '1': return NamedTextColor.DARK_BLUE;
            case '2': return NamedTextColor.DARK_GREEN;
            case '3': return NamedTextColor.DARK_AQUA;
            case '4': return NamedTextColor.DARK_RED;
            case '5': return NamedTextColor.DARK_PURPLE;
            case '6': return NamedTextColor.GOLD;
            case '7': return NamedTextColor.GRAY;
            case '8': return NamedTextColor.DARK_GRAY;
            case '9': return NamedTextColor.BLUE;
            case 'a': return NamedTextColor.GREEN;
            case 'b': return NamedTextColor.AQUA;
            case 'c': return NamedTextColor.RED;
            case 'd': return NamedTextColor.LIGHT_PURPLE;
            case 'e': return NamedTextColor.YELLOW;
            case 'f': return NamedTextColor.WHITE;
            default: return null;
        }
    }

    private static TextDecoration legacyDeco(char c) {
        switch (c) {
            case 'k': return TextDecoration.OBFUSCATED;
            case 'l': return TextDecoration.BOLD;
            case 'm': return TextDecoration.STRIKETHROUGH;
            case 'n': return TextDecoration.UNDERLINED;
            case 'o': return TextDecoration.ITALIC;
            default: return null;
        }
    }
}
