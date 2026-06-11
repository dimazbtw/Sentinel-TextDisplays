package github.dimazbtw.displays.display;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A text display definition stored in the DecentHolograms file format:
 * a {@code location} string, {@code display-range}/{@code update-range}/{@code update-interval},
 * {@code facing}, {@code down-origin} and a list of {@code pages} (each with {@code lines} and
 * {@code actions}). Text-display specific options (scale, billboard, background, ...) are kept as
 * extra flat keys.
 */
public class Display {

    private final String id;

    // Location (DecentHolograms: "world:x:y:z" + separate "facing" yaw)
    private String world = "world";
    private double x, y, z;
    private float facing;

    private boolean enabled = true;
    private double displayRange = 48;
    private double updateRange = 48;
    private int updateInterval = 20;
    private boolean downOrigin = false;

    private final List<Page> pages = new ArrayList<>();

    // Text-display options (extensions to the DH format)
    private float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    private BillboardMode billboard = BillboardMode.CENTER;
    private boolean defaultBackground = true;
    private int backgroundArgb = 0;
    private int textOpacity = 255;
    private int lineWidth = 200;
    private boolean shadow = false;
    private boolean seeThrough = false;
    private TextAlignment alignment = TextAlignment.CENTER;
    private int brightnessBlock = -1;
    private int brightnessSky = -1;
    private String viewPermission = "";
    private float clickWidth = 0f;
    private float clickHeight = 0f;

    // Runtime
    private int textEntityId;
    private int interactionEntityId;
    /** False for API-created displays that live only in memory (never written to disk). */
    private boolean persistent = true;

    public Display(String id) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.pages.add(new Page());
    }

    // ===================== Serialization (DecentHolograms format) =====================

    public static Display fromConfig(String id, FileConfiguration cfg) {
        Display d = new Display(id);

        String[] loc = cfg.getString("location", "world:0:0:0").split(":");
        d.world = loc.length > 0 ? loc[0] : "world";
        d.x = parse(loc, 1);
        d.y = parse(loc, 2);
        d.z = parse(loc, 3);
        float yawFromLoc = loc.length > 4 ? (float) parse(loc, 4) : 0f;
        d.facing = (float) cfg.getDouble("facing", yawFromLoc);

        d.enabled = cfg.getBoolean("enabled", true);
        d.displayRange = cfg.getDouble("display-range", 48);
        d.updateRange = cfg.getDouble("update-range", 48);
        d.updateInterval = Math.max(1, cfg.getInt("update-interval", 20));
        d.downOrigin = cfg.getBoolean("down-origin", false);

        d.pages.clear();
        d.pages.addAll(parsePages(cfg));

        // ---- text-display options ----
        if (cfg.isList("scale")) {
            List<Double> s = cfg.getDoubleList("scale");
            if (s.size() >= 3) {
                d.scaleX = s.get(0).floatValue();
                d.scaleY = s.get(1).floatValue();
                d.scaleZ = s.get(2).floatValue();
            } else if (!s.isEmpty()) {
                d.setScale(s.get(0).floatValue());
            }
        } else if (cfg.isSet("scale")) {
            d.setScale((float) cfg.getDouble("scale", 1d));
        }
        d.billboard = BillboardMode.from(cfg.getString("billboard"), BillboardMode.CENTER);
        d.setBackgroundSpec(cfg.getString("background", "default"));
        d.textOpacity = clamp(cfg.getInt("text-opacity", 255), 0, 255);
        d.lineWidth = Math.max(1, cfg.getInt("line-width", 200));
        d.shadow = cfg.getBoolean("text-shadow", false);
        d.seeThrough = cfg.getBoolean("see-through", false);
        d.alignment = TextAlignment.from(cfg.getString("text-alignment"), TextAlignment.CENTER);
        d.brightnessBlock = cfg.getInt("brightness-block", -1);
        d.brightnessSky = cfg.getInt("brightness-sky", -1);
        d.viewPermission = cfg.getString("view-permission", "");
        d.clickWidth = (float) cfg.getDouble("click-width", 0d);
        d.clickHeight = (float) cfg.getDouble("click-height", 0d);
        return d;
    }

    @SuppressWarnings("unchecked")
    private static List<Page> parsePages(FileConfiguration cfg) {
        List<Page> result = new ArrayList<>();
        for (Map<?, ?> pm : cfg.getMapList("pages")) {
            Page page = new Page();
            Object linesObj = pm.get("lines");
            if (linesObj instanceof List) {
                for (Object lo : (List<?>) linesObj) {
                    if (lo instanceof Map) {
                        Object c = ((Map<?, ?>) lo).get("content");
                        page.getLines().add(c == null ? "" : String.valueOf(c));
                    } else {
                        page.getLines().add(String.valueOf(lo));
                    }
                }
            } else if (pm.get("content") instanceof List) {
                for (Object lo : (List<?>) pm.get("content")) {
                    page.getLines().add(String.valueOf(lo));
                }
            }
            if (pm.get("actions") instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) pm.get("actions")).entrySet()) {
                    ClickType ct = ClickType.from(String.valueOf(e.getKey()), null);
                    if (ct != null && e.getValue() instanceof List) {
                        for (Object a : (List<?>) e.getValue()) {
                            page.addAction(ct, String.valueOf(a));
                        }
                    }
                }
            }
            result.add(page);
        }
        if (result.isEmpty()) {
            result.add(new Page());
        }
        return result;
    }

    public void toConfig(FileConfiguration cfg) {
        cfg.set("location", world + ":" + coord(x) + ":" + coord(y) + ":" + coord(z));
        cfg.set("facing", facing);
        cfg.set("enabled", enabled);
        cfg.set("display-range", displayRange);
        cfg.set("update-range", updateRange);
        cfg.set("update-interval", updateInterval);
        cfg.set("down-origin", downOrigin);

        List<Map<String, Object>> pageList = new ArrayList<>();
        for (Page page : pages) {
            Map<String, Object> pm = new LinkedHashMap<>();
            List<Map<String, Object>> lineList = new ArrayList<>();
            for (String line : page.getLines()) {
                Map<String, Object> lm = new LinkedHashMap<>();
                lm.put("content", line);
                lineList.add(lm);
            }
            pm.put("lines", lineList);
            if (page.hasAnyAction()) {
                Map<String, Object> am = new LinkedHashMap<>();
                for (Map.Entry<ClickType, List<String>> e : page.getActions().entrySet()) {
                    if (!e.getValue().isEmpty()) {
                        am.put(e.getKey().name(), new ArrayList<>(e.getValue()));
                    }
                }
                pm.put("actions", am);
            }
            pageList.add(pm);
        }
        cfg.set("pages", pageList);

        cfg.set("scale", new ArrayList<>(List.of((double) scaleX, (double) scaleY, (double) scaleZ)));
        cfg.set("billboard", billboard.name());
        cfg.set("background", backgroundSpec());
        cfg.set("text-opacity", textOpacity);
        cfg.set("line-width", lineWidth);
        cfg.set("text-shadow", shadow);
        cfg.set("see-through", seeThrough);
        cfg.set("text-alignment", alignment.name());
        cfg.set("brightness-block", brightnessBlock);
        cfg.set("brightness-sky", brightnessSky);
        cfg.set("view-permission", viewPermission);
        cfg.set("click-width", clickWidth);
        cfg.set("click-height", clickHeight);
    }

    // ===================== Pages / content =====================

    public List<Page> getPages() {
        return pages;
    }

    public int pageCount() {
        return pages.size();
    }

    public Page page(int index) {
        if (pages.isEmpty()) {
            pages.add(new Page());
        }
        return pages.get(Math.floorMod(index, pages.size()));
    }

    /** Convenience: lines of the first page (used by line commands and the GUI). */
    public List<String> getLines() {
        return page(0).getLines();
    }

    public List<String> linesOf(int pageIndex) {
        return page(pageIndex).getLines();
    }

    /** Dynamic = contains placeholders, so it must be re-resolved on the update loop. */
    public boolean isDynamic() {
        for (Page page : pages) {
            for (String line : page.getLines()) {
                if (line.indexOf('%') >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasClickActions() {
        for (Page page : pages) {
            if (page.hasAnyAction()) {
                return true;
            }
        }
        return false;
    }

    /**
     * A display is clickable when it has a hitbox. Configured actions are not required:
     * API consumers can set a click box and listen to DisplayClickEvent instead.
     */
    public boolean isClickable() {
        return clickWidth > 0f && clickHeight > 0f;
    }

    // ===================== Helpers =====================

    public boolean canSee(Player player) {
        return viewPermission == null || viewPermission.isEmpty() || player.hasPermission(viewPermission);
    }

    public Location getBukkitLocation() {
        World w = Bukkit.getWorld(world);
        return w == null ? null : new Location(w, x, y, z, facing, 0f);
    }

    public void setLocation(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.facing = loc.getYaw();
    }

    public void setScale(float uniform) {
        this.scaleX = uniform;
        this.scaleY = uniform;
        this.scaleZ = uniform;
    }

    public void setScale(float sx, float sy, float sz) {
        this.scaleX = sx;
        this.scaleY = sy;
        this.scaleZ = sz;
    }

    public boolean setBackgroundSpec(String spec) {
        if (spec == null || spec.equalsIgnoreCase("default")) {
            defaultBackground = true;
            return true;
        }
        if (spec.equalsIgnoreCase("transparent") || spec.equalsIgnoreCase("none")) {
            defaultBackground = false;
            backgroundArgb = 0;
            return true;
        }
        String hex = spec.startsWith("#") ? spec.substring(1) : spec;
        try {
            if (hex.length() == 6) {
                backgroundArgb = 0xFF000000 | (int) Long.parseLong(hex, 16);
            } else if (hex.length() == 8) {
                backgroundArgb = (int) Long.parseLong(hex, 16);
            } else {
                return false;
            }
            defaultBackground = false;
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public String backgroundSpec() {
        if (defaultBackground) {
            return "default";
        }
        if (((backgroundArgb >> 24) & 0xFF) == 0) {
            return "transparent";
        }
        return String.format("#%08X", backgroundArgb);
    }

    public boolean hasBrightnessOverride() {
        return brightnessBlock >= 0 || brightnessSky >= 0;
    }

    public int packedBrightness() {
        int block = Math.max(0, Math.min(15, brightnessBlock));
        int sky = Math.max(0, Math.min(15, brightnessSky));
        return (block << 4) | (sky << 20);
    }

    private static double parse(String[] arr, int i) {
        try {
            return i < arr.length ? Double.parseDouble(arr[i]) : 0d;
        } catch (NumberFormatException ex) {
            return 0d;
        }
    }

    private static String coord(double v) {
        return String.format(Locale.ROOT, "%.3f", v);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // ===================== Getters / setters =====================

    public String getId() { return id; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getFacing() { return facing; }
    public void setFacing(float facing) { this.facing = facing; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) { this.enabled = e; }
    public double getDisplayRange() { return displayRange; }
    public void setDisplayRange(double r) { this.displayRange = Math.max(1d, r); }
    public double getUpdateRange() { return updateRange; }
    public void setUpdateRange(double r) { this.updateRange = Math.max(1d, r); }
    public int getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(int t) { this.updateInterval = Math.max(1, t); }
    public boolean isDownOrigin() { return downOrigin; }
    public void setDownOrigin(boolean d) { this.downOrigin = d; }

    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    public BillboardMode getBillboard() { return billboard; }
    public void setBillboard(BillboardMode b) { this.billboard = b; }
    public boolean isDefaultBackground() { return defaultBackground; }
    public int getBackgroundArgb() { return backgroundArgb; }
    public int getTextOpacity() { return textOpacity; }
    public void setTextOpacity(int o) { this.textOpacity = clamp(o, 0, 255); }
    public int getLineWidth() { return lineWidth; }
    public void setLineWidth(int w) { this.lineWidth = Math.max(1, w); }
    public boolean isShadow() { return shadow; }
    public void setShadow(boolean s) { this.shadow = s; }
    public boolean isSeeThrough() { return seeThrough; }
    public void setSeeThrough(boolean s) { this.seeThrough = s; }
    public TextAlignment getAlignment() { return alignment; }
    public void setAlignment(TextAlignment a) { this.alignment = a; }
    public int getBrightnessBlock() { return brightnessBlock; }
    public int getBrightnessSky() { return brightnessSky; }
    public void setBrightness(int block, int sky) { this.brightnessBlock = block; this.brightnessSky = sky; }
    public String getViewPermission() { return viewPermission; }
    public void setViewPermission(String p) { this.viewPermission = p == null ? "" : p; }
    public float getClickWidth() { return clickWidth; }
    public float getClickHeight() { return clickHeight; }
    public void setClickBox(float width, float height) { this.clickWidth = width; this.clickHeight = height; }

    public int getTextEntityId() { return textEntityId; }
    public void setTextEntityId(int id) { this.textEntityId = id; }
    public int getInteractionEntityId() { return interactionEntityId; }
    public void setInteractionEntityId(int id) { this.interactionEntityId = id; }
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }
}
