package com.ggrantrichards.astralend.entity;

import java.util.EnumSet;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

// The Shardback: the Crystal Vaults' hunter. A low, heavy thing with a back grown over in Glimmer
// Crystal, it waits folded down in a geode, still as the crystals round it, and from a few steps off
// it's one more cluster. Come within six blocks (or hit it) and it unfolds and leaps. Back in the
// dark for a while with nothing to hunt, it folds itself down again. An EnderMan that blinks into a
// vault and stands staring at the crystals gets the same welcome.
public class Shardback extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_HIDDEN = SynchedEntityData.defineId(Shardback.class, EntityDataSerializers.BOOLEAN);
    private int idle;

    public Shardback(EntityType<? extends Shardback> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 6.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.FOLLOW_RANGE, 18.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HIDDEN, true);
    }

    public boolean isHidden() {
        return this.entityData.get(DATA_HIDDEN);
    }

    private void setHidden(boolean hidden) {
        this.entityData.set(DATA_HIDDEN, hidden);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoldStillGoal());
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new AmbushEndermanGoal());
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
        LivingEntity target = this.getTarget();
        if (this.isHidden() && target != null && target.distanceToSqr(this) > 36.0 && target != this.getLastHurtByMob()) {
            this.setTarget(null);      // hidden, it lets you pass - unless you come within six blocks
            target = null;
        }
        if (this.isHidden() && target != null) {
            this.setHidden(false);
            this.playSound(SoundEvents.AMETHYST_BLOCK_BREAK, 1.2F, 0.5F);
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.4, 0.3, 0.4, 0.05);
            this.idle = 0;
        } else if (!this.isHidden() && target == null && ++this.idle > 400) {
            this.setHidden(true);
            this.playSound(SoundEvents.AMETHYST_BLOCK_PLACE, 1.0F, 0.6F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, amount);
        }
        if (this.isHidden()) {
            this.setHidden(false);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Hidden", this.isHidden());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        this.setHidden((input.contains("Hidden") ? input.getBoolean("Hidden") : true));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isHidden() ? null : SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    // An EnderMan within six blocks (where a player would set it off too).
    private class AmbushEndermanGoal extends Goal {
        AmbushEndermanGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Shardback self = Shardback.this;
            if (self.getTarget() != null || self.random.nextInt(5) != 0) {
                return false;
            }
            for (var e : self.level().getEntitiesOfClass(net.minecraft.world.entity.monster.EnderMan.class, self.getBoundingBox().inflate(6.0),
                    LivingEntity::isAlive)) {
                self.setTarget(e);
                break;
            }
            return false;
        }
    }

    // Hidden: it doesn't move or turn at all.
    private class HoldStillGoal extends Goal {
        HoldStillGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Shardback.this.isHidden();
        }

        @Override
        public void tick() {
            Shardback.this.getNavigation().stop();
        }
    }
}
