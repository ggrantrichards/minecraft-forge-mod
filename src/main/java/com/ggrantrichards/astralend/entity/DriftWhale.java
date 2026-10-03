package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.item.ChorusFlowerOnAStickItem;
import com.ggrantrichards.astralend.world.ModAdvancements;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Drift Whale: a huge, translucent whale that swims slowly through the void, the last small
// descendant of the Star Leviathans.
//
// Whales travel in pods of three to five: one leads, the rest keep station on it, and now and then a
// pod's leader heads for the nearest Eclipse - follow a pod and it may take you there. It never
// touches ground; now and then it drifts down to graze on chorus. When a player comes close it turns
// toward them, slows down and hums. It never fights back: hurt it and it flees.
//
// Taming is by song. Eat Memory Moss (Remembering), then play a note block near a whale: it turns
// toward the music and sings back, and for half a minute it will take Chorus Fruit from you - feed
// it then, and it's yours. A tamed whale can be ridden with a Chorus Flower on a Stick (holding the
// stick, you steer: it swims where you look, jump to rise, use the stick to surge), stays forever,
// and sometimes sheds a glowing Whalelight Scale when it hums - the only way to get one.
//
// Its rewards are an advancement chain, which can't be farmed: see one (Something Big Out There), be
// near while it hums (Whale Song), follow one for a minute while flying (Tagalong), tame one (Sung
// Home), ride one (Whale Rider).
public class DriftWhale extends Mob implements ItemSteerable {
    private static final EntityDataAccessor<Boolean> DATA_HUMMING = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.INT);
    private static final double RIDDEN_SPEED = 0.025;   // about 5 blocks a second; a boost doubles it
    private static final int FLEE_TICKS = 200;
    private static final double CRUISE_SPEED = 0.12;
    private static final double NEAR_PLAYER = 24.0;

    private @Nullable Vec3 target;
    private int humCooldown = 100;
    private int humTicks;
    private final Map<UUID, Integer> followers = new HashMap<>();
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);
    private int fleeTicks;
    private @Nullable Vec3 fleeFrom;
    private @Nullable BlockPos grazing;
    private int grazeTicks;
    private @Nullable UUID leader;          // the pod's leader, if this whale follows one
    private @Nullable Vec3 station;         // where it keeps, relative to its leader
    private @Nullable UUID owner;           // tamed by song
    private @Nullable UUID singer;          // who sang to it, for a short while after
    private int listening;

    public DriftWhale(EntityType<? extends DriftWhale> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 400.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.CAMERA_DISTANCE, 11.0);     // riding in third person, the camera stands clear of its body
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUMMING, false);
        builder.define(DATA_BOOST_TIME, 0);
    }

    public boolean isTamed() {
        return this.owner != null;
    }

    // A pod member: keeps station on its leader.
    public void follow(DriftWhale leader, Vec3 station) {
        this.leader = leader.getUUID();
        this.station = station;
    }

    // A note block played near it by someone Remembering: it turns toward the music and sings back,
    // and for half a minute takes Chorus Fruit from them.
    public void hearSong(ServerLevel level, Player singer, BlockPos noteBlock) {
        if (this.isTamed() || this.fleeTicks > 0) {
            return;
        }
        this.singer = singer.getUUID();
        this.listening = 600;
        this.faceDirection(Vec3.atCenterOf(noteBlock).subtract(this.position()), 30.0F);
        this.humCooldown = 0;
        this.hum(level);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), this.getSoundSource(), 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 3.0, this.getZ(), 6, 1.5, 0.5, 1.5, 1.0);
    }

    // Fed Chorus Fruit by whoever sang to it, while it still listens: it's theirs. True if tamed.
    public boolean tryTame(ServerLevel level, Player player) {
        if (this.isTamed() || this.listening <= 0 || !player.getUUID().equals(this.singer)) {
            return false;
        }
        this.owner = player.getUUID();
        this.leader = null;
        this.setPersistenceRequired();
        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 3.0, this.getZ(), 10, 2.0, 1.0, 2.0, 0.0);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "sung_home");
        }
        return true;
    }

    public boolean isHumming() {
        return this.entityData.get(DATA_HUMMING);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        if (DATA_BOOST_TIME.equals(accessor) && this.level().isClientSide()) {
            this.steering.onSynced();
        }
        super.onSyncedDataUpdated(accessor);
    }

    // Hurt, it never fights back: it flees, fast, away from whatever hurt it.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.fleeTicks = FLEE_TICKS;
            Entity attacker = source.getEntity();
            this.fleeFrom = attacker != null ? attacker.position() : this.position().add(this.getLookAngle());
            this.target = null;
            this.grazing = null;
        }
        return hurt;
    }

    // ---- riding

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.CHORUS_FRUIT)) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel level) {
                this.heal(20.0F);
                stack.consume(1, player);
                this.tryTame(level, player);
                this.playSound(SoundEvents.GENERIC_EAT.value(), 1.5F, 0.5F);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.CHORUS_FRUIT),
                    this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.6, 0.4, 0.6, 0.05);
                this.humCooldown = 0;
                this.hum(level);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.getItem() instanceof ChorusFlowerOnAStickItem && !this.isVehicle() && !player.isSecondaryUseActive()
                && player.getUUID().equals(this.owner)) {
            if (!this.level().isClientSide()) {
                player.startRiding(this);
                this.setPersistenceRequired();
                if (player instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.award(serverPlayer, "whale_rider");
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().isEmpty() && passenger instanceof Player;
    }

    // The rider steers only with a Chorus Flower on a Stick in hand.
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player player && ChorusFlowerOnAStickItem.holding(player)
            ? player : super.getControllingPassenger();
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Override
    protected Vec3 getRiddenInput(Player controller, Vec3 selfInput) {
        float strafe = controller.xxa * 0.5F;
        float forward = 0.0F;
        float up = 0.0F;
        if (controller.zza != 0.0F) {
            float forwardLook = Mth.cos(controller.getXRot() * (float) (Math.PI / 180.0));
            float upLook = -Mth.sin(controller.getXRot() * (float) (Math.PI / 180.0));
            if (controller.zza < 0.0F) {
                forwardLook *= -0.4F;
                upLook *= -0.4F;
            }
            up = upLook;
            forward = forwardLook;
        }
        if (controller.isJumping()) {
            up += 0.5F;
        }
        return new Vec3(strafe, up, forward);
    }

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        // A heavy animal: it swings round toward where the rider looks, steadily (at most 4 degrees a
        // tick), and its turn is drawn smoothly between ticks (the old rotation is left for the
        // renderer to interpolate from).
        float turn = Mth.clamp(Mth.wrapDegrees(controller.getYRot() - this.getYRot()) * 0.12F, -4.0F, 4.0F);
        float yRot = this.getYRot() + turn;
        this.setRot(yRot, 0.0F);
        this.yBodyRot = this.yHeadRot = yRot;
    }

    @Override
    protected float getRiddenSpeed(Player controller) {
        return (float) (RIDDEN_SPEED * this.steering.boostFactor());
    }

    @Override
    public void travel(Vec3 input) {
        if (this.getControllingPassenger() instanceof Player) {
            float speed = this.getSpeed();
            this.travelFlying(input, speed, speed, speed);
        } else {
            super.travel(input);
        }
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
        this.steering.tickBoost();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.tickCount % 20 == 0) {
            this.checkAdvancements(level);
        }
        if (this.humTicks > 0 && --this.humTicks == 0) {
            this.entityData.set(DATA_HUMMING, false);
        }
        if (this.listening > 0 && --this.listening == 0) {
            this.singer = null;
        }
        if (this.getControllingPassenger() != null) {
            return;   // the rider steers
        }
        Player near = this.isVehicle() ? null : level.getNearestPlayer(this, NEAR_PLAYER);
        if (near != null && near.isSpectator()) {
            near = null;
        }
        Vec3 velocity;
        if (this.fleeTicks > 0) {
            // Away from whatever hurt it, three times as fast as it cruises.
            this.fleeTicks--;
            Vec3 away = this.position().subtract(this.fleeFrom != null ? this.fleeFrom : this.position()).multiply(1.0, 0.3, 1.0);
            velocity = away.lengthSqr() > 1.0E-4 ? away.normalize().scale(CRUISE_SPEED * 3.0) : this.getLookAngle().scale(CRUISE_SPEED * 3.0);
            this.faceDirection(velocity, 4.0F);
        } else if (this.grazing != null) {
            velocity = this.graze(level);
        } else if (this.leader != null && this.station != null && this.followLeader(level) != null) {
            velocity = this.followLeader(level);
        } else if (near != null) {
            // Turn toward the player and slow down.
            Vec3 toPlayer = near.getEyePosition().subtract(this.position().add(0.0, 1.0, 0.0));
            velocity = toPlayer.lengthSqr() > 100.0 ? toPlayer.normalize().scale(0.03) : Vec3.ZERO;
            this.faceDirection(toPlayer, 2.0F);
            this.hum(level);
        } else {
            if (this.target == null || this.position().distanceToSqr(this.target) < 36.0 || this.horizontalCollision || this.random.nextInt(1200) == 0) {
                this.target = this.random.nextInt(5) == 0 ? this.towardEclipse(level) : null;
                if (this.target == null) {
                    this.target = this.pickTarget(level);
                }
                if (this.random.nextInt(4) == 0) {
                    this.grazing = this.findChorus(level);
                    this.grazeTicks = 0;
                }
            }
            Vec3 toTarget = this.target != null ? this.target.subtract(this.position()) : Vec3.ZERO;
            velocity = toTarget.lengthSqr() > 1.0 ? toTarget.normalize().scale(CRUISE_SPEED) : Vec3.ZERO;
            this.faceDirection(velocity, 1.0F);
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9).add(velocity.scale(0.1)));
    }

    // Keeps station on its pod's leader; null if the leader is gone (then it swims its own way).
    private @Nullable Vec3 followLeader(ServerLevel level) {
        if (!(level.getEntity(this.leader) instanceof DriftWhale lead) || !lead.isAlive() || lead.distanceToSqr(this) > 160.0 * 160.0) {
            this.leader = null;
            return null;
        }
        Vec3 to = lead.position().add(this.station).subtract(this.position());
        Vec3 velocity = to.lengthSqr() > 4.0 ? to.normalize().scale(Math.min(CRUISE_SPEED * 1.6, to.length() * 0.02)) : Vec3.ZERO;
        this.faceDirection(to.lengthSqr() > 4.0 ? to : lead.getLookAngle(), 1.5F);
        return velocity;
    }

    // ---- grazing: it drifts down over a chorus plant and nibbles at it for a while (it leaves the
    // plant standing).

    private @Nullable BlockPos findChorus(ServerLevel level) {
        BlockPos centre = this.blockPosition();
        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos p = centre.offset(this.random.nextInt(49) - 24, this.random.nextInt(33) - 24, this.random.nextInt(49) - 24);
            BlockState s = level.getBlockState(p);
            if ((s.is(Blocks.CHORUS_FLOWER) || s.is(Blocks.CHORUS_PLANT)) && level.getBlockState(p.above()).isAir()) {
                return p;
            }
        }
        return null;
    }

    private Vec3 graze(ServerLevel level) {
        BlockPos at = this.grazing;
        BlockState s = level.getBlockState(at);
        if (++this.grazeTicks > 600 || !(s.is(Blocks.CHORUS_FLOWER) || s.is(Blocks.CHORUS_PLANT))) {
            this.grazing = null;
            return Vec3.ZERO;
        }
        Vec3 above = Vec3.atCenterOf(at).add(0.0, 2.5, 0.0);
        Vec3 to = above.subtract(this.position());
        if (to.lengthSqr() > 4.0) {
            this.faceDirection(to, 1.5F);
            return to.normalize().scale(CRUISE_SPEED);
        }
        if (this.tickCount % 30 == 0) {
            this.playSound(SoundEvents.GENERIC_EAT.value(), 1.2F, 0.5F);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.CHORUS_FRUIT),
                at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 8, 0.4, 0.3, 0.4, 0.04);
            this.heal(2.0F);
        }
        return Vec3.ZERO;
    }

    private void faceDirection(Vec3 direction, float maxTurn) {
        if (direction.horizontalDistanceSqr() < 1.0E-6) {
            return;
        }
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, maxTurn));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    // Migration: a point 60 blocks along the way to the nearest Eclipse (if one is within 2,500 blocks),
    // high enough to clear the land.
    private @Nullable Vec3 towardEclipse(ServerLevel level) {
        var eclipse = com.ggrantrichards.astralend.world.EclipseLayout.nearest(this.getX(), this.getZ(),
            com.ggrantrichards.astralend.world.EclipseLayout.sampler(level.getChunkSource().randomState()), 1);
        if (eclipse == null) {
            return null;
        }
        Vec3 to = new Vec3(eclipse.x() - this.getX(), 0.0, eclipse.z() - this.getZ());
        if (to.lengthSqr() > 2500.0 * 2500.0 || to.lengthSqr() < 40.0 * 40.0) {
            return null;
        }
        Vec3 p = this.position().add(to.normalize().scale(60.0));
        return new Vec3(p.x, Mth.clamp(Math.max(this.getY(), 90.0), 20.0, 140.0), p.z);
    }

    // A point far off in open air: 40-90 blocks away, at a similar height, with room to fit.
    private @Nullable Vec3 pickTarget(ServerLevel level) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double distance = 40.0 + this.random.nextDouble() * 50.0;
            double y = Mth.clamp(this.getY() + (this.random.nextDouble() - 0.5) * 24.0, 20.0, 140.0);
            Vec3 p = new Vec3(this.getX() + Math.cos(angle) * distance, y, this.getZ() + Math.sin(angle) * distance);
            AABB box = this.getDimensions(this.getPose()).makeBoundingBox(p).inflate(2.0);
            if (level.noCollision(this, box)) {
                return p;
            }
        }
        return null;
    }

    private void hum(ServerLevel level) {
        if (--this.humCooldown > 0) {
            return;
        }
        this.humCooldown = 200 + this.random.nextInt(200);
        this.humTicks = 60;
        this.entityData.set(DATA_HUMMING, true);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CONDUIT_AMBIENT, this.getSoundSource(), 4.0F, 0.5F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(16.0))) {
            ModAdvancements.award(player, "whale_song");
        }
        if (this.isTamed() && this.random.nextInt(8) == 0) {
            this.spawnAtLocation(level, new ItemStack(ModBlocks.WHALELIGHT_SCALE.get()), 0.5F);
        }
    }

    private void checkAdvancements(ServerLevel level) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0))) {
            if (player.hasLineOfSight(this)) {
                ModAdvancements.award(player, "something_big_out_there");
            }
            boolean flyingAlong = this.distanceToSqr(player) < 24.0 * 24.0 && (player.isFallFlying() || player.getAbilities().flying);
            int seconds = this.followers.getOrDefault(player.getUUID(), 0);
            seconds = flyingAlong ? seconds + 1 : Math.max(0, seconds - 1);
            this.followers.put(player.getUUID(), seconds);
            if (seconds >= 60) {
                ModAdvancements.award(player, "tagalong");
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        String owner = input.getStringOr("Owner", "");
        this.owner = owner.isEmpty() ? null : UUID.fromString(owner);
    }

    @Override
    public void checkDespawn() {
        if (!this.isPersistenceRequired() && !this.isVehicle() && this.level().getNearestPlayer(this, 192.0) == null) {
            this.discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return true;
    }
}
