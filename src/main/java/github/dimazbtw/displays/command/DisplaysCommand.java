package github.dimazbtw.displays.command;

import github.dimazbtw.displays.Main;
import github.dimazbtw.displays.config.ConfigManager;
import github.dimazbtw.displays.display.BillboardMode;
import github.dimazbtw.displays.display.ClickType;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import github.dimazbtw.displays.display.Page;
import github.dimazbtw.displays.display.TextAlignment;
import github.dimazbtw.displays.gui.DisplayEditMenu;
import github.dimazbtw.displays.gui.DisplayListMenu;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Command tree modelled on DecentHolograms: top-level hologram shortcuts plus the
 * {@code line} and {@code page} groups, on the base command {@code /td}.
 */
public class DisplaysCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION = "displays.admin";
    private static final Pattern NEWLINE = Pattern.compile(Pattern.quote("\\n"));

    private static final List<String> SUBS = Arrays.asList(
            "create", "delete", "clone", "enable", "disable", "info", "list", "movehere", "move",
            "teleport", "center", "rename", "facing", "nearby", "line", "page", "scale", "billboard",
            "background", "opacity", "linewidth", "shadow", "seethrough", "alignment", "brightness",
            "displayrange", "updaterange", "updateinterval", "downorigin", "viewpermission", "clickbox",
            "edit", "gui", "reload", "version", "display", "help");
    private static final List<String> LINE_SUBS =
            Arrays.asList("add", "set", "remove", "insertbefore", "insertafter");
    private static final List<String> PAGE_SUBS =
            Arrays.asList("add", "insert", "remove", "swap", "addaction", "removeaction", "clearactions");
    private static final List<String> CLICK_TYPES =
            Arrays.asList("LEFT", "RIGHT", "SHIFT_LEFT", "SHIFT_RIGHT");

    private final Main plugin;

    public DisplaysCommand(Main plugin) {
        this.plugin = plugin;
    }

    private ConfigManager cfg() {
        return plugin.getConfigManager();
    }

    private DisplayManager displays() {
        return plugin.getDisplayManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(cfg().getMessage("no-permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create":         handleCreate(sender, args); break;
            case "delete":
            case "del":
            case "remove":         handleDelete(sender, args); break;
            case "clone":          handleClone(sender, args); break;
            case "enable":         handleToggle(sender, args, true); break;
            case "disable":        handleToggle(sender, args, false); break;
            case "info":           handleInfo(sender, args); break;
            case "list":           handleList(sender); break;
            case "movehere":       handleMoveHere(sender, args); break;
            case "move":           handleMove(sender, args); break;
            case "teleport":
            case "tp":             handleTeleport(sender, args); break;
            case "center":         handleCenter(sender, args); break;
            case "rename":         handleRename(sender, args); break;
            case "facing":         handleFacing(sender, args); break;
            case "nearby":
            case "near":           handleNearby(sender, args); break;
            case "line":           if (isHelp(args)) lineHelp(sender); else handleLine(sender, args); break;
            case "page":           if (isHelp(args)) pageHelp(sender); else handlePage(sender, args); break;
            case "display":        displayHelp(sender); break;
            case "edit":           handleEdit(sender, args); break;
            case "scale":          handleScale(sender, args); break;
            case "billboard":      handleBillboard(sender, args); break;
            case "background":     handleBackground(sender, args); break;
            case "opacity":        handleOpacity(sender, args); break;
            case "linewidth":      handleLineWidth(sender, args); break;
            case "shadow":         handleBool(sender, args, "shadow"); break;
            case "seethrough":     handleBool(sender, args, "seethrough"); break;
            case "alignment":      handleAlignment(sender, args); break;
            case "brightness":     handleBrightness(sender, args); break;
            case "displayrange":   handleRange(sender, args, "display"); break;
            case "updaterange":    handleRange(sender, args, "update"); break;
            case "updateinterval": handleUpdateInterval(sender, args); break;
            case "downorigin":     handleBool(sender, args, "downorigin"); break;
            case "viewpermission": handleViewPermission(sender, args); break;
            case "clickbox":       handleClickBox(sender, args); break;
            case "gui":            handleGui(sender); break;
            case "reload":         handleReload(sender); break;
            case "version":        prefixed(sender, "&bSentinel-TextDisplays &7v" + plugin.getDescription().getVersion()); break;
            case "help":
            default:               sendHelp(sender); break;
        }
        return true;
    }

    // ===================== Hologram-level =====================

    private void handleCreate(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            return;
        }
        if (args.length < 2) {
            prefixed(sender, "&cUsage: /td create <name> [content]");
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!DisplayManager.isValidId(id)) {
            sender.sendMessage(cfg().getMessage("invalid-id"));
            return;
        }
        if (displays().exists(id)) {
            sender.sendMessage(cfg().getMessage("already-exists").replace("%id%", id));
            return;
        }
        Display d = plugin.createDisplay(id, player.getLocation());
        if (args.length > 2) {
            d.getLines().clear();
            d.getLines().addAll(Arrays.asList(NEWLINE.split(join(args, 2), -1)));
            apply(d);
        }
        sender.sendMessage(cfg().getMessage("created").replace("%id%", id));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null) {
            return;
        }
        plugin.getViewerTracker().removeDisplay(d);
        displays().delete(d.getId());
        sender.sendMessage(cfg().getMessage("deleted").replace("%id%", d.getId()));
    }

    private void handleClone(CommandSender sender, String[] args) {
        Display src = require(sender, args);
        if (src == null) {
            return;
        }
        if (args.length < 3) {
            prefixed(sender, "&cUsage: /td clone <name> <newName>");
            return;
        }
        String newId = args[2].toLowerCase(Locale.ROOT);
        if (!DisplayManager.isValidId(newId)) {
            sender.sendMessage(cfg().getMessage("invalid-id"));
            return;
        }
        if (displays().exists(newId)) {
            sender.sendMessage(cfg().getMessage("already-exists").replace("%id%", newId));
            return;
        }
        YamlConfiguration tmp = new YamlConfiguration();
        src.toConfig(tmp);
        Display copy = Display.fromConfig(newId, tmp);
        displays().register(copy);
        plugin.getViewerTracker().updateAllPlayers();
        prefixed(sender, "&aCloned &f" + src.getId() + " &ato &f" + newId + "&a.");
    }

    private void handleToggle(CommandSender sender, String[] args, boolean enable) {
        Display d = require(sender, args);
        if (d == null) {
            return;
        }
        d.setEnabled(enable);
        apply(d);
        prefixed(sender, "&f" + d.getId() + (enable ? " &ais now enabled." : " &cis now disabled."));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null) {
            return;
        }
        sender.sendMessage(cfg().color("&8&m                                "));
        sender.sendMessage(cfg().color("&bDisplay &f" + d.getId() + (d.isEnabled() ? " &a(enabled)" : " &c(disabled)")));
        sender.sendMessage(cfg().color("&7Location: &f" + d.getWorld() + " "
                + round(d.getX()) + ", " + round(d.getY()) + ", " + round(d.getZ()) + "  &7facing &f" + round(d.getFacing())));
        sender.sendMessage(cfg().color("&7Pages: &f" + d.pageCount() + "  &7Lines (page 1): &f" + d.getLines().size()));
        sender.sendMessage(cfg().color("&7Scale: &f" + d.getScaleX() + ", " + d.getScaleY() + ", " + d.getScaleZ()
                + "  &7Billboard: &f" + d.getBillboard()));
        sender.sendMessage(cfg().color("&7Background: &f" + d.backgroundSpec() + "  &7Alignment: &f" + d.getAlignment()));
        sender.sendMessage(cfg().color("&7display-range: &f" + d.getDisplayRange() + "  &7update-range: &f" + d.getUpdateRange()
                + "  &7update-interval: &f" + d.getUpdateInterval() + "t"));
        sender.sendMessage(cfg().color("&7down-origin: &f" + d.isDownOrigin() + "  &7permission: &f"
                + (d.getViewPermission().isEmpty() ? "none" : d.getViewPermission())));
        sender.sendMessage(cfg().color("&8&m                                "));
    }

    private void handleList(CommandSender sender) {
        List<String> ids = displays().ids();
        if (ids.isEmpty()) {
            prefixed(sender, "&7No displays yet. Create one with &f/td create <name>&7.");
            return;
        }
        prefixed(sender, "&bDisplays &7(" + ids.size() + "): &f" + String.join("&7, &f", ids));
    }

    private void handleMoveHere(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        Display d = require(sender, args);
        if (player == null || d == null) {
            return;
        }
        d.setLocation(player.getLocation());
        apply(d);
        prefixed(sender, "&aMoved &f" + d.getId() + " &ahere.");
    }

    private void handleMove(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 5) {
            if (d != null) {
                prefixed(sender, "&cUsage: /td move <name> <x> <y> <z> [world]");
            }
            return;
        }
        Double x = parseDouble(sender, args[2]);
        Double y = parseDouble(sender, args[3]);
        Double z = parseDouble(sender, args[4]);
        if (x == null || y == null || z == null) {
            return;
        }
        String world = args.length > 5 ? args[5] : d.getWorld();
        if (org.bukkit.Bukkit.getWorld(world) == null) {
            prefixed(sender, "&cWorld not found: " + world);
            return;
        }
        d.setLocation(new Location(org.bukkit.Bukkit.getWorld(world), x, y, z, d.getFacing(), 0f));
        apply(d);
        prefixed(sender, "&aMoved &f" + d.getId() + " &ato &f" + round(x) + ", " + round(y) + ", " + round(z));
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        Display d = require(sender, args);
        if (player == null || d == null) {
            return;
        }
        Location loc = d.getBukkitLocation();
        if (loc == null) {
            prefixed(sender, "&cThat display's world is not loaded.");
            return;
        }
        player.teleport(loc);
        prefixed(sender, "&aTeleported to &f" + d.getId() + "&a.");
    }

    private void handleCenter(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null) {
            return;
        }
        Location loc = d.getBukkitLocation();
        if (loc == null) {
            return;
        }
        loc.setX(Math.floor(d.getX()) + 0.5);
        loc.setZ(Math.floor(d.getZ()) + 0.5);
        d.setLocation(loc);
        apply(d);
        prefixed(sender, "&aCentered &f" + d.getId() + " &aon its block.");
    }

    private void handleRename(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            if (d != null) {
                prefixed(sender, "&cUsage: /td rename <name> <newName>");
            }
            return;
        }
        String newId = args[2].toLowerCase(Locale.ROOT);
        if (!DisplayManager.isValidId(newId)) {
            sender.sendMessage(cfg().getMessage("invalid-id"));
            return;
        }
        if (displays().exists(newId)) {
            sender.sendMessage(cfg().getMessage("already-exists").replace("%id%", newId));
            return;
        }
        YamlConfiguration tmp = new YamlConfiguration();
        d.toConfig(tmp);
        Display renamed = Display.fromConfig(newId, tmp);
        plugin.getViewerTracker().removeDisplay(d);
        displays().delete(d.getId());
        displays().register(renamed);
        plugin.getViewerTracker().updateAllPlayers();
        prefixed(sender, "&aRenamed to &f" + newId + "&a.");
    }

    private void handleFacing(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        Double deg = parseDouble(sender, args[2]);
        if (deg == null) {
            return;
        }
        d.setFacing(deg.floatValue());
        apply(d);
        prefixed(sender, "&aSet facing of &f" + d.getId() + " &ato &f" + deg);
    }

    private void handleNearby(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null || args.length < 2) {
            if (player != null) {
                prefixed(sender, "&cUsage: /td nearby <range>");
            }
            return;
        }
        Double range = parseDouble(sender, args[1]);
        if (range == null) {
            return;
        }
        double sq = range * range;
        List<String> found = new ArrayList<>();
        for (Display d : displays().all()) {
            Location loc = d.getBukkitLocation();
            if (loc != null && loc.getWorld() == player.getWorld() && player.getLocation().distanceSquared(loc) <= sq) {
                found.add(d.getId());
            }
        }
        prefixed(sender, found.isEmpty()
                ? "&7No displays within " + range + " blocks."
                : "&bNearby &7(" + found.size() + "): &f" + String.join("&7, &f", found));
    }

    // ===================== Lines =====================

    private void handleLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            prefixed(sender, "&cUsage: /td line <add|set|remove|insertbefore|insertafter> <name> ...");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Display d = displays().get(args[2]);
        if (d == null) {
            sender.sendMessage(cfg().getMessage("not-found").replace("%id%", args[2]));
            return;
        }
        List<String> lines = d.getLines(); // first page
        switch (sub) {
            case "add":
                if (args.length < 4) {
                    prefixed(sender, "&cUsage: /td line add <name> <content>");
                    return;
                }
                lines.add(join(args, 3));
                break;
            case "set": {
                Integer n = lineArg(sender, args, lines, 3);
                if (n == null || args.length < 5) {
                    if (n != null) {
                        prefixed(sender, "&cUsage: /td line set <name> <line> <content>");
                    }
                    return;
                }
                lines.set(n - 1, join(args, 4));
                break;
            }
            case "remove": {
                Integer n = lineArg(sender, args, lines, 3);
                if (n == null) {
                    return;
                }
                lines.remove(n - 1);
                break;
            }
            case "insertbefore": {
                Integer n = lineArg(sender, args, lines, 3);
                if (n == null || args.length < 5) {
                    return;
                }
                lines.add(n - 1, join(args, 4));
                break;
            }
            case "insertafter": {
                Integer n = lineArg(sender, args, lines, 3);
                if (n == null || args.length < 5) {
                    return;
                }
                lines.add(n, join(args, 4));
                break;
            }
            default:
                prefixed(sender, "&cUnknown line action.");
                return;
        }
        apply(d);
        prefixed(sender, "&aUpdated lines of &f" + d.getId() + "&a.");
    }

    private Integer lineArg(CommandSender sender, String[] args, List<String> lines, int index) {
        if (args.length <= index) {
            prefixed(sender, "&cMissing line number.");
            return null;
        }
        Integer n = parseInt(sender, args[index]);
        if (n == null) {
            return null;
        }
        if (n < 1 || n > lines.size()) {
            prefixed(sender, "&cLine number out of range (1-" + lines.size() + ").");
            return null;
        }
        return n;
    }

    // ===================== Pages =====================

    private void handlePage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            prefixed(sender, "&cUsage: /td page <add|insert|remove|swap|addaction|removeaction|clearactions> <name> ...");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        Display d = displays().get(args[2]);
        if (d == null) {
            sender.sendMessage(cfg().getMessage("not-found").replace("%id%", args[2]));
            return;
        }
        List<Page> pages = d.getPages();
        switch (sub) {
            case "add":
                pages.add(new Page());
                prefixed(sender, "&aAdded page " + pages.size() + " to &f" + d.getId() + "&a.");
                break;
            case "insert": {
                Integer n = parseInt(sender, args.length > 3 ? args[3] : "");
                if (n == null) {
                    return;
                }
                pages.add(Math.max(0, Math.min(pages.size(), n - 1)), new Page());
                prefixed(sender, "&aInserted a page into &f" + d.getId() + "&a.");
                break;
            }
            case "remove": {
                Integer n = pageArg(sender, args, pages, 3);
                if (n == null) {
                    return;
                }
                if (pages.size() <= 1) {
                    prefixed(sender, "&cA display must have at least one page.");
                    return;
                }
                pages.remove(n - 1);
                prefixed(sender, "&aRemoved page " + n + " from &f" + d.getId() + "&a.");
                break;
            }
            case "swap": {
                Integer a = pageArg(sender, args, pages, 3);
                Integer b = args.length > 4 ? pageArg(sender, args, pages, 4) : null;
                if (a == null || b == null) {
                    return;
                }
                Collections.swap(pages, a - 1, b - 1);
                prefixed(sender, "&aSwapped pages " + a + " and " + b + "&a.");
                break;
            }
            case "addaction": {
                Integer n = pageArg(sender, args, pages, 3);
                if (n == null || args.length < 6) {
                    if (n != null) {
                        prefixed(sender, "&cUsage: /td page addaction <name> <page> <clickType> <TYPE:data>");
                    }
                    return;
                }
                ClickType ct = ClickType.from(args[4], null);
                if (ct == null) {
                    prefixed(sender, "&cClick type: LEFT, RIGHT, SHIFT_LEFT, SHIFT_RIGHT");
                    return;
                }
                pages.get(n - 1).addAction(ct, join(args, 5));
                if (d.getClickWidth() <= 0f || d.getClickHeight() <= 0f) {
                    d.setClickBox((float) cfg().getDefaultClickWidth(), (float) cfg().getDefaultClickHeight());
                }
                prefixed(sender, "&aAdded a " + ct + " action to page " + n + " of &f" + d.getId() + "&a.");
                break;
            }
            case "removeaction": {
                Integer n = pageArg(sender, args, pages, 3);
                if (n == null || args.length < 6) {
                    return;
                }
                ClickType ct = ClickType.from(args[4], null);
                Integer idx = parseInt(sender, args[5]);
                if (ct == null || idx == null) {
                    return;
                }
                List<String> list = pages.get(n - 1).getActions().get(ct);
                if (list == null || idx < 1 || idx > list.size()) {
                    prefixed(sender, "&cAction index out of range.");
                    return;
                }
                list.remove(idx - 1);
                prefixed(sender, "&aRemoved action " + idx + "&a.");
                break;
            }
            case "clearactions": {
                Integer n = pageArg(sender, args, pages, 3);
                if (n == null) {
                    return;
                }
                if (args.length > 4) {
                    ClickType ct = ClickType.from(args[4], null);
                    if (ct != null) {
                        pages.get(n - 1).getActions().remove(ct);
                    }
                } else {
                    pages.get(n - 1).getActions().clear();
                }
                prefixed(sender, "&aCleared actions on page " + n + "&a.");
                break;
            }
            default:
                prefixed(sender, "&cUnknown page action.");
                return;
        }
        apply(d);
    }

    private Integer pageArg(CommandSender sender, String[] args, List<Page> pages, int index) {
        if (args.length <= index) {
            prefixed(sender, "&cMissing page number.");
            return null;
        }
        Integer n = parseInt(sender, args[index]);
        if (n == null) {
            return null;
        }
        if (n < 1 || n > pages.size()) {
            prefixed(sender, "&cPage number out of range (1-" + pages.size() + ").");
            return null;
        }
        return n;
    }

    // ===================== Display options =====================

    private void handleScale(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            if (d != null) {
                prefixed(sender, "&cUsage: /td scale <name> <x> [y] [z]");
            }
            return;
        }
        Float x = parseFloat(sender, args[2]);
        if (x == null) {
            return;
        }
        if (args.length >= 5) {
            Float y = parseFloat(sender, args[3]);
            Float z = parseFloat(sender, args[4]);
            if (y == null || z == null) {
                return;
            }
            d.setScale(x, y, z);
        } else {
            d.setScale(x);
        }
        apply(d);
        prefixed(sender, "&aSet scale of &f" + d.getId() + " &ato &f"
                + d.getScaleX() + ", " + d.getScaleY() + ", " + d.getScaleZ());
    }

    private void handleBillboard(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            if (d != null) {
                prefixed(sender, "&cBillboard: FIXED, VERTICAL, HORIZONTAL, CENTER");
            }
            return;
        }
        d.setBillboard(BillboardMode.from(args[2], d.getBillboard()));
        apply(d);
        prefixed(sender, "&aSet billboard of &f" + d.getId() + " &ato &f" + d.getBillboard());
    }

    private void handleBackground(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            if (d != null) {
                prefixed(sender, "&cBackground: default, transparent, #RRGGBB or #AARRGGBB");
            }
            return;
        }
        if (!d.setBackgroundSpec(args[2])) {
            prefixed(sender, "&cInvalid background. Use: default, transparent, #RRGGBB or #AARRGGBB");
            return;
        }
        apply(d);
        prefixed(sender, "&aSet background of &f" + d.getId() + " &ato &f" + d.backgroundSpec());
    }

    private void handleOpacity(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        Integer o = parseInt(sender, args[2]);
        if (o == null) {
            return;
        }
        d.setTextOpacity(o);
        apply(d);
        prefixed(sender, "&aSet opacity of &f" + d.getId() + " &ato &f" + d.getTextOpacity());
    }

    private void handleLineWidth(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        Integer w = parseInt(sender, args[2]);
        if (w == null) {
            return;
        }
        d.setLineWidth(w);
        apply(d);
        prefixed(sender, "&aSet line width of &f" + d.getId() + " &ato &f" + d.getLineWidth());
    }

    private void handleBool(CommandSender sender, String[] args, String which) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        boolean value = args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("on");
        switch (which) {
            case "shadow": d.setShadow(value); break;
            case "seethrough": d.setSeeThrough(value); break;
            case "downorigin": d.setDownOrigin(value); break;
            default: break;
        }
        apply(d);
        prefixed(sender, "&aSet " + which + " of &f" + d.getId() + " &ato &f" + value);
    }

    private void handleAlignment(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            if (d != null) {
                prefixed(sender, "&cAlignment: LEFT, CENTER, RIGHT");
            }
            return;
        }
        d.setAlignment(TextAlignment.from(args[2], d.getAlignment()));
        apply(d);
        prefixed(sender, "&aSet alignment of &f" + d.getId() + " &ato &f" + d.getAlignment());
    }

    private void handleBrightness(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 4) {
            if (d != null) {
                prefixed(sender, "&cUsage: /td brightness <name> <block 0-15> <sky 0-15>  &7(-1 -1 to clear)");
            }
            return;
        }
        Integer block = parseInt(sender, args[2]);
        Integer sky = parseInt(sender, args[3]);
        if (block == null || sky == null) {
            return;
        }
        d.setBrightness(block, sky);
        apply(d);
        prefixed(sender, "&aSet brightness of &f" + d.getId() + " &ato block=" + block + " sky=" + sky);
    }

    private void handleRange(CommandSender sender, String[] args, String which) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        Double r = parseDouble(sender, args[2]);
        if (r == null) {
            return;
        }
        if (which.equals("display")) {
            d.setDisplayRange(r);
        } else {
            d.setUpdateRange(r);
        }
        apply(d);
        prefixed(sender, "&aSet " + which + "-range of &f" + d.getId() + " &ato &f" + r);
    }

    private void handleUpdateInterval(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        Integer t = parseInt(sender, args[2]);
        if (t == null) {
            return;
        }
        d.setUpdateInterval(t);
        apply(d);
        prefixed(sender, "&aSet update-interval of &f" + d.getId() + " &ato &f" + d.getUpdateInterval() + "t");
    }

    private void handleViewPermission(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 3) {
            return;
        }
        d.setViewPermission(args[2].equalsIgnoreCase("none") ? "" : args[2]);
        apply(d);
        prefixed(sender, "&aSet view permission of &f" + d.getId() + " &ato &f"
                + (d.getViewPermission().isEmpty() ? "none" : d.getViewPermission()));
    }

    private void handleClickBox(CommandSender sender, String[] args) {
        Display d = require(sender, args);
        if (d == null || args.length < 4) {
            if (d != null) {
                prefixed(sender, "&cUsage: /td clickbox <name> <width> <height>");
            }
            return;
        }
        Float w = parseFloat(sender, args[2]);
        Float h = parseFloat(sender, args[3]);
        if (w == null || h == null) {
            return;
        }
        d.setClickBox(w, h);
        apply(d);
        prefixed(sender, "&aSet click hitbox of &f" + d.getId() + " &ato &f" + w + "x" + h);
    }

    private void handleGui(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player != null) {
            new DisplayListMenu(plugin, player, 0).open();
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadEverything();
        sender.sendMessage(cfg().getMessage("reloaded"));
    }

    // ===================== Helpers =====================

    private void apply(Display display) {
        displays().save(display);
        plugin.getViewerTracker().reloadDisplay(display);
    }

    private Display require(CommandSender sender, String[] args) {
        if (args.length < 2) {
            prefixed(sender, "&cUsage: /td " + args[0] + " <name>");
            return null;
        }
        Display d = displays().get(args[1]);
        if (d == null) {
            sender.sendMessage(cfg().getMessage("not-found").replace("%id%", args[1]));
        }
        return d;
    }

    private Player asPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(cfg().getMessage("player-only"));
            return null;
        }
        return (Player) sender;
    }

    private Integer parseInt(CommandSender sender, String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            prefixed(sender, "&cNot a whole number: " + s);
            return null;
        }
    }

    private Float parseFloat(CommandSender sender, String s) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException ex) {
            prefixed(sender, "&cNot a number: " + s);
            return null;
        }
    }

    private Double parseDouble(CommandSender sender, String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException ex) {
            prefixed(sender, "&cNot a number: " + s);
            return null;
        }
    }

    private void prefixed(CommandSender sender, String text) {
        sender.sendMessage(cfg().getPrefix() + cfg().color(text));
    }

    private static String join(String[] args, int from) {
        return String.join(" ", Arrays.copyOfRange(args, from, args.length));
    }

    private static String round(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static boolean isHelp(String[] args) {
        return args.length >= 2 && args[1].equalsIgnoreCase("help");
    }

    private void handleEdit(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        Display d = require(sender, args);
        if (player != null && d != null) {
            new DisplayEditMenu(plugin, player, d.getId()).open();
        }
    }

    private void helpLine(CommandSender sender, String cmd, String desc) {
        sender.sendMessage(cfg().color("&b" + cmd + " &8- &7" + desc));
    }

    private void helpHeader(CommandSender sender, String title) {
        sender.sendMessage(cfg().color("&8&m                                        "));
        sender.sendMessage(cfg().color("&b&l" + title));
        sender.sendMessage("");
    }

    private void sendHelp(CommandSender sender) {
        helpHeader(sender, "Text Displays");
        helpLine(sender, "/td create <name> [text]", "Create a display where you stand.");
        helpLine(sender, "/td delete <name>", "Delete a display.");
        helpLine(sender, "/td edit <name>", "Open the editor for a display.");
        helpLine(sender, "/td gui", "Open the displays manager.");
        helpLine(sender, "/td list", "List all displays.");
        helpLine(sender, "/td line help", "Commands for editing lines.");
        helpLine(sender, "/td page help", "Commands for editing pages.");
        helpLine(sender, "/td display help", "Position, size & appearance.");
        helpLine(sender, "/td reload", "Reload the plugin and displays.");
        helpLine(sender, "/td version", "Show the plugin version.");
        sender.sendMessage(cfg().color("&8&m                                        "));
    }

    private void lineHelp(CommandSender sender) {
        helpHeader(sender, "Text Displays - lines");
        helpLine(sender, "/td line add <name> <text>", "Add a line.");
        helpLine(sender, "/td line set <name> <line> <text>", "Replace a line.");
        helpLine(sender, "/td line remove <name> <line>", "Remove a line.");
        helpLine(sender, "/td line insertbefore <name> <line> <text>", "Insert before a line.");
        helpLine(sender, "/td line insertafter <name> <line> <text>", "Insert after a line.");
        sender.sendMessage(cfg().color("&8&m                                        "));
    }

    private void pageHelp(CommandSender sender) {
        helpHeader(sender, "Text Displays - pages");
        helpLine(sender, "/td page add <name>", "Add a page.");
        helpLine(sender, "/td page insert <name> <page>", "Insert a page.");
        helpLine(sender, "/td page remove <name> <page>", "Remove a page.");
        helpLine(sender, "/td page swap <name> <page1> <page2>", "Swap two pages.");
        helpLine(sender, "/td page addaction <name> <page> <click> <TYPE:data>", "Add a click action.");
        helpLine(sender, "/td page removeaction <name> <page> <click> <index>", "Remove a click action.");
        helpLine(sender, "/td page clearactions <name> <page> [click]", "Clear click actions.");
        sender.sendMessage(cfg().color("&8&m                                        "));
    }

    private void displayHelp(CommandSender sender) {
        helpHeader(sender, "Text Displays - position & appearance");
        helpLine(sender, "/td movehere <name>", "Move the display to you.");
        helpLine(sender, "/td move <name> <x> <y> <z> [world]", "Move to coordinates.");
        helpLine(sender, "/td teleport <name>", "Teleport to the display.");
        helpLine(sender, "/td center <name>", "Center it on its block.");
        helpLine(sender, "/td facing <name> <degrees>", "Set the rotation.");
        helpLine(sender, "/td rename <name> <newName>", "Rename the display.");
        helpLine(sender, "/td clone <name> <newName>", "Clone the display.");
        helpLine(sender, "/td enable <name>", "Show the display.");
        helpLine(sender, "/td disable <name>", "Hide the display.");
        helpLine(sender, "/td scale <name> <x> [y] [z]", "Set the text size.");
        helpLine(sender, "/td billboard <name> <mode>", "Facing mode (FIXED/VERTICAL/HORIZONTAL/CENTER).");
        helpLine(sender, "/td background <name> <value>", "default, transparent or #RRGGBB.");
        helpLine(sender, "/td opacity <name> <0-255>", "Text opacity.");
        helpLine(sender, "/td linewidth <name> <n>", "Max line width.");
        helpLine(sender, "/td shadow <name> <true/false>", "Toggle text shadow.");
        helpLine(sender, "/td seethrough <name> <true/false>", "See the display through blocks.");
        helpLine(sender, "/td alignment <name> <left/center/right>", "Text alignment.");
        helpLine(sender, "/td brightness <name> <block> <sky>", "Light override (-1 -1 to clear).");
        helpLine(sender, "/td displayrange <name> <blocks>", "Render distance.");
        helpLine(sender, "/td updaterange <name> <blocks>", "Placeholder update distance.");
        helpLine(sender, "/td updateinterval <name> <ticks>", "Placeholder refresh rate.");
        helpLine(sender, "/td downorigin <name> <true/false>", "Anchor at the bottom.");
        helpLine(sender, "/td viewpermission <name> <node/none>", "Restrict who can see it.");
        helpLine(sender, "/td clickbox <name> <width> <height>", "Click hitbox size.");
        sender.sendMessage(cfg().color("&8&m                                        "));
    }

    // ===================== Tab completion =====================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return new ArrayList<>();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (sub.equals("line")) {
            if (args.length == 2) {
                return filter(LINE_SUBS, args[1]);
            }
            if (args.length == 3) {
                return filter(displays().ids(), args[2]);
            }
            return new ArrayList<>();
        }
        if (sub.equals("page")) {
            if (args.length == 2) {
                return filter(PAGE_SUBS, args[1]);
            }
            if (args.length == 3) {
                return filter(displays().ids(), args[2]);
            }
            if (args.length == 5 && (args[1].equalsIgnoreCase("addaction")
                    || args[1].equalsIgnoreCase("removeaction") || args[1].equalsIgnoreCase("clearactions"))) {
                return filter(CLICK_TYPES, args[4]);
            }
            return new ArrayList<>();
        }
        if (args.length == 2) {
            if (sub.equals("create") || sub.equals("gui") || sub.equals("reload")
                    || sub.equals("list") || sub.equals("version") || sub.equals("help") || sub.equals("nearby")) {
                return new ArrayList<>();
            }
            return filter(displays().ids(), args[1]);
        }
        if (args.length == 3) {
            switch (sub) {
                case "billboard":
                    return filter(Arrays.asList("FIXED", "VERTICAL", "HORIZONTAL", "CENTER"), args[2]);
                case "alignment":
                    return filter(Arrays.asList("LEFT", "CENTER", "RIGHT"), args[2]);
                case "shadow":
                case "seethrough":
                case "downorigin":
                    return filter(Arrays.asList("true", "false"), args[2]);
                case "background":
                    return filter(Arrays.asList("default", "transparent", "#FFFFFF"), args[2]);
                default:
                    return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p))
                .collect(Collectors.toList());
    }
}
