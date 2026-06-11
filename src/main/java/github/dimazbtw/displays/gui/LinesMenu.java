package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.Display;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Editor for the text lines of one page. */
public class LinesMenu extends Menu {

    private final String id;
    private final int pageIndex;

    public LinesMenu(Main plugin, Player player, String id, int pageIndex) {
        super(plugin, player);
        this.id = id;
        this.pageIndex = pageIndex;
    }

    @Override
    protected String title() {
        return color("&bLines: &f" + id + " &7(page " + (pageIndex + 1) + ")");
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
        List<String> lines = d.page(pageIndex).getLines();

        for (int i = 0; i < lines.size() && i < 45; i++) {
            final int index = i;
            String raw = lines.get(i);
            set(i, icon(Material.PAPER,
                            color("&f#" + (i + 1) + " &r" + (raw.isEmpty() ? "&7(empty)" : raw)),
                            color("&eLeft-click &7to edit"),
                            color("&eRight-click &7to remove"),
                            color("&eShift-left &7to insert a line after")),
                    event -> {
                        if (event.isRightClick()) {
                            lines.remove(index);
                            apply(d);
                            refresh();
                        } else if (event.isShiftClick()) {
                            plugin.getChatInput().request(player, "&aType the text to insert after line " + (index + 1) + ":", text -> {
                                lines.add(index + 1, text);
                                apply(d);
                                new LinesMenu(plugin, player, id, pageIndex).open();
                            });
                        } else {
                            plugin.getChatInput().request(player, "&aType the new text for line " + (index + 1) + ":", text -> {
                                lines.set(index, text);
                                apply(d);
                                new LinesMenu(plugin, player, id, pageIndex).open();
                            });
                        }
                    });
        }

        set(45, icon(Material.LIME_DYE, color("&a&lAdd line"),
                        color("&7Add a new line of text")),
                event -> plugin.getChatInput().request(player, "&aType the text for the new line:", text -> {
                    lines.add(text);
                    apply(d);
                    new LinesMenu(plugin, player, id, pageIndex).open();
                }));

        if (d.pageCount() > 1) {
            if (pageIndex > 0) {
                set(47, icon(Material.ARROW, color("&ePrevious page")),
                        e -> new LinesMenu(plugin, player, id, pageIndex - 1).open());
            }
            set(48, icon(Material.PAPER, color("&7Page &f" + (pageIndex + 1) + "&7/&f" + d.pageCount())));
            if (pageIndex < d.pageCount() - 1) {
                set(51, icon(Material.ARROW, color("&eNext page")),
                        e -> new LinesMenu(plugin, player, id, pageIndex + 1).open());
            }
        }

        set(49, icon(Material.ARROW, color("&eBack")),
                e -> new DisplayEditMenu(plugin, player, id).open());
        fillEmpty();
    }
}
