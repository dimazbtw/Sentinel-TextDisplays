package github.dimazbtw.displays.compat;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

/**
 * Detects the running server version (through PacketEvents) and exposes the few
 * version-dependent facts this plugin needs.
 *
 * <p>Text Displays exist on 1.19.4+. The only structural change in the supported
 * range is the 1.20.2 "interpolation split", which shifts every Display metadata
 * index by one. {@link #isModernDisplayLayout()} captures that single boundary.</p>
 */
public final class VersionSupport {

    private static ServerVersion version;
    private static boolean modernDisplayLayout;

    private VersionSupport() {
    }

    /** Must be called after PacketEvents has been loaded. */
    public static void init() {
        version = PacketEvents.getAPI().getServerManager().getVersion();
        // 1.20.2 introduced the separate position/rotation interpolation field.
        modernDisplayLayout = version.isNewerThanOrEquals(ServerVersion.V_1_20_2);
    }

    /** Text Displays require 1.19.4 or newer. */
    public static boolean isSupported() {
        return version != null && version.isNewerThanOrEquals(ServerVersion.V_1_19_4);
    }

    /** True on 1.20.2+, where the Display metadata indices are shifted by one. */
    public static boolean isModernDisplayLayout() {
        return modernDisplayLayout;
    }

    public static ServerVersion getVersion() {
        return version;
    }

    public static String getDisplayName() {
        return version == null ? "unknown" : version.getReleaseName();
    }
}
