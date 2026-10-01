package com.ggrantrichards.astralend.flare;

// The flare cycle as this client last heard it from the server. Kept in common code (no client
// classes), so the network handler can reach it on either side; only the client ever fills it.
// Between packets the countdown runs on locally, so effects move smoothly.
public final class ClientFlares {
    private static FlareState state = FlareState.CALM;
    private static int ticksSinceUpdate;

    public static void receive(FlareState newState) {
        state = newState;
        ticksSinceUpdate = 0;
    }

    public static void tick() {
        ticksSinceUpdate++;
    }

    public static FlareState state() {
        return state;
    }

    public static FlarePhase phase() {
        return state.phase();
    }

    // 0..1 through the current phase, counting the time since the last packet.
    public static float progress(float partialTick) {
        if (state.phaseLength() <= 0) {
            return 1.0F;
        }
        float left = Math.max(0.0F, state.ticksLeft() - ticksSinceUpdate - partialTick);
        return 1.0F - left / state.phaseLength();
    }

    // How strongly the flare is going, 0..1: rising through the warning, full during the flare, fading in
    // the afterglow.
    public static float intensity(float partialTick) {
        float p = progress(partialTick);
        return switch (state.phase()) {
            case CALM -> 0.0F;
            case WARNING -> p * p * 0.5F;
            case FLARE -> 0.75F + 0.25F * (float) Math.sin(p * Math.PI);
            case AFTERGLOW -> 0.5F * (1.0F - p) * (1.0F - p);
        };
    }

    public static void reset() {
        state = FlareState.CALM;
        ticksSinceUpdate = 0;
    }

    private ClientFlares() {}
}
