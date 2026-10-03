package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Hushback: a herd grazer of the Eclipse's heaven half, its hide so pale and clear that its bones
// show through. It makes almost no sound. Hurt one and the whole herd round it blinks away at once,
// the way an enderman does, and reappears some way off - so it's food and fleece for the patient, not
// the greedy. Shear it for Hushfleece (it grows back as it grazes the turf); Breathcomb tempts it and
// makes it breed.
public class Hushback extends Animal {
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(Hushback.class, EntityDataSerializers.BOOLEAN);
    private int regrow;

    public Hushback(EntityType<? extends Hushback> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHEARED, false);
    }

    public boolean isSheared() {
        return this.entityData.get(SHEARED);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.BREATHCOMB.get().asItem());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.HUSHBACK.get().create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.SHEARS) && !this.isSheared() && !this.isBaby()) {
            if (this.level() instanceof ServerLevel level) {
                this.entityData.set(SHEARED, true);
                this.regrow = 2400 + this.random.nextInt(2400);
                int count = 1 + this.random.nextInt(2);
                for (int i = 0; i < count; i++) {
                    this.spawnAtLocation(level, new ItemStack(ModItems.HUSHFLEECE.get()));
                }
                level.playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.6F, 1.4F);
                held.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide() && this.isSheared() && --this.regrow <= 0
                && this.level().getBlockState(this.blockPosition().below()).is(ModBlocks.LIVING_TURF.get())) {
            this.entityData.set(SHEARED, false);
        }
    }

    // Hurt one, and the herd round it scatters through the void: each blinks 8-16 blocks away.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive()) {
            for (Hushback other : level.getEntitiesOfClass(Hushback.class, this.getBoundingBox().inflate(16.0))) {
                other.blinkAway(level);
            }
        }
        return hurt;
    }

    void blinkAway(ServerLevel level) {
        Vec3 from = this.position();
        BlockPos to = EndMobs.safeSpotNear(level, this.blockPosition(), 8, 16, this.random);
        if (to != null) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 0.6, from.z, 24, 0.3, 0.5, 0.3, 0.05);
            level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5F, 1.4F);
            this.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            this.getNavigation().stop();
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;   // a hush: it makes no sound of its own
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SHEEP_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHEEP_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.3F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Sheared", this.isSheared());
        output.putInt("Regrow", this.regrow);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SHEARED, input.getBooleanOr("Sheared", false));
        this.regrow = input.getIntOr("Regrow", 0);
    }
}
