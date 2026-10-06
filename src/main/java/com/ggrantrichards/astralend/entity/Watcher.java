package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.world.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The Watcher: a floating stone sentinel with one great eye, seen only at the edge of sight - over the
// Ashen Barrens, and among the Shattered Observatory's ruins. It hangs in the air, perfectly still
// but for the shards circling it.
//
// It appears only when you're alone, only far away, and only for a while. Look straight at it - with
// your eyes or through a spyglass - and it teleports away; but it turns up again a little later,
// further on, always on the way toward the nearest Leviathan: keep it in the corner of your eye and
// it leads you there. Come close and it is simply gone. It gives nothing (charts are the Void
// Wanderer's); catching it in a spyglass is "Something Looks Back". It never attacks and can't be
// reached. (MobSpawning decides where and when it appears.)
//
// One that's summoned stays where it's put: from a spawn egg, a command or a spawner it hangs there,
// watching, until it's killed with /kill (it still can't be hurt); the two the egg's pedestal calls
// stay for half a minute, then fade.
public class Watcher extends Mob {
    public static final int LIFETIME = 240;
    private static final double TOO_CLOSE = 24.0;
    private int lingerUntil = -1;      // summoned by the pedestal: stays (near or looked at) until this tick

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        this.vanish(level);
        return false;
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.EntitySpawnReason reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        if (reason == net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE || reason == net.minecraft.world.entity.EntitySpawnReason.COMMAND
                || reason == net.minecraft.world.entity.EntitySpawnReason.SPAWNER || reason == net.minecraft.world.entity.EntitySpawnReason.TRIAL_SPAWNER) {
            this.setPersistenceRequired();       // summoned: it stays (and stays across a reload)
        }
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // Called up for a while (the pedestal's two): it stays, near or looked at, for `ticks`, then fades.
    public void linger(int ticks) {
        this.lingerUntil = this.tickCount + ticks;
    }

    private boolean summoned() {
        return this.isPersistenceRequired() || this.lingerUntil >= 0;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Player nearest = level.getNearestPlayer(this, 160.0);
        if (this.summoned()) {
            if (this.lingerUntil >= 0 && this.tickCount > this.lingerUntil) {
                this.vanish(level);
            } else if (nearest != null) {
                this.getLookControl().setLookAt(nearest, 10.0F, 10.0F);
                Vec3 to = nearest.position().subtract(this.position());
                float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                this.yHeadRot = yaw;
            }
            return;
        }
        if (nearest == null || this.tickCount > LIFETIME) {
            this.vanish(level);
            return;
        }
        // Always facing whoever is nearest.
        Vec3 to = nearest.position().subtract(this.position());
        float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        if (this.distanceToSqr(nearest) < TOO_CLOSE * TOO_CLOSE) {
            this.vanish(level);
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (this.isLookedAtBy(player, player.isScoping() ? 0.997 : 0.9985)) {
                // Looked at straight on: it teleports away, and will turn up again further along.
                if (player.isScoping()) {
                    ModAdvancements.award(player, "something_looks_back");
                }
                com.ggrantrichards.astralend.world.MobSpawning.watcherSeen(player);
                this.vanish(level);
                return;
            }
        }
    }

    private boolean isLookedAtBy(Player player, double within) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 to = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0).subtract(player.getEyePosition());
        double distance = to.length();
        return distance < 160.0 && view.dot(to.normalize()) > within && player.hasLineOfSight(this);
    }

    private void vanish(ServerLevel level) {
        if (this.isRemoved() || this.isPersistenceRequired()) {
            return;
        }
        // Teleported away: the enderman's way of going, lower and slower.
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.4, this.getZ(), 48, 0.4, 1.0, 0.4, 0.4);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.6, this.getZ(), 16, 0.3, 0.8, 0.3, 0.02);
        level.playSound(null, this.getX(), this.getY() + 1.6, this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.5F);
        this.discard();
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, net.minecraft.world.level.block.state.BlockState onState, BlockPos pos) {
    }
}
