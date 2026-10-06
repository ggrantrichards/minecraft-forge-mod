package com.ggrantrichards.astralend.world;

// Where each astral biome goes, from two slow region noises. Shared by the biome source and the
// worldgen survey; the terrain in data/astralend/worldgen/density_function/terrain/ blends between
// shapes using the same thresholds (tools/generate_resources.py writes both from one table).
//
//  - "nebula" noise (the noise router's temperature slot): above NEBULA -> the Nebula Fields.
//  - "belt" noise (the vegetation slot) splits the rest of the outer End:
//        nebula noise low  (< COLD):  belt high -> Ashen Barrens (belt low stays vanilla)
//        nebula noise middle:         belt high -> the Deep Hollows (belt low stays vanilla)
//    and between those, vanilla's Highlands/Midlands/Barrens/Small Islands.
//  - "ruin" noise (the ridges slot): rare peaks inside what would be vanilla -> the Glimmering Crags.
//  - "variant" noise (the continents slot), faster: patches inside the Crags where they break up into
//    their variant, the Shattered Crags.
// (After beta.4 the Chorus Tangle was cut, and the Hollow Isles folded into the Deep Hollows, which
// was their variant: their belt is all Deep Hollows now.)
public final class AstralRegions {
    public static final double NEBULA = 0.18;
    public static final double COLD = -0.2;
    public static final double COLD_BELT = 0.2;    // |belt| beyond this in the cold band
    public static final double MID_BELT = 0.3;     // |belt| beyond this in the middle band
    public static final double CRAGS = 0.45;       // ruin above this, where it would be vanilla
    public static final double VARIANT = 0.2;      // variant above this: a territory's variant

    public enum Region { VANILLA, NEBULA_FIELDS, ASHEN_BARRENS, DEEP_HOLLOWS, GLIMMERING_CRAGS, SHATTERED_CRAGS }

    public static Region pick(double nebula, double belt, double ruin, double variant) {
        Region region = pick(nebula, belt, ruin);
        if (variant > VARIANT && region == Region.GLIMMERING_CRAGS) {
            return Region.SHATTERED_CRAGS;
        }
        return region;
    }

    private static Region pick(double nebula, double belt, double ruin) {
        if (nebula > NEBULA) {
            return Region.NEBULA_FIELDS;
        }
        if (nebula < COLD) {
            if (belt > COLD_BELT) return Region.ASHEN_BARRENS;
        } else {
            if (belt > MID_BELT) return Region.DEEP_HOLLOWS;
        }
        return ruin > CRAGS ? Region.GLIMMERING_CRAGS : Region.VANILLA;
    }

    private AstralRegions() {}
}
