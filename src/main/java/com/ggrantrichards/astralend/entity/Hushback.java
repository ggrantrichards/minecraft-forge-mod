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
import java.util.EnumSet;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
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

// The Hushback: a herd grazer of the Gilded Reach, its hide so pale and clear that its bones
// show through. It makes almost no sound. Hurt one and the whole herd round it blinks away at once,
// the way an enderman does, and reappears some way off - so it's food and fleece for the patient, not
// the greedy. Shear it for Hushfleece (it grows back as it grazes the turf); Breathcomb tempts it and
// makes it breed.
//
// The herd keeps watch: now and then one stands lookout, head up, turning slowly, while the rest
// graze. If something comes - a monster, an endermite, a player who isn't creeping (sneak up, or bring
// Breathcomb) - it chimes twice and the whole herd shies away from it. When a Sunstorm comes they
// crowd under the giant Lumineshrooms' caps (or any cover) and wait it out. A Hydragrip sometimes
// lounges on a resting one's back, and snaps up the endermites that pester the herd.
public class Hushback extends Animal {
    private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(Hushback.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LOOKOUT = SynchedEntityData.defineId(Hushback.class, EntityDataSerializers.BOOLEAN);
    private static final double WATCH_MONSTERS = 14.0, WATCH_PLAYERS = 9.0;
    private int regrow;
    private int lookoutTicks;
    private int lookoutCooldown = 200;
    private int alarmTicks;
    private @Nullable Vec3 alarmFrom;
    private int secondChime;

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
        builder.define(LOOKOUT, false);
    }

    public boolean isSheared() {
        return this.entityData.get(SHEARED);
    }

    // Standing lookout for the herd (the renderer lifts its head).
    public boolean isLookout() {
        return this.entityData.get(LOOKOUT);
    }

    public boolean isAlarmed() {
        return this.alarmTicks > 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new ShyAwayGoal());
        this.goalSelector.addGoal(2, new ShelterGoal());
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, this::isFood, false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new LookoutGoal());
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    // Take a turn as lookout (the survey calls it too).
    public void standLookout(int ticks) {
        this.lookoutTicks = ticks;
        this.entityData.set(LOOKOUT, true);
    }

    // What a lookout watches for: the nearest monster (an endermite too) within 14 blocks, or a player
    // within 9 who isn't sneaking up or bringing Breathcomb.
    public @Nullable LivingEntity threatNear(ServerLevel level) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(WATCH_MONSTERS), LivingEntity::isAlive)) {
            boolean threat = e instanceof Monster
                || (e instanceof Player p && !p.isSpectator() && !p.isCreative() && !p.isShiftKeyDown()
                    && e.distanceToSqr(this) < WATCH_PLAYERS * WATCH_PLAYERS && !this.isFood(p.getMainHandItem()) && !this.isFood(p.getOffhandItem()));
            if (threat && e.distanceToSqr(this) < bestDistance) {
                best = e;
                bestDistance = e.distanceToSqr(this);
            }
        }
        return best;
    }

    // Two soft chimes, and the herd round it shies away from `from`.
    public void raiseAlarm(ServerLevel level, Vec3 from) {
        level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.6F, 0.55F);
        this.secondChime = 6;
        for (Hushback other : level.getEntitiesOfClass(Hushback.class, this.getBoundingBox().inflate(16.0))) {
            other.alarmTicks = 100;
            other.alarmFrom = from;
        }
        this.endLookout();
    }

    private void endLookout() {
        this.lookoutTicks = 0;
        this.lookoutCooldown = 600 + this.random.nextInt(600);
        this.entityData.set(LOOKOUT, false);
    }

    // A Hydragrip may lounge on its back; it never steers.
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Hydragrip;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return null;
    }

    // Resting: standing still, not alarmed, not sheltering (a Hydragrip only climbs onto one like that).
    public boolean isResting() {
        return this.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 && this.alarmTicks <= 0 && !this.isVehicle() && !this.isBaby();
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
                held.hurtAndBreak(1, player, hand);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.ggrantrichards.astralend.world.ModAdvancements.award(sp, "harvest_moon");
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel level) {
            if (this.alarmTicks > 0) {
                this.alarmTicks--;
            }
            if (this.secondChime > 0 && --this.secondChime == 0) {
                level.playSound(null, this.getX(), this.getY() + 1.0, this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.6F, 0.7F);
            }
            if (this.lookoutCooldown > 0) {
                this.lookoutCooldown--;
            }
            if (this.isLookout() && --this.lookoutTicks <= 0) {
                this.endLookout();
            }
            // One of the herd takes a turn on watch, if none near is.
            if (this.tickCount % 40 == 0 && !this.isLookout() && !this.isBaby() && this.lookoutCooldown <= 0 && this.random.nextInt(3) == 0
                    && level.getEntitiesOfClass(Hushback.class, this.getBoundingBox().inflate(16.0), Hushback::isLookout).isEmpty()) {
                this.standLookout(400 + this.random.nextInt(300));
            }
        }
        if (!this.level().isClientSide() && this.isSheared() && --this.regrow <= 0
                && (this.level().getBlockState(this.blockPosition().below()).is(ModBlocks.LIVING_TURF.get())
                    || this.level().getBlockState(this.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.END_STONE))) {
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
            this.ejectPassengers();
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

    // Stands still, head up, and looks slowly round; chimes the alarm at the first sign of trouble.
    private class LookoutGoal extends Goal {
        LookoutGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Hushback.this.isLookout() && Hushback.this.alarmTicks <= 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Hushback self = Hushback.this;
            self.getNavigation().stop();
            if (self.tickCount % 30 == 0) {
                double a = self.random.nextDouble() * Math.PI * 2.0;
                self.getLookControl().setLookAt(self.getX() + Math.cos(a) * 10.0, self.getEyeY() + 1.0, self.getZ() + Math.sin(a) * 10.0, 6.0F, 30.0F);
            }
            if (self.tickCount % 10 == 0 && self.level() instanceof ServerLevel level) {
                LivingEntity threat = self.threatNear(level);
                if (threat != null) {
                    self.getLookControl().setLookAt(threat, 30.0F, 30.0F);
                    self.raiseAlarm(level, threat.position());
                }
            }
        }
    }

    // Alarmed: it trots away from whatever the lookout saw.
    private class ShyAwayGoal extends Goal {
        ShyAwayGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Hushback.this.alarmTicks > 0 && Hushback.this.alarmFrom != null;
        }

        @Override
        public void start() {
            Vec3 away = DefaultRandomPos.getPosAway(Hushback.this, 12, 4, Hushback.this.alarmFrom);
            if (away != null) {
                Hushback.this.getNavigation().moveTo(away.x, away.y, away.z, 1.35);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Hushback.this.alarmTicks > 0 && !Hushback.this.getNavigation().isDone();
        }
    }

    // A Sunstorm coming: get under a cap, and stay there till it's past.
    private class ShelterGoal extends Goal {
        ShelterGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Hushback.this.random.nextInt(5) == 0 && EndMobs.stormy(Hushback.this.level(), Hushback.this.blockPosition());
        }

        @Override
        public boolean canContinueToUse() {
            return EndMobs.stormy(Hushback.this.level(), Hushback.this.blockPosition());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Hushback self = Hushback.this;
            if (!com.ggrantrichards.astralend.flare.Sunstorms.underOpenSky(self.level(), self.blockPosition())) {
                self.getNavigation().stop();       // under cover: wait
            } else if (self.getNavigation().isDone() && self.tickCount % 20 == 0 && self.level() instanceof ServerLevel level) {
                BlockPos cover = EndMobs.coverNear(level, self.blockPosition(), 24, self.random);
                if (cover != null) {
                    self.getNavigation().moveTo(cover.getX() + 0.5, cover.getY(), cover.getZ() + 0.5, 1.25);
                }
            }
        }

        @Override
        public void stop() {
            Hushback.this.getNavigation().stop();
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(SHEARED, input.getBooleanOr("Sheared", false));
        this.regrow = input.getIntOr("Regrow", 0);
    }
}
