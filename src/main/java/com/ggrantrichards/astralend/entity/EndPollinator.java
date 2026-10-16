package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// End Pollinator: a small jellyfish-like drifter with glowing tendrils.
//
// It drifts between glowing flora (#astralend:pollinator_flora) and hovers over each plant for a
// few seconds; a pollinated plant spreads to a free spot next to it. Every few minutes it leaves
// Glowing Spores behind, the way a chicken lays eggs, so there's no reason to kill it (killing it
// drops nothing). Starblooms tempt it and make it breed.
//
// When a Drift Whale pod passes low overhead, Pollinators rise and drift along in its wake for a
// while, glowing behind its tail, then sink back home to their plants. Whale calves chase them in
// play (DriftWhale); a Pollinator only scatters a little.
public class EndPollinator extends Animal {
    public static final TagKey<Block> FLORA = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "pollinator_flora"));
    private static final int MIN_SPORE_TIME = 6000;   // 5 minutes
    private static final int MAX_SPORE_TIME = 12000;  // 10 minutes

    private int sporeTime;
    private int pollinateCooldown;
    private int wakeCooldown = 200;

    public EndPollinator(EntityType<? extends EndPollinator> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.sporeTime = MIN_SPORE_TIME + this.random.nextInt(MAX_SPORE_TIME - MIN_SPORE_TIME);
        this.pollinateCooldown = 100 + this.random.nextInt(200);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.FLYING_SPEED, 0.3)
            .add(Attributes.MOVEMENT_SPEED, 0.15);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(4, new RideTheWakeGoal());
        this.goalSelector.addGoal(4, new PollinateGoal());
        this.goalSelector.addGoal(5, new HoverNearGroundGoal(this, 1.0, 8, 1, 4));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModBlocks.STARBLOOM.get().asItem());
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.POLLINATOR.get().create(level, MobSpawnType.BREEDING);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(8) == 0) {
                this.level().addParticle(ParticleTypes.GLOW, this.getRandomX(0.4), this.getY() + 0.1, this.getRandomZ(0.4), 0.0, -0.02, 0.0);
            }
            return;
        }
        if (this.pollinateCooldown > 0) {
            this.pollinateCooldown--;
        }
        if (this.wakeCooldown > 0) {
            this.wakeCooldown--;
        }
        if (this.isAlive() && !this.isBaby() && --this.sporeTime <= 0) {
            this.spawnAtLocation((ServerLevel) this.level(), new ItemStack(ModItems.GLOWING_SPORES.get()));
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.6F);
            this.sporeTime = MIN_SPORE_TIME + this.random.nextInt(MAX_SPORE_TIME - MIN_SPORE_TIME);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ALLAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ALLAY_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.35F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        output.putInt("SporeTime", this.sporeTime);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        this.sporeTime = (input.contains("SporeTime") ? input.getInt("SporeTime") : MIN_SPORE_TIME);
    }

    // The nearest glowing flora within `range` blocks sideways and `height` up or down, or null.
    private static @Nullable BlockPos findFlora(Level level, BlockPos from, int range, int height) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(from.offset(-range, -height, -range), from.offset(range, height, range))) {
            if (level.getBlockState(pos).is(FLORA)) {
                double d = pos.distSqr(from);
                if (d < bestDistance) {
                    bestDistance = d;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    // A pollinated plant spreads: a copy of it grows on a free spot of ground next to it.
    static void pollinate(ServerLevel level, BlockPos flower, BlockState state) {
        if (state.getBlock() instanceof DoublePlantBlock) {
            return;
        }
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos pos = flower.offset(level.getRandom().nextInt(5) - 2, level.getRandom().nextInt(3) - 1, level.getRandom().nextInt(5) - 2);
            BlockState plant = state.getBlock().defaultBlockState();
            if (level.isEmptyBlock(pos) && plant.canSurvive(level, pos)) {
                level.setBlock(pos, plant, 3);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
                return;
            }
        }
    }

    // The nearest Drift Whale within 24 blocks, if any.
    public @Nullable DriftWhale passingWhale() {
        DriftWhale best = null;
        for (DriftWhale w : this.level().getEntitiesOfClass(DriftWhale.class, this.getBoundingBox().inflate(24.0), DriftWhale::isAlive)) {
            if (best == null || w.distanceToSqr(this) < best.distanceToSqr(this)) {
                best = w;
            }
        }
        return best;
    }

    // Where it drifts in a whale's wake: a little behind and below its tail, each its own place.
    public Vec3 wakeSpot(DriftWhale whale) {
        Vec3 back = whale.getLookAngle().multiply(1.0, 0.0, 1.0);
        back = back.lengthSqr() > 1.0E-4 ? back.normalize().scale(-(whale.getBbWidth() * 1.2 + 3.0)) : Vec3.ZERO;
        double t = this.getId() * 1.7;
        return whale.position().add(back).add(Math.cos(t) * 2.5, -1.0 + Math.sin(t * 0.7), Math.sin(t) * 2.5);
    }

    // Up into a passing whale's wake, and along with it - no further than 32 blocks from where it
    // rose, and for no more than half a minute; then back down to its plants.
    private class RideTheWakeGoal extends Goal {
        private @Nullable DriftWhale whale;
        private @Nullable Vec3 home;
        private int ticks;

        RideTheWakeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            EndPollinator self = EndPollinator.this;
            if (self.wakeCooldown > 0 || self.isInLove() || self.random.nextInt(10) != 0) {
                return false;
            }
            this.whale = self.passingWhale();
            return this.whale != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.whale != null && this.whale.isAlive() && this.ticks < 600 && this.home != null
                && this.whale.position().distanceToSqr(this.home) < 32.0 * 32.0;
        }

        @Override
        public void start() {
            this.ticks = 0;
            this.home = EndPollinator.this.position();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            EndPollinator self = EndPollinator.this;
            this.ticks++;
            if (this.whale == null) {
                return;
            }
            Vec3 spot = self.wakeSpot(this.whale);
            self.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 1.4);
            if (this.ticks % 8 == 0 && self.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.GLOW, self.getX(), self.getY(), self.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
            }
        }

        @Override
        public void stop() {
            EndPollinator self = EndPollinator.this;
            self.wakeCooldown = 1200 + self.random.nextInt(1200);
            if (this.home != null) {
                self.getNavigation().moveTo(this.home.x, this.home.y, this.home.z, 1.0);
            }
            this.whale = null;
        }
    }

    // Finds glowing flora nearby, flies over it and hovers there for a few seconds.
    private class PollinateGoal extends Goal {
        private @Nullable BlockPos flower;
        private int hoverTicks;
        private int totalTicks;

        PollinateGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (EndPollinator.this.pollinateCooldown > 0 || EndPollinator.this.isInLove()) {
                return false;
            }
            this.flower = findFlora(EndPollinator.this.level(), EndPollinator.this.blockPosition(), 10, 5);
            if (this.flower == null) {
                EndPollinator.this.pollinateCooldown = 200;
            }
            return this.flower != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.flower != null && this.totalTicks < 600 && this.hoverTicks < 80
                && EndPollinator.this.level().getBlockState(this.flower).is(FLORA);
        }

        @Override
        public void start() {
            this.hoverTicks = 0;
            this.totalTicks = 0;
        }

        @Override
        public void stop() {
            if (this.flower != null && this.hoverTicks >= 80 && EndPollinator.this.level() instanceof ServerLevel level) {
                pollinate(level, this.flower, level.getBlockState(this.flower));
            }
            this.flower = null;
            EndPollinator.this.pollinateCooldown = 400 + EndPollinator.this.random.nextInt(600);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.flower == null) {
                return;
            }
            this.totalTicks++;
            Vec3 above = Vec3.atBottomCenterOf(this.flower).add(0.0, 1.1, 0.0);
            if (EndPollinator.this.position().distanceToSqr(above) < 1.2) {
                this.hoverTicks++;
                EndPollinator.this.getMoveControl().setWantedPosition(above.x, above.y + Math.sin(this.hoverTicks * 0.2) * 0.15, above.z, 0.4);
                if (this.hoverTicks % 10 == 0 && EndPollinator.this.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.GLOW, above.x, above.y - 0.6, above.z, 2, 0.2, 0.1, 0.2, 0.0);
                }
            } else {
                EndPollinator.this.getNavigation().moveTo(above.x, above.y, above.z, 1.0);
            }
        }
    }
}
