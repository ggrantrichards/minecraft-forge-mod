package com.ggrantrichards.astralend.flare;

// The stages of a Stellar Flare (see StellarFlares). Only the Corona feels them.
public enum FlarePhase {
    CALM,       // 15-25 minutes between flares
    WARNING,    // about a minute: the Remnant brightens and a hum rises - time to find cover
    FLARE,      // 1-2 minutes: light rolls across the sky; open sky burns (Flare Burn)
    AFTERGLOW;  // about 2 minutes: everything glows, Skimmer wings refill, Cinder Mites swarm out

    public static FlarePhase byId(int id) {
        FlarePhase[] values = values();
        return values[Math.floorMod(id, values.length)];
    }
}
