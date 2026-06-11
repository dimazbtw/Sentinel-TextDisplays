package github.dimazbtw.displays.gui;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.display.ClickType;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.Page;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/** Edit the click actions of one page, one click type at a time. */
public class ActionsMenu extends Menu {

    private final String id;
    private final int pageIndex;
    private ClickType selected;

    public ActionsMenu(Main plugin, Player player, String id, int pageIndex, ClickType selected) {
        super(plugin, player);
        this.id = id;
        this.pageIndex = pageIndex;
        this.selected = selected == null ? ClickType.RIGHT : selected;
    }

    @Override
    protected String title() {
        return color("&bActions: &f" + id + " &7(page " + (pageIndex + 1) + ")");
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
        Page page = d.page(pageIndex);

        // Click-type selector across the top row.
        ClickType[] types = ClickType.values();
        for (int i = 0; i < types.length; i++) {
            ClickType type = types[i];
            boolean active = type == selected;
            set(i * 2, icon(active ? Material.LIME_DYE : Material.GRAY_DYE,
                            color((active ? "&a" : "&7") + type.name()),
                            color("&7Actions: &f" + page.getActions(type).size()),
                            color(active ? "&aSelected" : "&eClick to select")),
                    event -> {
                        selected = type;
                        refresh();
                    });
        }

        List<String> actions = page.getActions(selected);
        for (int i = 0; i < actions.size() && i < 36; i++) {
            final int index = i;
            set(9 + i, icon(Material.PAPER,
                            color("&f#" + (i + 1) + " &7" + actions.get(i)),
                            color("&eRight-click &7to remove")),
                    event -> {
                        if (event.isRightClick()) {
                            page.getActions().get(selected).remove(index);
                            apply(d);
                            refresh();
                        }
                    });
        }

        set(45, icon(Material.LIME_DYE, color("&a&lAdd " + selected.name() + " action"),
                        color("&7Format: &fTYPE:data"),
                        color("&7e.g. &fMESSAGE:&aHello %player_name%"),
                        color("&7TYPE: MESSAGE, COMMAND, CONSOLE, CONNECT,"),
                        color("&7TELEPORT, SOUND, PERMISSION, NEXT_PAGE, ...")),
                event -> {
                    final ClickType type = selected;
                    plugin.getChatInput().request(player, "&aType the action (TYPE:data):", input -> {
                        page.addAction(type, input);
                        if (d.getClickWidth() <= 0f || d.getClickHeight() <= 0f) {
                            d.setClickBox((float) plugin.getConfigManager().getDefaultClickWidth(),
                                    (float) plugin.getConfigManager().getDefaultClickHeight());
                        }
                        apply(d);
                        new ActionsMenu(plugin, player, id, pageIndex, type).open();
                    });
                });

        set(49, icon(Material.ARROW, color("&eBack")),
                e -> new PagesMenu(plugin, player, id).open());
        fillEmpty();
    }
}
