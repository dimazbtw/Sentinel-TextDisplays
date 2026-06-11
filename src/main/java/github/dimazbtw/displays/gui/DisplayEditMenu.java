package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.Display;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Per-display hub: toggle, lines, pages, appearance, settings, teleport, move, delete. */
public class DisplayEditMenu extends Menu {

    private final String id;

    public DisplayEditMenu(Main plugin, Player player, String id) {
        super(plugin, player);
        this.id = id;
    }

    @Override
    protected String title() {
        return color("&bEdit: &f" + id);
    }

    @Override
    protected int size() {
        return 36;
    }

    @Override
    protected void build() {
        Display d = plugin.getDisplayManager().get(id);
        if (d == null) {
            set(13, icon(Material.BARRIER, color("&cThis display no longer exists.")));
            set(31, icon(Material.ARROW, color("&eBack")), e -> new DisplayListMenu(plugin, player, 0).open());
            fillEmpty();
            return;
        }

        set(4, icon(Material.PAPER, color("&b&l" + d.getId()),
                color("&7World: &f" + d.getWorld()),
                color("&7Location: &f" + (int) d.getX() + ", " + (int) d.getY() + ", " + (int) d.getZ()),
                color("&7Pages: &f" + d.pageCount() + "   &7Lines: &f" + d.getLines().size()),
                color("&7Status: " + (d.isEnabled() ? "&aenabled" : "&cdisabled"))));

        set(10, icon(d.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                        d.isEnabled() ? color("&aEnabled") : color("&cDisabled"),
                        color("&7Click to toggle visibility")),
                e -> {
                    d.setEnabled(!d.isEnabled());
                    apply(d);
                    refresh();
                });

        set(11, icon(Material.WRITABLE_BOOK, color("&eLines"),
                        color("&7Edit the text lines"),
                        color("&7Lines: &f" + d.getLines().size())),
                e -> new LinesMenu(plugin, player, id, 0).open());

        set(12, icon(Material.BOOK, color("&ePages & actions"),
                        color("&7Manage pages and click actions"),
                        color("&7Pages: &f" + d.pageCount())),
                e -> new PagesMenu(plugin, player, id).open());

        set(13, icon(Material.GLOWSTONE_DUST, color("&eAppearance"),
                        color("&7Size, billboard, background,"),
                        color("&7opacity, alignment, brightness")),
                e -> new AppearanceMenu(plugin, player, id).open());

        set(14, icon(Material.COMPARATOR, color("&eSettings"),
                        color("&7Ranges, update interval,"),
                        color("&7facing, permission, hitbox")),
                e -> new SettingsMenu(plugin, player, id).open());

        set(15, icon(Material.ENDER_PEARL, color("&eTeleport"),
                        color("&7Teleport to this display")),
                e -> {
                    Location loc = d.getBukkitLocation();
                    if (loc != null) {
                        player.teleport(loc);
                    }
                    player.closeInventory();
                });

        set(16, icon(Material.COMPASS, color("&eMove here"),
                        color("&7Move it to your location")),
                e -> {
                    d.setLocation(player.getLocation());
                    apply(d);
                    refresh();
                });

        set(31, icon(Material.BARRIER, color("&c&lDelete"),
                        color("&7Shift-click to confirm")),
                e -> {
                    if (e.isShiftClick()) {
                        plugin.getViewerTracker().removeDisplay(d);
                        plugin.getDisplayManager().delete(d.getId());
                        player.closeInventory();
                        player.sendMessage(plugin.getConfigManager().getMessage("deleted").replace("%id%", d.getId()));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getPrefix()
                                + color("&cShift-click the barrier to confirm deletion."));
                    }
                });

        set(27, icon(Material.ARROW, color("&eBack to list")),
                e -> new DisplayListMenu(plugin, player, 0).open());

        fillEmpty();
    }
}
