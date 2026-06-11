package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Paginated list of displays, with a "create" button and reload. */
public class DisplayListMenu extends Menu {

    private static final int PAGE_SIZE = 45;
    private final int page;

    public DisplayListMenu(Main plugin, Player player, int page) {
        super(plugin, player);
        this.page = Math.max(0, page);
    }

    @Override
    protected String title() {
        return color("&b&lText Displays");
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected void build() {
        List<Display> all = new ArrayList<>(plugin.getDisplayManager().all());
        int maxPage = Math.max(0, (all.size() - 1) / PAGE_SIZE);
        int current = Math.min(page, maxPage);
        int from = current * PAGE_SIZE;
        int to = Math.min(all.size(), from + PAGE_SIZE);

        for (int i = from; i < to; i++) {
            Display d = all.get(i);
            set(i - from, icon(Material.PAPER,
                            color((d.isEnabled() ? "&a" : "&c") + d.getId()),
                            color("&7World: &f" + d.getWorld()),
                            color("&7Lines: &f" + d.getLines().size() + "   &7Pages: &f" + d.pageCount()),
                            color("&7Status: " + (d.isEnabled() ? "&aenabled" : "&cdisabled")),
                            "",
                            color("&eLeft-click &7to manage"),
                            color("&eRight-click &7to enable/disable")),
                    event -> {
                        if (event.isRightClick()) {
                            d.setEnabled(!d.isEnabled());
                            apply(d);
                            refresh();
                        } else {
                            new DisplayEditMenu(plugin, player, d.getId()).open();
                        }
                    });
        }

        set(45, icon(Material.EMERALD, color("&a&lCreate display"),
                        color("&7Create a new display where"),
                        color("&7you are standing.")),
                event -> plugin.getChatInput().request(player, "&aType a name for the new display:", name -> {
                    String id = name.trim().toLowerCase(Locale.ROOT);
                    if (!DisplayManager.isValidId(id)) {
                        player.sendMessage(plugin.getConfigManager().getMessage("invalid-id"));
                        new DisplayListMenu(plugin, player, page).open();
                        return;
                    }
                    if (plugin.getDisplayManager().exists(id)) {
                        player.sendMessage(plugin.getConfigManager().getMessage("already-exists").replace("%id%", id));
                        new DisplayListMenu(plugin, player, page).open();
                        return;
                    }
                    plugin.createDisplay(id, player.getLocation());
                    new DisplayEditMenu(plugin, player, id).open();
                }));

        if (current > 0) {
            set(48, icon(Material.ARROW, color("&ePrevious page")),
                    event -> new DisplayListMenu(plugin, player, current - 1).open());
        }
        set(49, icon(Material.BOOK, color("&bSentinel-TextDisplays"),
                        color("&7Total displays: &f" + all.size()),
                        "",
                        color("&eClick to reload")),
                event -> {
                    plugin.reloadEverything();
                    refresh();
                });
        if (current < maxPage) {
            set(50, icon(Material.ARROW, color("&eNext page")),
                    event -> new DisplayListMenu(plugin, player, current + 1).open());
        }
        fillEmpty();
    }
}
