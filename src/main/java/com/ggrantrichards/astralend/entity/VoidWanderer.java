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
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Void Wanderer: a purpur sentinel left by the End's builders, still drifting the dark and charting
// whatever it finds.
//
// Now and then, when it notices a player nearby, it floats over and studies them, a faint ring of
// sparks sweeping their legs and body. If the player stands still for the whole study (5 seconds) it
// leaves them a Chart Fragment - a shard of the star map it carries - and drifts away; if they move, or
// don't stand still for it, it gives up and drifts away all the same. Each Wanderer tries each player
// once; one at a time, and after a study no Wanderer comes for that player for five minutes. Attack
// it and it looses homing starbolts back (shulker bullets: they lift you if they hit), and it blinks
// away when badly hurt; killed, it drops its Cracked Lens.
public class VoidWanderer extends PathfinderMob {
    public static final int SCAN_TICKS = 100;
    private static final EntityDataAccessor<Integer> DATA_SCAN = SynchedEntityData.defineId(VoidWanderer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HOSTILE = SynchedEntityData.defineId(VoidWanderer.class, EntityDataSerializers.BOOLEAN);

    private static final long STUDY_REST = 6000L;          // ticks between studies of one player, by any Wanderer
    private static final java.util.Map<UUID, Long> LAST_STUDIED = new java.util.concurrent.ConcurrentHashMap<>();
    private final Set<UUID> scanned = new HashSet<>();
    private int departTicks = -1;
    private @Nullable UUID studying;

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
        this.goalSelector.addGoal(1, new StarboltGoal());
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
                ServerLevel level = (ServerLevel) this.level();
                level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.4, this.getZ(), 24, 0.3, 0.3, 0.3, 0.2);
                this.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
                this.getNavigation().stop();
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.4F);
                this.setTarget(null);
                this.entityData.set(DATA_HOSTILE, false);
                return;
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

    // Done with a player: rise and drift away from them, then fade out (aiStep).
    private void leave(Player from) {
        this.departTicks = 160;
        this.getNavigation().stop();
        Vec3 away = this.position().subtract(from.position()).multiply(1.0, 0.0, 1.0).normalize().scale(20.0);
        this.getMoveControl().setWantedPosition(this.getX() + away.x, this.getY() + 8.0, this.getZ() + away.z, 1.0);
    }

    // Another Wanderer is studying this player already.
    private static boolean beingStudied(VoidWanderer self, Player player) {
        return !self.level().getEntitiesOfClass(VoidWanderer.class, player.getBoundingBox().inflate(32.0),
            w -> w != self && player.getUUID().equals(w.studying)).isEmpty();
    }

    // Floats in front of a player and studies them. Moving more than a little spoils it.
    private class ScanGoal extends Goal {
        private @Nullable Player player;
        private @Nullable Vec3 standingAt;
        private int waited;
        private boolean done;

        ScanGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            VoidWanderer self = VoidWanderer.this;
            if (self.isHostile() || self.departTicks >= 0 || self.random.nextInt(reducedTickDelay(40)) != 0) {
                return false;
            }
            long now = self.level().getGameTime();
            List<Player> players = self.level().getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(12.0),
                p -> p.isAlive() && !p.isSpectator() && !self.scanned.contains(p.getUUID()) && self.hasLineOfSight(p)
                    && now - LAST_STUDIED.getOrDefault(p.getUUID(), -STUDY_REST) >= STUDY_REST && !beingStudied(self, p));
            this.player = players.isEmpty() ? null : players.get(0);
            if (this.player == null) {
                return false;
            }
            this.waited = 0;
            this.done = false;
            self.studying = this.player.getUUID();
            self.scanned.add(this.player.getUUID());       // one try per player, whatever comes of it
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            VoidWanderer self = VoidWanderer.this;
            return !this.done && this.player != null && this.player.isAlive() && !self.isHostile() && self.departTicks < 0
                && self.distanceToSqr(this.player) < 20 * 20;
        }

        @Override
        public void stop() {
            VoidWanderer self = VoidWanderer.this;
            if (this.player != null) {
                LAST_STUDIED.put(this.player.getUUID(), self.level().getGameTime());
                if (self.departTicks < 0 && !self.isHostile()) {
                    self.leave(this.player);         // studied or not, it has seen enough
                }
            }
            self.studying = null;
            self.entityData.set(DATA_SCAN, 0);
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
            // Hover four or five blocks in front of the player, a little below their eyes.
            Vec3 front = target.getEyePosition().add(target.getViewVector(1.0F).multiply(1.0, 0.0, 1.0).normalize().scale(4.5));
            if (self.position().distanceToSqr(front) > 2.0) {
                self.getMoveControl().setWantedPosition(front.x, front.y - 0.6, front.z, 0.8);
            }
            if (++this.waited > 200 && self.getScanProgress() == 0) {
                this.done = true;                    // ten seconds and they haven't stood still for it
                return;
            }
            if (self.distanceToSqr(target) > 64.0) {
                self.entityData.set(DATA_SCAN, 0);
                this.standingAt = null;
                return;
            }
            if (this.standingAt == null || this.standingAt.distanceToSqr(target.position()) > 0.25) {
                if (this.standingAt != null && self.getScanProgress() > 0) {
                    self.playSound(SoundEvents.BEACON_DEACTIVATE, 0.5F, 1.8F);  // the study is spoilt: it gives up
                    this.done = true;
                    return;
                }
                this.standingAt = target.position();
                self.entityData.set(DATA_SCAN, 0);
                return;
            }
            int progress = self.getScanProgress() + 1;
            self.entityData.set(DATA_SCAN, progress);
            ServerLevel level = (ServerLevel) self.level();
            // The beam: a faint ring of sparks sweeping from the feet to the chest (never across the eyes).
            if (progress % 3 == 0) {
                double sweep = (progress % 40) / 40.0;
                double y = target.getY() + target.getBbHeight() * 0.7 * (sweep < 0.5 ? sweep * 2 : 2 - sweep * 2);
                for (int i = 0; i < 3; i++) {
                    double a = i * Math.PI * 2 / 3 + progress * 0.2;
                    level.sendParticles(ParticleTypes.WITCH, target.getX() + Math.cos(a) * 0.6, y, target.getZ() + Math.sin(a) * 0.6,
                        1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            if (progress % 20 == 0) {
                self.playSound(SoundEvents.BEACON_AMBIENT, 0.6F, 2.0F);
            }
            if (progress >= SCAN_TICKS) {
                self.scanned.add(target.getUUID());
                if (target instanceof net.minecraft.server.level.ServerPlayer studied) {
                    com.ggrantrichards.astralend.world.ModAdvancements.award(studied, "hold_still");
                }
                self.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
                var shard = self.spawnAtLocation(level, new ItemStack(ModItems.CHART_FRAGMENT.get()));
                if (shard != null) {
                    shard.setDeltaMovement(target.position().subtract(self.position()).normalize().scale(0.2));
                }
                this.done = true;
            }
        }
    }

    // When angered: keep some distance and loose starbolts.
    private class StarboltGoal extends Goal {
        private int cooldown;

        StarboltGoal() {
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
                this.cooldown = 40 + self.getRandom().nextInt(20);
                ServerLevel level = (ServerLevel) self.level();
                // A starbolt: the End's own homing shot, slow enough to dodge or knock away.
                ShulkerBullet bolt = new ShulkerBullet(level, self, target, net.minecraft.core.Direction.Axis.Y);
                level.addFreshEntity(bolt);
                self.playSound(SoundEvents.SHULKER_SHOOT, 1.0F, 1.4F);
            }
        }
    }
}
