package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// Cinder Mite: a small glowing burrower of the Corona, about the size of an Endermite.
//
// They come out of the Sunscorched Stone in small swarms, go for players and set them alight,
// and burrow back into the stone after about a minute - timed pressure, not a constant threat.
// Fire can't hurt them, but water puts them out: they die in about two seconds in water or rain.
// (Once Stellar Flares arrive in Phase 5, the swarms will come out right after each flare.)
public class CinderMite extends Monster {
    private static final int BURROW_AFTER = 1200;  // one minute
    private int life;

    public CinderMite(EntityType<? extends CinderMite> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    public static boolean checkCinderMiteSpawnRules(EntityType<CinderMite> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                    BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)
            && (below.is(ModBlocks.SUNSCORCHED_STONE.get()) || below.is(ModBlocks.CHARRED_END_STONE.get()));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME, this.getRandomX(0.4), this.getY() + 0.2, this.getRandomZ(0.4), 0.0, 0.02, 0.0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        // Water puts them out.
        if (this.isInWaterOrRain()) {
            if (this.tickCount % 7 == 0) {
                this.hurtServer(level, this.damageSources().drown(), 1.0F);
                level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
            }
            if (this.tickCount % 20 == 0) {
                this.playSound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.6F);
            }
        }
        if (!this.isPersistenceRequired()) {
            this.life++;
        }
        if (this.life >= BURROW_AFTER && (this.getTarget() == null || this.life >= BURROW_AFTER * 2) && this.onGround()) {
            this.burrow(level);
        }
    }

    private void burrow(ServerLevel level) {
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.1, 0.2, 0.1);
        }
        this.playSound(SoundEvents.GRAVEL_BREAK, 0.6F, 1.4F);
        this.discard();
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            target.igniteForSeconds(3.0F);
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMITE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMITE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMITE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.ENDERMITE_STEP, 0.15F, 1.2F);
    }

    @Override
    public float getVoicePitch() {
        return 1.4F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Lifetime", this.life);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.life = input.getIntOr("Lifetime", 0);
    }
}
