package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.entity.EndMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

// What the End's Endermen do that vanilla's don't.
//   - The Gilded Reach is the one place they won't stay: too much light. One that wanders or teleports
//     in panics - it screams and blinks out again, far off.
//   - In the Ashen Barrens they walk to the islands' edges and stand there, motionless, facing the
//     void - or, when a Mirror Wretch is near, facing it: listening to the voices it has stolen. The game
//     never says why.
public final class EclipseLife {
    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(EclipseLife::onLevelTick);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.END || level.getGameTime() % 20 != 7) {
            return;
        }
        for (Enderman enderman : level.getEntities(EntityTypes.ENDERMAN, e -> e.isAlive() && e.getTarget() == null
                && level.getBiome(e.blockPosition()).is(ModBiomes.ASHEN_BARRENS))) {
            standAtEdge(level, enderman);
        }
        for (Enderman enderman : level.getEntities(EntityTypes.ENDERMAN, e -> e.isAlive() && level.getBiome(e.blockPosition()).is(ModBiomes.ECLIPSE))) {
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

    // At an edge (the void within two blocks to one side): stand still, facing it. Otherwise walk to the
    // nearest edge within 12 blocks.
    private static void standAtEdge(ServerLevel level, Enderman enderman) {
        BlockPos at = enderman.blockPosition();
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= 2; i++) {
                BlockPos side = at.relative(d, i);
                if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isAir() && EndMobs.voidBelow(level, side)) {
                    enderman.getNavigation().stop();
                    float yaw = d.toYRot();
                    com.ggrantrichards.astralend.entity.MirrorWretch wretch = null;
                    for (var w : level.getEntitiesOfClass(com.ggrantrichards.astralend.entity.MirrorWretch.class,
                            enderman.getBoundingBox().inflate(32.0), net.minecraft.world.entity.Entity::isAlive)) {
                        if (wretch == null || w.distanceToSqr(enderman) < wretch.distanceToSqr(enderman)) {
                            wretch = w;
                        }
                    }
                    if (wretch != null) {
                        yaw = (float) Math.toDegrees(Math.atan2(-(wretch.getX() - enderman.getX()), wretch.getZ() - enderman.getZ()));
                    }
                    enderman.setYRot(yaw);
                    enderman.setYHeadRot(yaw);
                    enderman.yBodyRot = yaw;
                    return;
                }
            }
        }
        if (!enderman.getNavigation().isDone()) {
            return;
        }
        for (int r = 3; r <= 12; r++) {
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos side = at.relative(d, r);
                if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isAir() && EndMobs.voidBelow(level, side)) {
                    BlockPos stand = at.relative(d, r - 2);
                    enderman.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.8);
                    return;
                }
            }
        }
    }

    private EclipseLife() {}
}
