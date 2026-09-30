package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModItems;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Void Wanderer: a Cartographer survey drone that is still running.
//
// When it notices a player it floats over and shines a slow scanning beam over them. If the player
// stands still for the whole scan (5 seconds) it drops a Data Shard and leaves. Each Wanderer
// scans each player once. Attack it and it fires short laser pulses back, and teleports away when
// badly hurt; killed, it drops only a Cracked Lens.
public class VoidWanderer extends PathfinderMob {
    public static final int SCAN_TICKS = 100;
    private static final EntityDataAccessor<Integer> DATA_SCAN = SynchedEntityData.defineId(VoidWanderer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HOSTILE = SynchedEntityData.defineId(VoidWanderer.class, EntityDataSerializers.BOOLEAN);

    private final Set<UUID> scanned = new HashSet<>();
    private int departTicks = -1;

    public VoidWanderer(EntityType<? extends VoidWanderer> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 16.0)
            .add(Attributes.FLYING_SPEED, 0.35)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCAN, 0);
        builder.define(DATA_HOSTILE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LaserGoal());
        this.goalSelector.addGoal(2, new ScanGoal());
        this.goalSelector.addGoal(3, new HoverNearGroundGoal(this, 1.0, 12, 2, 6));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
    }

    public int getScanProgress() {
        return this.entityData.get(DATA_SCAN);
    }

    public boolean isHostile() {
        return this.entityData.get(DATA_HOSTILE);
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() || this.departTicks < 0) {
            return;
        }
        // Done here: rise and drift away, then fade out.
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.01, 0.0));
        if (--this.departTicks <= 0) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.4, this.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
            this.discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof Player player && !player.isCreative()) {
            this.entityData.set(DATA_HOSTILE, true);
            this.entityData.set(DATA_SCAN, 0);
            this.setTarget(player);
        }
        if (hurt && this.isAlive() && this.getHealth() < this.getMaxHealth() * 0.3F) {
            this.teleportAway();
        }
        return hurt;
    }

    private void teleportAway() {
        for (int attempt = 0; attempt < 16; attempt++) {
            BlockPos spot = HoverNearGroundGoal.pickSpot(this, 24, 2, 6);
            if (spot != null && spot.distSqr(this.blockPosition()) > 100) {
                if (this.randomTeleport(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, true)) {
                    this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.4F);
                    this.setTarget(null);
                    this.entityData.set(DATA_HOSTILE, false);
                    return;
                }
            }
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }

    @Override
    public float getVoicePitch() {
        return 1.8F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_BREAK;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Scanned", UUIDUtil.CODEC.listOf(), new ArrayList<>(this.scanned));
        output.putBoolean("Hostile", this.isHostile());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.scanned.clear();
        input.read("Scanned", UUIDUtil.CODEC.listOf()).ifPresent(this.scanned::addAll);
        this.entityData.set(DATA_HOSTILE, input.getBooleanOr("Hostile", false));
    }

    // Floats in front of a player and scans them. Moving more than a little restarts the scan.
    private class ScanGoal extends Goal {
        private @Nullable Player player;
        private @Nullable Vec3 standingAt;

        ScanGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            VoidWanderer self = VoidWanderer.this;
            if (self.isHostile() || self.departTicks >= 0 || self.random.nextInt(reducedTickDelay(10)) != 0) {
                return false;
            }
            List<Player> players = self.level().getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(16.0),
                p -> p.isAlive() && !p.isSpectator() && !self.scanned.contains(p.getUUID()) && self.hasLineOfSight(p));
            this.player = players.isEmpty() ? null : players.get(0);
            return this.player != null;
        }

        @Override
        public boolean canContinueToUse() {
            VoidWanderer self = VoidWanderer.this;
            return this.player != null && this.player.isAlive() && !self.isHostile() && self.departTicks < 0
                && self.distanceToSqr(this.player) < 24 * 24;
        }

        @Override
        public void stop() {
            VoidWanderer.this.entityData.set(DATA_SCAN, 0);
            this.player = null;
            this.standingAt = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            VoidWanderer self = VoidWanderer.this;
            Player target = this.player;
            if (target == null) {
                return;
            }
            self.getLookControl().setLookAt(target, 30.0F, 30.0F);
            // Hover about three blocks in front of the player, at their eye level.
            Vec3 front = target.getEyePosition().add(target.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(3.0));
            if (self.position().distanceToSqr(front) > 2.0) {
                self.getMoveControl().setWantedPosition(front.x, front.y - 0.4, front.z, 0.8);
            }
            if (self.distanceToSqr(target) > 36.0) {
                self.entityData.set(DATA_SCAN, 0);
                this.standingAt = null;
                return;
            }
            if (this.standingAt == null || this.standingAt.distanceToSqr(target.position()) > 0.25) {
                if (this.standingAt != null && self.getScanProgress() > 10) {
                    self.playSound(SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.8F);  // scan interrupted
                }
                this.standingAt = target.position();
                self.entityData.set(DATA_SCAN, 0);
                return;
            }
            int progress = self.getScanProgress() + 1;
            self.entityData.set(DATA_SCAN, progress);
            ServerLevel level = (ServerLevel) self.level();
            // The beam: a ring of light sweeping up and down the player.
            double sweep = (progress % 40) / 40.0;
            double y = target.getY() + target.getBbHeight() * (sweep < 0.5 ? sweep * 2 : 2 - sweep * 2);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + progress * 0.2;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX() + Math.cos(a) * 0.6, y, target.getZ() + Math.sin(a) * 0.6,
                    1, 0.0, 0.0, 0.0, 0.0);
            }
            if (progress % 20 == 0) {
                self.playSound(SoundEvents.BEACON_AMBIENT, 0.6F, 2.0F);
            }
            if (progress >= SCAN_TICKS) {
                self.scanned.add(target.getUUID());
                self.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
                var shard = self.spawnAtLocation(level, new ItemStack(ModItems.DATA_SHARD.get()));
                if (shard != null) {
                    shard.setDeltaMovement(target.position().subtract(self.position()).normalize().scale(0.2));
                }
                self.departTicks = 160;
                self.getNavigation().stop();
                Vec3 away = self.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize().scale(20.0);
                self.getMoveControl().setWantedPosition(self.getX() + away.x, self.getY() + 8.0, self.getZ() + away.z, 1.0);
            }
        }
    }

    // When angered: keep some distance and fire short laser pulses.
    private class LaserGoal extends Goal {
        private int cooldown;

        LaserGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = VoidWanderer.this.getTarget();
            return VoidWanderer.this.isHostile() && target != null && target.isAlive();
        }

        @Override
        public void stop() {
            VoidWanderer.this.setTarget(null);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            VoidWanderer self = VoidWanderer.this;
            LivingEntity target = self.getTarget();
            if (target == null) {
                return;
            }
            self.getLookControl().setLookAt(target, 30.0F, 30.0F);
            double distance = self.distanceTo(target);
            if (distance < 5.0 || distance > 10.0) {
                Vec3 dir = self.position().subtract(target.position()).normalize();
                Vec3 want = target.position().add(dir.scale(7.0)).add(0.0, 2.5, 0.0);
                self.getMoveControl().setWantedPosition(want.x, want.y, want.z, 1.0);
            }
            if (--this.cooldown <= 0 && self.hasLineOfSight(target) && distance < 16.0) {
                this.cooldown = 30;
                ServerLevel level = (ServerLevel) self.level();
                Vec3 from = self.getEyePosition();
                Vec3 to = target.getEyePosition().subtract(0.0, 0.3, 0.0);
                Vec3 step = to.subtract(from).scale(1.0 / 16.0);
                for (int i = 0; i <= 16; i++) {
                    Vec3 p = from.add(step.scale(i));
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                self.playSound(SoundEvents.BLAZE_SHOOT, 0.7F, 1.9F);
                target.hurtServer(level, self.damageSources().mobAttack(self), 3.0F);
            }
        }
    }
}
