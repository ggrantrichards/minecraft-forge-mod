package com.ggrantrichards.astralend.flare;

import net.minecraft.network.RegistryFriendlyByteBuf;

// What a client needs to know about the flare cycle, sent from the server (FlareNetwork).
//
//   phase, ticksLeft, phaseLength  where the cycle is
//   windYaw                        the direction the flare's light and solar wind travel, in degrees
//   remnantX/Y/Z, hasRemnant       where the nearest Remnant hangs, high over its Corona
public record FlareState(FlarePhase phase, int ticksLeft, int phaseLength, float windYaw,
                         boolean hasRemnant, int remnantX, int remnantY, int remnantZ) {
    public static final FlareState CALM = new FlareState(FlarePhase.CALM, 24000, 24000, 0.0F, false, 0, 0, 0);

    // 0 at the start of the phase, 1 at its end.
    public float progress() {
        return this.phaseLength <= 0 ? 1.0F : 1.0F - (float) this.ticksLeft / this.phaseLength;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.phase.ordinal());
        buf.writeVarInt(this.ticksLeft);
        buf.writeVarInt(this.phaseLength);
        buf.writeFloat(this.windYaw);
        buf.writeBoolean(this.hasRemnant);
        buf.writeInt(this.remnantX);
        buf.writeInt(this.remnantY);
        buf.writeInt(this.remnantZ);
    }

    public static FlareState decode(RegistryFriendlyByteBuf buf) {
        return new FlareState(FlarePhase.byId(buf.readVarInt()), buf.readVarInt(), buf.readVarInt(), buf.readFloat(),
            buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt());
    }
}
