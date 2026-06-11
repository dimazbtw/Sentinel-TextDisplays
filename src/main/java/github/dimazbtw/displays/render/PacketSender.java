package github.dimazbtw.displays.render;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Thin wrappers around PacketEvents for sending the spawn / metadata / destroy packets
 * that make up a per-player Text Display (and its Interaction hitbox).
 */
public final class PacketSender {

    private static final Vector3d ZERO_VELOCITY = new Vector3d(0d, 0d, 0d);

    private PacketSender() {
    }

    public static void spawn(Player player, int entityId, EntityType type,
                             double x, double y, double z, float yaw, float pitch) {
        Location location = new Location(new Vector3d(x, y, z), yaw, pitch);
        send(player, new WrapperPlayServerSpawnEntity(
                entityId, UUID.randomUUID(), type, location, 0f, 0, ZERO_VELOCITY));
    }

    public static void metadata(Player player, int entityId, List<EntityData> data) {
        send(player, new WrapperPlayServerEntityMetadata(entityId, data));
    }

    public static void destroy(Player player, int... entityIds) {
        send(player, new WrapperPlayServerDestroyEntities(entityIds));
    }

    private static void send(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
