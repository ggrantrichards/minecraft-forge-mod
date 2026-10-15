package com.ggrantrichards.astralend.flare;

import net.minecraft.network.RegistryFriendlyByteBuf;

// What a client needs to know about the flare cycle, sent from the server (FlareNetwork).
//
//   phase, ticksLeft, phaseLength  where the cycle is
//   windYaw                        the direction the flare's light and solar wind travel, in degrees
public record FlareState(FlarePhase phase, int ticksLeft, int phaseLength, float windYaw) {
    public static final FlareState CALM = new FlareState(FlarePhase.CALM, 24000, 24000, 0.0F);

    // 0 at the start of the phase, 1 at its end.
    public float progress() {
        return this.phaseLength <= 0 ? 1.0F : 1.0F - (float) this.ticksLeft / this.phaseLength;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.phase.ordinal());
        buf.writeVarInt(this.ticksLeft);
        buf.writeVarInt(this.phaseLength);
        buf.writeFloat(this.windYaw);
    }

    public static FlareState decode(RegistryFriendlyByteBuf buf) {
        return new FlareState(FlarePhase.byId(buf.readVarInt()), buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }
}
