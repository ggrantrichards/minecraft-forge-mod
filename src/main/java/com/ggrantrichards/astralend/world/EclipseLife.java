package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.entity.EndMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

// The Eclipse's heaven half is the one place Endermen won't stay: one that wanders or teleports in
// panics - it screams and blinks out again, far off. (The Mirror Wretch hunts them; they know.)
public final class EclipseLife {
    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(EclipseLife::onLevelTick);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.END || level.getGameTime() % 20 != 7) {
            return;
        }
        for (EnderMan enderman : level.getEntities(EntityTypes.ENDERMAN, e -> e.isAlive() && level.getBiome(e.blockPosition()).is(ModBiomes.ECLIPSE))) {
            BlockPos to = EndMobs.safeSpotNear(level, enderman.blockPosition(), 48, 80, enderman.getRandom());
            level.playSound(null, enderman.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 1.0F, 1.2F);
            level.sendParticles(ParticleTypes.PORTAL, enderman.getX(), enderman.getY() + 1.5, enderman.getZ(), 40, 0.4, 1.0, 0.4, 0.3);
            if (to != null && !level.getBiome(to).is(ModBiomes.ECLIPSE)) {
                enderman.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            } else {
                enderman.discard();   // nowhere to go but out of the world
            }
        }
    }

    private EclipseLife() {}
}
