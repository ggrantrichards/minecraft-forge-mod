package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.EntityTypes;
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
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Hydragrip: a sleek, otter-like thing of the Eclipse's pools. Its broods are born in the water and
// swim there until grown; the adults live on the banks and hunt endermites - and every endermite one
// eats, it coughs up an Ender Pearl. It never bothers anything else. Chorus Fruit tempts it.
//
// It keeps the Hushback herds clean: endermites that come pestering a herd are the first it goes for,
// and now and then one climbs onto a resting Hushback's back and lounges there a while. The young
// chase each other round the ponds - tag, and the other is "it" - and so will an axolotl, if you bring
// one. When a Sunstorm comes they all slip into the water.
public class Hydragrip extends Animal {
    private static final int PLAY_TICKS = 300;
    private int loungeTicks;
    private int loungeCooldown = 600;
    private int playTicks;
    private int playCooldown = 200;
    private boolean playIt;                 // chasing (true) or being chased
    private @Nullable LivingEntity playMate;

    public Hydragrip(EntityType<? extends Hydragrip> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new PondInStormGoal());
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new PlayChaseGoal());
        this.goalSelector.addGoal(5, new LoungeGoal());
        this.goalSelector.addGoal(6, new RandomSwimmingGoal(this, 1.0, 10) {
            @Override
            public boolean canUse() {
                return Hydragrip.this.isBaby() && super.canUse();     // the young keep to the water
            }
        });
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !Hydragrip.this.isBaby() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new GuardHerdGoal());
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
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.loungeCooldown > 0) {
            this.loungeCooldown--;
        }
        if (this.playCooldown > 0) {
            this.playCooldown--;
        }
        // Lounging: until it's had enough, there's a mite to catch, or a storm.
        if (this.isPassenger() && (--this.loungeTicks <= 0 || this.getTarget() != null || EndMobs.stormy(level, this.blockPosition()))) {
            this.stopRiding();
            this.loungeCooldown = 2400 + this.random.nextInt(2400);
        }
        if (this.playTicks > 0) {
            if (--this.playTicks == 0 || this.playMate == null || !this.playMate.isAlive() || this.playMate.distanceToSqr(this) > 24.0 * 24.0) {
                this.stopPlay();
            }
        } else if (this.tickCount % 40 == 0 && this.playCooldown <= 0 && !this.isPassenger() && this.getTarget() == null) {
            LivingEntity mate = this.findPlaymate(level);
            if (mate != null) {
                this.startPlay(mate, this.random.nextBoolean());
            }
        }
    }

    // ---- play: tag round the pond, with another of the brood (the young) or an axolotl (anyone).

    public boolean isPlaying() {
        return this.playTicks > 0;
    }

    public @Nullable LivingEntity playMate() {
        return this.playMate;
    }

    public boolean isIt() {
        return this.playIt;
    }

    private @Nullable LivingEntity findPlaymate(ServerLevel level) {
        if (!this.isInWater() && EndMobs.waterNear(level, this.blockPosition(), 4) == null) {
            return null;
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(12.0), e -> e != this && e.isAlive())) {
            if (e.getType() == EntityTypes.AXOLOTL || (this.isBaby() && e instanceof Hydragrip other && other.isBaby() && !other.isPlaying())) {
                return e;
            }
        }
        return null;
    }

    public void startPlay(LivingEntity mate, boolean it) {
        this.playMate = mate;
        this.playIt = it;
        this.playTicks = PLAY_TICKS;
        if (mate instanceof Hydragrip other) {
            other.playMate = this;
            other.playIt = !it;
            other.playTicks = PLAY_TICKS;
        }
    }

    private void stopPlay() {
        this.playTicks = 0;
        this.playMate = null;
        this.playCooldown = 1200 + this.random.nextInt(1200);
    }

    // Caught: the other is "it" now.
    public void tag() {
        this.playIt = !this.playIt;
        this.playSound(SoundEvents.AXOLOTL_SPLASH, 0.6F, 1.4F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.4, this.getZ(), 8, 0.3, 0.2, 0.3, 0.1);
        }
        if (this.playMate instanceof Hydragrip other) {
            other.playIt = !this.playIt;
        }
    }

    // ---- lounging on a Hushback

    public void lounge(Hushback back) {
        this.startRiding(back);
        if (this.isPassenger()) {
            this.loungeTicks = 400 + this.random.nextInt(400);
        }
    }

    // A Sunstorm: into the nearest pond, and stay under till it passes.
    private class PondInStormGoal extends Goal {
        PondInStormGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Hydragrip.this.random.nextInt(5) == 0 && EndMobs.stormy(Hydragrip.this.level(), Hydragrip.this.blockPosition());
        }

        @Override
        public boolean canContinueToUse() {
            return EndMobs.stormy(Hydragrip.this.level(), Hydragrip.this.blockPosition());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Hydragrip self = Hydragrip.this;
            if (self.isInWater()) {
                self.getNavigation().stop();
                self.setDeltaMovement(self.getDeltaMovement().add(0.0, -0.02, 0.0));     // keeps under
            } else if (self.getNavigation().isDone() && self.tickCount % 20 == 0 && self.level() instanceof ServerLevel level) {
                BlockPos water = EndMobs.waterNear(level, self.blockPosition(), 16);
                if (water != null) {
                    self.getNavigation().moveTo(water.getX() + 0.5, water.getY(), water.getZ() + 0.5, 1.3);
                }
            }
        }
    }

    // Tag: the one who's "it" chases; the other darts away. Touch, and they swap.
    private class PlayChaseGoal extends Goal {
        PlayChaseGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Hydragrip.this.playTicks > 0 && Hydragrip.this.playMate != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Hydragrip self = Hydragrip.this;
            LivingEntity mate = self.playMate;
            if (mate == null) {
                return;
            }
            boolean axolotl = mate.getType() == EntityTypes.AXOLOTL;
            if (self.playIt) {
                if (self.tickCount % 5 == 0) {
                    self.getNavigation().moveTo(mate, 1.4);
                }
                if (self.distanceToSqr(mate) < 1.6 * 1.6) {
                    self.tag();
                }
            } else {
                if (self.tickCount % 20 == 0) {
                    Vec3 away = DefaultRandomPos.getPosAway(self, 7, 3, mate.position());
                    if (away != null) {
                        self.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                    }
                }
                // An axolotl doesn't know the game, but it'll chase something darting off.
                if (axolotl && mate instanceof net.minecraft.world.entity.Mob m && self.tickCount % 10 == 0) {
                    m.getNavigation().moveTo(self, 1.2);
                }
                if (axolotl && self.distanceToSqr(mate) < 1.6 * 1.6 && self.playTicks < PLAY_TICKS - 20) {
                    self.tag();
                }
            }
        }
    }

    // Climbs onto a resting Hushback and lounges there.
    private class LoungeGoal extends Goal {
        private @Nullable Hushback back;
        private int ticks;

        LoungeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Hydragrip self = Hydragrip.this;
            if (self.isBaby() || self.loungeCooldown > 0 || self.isPassenger() || self.isPlaying() || self.random.nextInt(100) != 0
                    || EndMobs.stormy(self.level(), self.blockPosition())) {
                return false;
            }
            for (Hushback h : self.level().getEntitiesOfClass(Hushback.class, self.getBoundingBox().inflate(10.0), Hushback::isResting)) {
                this.back = h;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.back != null && this.back.isAlive() && !Hydragrip.this.isPassenger() && this.ticks < 200;
        }

        @Override
        public void start() {
            this.ticks = 0;
        }

        @Override
        public void tick() {
            this.ticks++;
            Hydragrip self = Hydragrip.this;
            if (this.back == null) {
                return;
            }
            if (self.distanceToSqr(this.back) < 2.2 * 2.2) {
                self.lounge(this.back);
            } else if (this.ticks % 10 == 1) {
                self.getNavigation().moveTo(this.back, 1.0);
            }
        }

        @Override
        public void stop() {
            this.back = null;
            if (!Hydragrip.this.isPassenger()) {
                Hydragrip.this.loungeCooldown = 600;
            }
        }
    }

    // The herd's cleaner: an endermite near a Hushback is the first thing it goes for.
    private class GuardHerdGoal extends Goal {
        GuardHerdGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Hydragrip self = Hydragrip.this;
            if (self.isBaby() || self.getTarget() != null || self.random.nextInt(5) != 0) {
                return false;
            }
            Endermite mite = self.pesteringMite();
            if (mite != null) {
                self.setTarget(mite);
            }
            return false;
        }
    }

    // An endermite within 8 blocks of a Hushback, within 20 of this one.
    public @Nullable Endermite pesteringMite() {
        for (Endermite mite : this.level().getEntitiesOfClass(Endermite.class, this.getBoundingBox().inflate(20.0), Entity::isAlive)) {
            if (!this.level().getEntitiesOfClass(Hushback.class, mite.getBoundingBox().inflate(8.0)).isEmpty()) {
                return mite;
            }
        }
        return null;
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
