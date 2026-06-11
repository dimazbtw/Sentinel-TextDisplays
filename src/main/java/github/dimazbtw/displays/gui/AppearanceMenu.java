package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.BillboardMode;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.TextAlignment;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Visual options: scale, billboard, alignment, background, opacity, line width, shadow, see-through, brightness. */
public class AppearanceMenu extends Menu {

    private final String id;

    public AppearanceMenu(Main plugin, Player player, String id) {
        super(plugin, player);
        this.id = id;
    }

    @Override
    protected String title() {
        return color("&bAppearance: &f" + id);
    }

    @Override
    protected int size() {
        return 45;
    }

    @Override
    protected void build() {
        Display d = plugin.getDisplayManager().get(id);
        if (d == null) {
            set(40, icon(Material.ARROW, color("&eBack")), e -> new DisplayListMenu(plugin, player, 0).open());
            fillEmpty();
            return;
        }

        set(10, numberItem(Material.SLIME_BALL, color("&eScale X"),
                String.format(Locale.ROOT, "%.2f", d.getScaleX()), 0.1, 0.5),
                e -> { float v = round2(adjust(e, d.getScaleX(), 0.1, 0.5, 0.05, 10));
                    d.setScale(v, d.getScaleY(), d.getScaleZ()); apply(d); refresh(); });
        set(11, numberItem(Material.SLIME_BALL, color("&eScale Y"),
                String.format(Locale.ROOT, "%.2f", d.getScaleY()), 0.1, 0.5),
                e -> { float v = round2(adjust(e, d.getScaleY(), 0.1, 0.5, 0.05, 10));
                    d.setScale(d.getScaleX(), v, d.getScaleZ()); apply(d); refresh(); });
        set(12, numberItem(Material.SLIME_BALL, color("&eScale Z"),
                String.format(Locale.ROOT, "%.2f", d.getScaleZ()), 0.1, 0.5),
                e -> { float v = round2(adjust(e, d.getScaleZ(), 0.1, 0.5, 0.05, 10));
                    d.setScale(d.getScaleX(), d.getScaleY(), v); apply(d); refresh(); });

        set(14, icon(Material.COMPASS, color("&eBillboard: &f" + d.getBillboard()),
                        color("&7How it rotates to face players"),
                        color("&eClick to cycle")),
                e -> {
                    BillboardMode[] vals = BillboardMode.values();
                    d.setBillboard(vals[(d.getBillboard().ordinal() + 1) % vals.length]);
                    apply(d);
                    refresh();
                });

        set(15, icon(Material.LECTERN, color("&eAlignment: &f" + d.getAlignment()),
                        color("&7Multi-line text alignment"),
                        color("&eClick to cycle")),
                e -> {
                    TextAlignment[] vals = TextAlignment.values();
                    d.setAlignment(vals[(d.getAlignment().ordinal() + 1) % vals.length]);
                    apply(d);
                    refresh();
                });

        set(16, icon(Material.PAINTING, color("&eBackground: &f" + d.backgroundSpec()),
                        color("&7Cycles default / transparent / custom"),
                        color("&eClick to change")),
                e -> {
                    if (d.isDefaultBackground()) {
                        d.setBackgroundSpec("transparent");
                        apply(d);
                        refresh();
                    } else if (((d.getBackgroundArgb() >> 24) & 0xFF) == 0) {
                        plugin.getChatInput().request(player, "&aType a background hex (e.g. #80000000 or #FF5555):", hex -> {
                            d.setBackgroundSpec(hex.trim());
                            apply(d);
                            new AppearanceMenu(plugin, player, id).open();
                        });
                    } else {
                        d.setBackgroundSpec("default");
                        apply(d);
                        refresh();
                    }
                });

        set(19, numberItem(Material.GLASS, color("&eText opacity"),
                String.valueOf(d.getTextOpacity()), 5, 25),
                e -> { d.setTextOpacity((int) Math.round(adjust(e, d.getTextOpacity(), 5, 25, 0, 255)));
                    apply(d); refresh(); });

        set(20, numberItem(Material.STRING, color("&eLine width"),
                String.valueOf(d.getLineWidth()), 10, 50),
                e -> { d.setLineWidth((int) Math.round(adjust(e, d.getLineWidth(), 10, 50, 1, 4000)));
                    apply(d); refresh(); });

        set(21, icon(d.isShadow() ? Material.LIME_DYE : Material.GRAY_DYE,
                        color("&eText shadow: " + (d.isShadow() ? "&aon" : "&7off")),
                        color("&eClick to toggle")),
                e -> { d.setShadow(!d.isShadow()); apply(d); refresh(); });

        set(22, icon(d.isSeeThrough() ? Material.LIME_DYE : Material.GRAY_DYE,
                        color("&eSee-through: " + (d.isSeeThrough() ? "&aon" : "&7off")),
                        color("&7Visible through blocks"),
                        color("&eClick to toggle")),
                e -> { d.setSeeThrough(!d.isSeeThrough()); apply(d); refresh(); });

        set(23, numberItem(Material.GLOWSTONE_DUST, color("&eBrightness (block)"),
                d.getBrightnessBlock() < 0 ? "off" : String.valueOf(d.getBrightnessBlock()), 1, 1),
                e -> { d.setBrightness((int) Math.round(adjust(e, d.getBrightnessBlock(), 1, 1, -1, 15)), d.getBrightnessSky());
                    apply(d); refresh(); });
        set(24, numberItem(Material.GLOWSTONE, color("&eBrightness (sky)"),
                d.getBrightnessSky() < 0 ? "off" : String.valueOf(d.getBrightnessSky()), 1, 1),
                e -> { d.setBrightness(d.getBrightnessBlock(), (int) Math.round(adjust(e, d.getBrightnessSky(), 1, 1, -1, 15)));
                    apply(d); refresh(); });

        set(40, icon(Material.ARROW, color("&eBack")),
                e -> new DisplayEditMenu(plugin, player, id).open());
        fillEmpty();
    }

    private static float round2(double v) {
        return (float) (Math.round(v * 100.0) / 100.0);
    }
}
