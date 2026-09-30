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
import net.minecraft.sounds.SoundSource;
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

// Umbral Stalker: a hunched, almost invisible brute of the Dark Matter Belts that walks on its knuckles.
//
// It is only a faint shimmer until it attacks, is hurt, or stands in light (block light 7 or more:
// Null Lanterns, Spore Lanterns, torches). It creeps up on a player from behind and backs off when
// the player turns to face it - the reverse of an Enderman. Its blows hit hard and throw you away
// from it, which near an edge means toward the void. Every few seconds, when you're close, it rears
// up and brings both fists down: a Ground Slam that hurts and throws everything around it outward.
public class UmbralStalker extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_REVEALED = SynchedEntityData.defineId(UmbralStalker.class, EntityDataSerializers.BOOLEAN);
    public static final int REVEALING_LIGHT = 7;
    public static final byte SLAM_EVENT = 70;
    public static final int SLAM_TICKS = 20;      // the whole slam: rearing up, then the fists come down
    private static final int SLAM_HIT_TICK = 12;
    private static final double SLAM_RADIUS = 3.5;
    private int revealTicks;
    private int slamCooldown = 60;
    private int attackAnimationTicks;  // client side: counts down while the arms lash out
    private int slamAnimationTicks;    // client side: counts down through a slam

    public UmbralStalker(EntityType<? extends UmbralStalker> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
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
        this.goalSelector.addGoal(2, new GroundSlamGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackAnimationTicks = 10;
        } else if (id == SLAM_EVENT) {
            this.slamAnimationTicks = SLAM_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // 0..1 through the strike, for the renderer.
    public float getAttackAnimation(float partialTicks) {
        return this.attackAnimationTicks > 0 ? 1.0F - (this.attackAnimationTicks - partialTicks) / 10.0F : 0.0F;
    }

    // 0..1 through a Ground Slam, for the renderer (0 when not slamming).
    public float getSlamAnimation(float partialTicks) {
        return this.slamAnimationTicks > 0 ? 1.0F - (this.slamAnimationTicks - partialTicks) / SLAM_TICKS : 0.0F;
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
            if (this.slamAnimationTicks > 0) {
                this.slamAnimationTicks--;
            }
            // Distortion where it moves.
            if (!this.isRevealed() && this.walkAnimation.speed() > 0.1F && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.revealTicks > 0) {
            this.revealTicks--;
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        boolean lit = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition().above()) >= REVEALING_LIGHT;
        this.entityData.set(DATA_REVEALED, this.revealTicks > 0 || this.hurtTime > 0 || lit);
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

    // Both fists into the ground: hurts everything close and throws it outward - off the edge, if
    // there is one.
    private void slam(ServerLevel level) {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SLAM_RADIUS, 1.0, SLAM_RADIUS),
            e -> e != this && e.isAlive() && !(e instanceof UmbralStalker))) {
            double distance = Math.sqrt(victim.distanceToSqr(x, y, z));
            if (distance > SLAM_RADIUS + 0.7) {
                continue;
            }
            if (victim.hurtServer(level, this.damageSources().mobAttack(this), 6.0F)) {
                Vec3 away = new Vec3(victim.getX() - x, 0.0, victim.getZ() - z);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : away.normalize();
                double strength = 1.4 - distance * 0.2;
                victim.push(away.x * strength, 0.55, away.z * strength);
                if (victim instanceof ServerPlayer player) {
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                }
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.2, z, 2, 0.6, 0.0, 0.6, 0.0);
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI * 2.0 / 32.0;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, x + Math.cos(angle) * 2.5, y + 0.1, z + Math.sin(angle) * 2.5,
                2, 0.1, 0.05, 0.1, 0.05);
        }
        level.playSound(null, x, y, z, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    // When close to its target and rested, it rears up (and shows itself) and slams the ground.
    private class GroundSlamGoal extends Goal {
        private int ticks;

        GroundSlamGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            LivingEntity target = self.getTarget();
            return self.slamCooldown <= 0 && self.onGround() && target != null && target.isAlive()
                && self.distanceToSqr(target) < SLAM_RADIUS * SLAM_RADIUS;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < SLAM_TICKS;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            UmbralStalker self = UmbralStalker.this;
            this.ticks = 0;
            self.getNavigation().stop();
            self.revealTicks = SLAM_TICKS + 40;
            self.level().broadcastEntityEvent(self, SLAM_EVENT);
            self.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 0.6F);
        }

        @Override
        public void tick() {
            UmbralStalker self = UmbralStalker.this;
            this.ticks++;
            if (self.getTarget() != null) {
                self.getLookControl().setLookAt(self.getTarget(), 30.0F, 30.0F);
            }
            if (this.ticks == SLAM_HIT_TICK && self.level() instanceof ServerLevel level) {
                self.slam(level);
            }
        }

        @Override
        public void stop() {
            UmbralStalker.this.slamCooldown = 120;
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
