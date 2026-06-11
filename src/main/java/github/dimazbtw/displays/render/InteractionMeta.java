package github.dimazbtw.displays.render;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import github.dimazbtw.displays.display.Display;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds metadata for the invisible Interaction entity that provides the click hitbox.
 * Interaction fields are not affected by the Display index shift, so they are constant.
 */
public final class InteractionMeta {

    private InteractionMeta() {
    }

    public static List<EntityData> build(Display d, MetaIndex idx) {
        List<EntityData> data = new ArrayList<>(3);
        data.add(new EntityData(idx.interactionWidth(), EntityDataTypes.FLOAT, d.getClickWidth()));
        data.add(new EntityData(idx.interactionHeight(), EntityDataTypes.FLOAT, d.getClickHeight()));
        data.add(new EntityData(idx.interactionResponsive(), EntityDataTypes.BOOLEAN, Boolean.TRUE));
        return data;
    }
}
