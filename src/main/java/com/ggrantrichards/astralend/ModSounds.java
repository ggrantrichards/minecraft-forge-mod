package com.ggrantrichards.astralend;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// The mod's music and its own sounds. The tracks are free to use (CC0, and one CC-BY 3.0); who made them is in
// assets/astralend/sounds/credits.txt. Which biome plays which is in its JSON
// ("minecraft:audio/background_music"); the music disc's song is data/astralend/jukebox_song/.
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, AstralEnd.MODID);

    public static final RegistryObject<SoundEvent> MUSIC_NEBULA_FIELDS = music("music.nebula_fields");
    public static final RegistryObject<SoundEvent> MUSIC_CHORUS_THICKET = music("music.chorus_thicket");
    public static final RegistryObject<SoundEvent> MUSIC_GLIMMERING_CRAGS = music("music.glimmering_crags");
    public static final RegistryObject<SoundEvent> MUSIC_OUTER_END = music("music.outer_end");
    public static final RegistryObject<SoundEvent> DISC_OBSERVING_THE_STAR = music("music_disc.observing_the_star");

    // The Spire Wyrm's own voice (tools/sounds/wyrm_sounds.py synthesises it).
    public static final RegistryObject<SoundEvent> WYRM_ROAR = music("entity.spire_wyrm.roar");
    public static final RegistryObject<SoundEvent> WYRM_AMBIENT = music("entity.spire_wyrm.ambient");
    public static final RegistryObject<SoundEvent> WYRM_HURT = music("entity.spire_wyrm.hurt");
    public static final RegistryObject<SoundEvent> WYRM_DEATH = music("entity.spire_wyrm.death");
    public static final RegistryObject<SoundEvent> WYRM_CHARGE = music("entity.spire_wyrm.charge");
    public static final RegistryObject<SoundEvent> WYRM_BREATH = music("entity.spire_wyrm.breath");
    public static final RegistryObject<SoundEvent> WYRM_SLAM = music("entity.spire_wyrm.slam");
    public static final RegistryObject<SoundEvent> WYRM_SNORE = music("entity.spire_wyrm.snore");

    private static RegistryObject<SoundEvent> music(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(AstralEnd.MODID, name)));
    }

    private ModSounds() {}
}
