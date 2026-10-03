package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import org.jspecify.annotations.Nullable;

// The Hydragrip: a sleek, otter-like thing of the Eclipse's pools. Its broods are born in the water and
// swim there until grown; the adults live on the banks and hunt endermites - and every endermite one
// eats, it coughs up an Ender Pearl. It never bothers anything else. Chorus Fruit tempts it.
public class Hydragrip extends Animal {
    public Hydragrip(EntityType<? extends Hydragrip> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 10) {
            @Override
            public boolean canUse() {
                return Hydragrip.this.isBaby() && super.canUse();     // the young keep to the water
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !Hydragrip.this.isBaby() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Endermite.class, true) {
            @Override
            public boolean canUse() {
                return !Hydragrip.this.isBaby() && super.canUse();
            }
        });
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.CHORUS_FRUIT);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.HYDRAGRIP.get().create(level, EntitySpawnReason.BREEDING);
    }

    // An endermite it kills, it eats - and coughs up a pearl.
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof Endermite mite && !mite.isAlive()) {
            this.playSound(SoundEvents.GENERIC_EAT.value(), 0.8F, 1.2F);
            level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
            this.spawnAtLocation(level, new ItemStack(Items.ENDER_PEARL));
        }
        return hit;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return target instanceof Endermite && super.canAttack(target);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.isInWater() && this.random.nextInt(10) == 0) {
            this.level().addParticle(ParticleTypes.BUBBLE, this.getRandomX(0.4), this.getY() + 0.2, this.getRandomZ(0.4), 0, 0.05, 0);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isInWater() ? SoundEvents.AXOLOTL_IDLE_WATER : SoundEvents.AXOLOTL_IDLE_AIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AXOLOTL_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AXOLOTL_DEATH;
    }

    static boolean livesOn(net.minecraft.world.level.block.state.BlockState below) {
        return below.is(ModBlocks.LIVING_TURF.get()) || below.is(net.minecraft.world.level.block.Blocks.WATER);
    }
}
