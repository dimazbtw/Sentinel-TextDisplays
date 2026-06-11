package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.Page;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/** Manage a display's pages (add / remove / reorder) and open lines or actions per page. */
public class PagesMenu extends Menu {

    private final String id;

    public PagesMenu(Main plugin, Player player, String id) {
        super(plugin, player);
        this.id = id;
    }

    @Override
    protected String title() {
        return color("&bPages: &f" + id);
    }

    @Override
    protected int size() {
        return 54;
    }

    @Override
    protected void build() {
        Display d = plugin.getDisplayManager().get(id);
        if (d == null) {
            set(49, icon(Material.ARROW, color("&eBack")), e -> new DisplayListMenu(plugin, player, 0).open());
            fillEmpty();
            return;
        }
        List<Page> pages = d.getPages();

        for (int i = 0; i < pages.size() && i < 45; i++) {
            final int index = i;
            Page page = pages.get(i);
            set(i, icon(Material.BOOK,
                            color("&f&lPage " + (i + 1)),
                            color("&7Lines: &f" + page.getLines().size()),
                            color("&7Actions: &f" + (page.hasAnyAction() ? "yes" : "none")),
                            "",
                            color("&eLeft-click &7edit lines"),
                            color("&eRight-click &7edit click actions"),
                            color("&eShift-left &7move up   &eShift-right &7remove")),
                    event -> {
                        if (event.isShiftClick() && event.isRightClick()) {
                            if (pages.size() <= 1) {
                                player.sendMessage(plugin.getConfigManager().getPrefix()
                                        + color("&cA display must keep at least one page."));
                                return;
                            }
                            pages.remove(index);
                            apply(d);
                            refresh();
                        } else if (event.isShiftClick()) {
                            if (index > 0) {
                                Collections.swap(pages, index, index - 1);
                                apply(d);
                                refresh();
                            }
                        } else if (event.isRightClick()) {
                            new ActionsMenu(plugin, player, id, index, null).open();
                        } else {
                            new LinesMenu(plugin, player, id, index).open();
                        }
                    });
        }

        set(45, icon(Material.LIME_DYE, color("&a&lAdd page"),
                        color("&7Append a new empty page")),
                event -> {
                    pages.add(new Page());
                    apply(d);
                    refresh();
                });

        set(49, icon(Material.ARROW, color("&eBack")),
                e -> new DisplayEditMenu(plugin, player, id).open());
        fillEmpty();
    }
}
