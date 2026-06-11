package github.dimazbtw.displays.render;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.util.Vector3f;
import github.dimazbtw.displays.display.Display;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the entity-metadata list for a Text Display, using the version-aware
 * {@link MetaIndex} so the same code produces correct packets on 1.19.4 - 1.21+.
 */
public final class DisplayMeta {

    /** Approximate vanilla line height of a text display (blocks) at scale 1. */
    private static final float LINE_HEIGHT = 0.25f;

    private DisplayMeta() {
    }

    /** Full metadata sent on spawn (appearance + text). lineCount drives the down-origin offset. */
    public static List<EntityData> full(Display d, Component text, int lineCount, MetaIndex idx) {
        List<EntityData> data = new ArrayList<>(11);

        // down-origin: location is the bottom (offset up) or top (offset down) of the text block.
        data.add(new EntityData(idx.translation(), EntityDataTypes.VECTOR3F,
                new Vector3f(0f, verticalOffset(d, lineCount), 0f)));

        data.add(new EntityData(idx.scale(), EntityDataTypes.VECTOR3F,
                new Vector3f(d.getScaleX(), d.getScaleY(), d.getScaleZ())));
        data.add(new EntityData(idx.billboard(), EntityDataTypes.BYTE, (byte) d.getBillboard().id()));
        data.add(new EntityData(idx.viewRange(), EntityDataTypes.FLOAT, viewRangeMultiplier(d)));
        if (d.hasBrightnessOverride()) {
            data.add(new EntityData(idx.brightness(), EntityDataTypes.INT, d.packedBrightness()));
        }

        data.add(new EntityData(idx.text(), EntityDataTypes.ADV_COMPONENT, text));
        data.add(new EntityData(idx.lineWidth(), EntityDataTypes.INT, d.getLineWidth()));
        data.add(new EntityData(idx.background(), EntityDataTypes.INT,
                d.isDefaultBackground() ? 0 : d.getBackgroundArgb()));
        data.add(new EntityData(idx.textOpacity(), EntityDataTypes.BYTE, (byte) (d.getTextOpacity() & 0xFF)));
        data.add(new EntityData(idx.styleFlags(), EntityDataTypes.BYTE, styleFlags(d)));
        return data;
    }

    /** Just the text field - sent on the per-player update loop. */
    public static List<EntityData> textOnly(Component text, MetaIndex idx) {
        return Collections.singletonList(new EntityData(idx.text(), EntityDataTypes.ADV_COMPONENT, text));
    }

    /** Vertical translation applied to honour down-origin, given the number of lines shown. */
    public static float verticalOffset(Display d, int lineCount) {
        float height = Math.max(1, lineCount) * LINE_HEIGHT * d.getScaleY();
        return d.isDownOrigin() ? (height / 2f) : (-height / 2f);
    }

    private static byte styleFlags(Display d) {
        int flags = 0;
        if (d.isShadow()) {
            flags |= 0x01;
        }
        if (d.isSeeThrough()) {
            flags |= 0x02;
        }
        if (d.isDefaultBackground()) {
            flags |= 0x04;
        }
        flags |= d.getAlignment().flagBits();
        return (byte) flags;
    }

    private static float viewRangeMultiplier(Display d) {
        return Math.max(1.0f, (float) (d.getDisplayRange() / 64.0));
    }
}
