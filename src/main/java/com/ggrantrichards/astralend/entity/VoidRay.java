package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.Sunstorms;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Void Ray: a flat manta of the open void, its back dark as the sky between the stars and its wing
// edges and underside gold with stored sunlight.
//
// Most of the time it rests flat on the ground. Now and then - or
// when a player walks right up to it - it lifts off and glides in wide circles over its island,
// then lands again. Whole flocks take off when a Sunstorm comes and ride it. Shearing full,
// shimmering wings gives Ray Film, a heat-proof membrane; sheared wings fill up again in the
// next flare's afterglow.
public class VoidRay extends Animal implements Shearable {
    private static final EntityDataAccessor<Boolean> DATA_FULL = SynchedEntityData.defineId(VoidRay.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLYING = SynchedEntityData.defineId(VoidRay.class, EntityDataSerializers.BOOLEAN);
    private static final int REGROW_TICKS = 200;   // into the afterglow, the wings fill again

    private int restTicks;
    private int flightTicks;
    private int regrowTicks;
    private double circleAngle;
    private @Nullable Vec3 flightCentre;

    public VoidRay(EntityType<? extends VoidRay> type, Level level) {
        super(type, level);
        this.restTicks = 400 + this.random.nextInt(2400);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1);
    }

    public static boolean checkVoidRaySpawnRules(EntityType<VoidRay> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                 BlockPos pos, RandomSource random) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.SUNSCORCHED_STONE.get()) || below.is(ModBlocks.CHARRED_END_STONE.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FULL, true);
        builder.define(DATA_FLYING, false);
    }

    public boolean isFull() {
        return this.entityData.get(DATA_FULL);
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        FlarePhase flare = Sunstorms.phaseIn(level);
        if (!this.isFull() && flare == FlarePhase.AFTERGLOW && --this.regrowTicks <= 0) {
            this.entityData.set(DATA_FULL, true);
        }
        if (flare == FlarePhase.FLARE && !this.isFlying() && this.random.nextInt(40) == 0) {
            this.takeOff();   // riding the flare
        }
        if (this.isFlying() && flare == FlarePhase.FLARE && this.flightTicks < 40) {
            this.flightTicks = 40;   // stays up until the flare is over
        }
        if (this.isFlying()) {
            this.fly(level);
        } else {
            this.getNavigation().stop();
            boolean startled = level.getNearestPlayer(this, 2.5) instanceof Player p && !p.isShiftKeyDown() && !p.isSpectator();
            if (--this.restTicks <= 0 || startled) {
                this.takeOff();
            }
        }
    }

    private void takeOff() {
        this.entityData.set(DATA_FLYING, true);
        this.setNoGravity(true);
        this.flightTicks = 200 + this.random.nextInt(300);
        this.flightCentre = this.position();
        this.circleAngle = this.random.nextDouble() * Math.PI * 2;
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.35, 0.0));
        this.playSound(SoundEvents.PHANTOM_FLAP, 0.6F, 1.5F);
    }

    private void fly(ServerLevel level) {
        Vec3 centre = this.flightCentre != null ? this.flightCentre : this.position();
        Vec3 wanted;
        if (--this.flightTicks > 0) {
            // Wide, gently rising and falling circles over where it took off.
            this.circleAngle += 0.035;
            wanted = centre.add(Math.cos(this.circleAngle) * 8.0, 5.0 + Math.sin(this.circleAngle * 2.0) * 1.5, Math.sin(this.circleAngle) * 8.0);
        } else {
            // Land back where it took off.
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(centre.x), Mth.floor(centre.z));
            wanted = new Vec3(centre.x, ground + 0.1, centre.z);
            if (this.onGround() || this.position().distanceToSqr(wanted) < 0.5) {
                this.land();
                return;
            }
        }
        Vec3 delta = wanted.subtract(this.position());
        double speed = this.flightTicks > 0 ? 0.28 : 0.15;
        Vec3 velocity = delta.lengthSqr() > 0.01 ? delta.normalize().scale(speed) : Vec3.ZERO;
        this.setDeltaMovement(this.getDeltaMovement().scale(0.85).add(velocity.scale(0.15)));
        Vec3 motion = this.getDeltaMovement();
        if (motion.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * (180.0 / Math.PI)) - 90.0F;
            this.setYRot(Mth.rotLerp(0.2F, this.getYRot(), yaw));
            this.yBodyRot = this.getYRot();
        }
        if (this.tickCount % 18 == 0) {
            this.playSound(SoundEvents.PHANTOM_FLAP, 0.3F, 1.7F);
        }
    }

    private void land() {
        this.entityData.set(DATA_FLYING, false);
        this.setNoGravity(false);
        this.flightCentre = null;
        this.restTicks = 1200 + this.random.nextInt(3600);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS)) {
            if (this.level() instanceof ServerLevel level && this.readyForShearing()) {
                this.shear(level, SoundSource.PLAYERS, stack);
                if (player instanceof net.minecraft.server.level.ServerPlayer shearer) {
                    com.ggrantrichards.astralend.world.ModAdvancements.award(shearer, "harvest_moon");
                }
                this.gameEvent(GameEvent.SHEAR, player);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void shear(ServerLevel level, SoundSource source, ItemStack tool) {
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0F, 1.3F);
        int count = 1 + this.random.nextInt(2);
        this.spawnAtLocation(level, new ItemStack(ModItems.RAY_FILM.get(), count), 0.3F);
        this.entityData.set(DATA_FULL, false);
        this.regrowTicks = REGROW_TICKS;
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && this.isFull();
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Full", this.isFull());
        output.putInt("RegrowTicks", this.regrowTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_FULL, input.getBooleanOr("Full", true));
        this.regrowTicks = input.getIntOr("RegrowTicks", 0);
    }
}
