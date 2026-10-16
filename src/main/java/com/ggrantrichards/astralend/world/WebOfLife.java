package com.ggrantrichards.astralend.world;

import net.minecraft.world.entity.EntityType;
import com.ggrantrichards.astralend.entity.Hushback;
import com.ggrantrichards.astralend.entity.UmbralStalker;
import com.ggrantrichards.astralend.entity.Watcher;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.jspecify.annotations.Nullable;

// The threads of the End's web that run through vanilla's mobs, which the mod can't give goals of its
// own (the rest live in the mod's mobs):
//   - A wolf knows where an Umbral Stalker is: it stares at the spot and growls, and the Stalker, given
//     away, shows itself - the way a cat keeps creepers off.
//   - Endermen won't meet the Watcher's eye: one near it turns its back and walks off.
//   - Endermites in the Gilded Reach pester the Hushback herds, creeping after them - which is why the
//     Hydragrips keep near the herds.
public final class WebOfLife {
    public static void register() {
        com.ggrantrichards.astralend.Events.post(TickEvent.LevelTickEvent.class, WebOfLife::onLevelTick);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (!(event.level instanceof ServerLevel level) || level.dimension() != Level.END || level.getGameTime() % 10 != 3) {
            return;
        }
        boolean growlTime = level.getGameTime() % 40 == 3;
        for (Entity wolf : level.getEntities(EntityType.WOLF, Entity::isAlive)) {
            if (wolf instanceof Mob mob) {
                wolfSenses(level, mob, growlTime);
            }
        }
        if (level.getGameTime() % 20 == 13) {
            for (EnderMan enderman : level.getEntities(EntityType.ENDERMAN, e -> e.isAlive() && e.getTarget() == null)) {
                shunWatcher(level, enderman);
            }
            for (Endermite mite : level.getEntities(EntityType.ENDERMITE, e -> e.isAlive() && e.getTarget() == null)) {
                pesterHerd(level, mite);
            }
        }
    }

    // The nearest hidden Stalker within 16 blocks of the wolf: it stares, and (now and then) growls,
    // giving it away. Returns the Stalker it sensed, or null.
    public static @Nullable UmbralStalker wolfSenses(ServerLevel level, Mob wolf, boolean growl) {
        UmbralStalker found = null;
        for (UmbralStalker s : level.getEntitiesOfClass(UmbralStalker.class, wolf.getBoundingBox().inflate(16.0), Entity::isAlive)) {
            if (!s.isRevealed() && (found == null || s.distanceToSqr(wolf) < found.distanceToSqr(wolf))) {
                found = s;
            }
        }
        if (found == null) {
            return null;
        }
        wolf.getNavigation().stop();
        wolf.getLookControl().setLookAt(found, 30.0F, 30.0F);
        if (growl) {
            var sound = BuiltInRegistries.SOUND_EVENT.getOptional(ResourceLocation.withDefaultNamespace("entity.wolf.growl"));
            if (sound.isPresent()) {
                level.playSound(null, wolf.getX(), wolf.getY(), wolf.getZ(), sound.get(), SoundSource.NEUTRAL, 1.0F, 0.9F);
            } else {
                wolf.playAmbientSound();
            }
            found.betray(30);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, found.getX(), found.getY() + 1.0, found.getZ(), 12, 0.4, 0.6, 0.4, 0.02);
        }
        return found;
    }

    // An EnderMan within 24 blocks of a Watcher turns its back on it and walks away. True if it did.
    public static boolean shunWatcher(ServerLevel level, EnderMan enderman) {
        Watcher watcher = null;
        for (Watcher w : level.getEntitiesOfClass(Watcher.class, enderman.getBoundingBox().inflate(24.0), Entity::isAlive)) {
            watcher = w;
            break;
        }
        if (watcher == null) {
            return false;
        }
        Vec3 away = enderman.position().subtract(watcher.position());
        float yaw = (float) Math.toDegrees(Math.atan2(-away.x, away.z));
        enderman.setYRot(yaw);
        enderman.setYHeadRot(yaw);
        enderman.yBodyRot = yaw;
        if (enderman.getNavigation().isDone()) {
            Vec3 to = DefaultRandomPos.getPosAway(enderman, 16, 4, watcher.position());
            if (to != null) {
                enderman.getNavigation().moveTo(to.x, to.y, to.z, 1.0);
            }
        }
        return true;
    }

    // An endermite in the Gilded Reach creeps after the nearest Hushback within 16 blocks, and the
    // herd shifts and stamps. True if it found one.
    public static boolean pesterHerd(ServerLevel level, Endermite mite) {
        if (!level.getBiome(mite.blockPosition()).is(ModBiomes.ECLIPSE)) {
            return false;
        }
        Hushback nearest = null;
        for (Hushback h : level.getEntitiesOfClass(Hushback.class, mite.getBoundingBox().inflate(16.0), Entity::isAlive)) {
            if (nearest == null || h.distanceToSqr(mite) < nearest.distanceToSqr(mite)) {
                nearest = h;
            }
        }
        if (nearest == null) {
            return false;
        }
        mite.getNavigation().moveTo(nearest, 1.0);
        if (nearest.distanceToSqr(mite) < 2.5 * 2.5 && nearest.onGround()) {
            nearest.setDeltaMovement(nearest.getDeltaMovement().add(0.0, 0.3, 0.0));     // a startled hop
            level.playSound(null, nearest.getX(), nearest.getY(), nearest.getZ(), SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.NEUTRAL, 0.6F, 1.4F);
        }
        return true;
    }

    private WebOfLife() {}
}
