package com.ggrantrichards.astralend.entity;

import net.minecraft.nbt.CompoundTag;
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

// Drift Whale: a huge, glowing whale that swims slowly through the void, the last small
// descendant of the Star Leviathans.
//
// Whales travel in pods of three to five: one leads, the rest swim loosely round it, and now and then
// a pod's leader heads for the nearest Eclipse - follow a pod and it may take you there. It never
// touches ground; now and then it drifts down to graze on chorus. It never fights back: hurt it and
// it flees.
//
// No two are alike. Each has its own size (the leader biggest; often a calf, which keeps close by the
// leader, its mother), its own pace and its own swing as it swims, and its own temper: a curious one
// comes close and circles you, humming; a shy one keeps its distance; most turn toward you and hum.
// When one hums, the others answer, each in its own voice - the bigger, the deeper. And they play:
// one rolls over and over as it swims, one breaches in a great loop, two chase each other round in a
// spiral - the calves most of all.
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
//
// A pod lives as one. It swims as a flock: each keeps its place round the leader and turns as the
// leader turned, a beat late. A calf strays - often after a Pollinator, chasing it round without
// ever touching it - and its mother calls and goes after it, and nudges it back. Now and then the pod
// sleeps, each hanging nose-up in the void and slowly turning. Passing a Leviathan, the leader takes
// the pod once round the skeleton, humming its lowest. A Mirror Wretch can hum in a whale's voice:
// the pod answers, then falls silent and steers away from it. And when a Sunstorm comes, pods swim
// out of the Gilded Reach till it's past.
public class DriftWhale extends Mob implements ItemSteerable {
    private static final EntityDataAccessor<Boolean> DATA_HUMMING = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.INT);
    // (1.21.1 port: the steering keeps a saddle flag; the whale needs none.)
    private static final EntityDataAccessor<Boolean> DATA_SADDLE = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_PLAY = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.INT);
    public static final int PLAY_NONE = 0, PLAY_ROLL = 1, PLAY_LOOP = 2, PLAY_CHASE = 3;
    private static final double RIDDEN_SPEED = 0.025;   // about 5 blocks a second; a boost doubles it
    private static final int FLEE_TICKS = 200;
    private static final double CRUISE_SPEED = 0.12;
    private static final double NEAR_PLAYER = 24.0;

    private @Nullable Vec3 target;
    private int humCooldown = 100;
    private int humTicks;
    private final Map<UUID, Integer> followers = new HashMap<>();
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME, DATA_SADDLE);
    private int fleeTicks;
    private @Nullable Vec3 fleeFrom;
    private @Nullable BlockPos grazing;
    private int grazeTicks;
    private @Nullable UUID leader;          // the pod's leader, if this whale follows one
    private @Nullable Vec3 station;         // where it keeps, relative to its leader
    private @Nullable UUID owner;           // tamed by song
    private @Nullable UUID singer;          // who sang to it, for a short while after
    private int listening;
    private int playTicks;
    private int playLength;
    private @Nullable Vec3 playCentre;
    private Vec3 playHeading = Vec3.ZERO;
    private double playPhase;
    private int answerIn;                   // answering another's hum in this many ticks
    private boolean answering;
    private int wary;                       // heard a Wretch's false hum: silent, and keeping away
    private @Nullable Vec3 waryFrom;
    private boolean answeringMimic;
    private int sleepTicks;
    private int circleTicks;
    private @Nullable Vec3 circleCentre;
    private double circleRadius, circleAngle;
    private long lastCircle = -100000L;
    private int strayTicks, strayCooldown = 1200;
    private @Nullable Vec3 strayTarget;
    private @Nullable Entity strayChase;
    private @Nullable UUID fetching;        // a mother going after her straying calf
    private final float[] yawHistory = new float[40];
    private float bodyPitch;                // set after the look control, which zeroes the pitch each tick
    // Client side: how far over it has rolled (it eases back upright when it stops).
    private float roll;
    private float rollO;

    public DriftWhale(EntityType<? extends DriftWhale> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 400.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUMMING, false);
        builder.define(DATA_BOOST_TIME, 0);
        builder.define(DATA_SADDLE, false);
        builder.define(DATA_PLAY, PLAY_NONE);
    }

    // ---- who it is: fixed for each whale by its UUID, so nothing needs saving.

    // A number in [0, 1) that's this whale's own, one for each trait k.
    private double trait(int k) {
        long h = this.getUUID().getLeastSignificantBits() ^ this.getUUID().getMostSignificantBits() * 31L ^ k * 0x9E3779B97F4A7C15L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    // Its pace: 0.85 to 1.15 of a whale's, and a calf's a little quicker.
    public float tempo() {
        return (float) ((0.85 + 0.3 * this.trait(1)) * (this.isCalf() ? 1.15 : 1.0));
    }

    private double curiosity() {
        return this.trait(2);
    }

    public boolean isCalf() {
        return this.getScale() < 0.75F;
    }

    // Its size, as the SCALE attribute (saved with it): the body, its hitbox and its voice follow.
    public void setSize(float scale) {
        var attribute = this.getAttribute(Attributes.SCALE);
        if (attribute != null) {
            attribute.setBaseValue(scale);
        }
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        this.setSize((float) (0.85 + 0.3 * this.trait(0)));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    public int play() {
        return this.entityData.get(DATA_PLAY);
    }

    // Rolled over this far (degrees), for the renderer.
    public float roll(float partialTick) {
        return Mth.lerp(partialTick, this.rollO, this.roll);
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
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level)) {
            return super.hurt(source, damage);
        }
        boolean hurt = super.hurt(source, damage);
        if (hurt && this.isAlive()) {
            this.fleeTicks = FLEE_TICKS;
            this.sleepTicks = 0;
            this.circleTicks = 0;
            Entity attacker = source.getEntity();
            this.fleeFrom = attacker != null ? attacker.position() : this.position().add(this.getLookAngle());
            this.target = null;
            this.grazing = null;
            this.playTicks = 0;
            this.entityData.set(DATA_PLAY, PLAY_NONE);
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
                this.playSound(SoundEvents.GENERIC_EAT, 1.5F, 0.5F);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CHORUS_FRUIT)),
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
        if (com.ggrantrichards.astralend.compat.Compat.isJumping(controller)) {
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
            com.ggrantrichards.astralend.compat.Compat.travelFlying(this, input, speed, speed, speed);
        } else {
            super.travel(input);
        }
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
        if (!this.level().isClientSide() && this.getControllingPassenger() == null) {
            this.setXRot(this.bodyPitch);       // (the AI's look control resets it during the tick)
        }
        this.steering.tickBoost();
        if (this.level().isClientSide()) {
            this.rollO = this.roll;
            if (this.play() == PLAY_ROLL) {
                this.roll += 7.0F * this.tempo();
            } else {
                float upright = Math.round(this.roll / 360.0F) * 360.0F;
                this.roll = Mth.approach(this.roll, upright, 6.0F);
            }
            if (Math.abs(this.roll) > 3600.0F && this.roll == this.rollO) {
                this.roll = this.rollO = 0.0F;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) this.level();
        super.customServerAiStep();
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
        this.yawHistory[this.tickCount % this.yawHistory.length] = this.getYRot();
        if (this.wary > 0) {
            this.wary--;
        }
        if (this.strayCooldown > 0) {
            this.strayCooldown--;
        }
        if (this.leader == null && !this.isTamed()) {
            this.leadThePod(level, near);
        }
        Vec3 velocity;
        Vec3 storm = this.fleeTicks > 0 ? null : this.outOfTheStorm(level);
        Vec3 fetch = this.fetching != null && this.fleeTicks <= 0 ? this.fetchStep(level) : null;
        boolean sleeping = false;
        if (this.fleeTicks > 0) {
            // Away from whatever hurt it, three times as fast as it cruises.
            this.fleeTicks--;
            Vec3 away = this.position().subtract(this.fleeFrom != null ? this.fleeFrom : this.position()).multiply(1.0, 0.3, 1.0);
            velocity = away.lengthSqr() > 1.0E-4 ? away.normalize().scale(CRUISE_SPEED * 3.0) : this.getLookAngle().scale(CRUISE_SPEED * 3.0);
            this.faceDirection(velocity, 4.0F);
        } else if (storm != null) {
            velocity = storm;
            this.faceDirection(velocity, 3.0F);
        } else if (this.wary > 0 && this.waryFrom != null) {
            // Keeping away from where the false hum came from, and silent.
            Vec3 away = this.position().subtract(this.waryFrom).multiply(1.0, 0.2, 1.0);
            velocity = away.length() < 64.0 && away.lengthSqr() > 1.0E-4 ? away.normalize().scale(CRUISE_SPEED * 1.4 * this.tempo()) : this.getLookAngle().scale(CRUISE_SPEED);
            this.faceDirection(velocity, 2.5F);
        } else if (this.sleepTicks > 0) {
            velocity = this.sleepStep(level, near);
            sleeping = true;
        } else if (fetch != null) {
            velocity = fetch;
        } else if (this.circleTicks > 0) {
            velocity = this.circleStep(level);
        } else if (this.grazing != null) {
            velocity = this.graze(level);
        } else if (this.playTicks > 0) {
            velocity = this.playStep(level);
        } else if (this.strayTicks > 0) {
            velocity = this.strayStep(level);
        } else if (this.leader != null && this.station != null && this.followLeader(level) != null) {
            velocity = this.followLeader(level);
        } else if (near != null && this.curiosity() > 0.65) {
            // Curious: it comes in close and circles you, humming.
            Vec3 toPlayer = near.getEyePosition().subtract(this.position().add(0.0, 1.0, 0.0));
            Vec3 round = new Vec3(-toPlayer.z, 0.0, toPlayer.x).normalize();
            double pull = toPlayer.length() > 12.0 ? 1.0 : toPlayer.length() < 8.0 ? -0.6 : 0.0;
            velocity = toPlayer.normalize().scale(pull * 0.05).add(round.scale(0.05)).scale(this.tempo());
            this.faceDirection(velocity.lengthSqr() > 1.0E-4 ? velocity : toPlayer, 2.5F);
            this.hum(level);
        } else if (near != null && this.curiosity() < 0.3) {
            // Shy: it keeps its distance, and slips away if you press it.
            Vec3 away = this.position().subtract(near.position()).multiply(1.0, 0.2, 1.0);
            velocity = away.length() < 22.0 ? away.normalize().scale(CRUISE_SPEED * this.tempo()) : Vec3.ZERO;
            this.faceDirection(velocity.lengthSqr() > 1.0E-4 ? velocity : near.position().subtract(this.position()), 1.5F);
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
            velocity = toTarget.lengthSqr() > 1.0 ? toTarget.normalize().scale(CRUISE_SPEED * this.tempo()).add(this.sway(toTarget)) : Vec3.ZERO;
            this.faceDirection(velocity, 1.0F);
        }
        if (this.playTicks <= 0 && this.fleeTicks <= 0 && this.grazing == null && !this.isVehicle() && this.sleepTicks <= 0
                && this.strayTicks <= 0 && this.random.nextInt(this.isCalf() ? 500 : 1500 + (int) (this.trait(3) * 1500)) == 0) {
            this.startPlay(level);
        }
        // A calf wanders off now and then - after a Pollinator, if there's one about.
        if (this.isCalf() && this.leader != null && this.strayTicks <= 0 && this.strayCooldown <= 0 && this.sleepTicks <= 0
                && this.fleeTicks <= 0 && this.random.nextInt(1200) == 0) {
            this.startStray(level);
        }
        if (this.answerIn > 0 && --this.answerIn == 0) {
            this.answering = true;
            this.humCooldown = 0;
            this.hum(level);
            this.answering = false;
            if (this.answeringMimic) {
                // It answered something that wasn't a whale: silent, and away from it, for a minute.
                this.answeringMimic = false;
                this.wary = 1200;
            }
        }
        // It pitches its body as it climbs and dives (asleep, it hangs nose-up).
        double horizontal = velocity.horizontalDistance();
        float pitch = sleeping ? -80.0F
            : velocity.lengthSqr() > 1.0E-4 ? (float) Mth.clamp(-Mth.atan2(velocity.y, horizontal) * (180.0 / Math.PI), -55.0, 55.0) : 0.0F;
        this.bodyPitch = Mth.approach(this.bodyPitch, pitch, sleeping ? 1.5F : 2.0F);
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9).add(velocity.scale(0.1)));
    }

    // Keeps station on its pod's leader; null if the leader is gone (then it swims its own way).
    private @Nullable Vec3 followLeader(ServerLevel level) {
        if (!(level.getEntity(this.leader) instanceof DriftWhale lead) || !lead.isAlive() || lead.distanceToSqr(this) > 160.0 * 160.0) {
            this.leader = null;
            return null;
        }
        // Not a fixed station: each wanders round its place in the pod, on its own slow rhythm. A calf
        // keeps tucked in close beside its mother.
        double t = this.tickCount / (140.0 + 260.0 * this.trait(4)) + this.trait(5) * Math.PI * 2;
        Vec3 place = this.isCalf()
            ? new Vec3(lead.getBbWidth() * 0.7 * (this.trait(6) < 0.5 ? -1 : 1), -2.0, 3.0)
            : this.station.add(Math.sin(t) * 6.0, Math.sin(t * 1.7) * 2.5, Math.cos(t * 0.8) * 7.0);
        // The flock: its place turns round the leader as the leader turned, a beat ago.
        float heading = this.flockHeading(lead);
        Vec3 to = lead.position().add(inFrame(place, heading)).subtract(this.position());
        double speed = Math.min(CRUISE_SPEED * 1.6 * this.tempo(), to.length() * 0.02);
        Vec3 velocity = to.lengthSqr() > 4.0 ? to.normalize().scale(speed) : lead.getDeltaMovement().scale(0.9);
        if (to.lengthSqr() > 36.0) {
            this.faceDirection(to, 1.5F);
        } else {
            this.setYRot(Mth.approachDegrees(this.getYRot(), heading, 2.5F));
            this.yBodyRot = this.yHeadRot = this.getYRot();
        }
        return velocity;
    }

    // How many ticks behind its leader's turns this one is: a beat, its own.
    public int flockDelay() {
        return 6 + (int) (this.trait(10) * 14.0);
    }

    // Which way this whale was heading `ticks` ago (up to two seconds).
    public float pastYaw(int ticks) {
        int n = this.yawHistory.length;
        return this.yawHistory[Math.floorMod(this.tickCount - Math.min(ticks, n - 1), n)];
    }

    // The heading a follower keeps: its leader's, a beat late.
    public float flockHeading(DriftWhale lead) {
        return lead.pastYaw(this.flockDelay());
    }

    // An offset in a frame turned to `yaw` (x to the left, z ahead) as a world offset.
    private static Vec3 inFrame(Vec3 local, float yaw) {
        double r = yaw * (Math.PI / 180.0);
        double sin = Math.sin(r), cos = Math.cos(r);
        return new Vec3(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos);
    }

    // ---- the pod's life, decided by its leader

    private void leadThePod(ServerLevel level, @Nullable Player near) {
        // Mind the calves: one strayed too far, and she goes after it.
        if (this.tickCount % 20 == 0 && this.fetching == null) {
            this.checkCalves(level);
        }
        if (this.fleeTicks > 0 || this.playTicks > 0 || this.sleepTicks > 0 || this.circleTicks > 0) {
            return;
        }
        // Passing a Leviathan: once round it, humming low.
        if (this.tickCount % 100 == 0) {
            this.noticeLeviathan(level);
        }
        // Now and then, with no one close, the pod sleeps.
        if (near == null && this.circleTicks <= 0 && this.random.nextInt(9000) == 0) {
            this.sleepPod(level, 1200 + this.random.nextInt(600));
        }
    }

    // The pod: whales following this one.
    private java.util.List<DriftWhale> pod(ServerLevel level) {
        return level.getEntitiesOfClass(DriftWhale.class, this.getBoundingBox().inflate(96.0),
            w -> w != this && this.getUUID().equals(w.leader));
    }

    public void sleepPod(ServerLevel level, int ticks) {
        this.sleepTicks = ticks;
        for (DriftWhale w : this.pod(level)) {
            w.sleepTicks = ticks;
            w.strayTicks = 0;
        }
    }

    public boolean isAsleep() {
        return this.sleepTicks > 0;
    }

    private Vec3 sleepStep(ServerLevel level, @Nullable Player near) {
        if (--this.sleepTicks <= 0 || (near != null && near.distanceToSqr(this) < 10.0 * 10.0)) {
            this.sleepTicks = 0;
            return Vec3.ZERO;
        }
        this.setYRot(this.getYRot() + 0.25F);
        this.yBodyRot = this.yHeadRot = this.getYRot();
        if (this.tickCount % 200 == 0) {
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CONDUIT_AMBIENT, this.getSoundSource(), 1.2F, 0.4F);
        }
        return new Vec3(0.0, Math.sin(this.tickCount * 0.02 + this.trait(11) * 6.0) * 0.01, 0.0);
    }

    // Inside a Leviathan's bounds (or 24 blocks over or under it)? Then once round it, unless it
    // circled one in the last ten minutes. True if it set off.
    public boolean noticeLeviathan(ServerLevel level) {
        if (level.getGameTime() - this.lastCircle < 12000L) {
            return false;
        }
        for (int dy : new int[] {0, -24, 24}) {
            var start = level.structureManager().getStructureWithPieceAt(this.blockPosition().above(dy),
                holder -> holder.is(com.ggrantrichards.astralend.maze.MazeScoping.LEVIATHAN));
            if (start.isValid()) {
                var box = start.getBoundingBox();
                this.circleCentre = new Vec3((box.minX() + box.maxX()) / 2.0, Math.max(this.getY(), box.maxY() + 8.0), (box.minZ() + box.maxZ()) / 2.0);
                this.circleRadius = Mth.clamp(Math.max(box.getXSpan(), box.getZSpan()) / 2.0 + 12.0, 30.0, 120.0);
                this.circleAngle = Math.atan2(this.getZ() - this.circleCentre.z, this.getX() - this.circleCentre.x);
                double speed = CRUISE_SPEED * 2.5;
                this.circleTicks = (int) (Math.PI * 2.0 * this.circleRadius / speed);
                this.lastCircle = level.getGameTime();
                this.humLow(level);
                return true;
            }
        }
        return false;
    }

    public boolean isCircling() {
        return this.circleTicks > 0;
    }

    private Vec3 circleStep(ServerLevel level) {
        this.circleTicks--;
        if (this.circleCentre == null) {
            this.circleTicks = 0;
            return Vec3.ZERO;
        }
        this.circleAngle += CRUISE_SPEED * 2.5 / this.circleRadius;
        Vec3 point = this.circleCentre.add(Math.cos(this.circleAngle) * this.circleRadius, 0.0, Math.sin(this.circleAngle) * this.circleRadius);
        Vec3 to = point.subtract(this.position());
        Vec3 velocity = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(Math.min(CRUISE_SPEED * 2.5, 0.05 + to.length() * 0.05)) : Vec3.ZERO;
        this.faceDirection(velocity, 3.0F);
        if (this.circleTicks % 240 == 0) {
            this.humLow(level);
        }
        return velocity;
    }

    // Its lowest hum (for the Leviathans): the pod joins in.
    private void humLow(ServerLevel level) {
        this.humTicks = 80;
        this.entityData.set(DATA_HUMMING, true);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CONDUIT_AMBIENT, this.getSoundSource(), 5.0F, 0.32F);
        for (DriftWhale w : this.pod(level)) {
            w.answerIn = 10 + w.random.nextInt(30);
        }
    }

    // ---- calves

    public void startStray(ServerLevel level) {
        this.strayChase = null;
        for (EndPollinator p : level.getEntitiesOfClass(EndPollinator.class, this.getBoundingBox().inflate(24.0), EndPollinator::isAlive)) {
            this.strayChase = p;
            break;
        }
        double a = this.random.nextDouble() * Math.PI * 2.0;
        this.strayTarget = this.position().add(Math.cos(a) * 26.0, (this.random.nextDouble() - 0.5) * 8.0, Math.sin(a) * 26.0);
        this.strayTicks = 400;
    }

    public boolean isStraying() {
        return this.strayTicks > 0;
    }

    public @Nullable Entity strayingAfter() {
        return this.strayChase;
    }

    private Vec3 strayStep(ServerLevel level) {
        this.strayTicks--;
        Vec3 point;
        if (this.strayChase != null && this.strayChase.isAlive()) {
            // Round and round a Pollinator, close but never touching.
            double a = this.tickCount * 0.08;
            point = this.strayChase.position().add(Math.cos(a) * 4.0, 1.0, Math.sin(a) * 4.0);
        } else {
            this.strayChase = null;
            point = this.strayTarget != null ? this.strayTarget : this.position();
        }
        Vec3 to = point.subtract(this.position());
        Vec3 velocity = to.lengthSqr() > 1.0 ? to.normalize().scale(CRUISE_SPEED * 1.5 * this.tempo()) : Vec3.ZERO;
        this.faceDirection(velocity.lengthSqr() > 1.0E-4 ? velocity : to, 4.0F);
        return velocity;
    }

    // A mother's watch: a calf of hers more than 18 blocks off, and she calls and goes after it.
    // Returns the calf she's gone after, or null.
    public @Nullable DriftWhale checkCalves(ServerLevel level) {
        for (DriftWhale w : this.pod(level)) {
            if (w.isCalf() && w.distanceToSqr(this) > 18.0 * 18.0) {
                this.fetching = w.getUUID();
                this.humLow(level);
                return w;
            }
        }
        return null;
    }

    public boolean isFetching() {
        return this.fetching != null;
    }

    // Swims to the calf; close enough, nudges it back toward her, and it answers.
    private @Nullable Vec3 fetchStep(ServerLevel level) {
        if (!(level.getEntity(this.fetching) instanceof DriftWhale calf) || !calf.isAlive() || calf.distanceToSqr(this) > 160.0 * 160.0) {
            this.fetching = null;
            return null;
        }
        Vec3 to = calf.position().subtract(this.position());
        if (to.lengthSqr() < 8.0 * 8.0) {
            this.nudgeBack(level, calf);
            return Vec3.ZERO;
        }
        this.faceDirection(to, 3.0F);
        return to.normalize().scale(CRUISE_SPEED * 1.6 * this.tempo());
    }

    public void nudgeBack(ServerLevel level, DriftWhale calf) {
        Vec3 home = this.position().subtract(calf.position());
        calf.strayTicks = 0;
        calf.strayChase = null;
        calf.strayCooldown = 1800 + calf.random.nextInt(1200);
        if (home.lengthSqr() > 1.0E-4) {
            calf.setDeltaMovement(calf.getDeltaMovement().add(home.normalize().scale(0.35)));
        }
        calf.answerIn = 10;
        this.fetching = null;
        level.sendParticles(ParticleTypes.BUBBLE_POP, calf.getX(), calf.getY() + 1.0, calf.getZ(), 12, 1.0, 0.6, 1.0, 0.02);
    }

    // ---- a Mirror Wretch's false hum: answer it, then go quiet and keep away.

    public void hearMimic(Vec3 from) {
        if (this.wary > 0 || this.isTamed()) {
            return;
        }
        this.waryFrom = from;
        this.answeringMimic = true;
        this.answerIn = 15 + this.random.nextInt(25);
    }

    public boolean isWary() {
        return this.wary > 0;
    }

    // ---- Sunstorms: out of the Gilded Reach till it's past. Null when there's no storm over it.
    public @Nullable Vec3 outOfTheStorm(ServerLevel level) {
        if (this.isVehicle() || !EndMobs.stormy(level, this.blockPosition())) {
            return null;
        }
        var eclipse = com.ggrantrichards.astralend.world.EclipseLayout.nearest(this.getX(), this.getZ(),
            com.ggrantrichards.astralend.world.EclipseLayout.sampler(level.getChunkSource().randomState()), 1);
        Vec3 away = eclipse != null ? new Vec3(this.getX() - eclipse.x(), 0.0, this.getZ() - eclipse.z()) : this.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (away.lengthSqr() < 1.0E-4) {
            away = new Vec3(1.0, 0.0, 0.0);
        }
        return away.normalize().scale(CRUISE_SPEED * 2.0 * this.tempo());
    }

    // Its own swing as it swims: a slow side-to-side weave across its way, and a rise and fall.
    private Vec3 sway(Vec3 heading) {
        Vec3 side = new Vec3(-heading.z, 0.0, heading.x);
        if (side.lengthSqr() < 1.0E-6) {
            return Vec3.ZERO;
        }
        double t = this.tickCount / (60.0 + 120.0 * this.trait(7)) + this.trait(8) * Math.PI * 2;
        double amount = 0.02 + 0.04 * this.trait(9);
        return side.normalize().scale(Math.sin(t) * amount).add(0.0, Math.sin(t * 0.6) * amount * 0.4, 0.0);
    }

    // ---- play

    // The survey starts a bout of play to see it happen.
    public void playForTest(ServerLevel level) {
        this.startPlay(level);
    }

    private void startPlay(ServerLevel level) {
        DriftWhale mate = null;
        for (DriftWhale other : level.getEntitiesOfClass(DriftWhale.class, this.getBoundingBox().inflate(40.0),
                w -> w != this && !w.isVehicle() && w.playTicks <= 0 && w.fleeTicks <= 0)) {
            mate = other;
            break;
        }
        int kind = mate != null && this.random.nextInt(3) == 0 ? PLAY_CHASE : this.random.nextInt(3) == 0 ? PLAY_LOOP : PLAY_ROLL;
        Vec3 heading = this.getLookAngle().multiply(1.0, 0.0, 1.0);
        heading = heading.lengthSqr() > 1.0E-4 ? heading.normalize() : new Vec3(0.0, 0.0, 1.0);
        switch (kind) {
            case PLAY_LOOP -> this.beginPlay(PLAY_LOOP, 120, this.position().add(0.0, 9.0, 0.0), heading, 0.0);
            case PLAY_CHASE -> {
                // Round and round each other, a spiral about the point between them.
                Vec3 centre = this.position().add(mate.position()).scale(0.5);
                double a = Math.atan2(this.getZ() - centre.z, this.getX() - centre.x);
                this.beginPlay(PLAY_CHASE, 200, centre, heading, a);
                mate.beginPlay(PLAY_CHASE, 200, centre, heading, a + Math.PI);
                this.humCooldown = 0;
                this.hum(level);
            }
            default -> this.beginPlay(PLAY_ROLL, 60 + this.random.nextInt(50), null, heading, 0.0);
        }
    }

    private void beginPlay(int kind, int ticks, @Nullable Vec3 centre, Vec3 heading, double phase) {
        this.entityData.set(DATA_PLAY, kind);
        this.playTicks = ticks;
        this.playLength = ticks;
        this.playCentre = centre;
        this.playHeading = heading;
        this.playPhase = phase;
    }

    private Vec3 playStep(ServerLevel level) {
        int kind = this.play();
        double f = 1.0 - --this.playTicks / (double) this.playLength;     // 0 .. 1 through the bout
        if (this.playTicks <= 0) {
            this.entityData.set(DATA_PLAY, PLAY_NONE);
        }
        Vec3 velocity;
        if (kind == PLAY_LOOP && this.playCentre != null) {
            // A breach: up and over in a great loop, ending where it began, on its way.
            double a = -Math.PI / 2 + f * Math.PI * 2;
            Vec3 point = this.playCentre.add(this.playHeading.scale(Math.cos(a) * 9.0)).add(0.0, Math.sin(a) * 9.0, 0.0);
            Vec3 to = point.subtract(this.position());
            velocity = to.scale(0.25).add(this.playHeading.scale(0.02));
            if (velocity.length() > CRUISE_SPEED * 2.5) {
                velocity = velocity.normalize().scale(CRUISE_SPEED * 2.5);
            }
            this.faceDirection(this.playHeading, 3.0F);
        } else if (kind == PLAY_CHASE && this.playCentre != null) {
            double a = this.playPhase + f * Math.PI * 4;
            Vec3 point = this.playCentre.add(Math.cos(a) * 10.0, Math.sin(f * Math.PI * 2) * 3.0, Math.sin(a) * 10.0);
            Vec3 to = point.subtract(this.position());
            velocity = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(Math.min(CRUISE_SPEED * 2.0, to.length() * 0.2)) : Vec3.ZERO;
            this.faceDirection(new Vec3(-Math.sin(a), 0.0, Math.cos(a)), 6.0F);
        } else {
            // A roll (the renderer turns it over and over): it swims on as it rolls.
            velocity = this.getLookAngle().multiply(1.0, 0.0, 1.0).normalize().scale(CRUISE_SPEED * this.tempo());
        }
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
            this.playSound(SoundEvents.GENERIC_EAT, 1.2F, 0.5F);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CHORUS_FRUIT)),
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
        if (--this.humCooldown > 0 || this.wary > 0) {
            return;
        }
        this.humCooldown = 200 + this.random.nextInt(200);
        this.humTicks = 60;
        this.entityData.set(DATA_HUMMING, true);
        // Its own voice: the bigger, the deeper.
        float pitch = Mth.clamp(0.5F / this.getScale(), 0.38F, 0.85F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CONDUIT_AMBIENT, this.getSoundSource(), 4.0F, pitch);
        if (!this.answering) {
            // The others answer, each in its own time.
            for (DriftWhale other : level.getEntitiesOfClass(DriftWhale.class, this.getBoundingBox().inflate(48.0), w -> w != this)) {
                if (other.answerIn <= 0 && other.humTicks <= 0) {
                    other.answerIn = 15 + other.random.nextInt(50);
                }
            }
        }
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(16.0))) {
            ModAdvancements.award(player, "whale_song");
        }
        if (this.isTamed() && this.random.nextInt(8) == 0) {
            this.spawnAtLocation(new ItemStack(ModBlocks.WHALELIGHT_SCALE.get()), 0.5F);
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
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag input) {
        super.readAdditionalSaveData(input);
        String owner = (input.contains("Owner") ? input.getString("Owner") : "");
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
