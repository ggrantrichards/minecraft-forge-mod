package com.ggrantrichards.astralend.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Umbral Stalker: a hunched, almost invisible brute of the Hollow Isless that walks on its knuckles.
//
// While it has no prey it is completely invisible in the dark - not even a shimmer. Once it picks a
// target it activates: a faint shimmer shows where it moves, and it becomes fully visible when it
// attacks, is hurt, or stands in light (block light 7 or more: Null Lanterns, Spore Lanterns, torches). It creeps up on a player from behind and backs off when
// the player turns to face it - the reverse of an Enderman. Its blows hit hard and throw you away
// from it, which near an edge means toward the void.
//
// It hunts like a lion. Hidden, it creeps in low and slow, lumbering on its knuckles; once it is a
// few blocks behind you and unseen, it pounces - a sudden leap that reveals it and lands a heavy
// ambush blow. After that it fights in the open for a moment, then melts back into the dark.
public class UmbralStalker extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_REVEALED = SynchedEntityData.defineId(UmbralStalker.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(UmbralStalker.class, EntityDataSerializers.BOOLEAN);
    public static final int REVEALING_LIGHT = 7;
    public static final byte POUNCE_EVENT = 70;
    public static final int POUNCE_TICKS = 14;    // the leap, arms reaching out
    private int revealTicks;
    private boolean displayHunting;
    private int pounceCooldown = 60;
    private int attackAnimationTicks;  // client side: counts down while the arms lash out
    private int pounceAnimationTicks;  // client side: counts down through a pounce

    public UmbralStalker(EntityType<? extends UmbralStalker> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEALED, false);
        builder.define(DATA_HUNTING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BackOffWhenWatchedGoal());
        this.goalSelector.addGoal(2, new PounceGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
    }

    // True once it has a target: the shimmer shows. Without one it is invisible in the dark.
    public boolean isHunting() {
        return this.entityData.get(DATA_HUNTING);
    }

    // For the screenshot tool, which summons it without AI: show it as if it were hunting.
    public void showAsHunting() {
        this.displayHunting = true;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackAnimationTicks = 10;
        } else if (id == POUNCE_EVENT) {
            this.pounceAnimationTicks = POUNCE_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // 0..1 through the strike, for the renderer.
    public float getAttackAnimation(float partialTicks) {
        return this.attackAnimationTicks > 0 ? 1.0F - (this.attackAnimationTicks - partialTicks) / 10.0F : 0.0F;
    }

    // 0..1 through a pounce, for the renderer (0 when not pouncing).
    public float getPounceAnimation(float partialTicks) {
        return this.pounceAnimationTicks > 0 ? 1.0F - (this.pounceAnimationTicks - partialTicks) / POUNCE_TICKS : 0.0F;
    }

    // True if the player is facing this stalker (within about 60 degrees) and can see it.
    public boolean isWatchedBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ()).normalize();
        return view.dot(toMe) > 0.5 && player.hasLineOfSight(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.attackAnimationTicks > 0) {
                this.attackAnimationTicks--;
            }
            if (this.pounceAnimationTicks > 0) {
                this.pounceAnimationTicks--;
            }
            // Distortion where it moves.
            if (this.isHunting() && !this.isRevealed() && this.walkAnimation.speed() > 0.1F && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.revealTicks > 0) {
            this.revealTicks--;
        }
        if (this.pounceCooldown > 0) {
            this.pounceCooldown--;
        }
        boolean lit = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition().above()) >= REVEALING_LIGHT;
        this.entityData.set(DATA_REVEALED, this.revealTicks > 0 || this.hurtTime > 0 || lit);
        this.entityData.set(DATA_HUNTING, this.displayHunting || this.getTarget() != null);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.revealTicks = 40;
        level.broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) {
            // Thrown away from the stalker - toward the void, if that's what's behind you.
            Vec3 away = living.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
            living.push(away.x * 1.1, 0.35, away.z * 1.1);
            if (living instanceof ServerPlayer player) {
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.revealTicks = 60;
        }
        return hurt;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.random.nextInt(4) == 0 ? SoundEvents.WARDEN_NEARBY_CLOSER : null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    // The ambush: from a few blocks behind an unsuspecting player, it leaps. If the leap carries it
    // into reach, the blow lands hard (12) and throws the player. Then it fights in the open until
    // it is watched again, and slinks off.
    private class PounceGoal extends Goal {
        private int ticks;
        private boolean struck;

        PounceGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            if (!(self.getTarget() instanceof Player player) || self.pounceCooldown > 0 || self.isRevealed() || !self.onGround()) {
                return false;
            }
            double distance = self.distanceToSqr(player);
            return distance > 3.0 * 3.0 && distance < 7.0 * 7.0 && !self.isWatchedBy(player) && self.hasLineOfSight(player);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < POUNCE_TICKS && !this.struck;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            UmbralStalker self = UmbralStalker.this;
            LivingEntity target = self.getTarget();
            this.ticks = 0;
            this.struck = false;
            self.getNavigation().stop();
            self.revealTicks = 60;
            Vec3 leap = target.position().subtract(self.position()).multiply(1.0, 0.0, 1.0);
            double distance = leap.length();
            leap = leap.normalize().scale(Math.min(1.6, 0.25 + distance * 0.2));
            self.setDeltaMovement(leap.x, 0.45, leap.z);
            self.getLookControl().setLookAt(target, 60.0F, 30.0F);
            self.level().broadcastEntityEvent(self, POUNCE_EVENT);
            self.playSound(SoundEvents.RAVAGER_ROAR, 0.9F, 0.8F);
        }

        @Override
        public void tick() {
            UmbralStalker self = UmbralStalker.this;
            LivingEntity target = self.getTarget();
            this.ticks++;
            if (target == null || !(self.level() instanceof ServerLevel level)) {
                return;
            }
            self.getLookControl().setLookAt(target, 60.0F, 30.0F);
            if (self.distanceToSqr(target) < 2.6 * 2.6) {
                this.struck = true;
                level.broadcastEntityEvent(self, (byte) 4);
                if (target.hurtServer(level, self.damageSources().mobAttack(self), 12.0F)) {
                    Vec3 away = target.position().subtract(self.position()).multiply(1.0, 0.0, 1.0).normalize();
                    target.push(away.x * 1.3, 0.4, away.z * 1.3);
                    if (target instanceof ServerPlayer player) {
                        player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    }
                }
            }
        }

        @Override
        public void stop() {
            UmbralStalker.this.pounceCooldown = 200;
        }
    }

    // When the target turns to face it, the stalker slips away and waits for another chance.
    private class BackOffWhenWatchedGoal extends Goal {
        private int ticks;

        BackOffWhenWatchedGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            return self.getTarget() instanceof Player player && self.revealTicks <= 0
                && self.distanceToSqr(player) > 6.0 && self.isWatchedBy(player);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < 60 && !UmbralStalker.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            UmbralStalker self = UmbralStalker.this;
            this.ticks = 0;
            if (self.getTarget() != null) {
                Vec3 away = DefaultRandomPos.getPosAway(self, 12, 5, self.getTarget().position());
                if (away != null) {
                    self.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                }
            }
        }

        @Override
        public void tick() {
            this.ticks++;
        }
    }
}
