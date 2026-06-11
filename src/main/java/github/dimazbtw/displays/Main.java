package github.dimazbtw.displays;

import com.github.retrooper.packetevents.PacketEvents;
import github.dimazbtw.displays.action.ActionExecutor;
import github.dimazbtw.displays.action.ClickListener;
import github.dimazbtw.displays.command.DisplaysCommand;
import github.dimazbtw.displays.compat.VersionSupport;
import github.dimazbtw.displays.config.ConfigManager;
import github.dimazbtw.displays.display.BillboardMode;
import github.dimazbtw.displays.display.Display;
import github.dimazbtw.displays.display.DisplayManager;
import github.dimazbtw.displays.gui.ChatInput;
import github.dimazbtw.displays.gui.GuiListener;
import github.dimazbtw.displays.listener.PlayerListener;
import github.dimazbtw.displays.render.DisplayTask;
import github.dimazbtw.displays.render.ViewerTracker;
import github.dimazbtw.displays.text.TextPipeline;
import github.dimazbtw.displays.text.hooks.Hooks;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private Hooks hooks;
    private TextPipeline textPipeline;
    private DisplayManager displayManager;
    private ViewerTracker viewerTracker;
    private ActionExecutor actionExecutor;
    private ClickListener clickListener;
    private DisplayTask displayTask;
    private ChatInput chatInput;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new ConfigManager(this);

        // PacketEvents is provided by its own plugin (declared as a hard dependency),
        // so it is already loaded and initialised by the time we enable.
        VersionSupport.init();
        if (!VersionSupport.isSupported()) {
            getLogger().severe("Text Displays require Minecraft 1.19.4 or newer (detected "
                    + VersionSupport.getDisplayName() + "). Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        hooks = new Hooks();
        hooks.init();
        textPipeline = new TextPipeline(hooks);

        displayManager = new DisplayManager(this);
        displayManager.loadAll();

        viewerTracker = new ViewerTracker(displayManager, textPipeline);
        actionExecutor = new ActionExecutor(this, hooks.placeholders(), viewerTracker,
                displayManager, configManager.getClickCooldownMillis());

        // Packet click listener (off-thread) + Bukkit events (visibility)
        clickListener = new ClickListener(this, displayManager, actionExecutor, viewerTracker);
        PacketEvents.getAPI().getEventManager().registerListener(clickListener);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        chatInput = new ChatInput(this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        DisplaysCommand command = new DisplaysCommand(this);
        if (getCommand("textdisplays") != null) {
            getCommand("textdisplays").setExecutor(command);
            getCommand("textdisplays").setTabCompleter(command);
        }

        // BungeeCord/Velocity channel for the [connect] click action
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        displayTask = new DisplayTask(displayManager, viewerTracker);
        displayTask.runTaskTimer(this, 1L, 1L);

        viewerTracker.updateAllPlayers();

        getLogger().info("==================================================");
        getLogger().info("  Sentinel-TextDisplays v" + getDescription().getVersion());
        getLogger().info("  Server: " + VersionSupport.getDisplayName()
                + " (" + (VersionSupport.isModernDisplayLayout() ? "1.20.2+ layout" : "1.19.4-1.20.1 layout") + ")");
        getLogger().info("  Displays loaded: " + displayManager.all().size());
        getLogger().info("  Integrations: " + hooks.summary());
        getLogger().info("==================================================");
    }

    @Override
    public void onDisable() {
        if (viewerTracker != null) {
            viewerTracker.clearAll();
        }
        if (displayTask != null) {
            displayTask.cancel();
        }
        // Unregister our packet listener from the shared (external) PacketEvents instance.
        if (clickListener != null) {
            try {
                PacketEvents.getAPI().getEventManager().unregisterListener(clickListener);
            } catch (Throwable ignored) {
                // PacketEvents may already be terminating - safe to ignore.
            }
        }
    }

    /** Full reload: config + every display definition, re-spawning for online players. */
    public void reloadEverything() {
        configManager.reload();
        viewerTracker.clearAll();
        displayManager.loadAll();
        actionExecutor.setCooldownMillis(configManager.getClickCooldownMillis());
        viewerTracker.updateAllPlayers();
    }

    /** Creates a display at the location with the config defaults applied, then renders it. */
    public Display createDisplay(String id, org.bukkit.Location location) {
        Display d = displayManager.create(id, location);
        d.setScale((float) configManager.getDefaultScale());
        d.setBillboard(BillboardMode.from(configManager.getDefaultBillboard(), BillboardMode.CENTER));
        d.setBackgroundSpec(configManager.getDefaultBackground());
        d.setDisplayRange(configManager.getDefaultDisplayRange());
        d.setUpdateRange(configManager.getDefaultUpdateRange());
        d.setUpdateInterval(configManager.getDefaultUpdateInterval());
        displayManager.save(d);
        viewerTracker.reloadDisplay(d);
        return d;
    }

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Hooks getHooks() {
        return hooks;
    }

    public TextPipeline getTextPipeline() {
        return textPipeline;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public ViewerTracker getViewerTracker() {
        return viewerTracker;
    }

    public ActionExecutor getActionExecutor() {
        return actionExecutor;
    }

    public ChatInput getChatInput() {
        return chatInput;
    }
}
