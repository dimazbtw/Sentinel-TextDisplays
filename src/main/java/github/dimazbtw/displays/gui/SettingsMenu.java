package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.Display;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;

/** Behaviour options: ranges, update interval, down-origin, facing, view permission, click hitbox. */
public class SettingsMenu extends Menu {

    private final String id;

    public SettingsMenu(Main plugin, Player player, String id) {
        super(plugin, player);
        this.id = id;
    }

    @Override
    protected String title() {
        return color("&bSettings: &f" + id);
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

        set(10, numberItem(Material.SPYGLASS, color("&eDisplay range"),
                trim(d.getDisplayRange()) + " blocks", 1, 10),
                e -> { d.setDisplayRange(adjust(e, d.getDisplayRange(), 1, 10, 1, 256)); apply(d); refresh(); });

        set(11, numberItem(Material.ENDER_EYE, color("&eUpdate range"),
                trim(d.getUpdateRange()) + " blocks", 1, 10),
                e -> { d.setUpdateRange(adjust(e, d.getUpdateRange(), 1, 10, 1, 256)); apply(d); refresh(); });

        set(12, numberItem(Material.CLOCK, color("&eUpdate interval"),
                d.getUpdateInterval() + " ticks", 1, 10),
                e -> { d.setUpdateInterval((int) Math.round(adjust(e, d.getUpdateInterval(), 1, 10, 1, 1200))); apply(d); refresh(); });

        set(14, icon(d.isDownOrigin() ? Material.LIME_DYE : Material.GRAY_DYE,
                        color("&eDown-origin: " + (d.isDownOrigin() ? "&aon" : "&7off")),
                        color("&7Anchor at the bottom instead of top"),
                        color("&eClick to toggle")),
                e -> { d.setDownOrigin(!d.isDownOrigin()); apply(d); refresh(); });

        set(15, numberItem(Material.COMPASS, color("&eFacing"),
                String.format(Locale.ROOT, "%.0f", d.getFacing()) + "°", 15, 45),
                e -> {
                    double delta = e.isShiftClick() ? 45 : 15;
                    double v = d.getFacing() + (e.isLeftClick() ? delta : -delta);
                    v = ((v % 360) + 360) % 360;
                    d.setFacing((float) v);
                    apply(d);
                    refresh();
                });

        set(16, icon(Material.NAME_TAG, color("&eView permission"),
                        color("&7Current: &f" + (d.getViewPermission().isEmpty() ? "none" : d.getViewPermission())),
                        "",
                        color("&eLeft-click &7to set   &eShift-click &7to clear")),
                e -> {
                    if (e.isShiftClick()) {
                        d.setViewPermission("");
                        apply(d);
                        refresh();
                    } else {
                        plugin.getChatInput().request(player, "&aType the permission node:", node -> {
                            d.setViewPermission(node.trim());
                            apply(d);
                            new SettingsMenu(plugin, player, id).open();
                        });
                    }
                });

        set(19, numberItem(Material.OAK_BUTTON, color("&eClick hitbox width"),
                String.format(Locale.ROOT, "%.1f", d.getClickWidth()), 0.5, 1),
                e -> { d.setClickBox(round1(adjust(e, d.getClickWidth(), 0.5, 1, 0, 16)), d.getClickHeight()); apply(d); refresh(); });

        set(20, numberItem(Material.STONE_BUTTON, color("&eClick hitbox height"),
                String.format(Locale.ROOT, "%.1f", d.getClickHeight()), 0.5, 1),
                e -> { d.setClickBox(d.getClickWidth(), round1(adjust(e, d.getClickHeight(), 0.5, 1, 0, 16))); apply(d); refresh(); });

        set(40, icon(Material.ARROW, color("&eBack")),
                e -> new DisplayEditMenu(plugin, player, id).open());
        fillEmpty();
    }

    private static float round1(double v) {
        return (float) (Math.round(v * 10.0) / 10.0);
    }
}
