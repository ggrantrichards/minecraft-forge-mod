package com.ggrantrichards.astralend.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Orrery Sentinel: the guardian of an Abandoned Orrery, a big, slow clockwork construct of the
// Cartographers, built like a Void Wanderer. It stands dormant by the vault (and can't be hurt) until
// someone rekindles the Orrery Heart; then it wakes and guards the vault.
//
// Its attack is a beam of focused starlight from its lens: it locks on, the beam traces toward you for
// a second and a half (you can see and hear it charge), then it fires. Break its line of sight before
// it fires and the shot misses; that's the fight. Up close it also swings its heavy fists.
// It always drops a Starforge Template and a Rift Pearl.
public class OrrerySentinel extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_AWAKE = SynchedEntityData.defineId(OrrerySentinel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_CHARGE = SynchedEntityData.defineId(OrrerySentinel.class, EntityDataSerializers.INT);
    public static final int CHARGE_TICKS = 30;
    private static final float BEAM_DAMAGE = 9.0F;

    private final ServerBossEvent bossBar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.astralend.orrery_sentinel"),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);

    public OrrerySentinel(EntityType<? extends OrrerySentinel> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        this.bossBar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 160.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_AWAKE, false);
        builder.define(DATA_CHARGE, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BeamGoal());
        this.goalSelector.addGoal(2, new AwakeOnly(new MeleeAttackGoal(this, 1.0, true)));
        this.goalSelector.addGoal(8, new AwakeOnly(new LookAtPlayerGoal(this, Player.class, 16.0F)));
        this.goalSelector.addGoal(8, new AwakeOnly(new RandomLookAroundGoal(this)));
        this.targetSelector.addGoal(1, new AwakeOnly(new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new AwakeOnly(new NearestAttackableTargetGoal<>(this, Player.class, true)));
    }

    public boolean isAwake() {
        return this.entityData.get(DATA_AWAKE);
    }

    // 0 while not charging; otherwise how far through its wind-up the beam is (1..CHARGE_TICKS).
    public int getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    public void wake() {
        if (!this.isAwake()) {
            this.entityData.set(DATA_AWAKE, true);
            this.bossBar.setVisible(true);
            this.playSound(SoundEvents.BEACON_ACTIVATE, 3.0F, 0.5F);
            this.setPersistenceRequired();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!this.isAwake() && !source.isCreativePlayer()) {
            return false;   // asleep, it's just a statue of bronze
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean isPushable() {
        return this.isAwake();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        this.bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.isAwake() && this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.6), this.getY() + 2.4 + this.random.nextDouble() * 0.6,
                this.getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isAwake() ? SoundEvents.BEACON_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Awake", this.isAwake());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getBooleanOr("Awake", false)) {
            this.entityData.set(DATA_AWAKE, true);
            this.bossBar.setVisible(true);
        }
    }

    // A goal that only runs while the Sentinel is awake.
    private class AwakeOnly extends Goal {
        private final Goal goal;

        AwakeOnly(Goal goal) {
            this.goal = goal;
            this.setFlags(goal.getFlags());
        }

        @Override
        public boolean canUse() {
            return OrrerySentinel.this.isAwake() && this.goal.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return OrrerySentinel.this.isAwake() && this.goal.canContinueToUse();
        }

        @Override
        public void start() {
            this.goal.start();
        }

        @Override
        public void stop() {
            this.goal.stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return this.goal.requiresUpdateEveryTick();
        }

        @Override
        public void tick() {
            this.goal.tick();
        }
    }

    // Lock on, charge (a beam of sparks traces toward the target), fire if it can still see them.
    private class BeamGoal extends Goal {
        private int cooldown = 60;
        private int charge;
        private @Nullable LivingEntity target;

        BeamGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            OrrerySentinel self = OrrerySentinel.this;
            if (!self.isAwake() || --this.cooldown > 0) {
                return false;
            }
            LivingEntity t = self.getTarget();
            if (t == null || !t.isAlive() || self.distanceTo(t) > 24.0F || self.distanceTo(t) < 3.5F || !self.hasLineOfSight(t)) {
                this.cooldown = 10;
                return false;
            }
            this.target = t;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return this.target != null && this.target.isAlive() && this.charge <= CHARGE_TICKS;
        }

        @Override
        public void start() {
            this.charge = 0;
            OrrerySentinel.this.getNavigation().stop();
            OrrerySentinel.this.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.7F);
        }

        @Override
        public void stop() {
            OrrerySentinel.this.entityData.set(DATA_CHARGE, 0);
            this.cooldown = 80 + OrrerySentinel.this.random.nextInt(40);
            this.target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            OrrerySentinel self = OrrerySentinel.this;
            if (this.target == null || !(self.level() instanceof ServerLevel level)) {
                return;
            }
            self.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
            this.charge++;
            self.entityData.set(DATA_CHARGE, this.charge);
            Vec3 from = self.getEyePosition();
            Vec3 to = this.target.getEyePosition();
            // The wind-up: sparks creep along the line toward the target, further each tick.
            Vec3 step = to.subtract(from);
            double reach = Math.min(1.0, this.charge / (double) CHARGE_TICKS);
            for (int i = 0; i < 6; i++) {
                Vec3 p = from.add(step.scale(reach * self.random.nextDouble()));
                level.sendParticles(new DustParticleOptions(0x8FF6FF, 1.2F), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
            if (this.charge == CHARGE_TICKS) {
                if (self.hasLineOfSight(this.target)) {
                    for (int i = 0; i <= 24; i++) {
                        Vec3 p = from.add(step.scale(i / 24.0));
                        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                    this.target.hurtServer(level, self.damageSources().indirectMagic(self, self), BEAM_DAMAGE);
                    level.playSound(null, this.target.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.5F, 0.6F);
                } else {
                    level.playSound(null, self.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.0F, 1.5F);
                }
                this.charge++;
            }
        }
    }
}
