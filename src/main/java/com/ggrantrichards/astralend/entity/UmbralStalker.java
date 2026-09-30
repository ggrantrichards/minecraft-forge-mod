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

// Umbral Stalker: a lanky, almost invisible predator of the Dark Matter Belts.
//
// It is only a faint shimmer until it attacks, is hurt, or stands in light (block light 7 or more:
// Null Lanterns, Spore Lanterns, torches). It creeps up on a player from behind and backs off when
// the player turns to face it - the reverse of an Enderman. Its blows hit hard and throw you away
// from it, which near an edge means toward the void.
public class UmbralStalker extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_REVEALED = SynchedEntityData.defineId(UmbralStalker.class, EntityDataSerializers.BOOLEAN);
    public static final int REVEALING_LIGHT = 7;
    private int revealTicks;

    public UmbralStalker(EntityType<? extends UmbralStalker> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEALED, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new BackOffWhenWatchedGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
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
            // Distortion where it moves.
            if (!this.isRevealed() && this.walkAnimation.speed() > 0.1F && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.revealTicks > 0) {
            this.revealTicks--;
        }
        boolean lit = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition().above()) >= REVEALING_LIGHT;
        this.entityData.set(DATA_REVEALED, this.revealTicks > 0 || this.hurtTime > 0 || lit);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.revealTicks = 40;
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
