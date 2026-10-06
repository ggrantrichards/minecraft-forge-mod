package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

// The Gloamgrub: a fat, slow grub of the Hollow Roots, pale and soft, with lights down its back like a
// row of lanterns. It grazes the Hollow Glowcaps on the cave floors - eat one and its lights burn
// brighter for a while - and it's harmless: hurt it and it curls up and shuffles off. It's how you
// find your way in the Roots: follow the lights.
public class Gloamgrub extends PathfinderMob {
    private static final EntityDataAccessor<Integer> DATA_FED = SynchedEntityData.defineId(Gloamgrub.class, EntityDataSerializers.INT);

    public Gloamgrub(EntityType<? extends Gloamgrub> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.14);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FED, 0);
    }

    // How bright it glows: 0 (dim) to 1 (just fed).
    public float glow() {
        return Math.min(1.0F, this.entityData.get(DATA_FED) / 600.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        this.goalSelector.addGoal(3, new GrazeGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        int fed = this.entityData.get(DATA_FED);
        if (fed > 0) {
            this.entityData.set(DATA_FED, fed - 1);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    // Walks to a Hollow Glowcap nearby and eats it.
    private class GrazeGoal extends Goal {
        private @Nullable BlockPos cap;
        private int time;

        GrazeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Gloamgrub.this.getRandom().nextInt(200) != 0) {
                return false;
            }
            BlockPos at = Gloamgrub.this.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -2, -6), at.offset(6, 2, 6))) {
                if (Gloamgrub.this.level().getBlockState(p).is(ModBlocks.HOLLOW_GLOWCAP.get())) {
                    this.cap = p.immutable();
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.cap != null && this.time < 240 && Gloamgrub.this.level().getBlockState(this.cap).is(ModBlocks.HOLLOW_GLOWCAP.get());
        }

        @Override
        public void start() {
            this.time = 0;
            Gloamgrub.this.getNavigation().moveTo(this.cap.getX() + 0.5, this.cap.getY(), this.cap.getZ() + 0.5, 0.9);
        }

        @Override
        public void tick() {
            this.time++;
            Gloamgrub.this.getLookControl().setLookAt(this.cap.getX() + 0.5, this.cap.getY() + 0.3, this.cap.getZ() + 0.5);
            if (Gloamgrub.this.distanceToSqr(this.cap.getX() + 0.5, this.cap.getY(), this.cap.getZ() + 0.5) < 1.6
                    && Gloamgrub.this.level() instanceof ServerLevel level) {
                level.setBlock(this.cap, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.GLOW, this.cap.getX() + 0.5, this.cap.getY() + 0.3, this.cap.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.02);
                Gloamgrub.this.playSound(SoundEvents.GENERIC_EAT.value(), 0.6F, 1.4F);
                Gloamgrub.this.entityData.set(DATA_FED, 1200);
                Gloamgrub.this.heal(2.0F);
                this.cap = null;
            }
        }
    }
}
