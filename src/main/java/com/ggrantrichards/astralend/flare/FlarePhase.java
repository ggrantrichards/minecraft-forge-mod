package com.ggrantrichards.astralend.flare;

// The stages of a Sunstorm (see Sunstorms). Only the Eclipse feels them.
public enum FlarePhase {
    CALM,       // 15-25 minutes between flares
    WARNING,    // about a minute: the Eclipse's star brightens and a hum rises - time to find cover
    FLARE,      // 1-2 minutes: light rolls across the sky; open sky burns (Sunburn)
    AFTERGLOW;  // about 2 minutes: everything glows, Void Ray wings refill

    public static FlarePhase byId(int id) {
        FlarePhase[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
