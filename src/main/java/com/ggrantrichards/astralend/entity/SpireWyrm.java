package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Spire Wyrm: a serpent kin of the Ender Dragon that nests atop a Resonant Spire, the mini-boss
// of the Glimmering Crags.
//
// It sleeps coiled on its nest on the platform at the top of the spire, snoring, until someone steps
// onto the platform or strikes it. Then it wakes: a roar, a wave of darkness, and it rises off the
// nest as its boss bar appears. The fight is around the top of its spire - it never strays more than
// 26 blocks from its nest, nor goes down the shaft after anyone still climbing. Its long body (a
// chain of SpireWyrmParts) follows the path its head flies, and hurts and throws whoever it brushes.
//
// It circles the top, and every few seconds attacks - always with a warning first:
//   - the dive: it stops and rears back, its eyes flaring and void gathering at its jaws, then dives
//     at where you stood; the floor shakes where it lands. Then it lies dazed with its head down for
//     a couple of seconds - the time to hit it;
//   - the breath: it pulls back, draws breath with its jaws open, then breathes a line of lingering
//     void clouds across the platform, from below it toward you and past.
// Flying, it shrugs off much of every blow. But bells hang on posts round the platform: ring one
// (strike it, or shoot it) while the Wyrm is near and it crashes down beside it, stunned, for six
// seconds, taking half again as much damage. No bell stuns it again for fifteen seconds. Below half
// health it is enraged: faster, quicker to attack, a longer breath. With no one left at the top it
// flies back to its nest and sleeps again.
//
// Killed, it drops the Resonant Key that opens the Resonance Vault on the platform, and a Rift Pearl.
public class SpireWyrm extends Monster {
    public static final int SEGMENTS = 12;
    public static final double SEGMENT_LENGTH = 1.5;
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(SpireWyrm.class, EntityDataSerializers.INT);
    private static final TargetingConditions TARGETS = TargetingConditions.forCombat().range(64.0).ignoreLineOfSight();
    private static final int STUN_TICKS = 120, STUN_COOLDOWN = 300;

    public enum Phase { CIRCLE, WIND_DIVE, DIVE, GROUNDED, WIND_BREATH, BREATH, STUNNED, ASLEEP, WAKING, RETURNING }
    private static final int WAKING_TICKS = 70;

    private final SpireWyrmPart[] parts = new SpireWyrmPart[SEGMENTS];
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(false);
    private @Nullable Vec3 home;
    private double angle;
    private int phaseTicks;
    private int stunCooldown;
    private @Nullable Vec3 stunAt;
    private @Nullable Vec3 diveAt;
    private @Nullable Vec3 breathFrom;
    private @Nullable Vec3 breathAt;
    private boolean enraged;
    private int divesInRow;             // never more than two dives without a breath between
    private int idleTicks;
    private final List<BlockPos> bells = new ArrayList<>();

    public SpireWyrm(EntityType<? extends SpireWyrm> type, Level level) {
        super(type, level);
        for (int i = 0; i < SEGMENTS; i++) {
            this.parts[i] = new SpireWyrmPart(this, i, i >= 10 ? 1.2F : 1.6F);
        }
        this.setNoGravity(true);
        this.noPhysics = true;
        this.xpReward = 250;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, Phase.ASLEEP.ordinal());
    }

    public Phase phase() {
        return Phase.values()[this.entityData.get(DATA_PHASE)];
    }

    private void setPhase(Phase phase) {
        this.entityData.set(DATA_PHASE, phase.ordinal());
        this.phaseTicks = 0;
    }

    // ---- parts

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
        return this.parts;
    }

    public SpireWyrmPart part(int i) {
        return this.parts[i];
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < SEGMENTS; i++) {
            this.parts[i].setId(id + i + 1);
        }
    }

    // ---- movement

    @Override
    public void travel(Vec3 input) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        this.setNoGravity(true);
        super.tick();
        if (!this.level().isClientSide()) {
            this.bossEvent.setVisible(this.phase() != Phase.ASLEEP);   // no boss bar until it wakes
        }
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
        this.updateTrail();
    }

    // The head's path, a point every quarter block; each segment sits SEGMENT_LENGTH further along it.
    private void updateTrail() {
        Vec3 head = this.position().add(0.0, this.getBbHeight() / 2.0, 0.0);
        if (this.trail.isEmpty() && this.phase() == Phase.ASLEEP) {
            // Asleep: the body lies coiled round the head, a slowly widening spiral on the nest.
            Vec3 c = head.add(0.0, -0.4, 0.0);
            double a = Math.toRadians(this.getYRot() + 90.0F);
            double length = (SEGMENTS + 2) * SEGMENT_LENGTH;
            for (int i = 0; i <= length * 4; i++) {
                double walked = i * 0.25;
                double r = 2.2 + walked * 0.09;
                a -= 0.25 / r;
                this.trail.addLast(c.add(Math.cos(a) * r - Math.cos(Math.toRadians(this.getYRot() + 90.0F)) * 2.2,
                    walked * 0.02, Math.sin(a) * r - Math.sin(Math.toRadians(this.getYRot() + 90.0F)) * 2.2));
            }
        }
        if (this.trail.isEmpty()) {
            Vec3 back = this.getLookAngle().scale(-1.0);
            for (int i = (int) ((SEGMENTS + 2) * SEGMENT_LENGTH * 4); i >= 0; i--) {
                this.trail.addLast(head.add(back.scale(i * 0.25)));
            }
        }
        if (this.trail.peekFirst().distanceToSqr(head) > 0.0625) {
            this.trail.addFirst(head);
            while (this.trail.size() > (SEGMENTS + 2) * SEGMENT_LENGTH * 4 + 8) {
                this.trail.removeLast();
            }
        }
        Vec3 ahead = head;
        double walked = 0.0;
        int next = 0;
        Vec3 prev = head;
        for (Vec3 p : this.trail) {
            walked += p.distanceTo(prev);
            prev = p;
            while (next < SEGMENTS && walked >= (next + 1) * SEGMENT_LENGTH) {
                SpireWyrmPart part = this.parts[next];
                double h = part.getBbHeight() / 2.0;
                part.xo = part.getX();
                part.yo = part.getY();
                part.zo = part.getZ();
                part.xOld = part.getX();
                part.yOld = part.getY();
                part.zOld = part.getZ();
                part.setPos(p.x, p.y - h, p.z);
                Vec3 d = ahead.subtract(p);
                if (d.lengthSqr() > 1.0E-4) {
                    part.setYRot((float) (Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F);
                    part.setXRot((float) (-Mth.atan2(d.y, d.horizontalDistance()) * (180.0 / Math.PI)));
                }
                ahead = p;
                next++;
            }
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.home == null) {
            this.home = this.position();
        }
        if (this.tickCount % 200 == 1) {
            this.findBells(level);
        }
        this.phaseTicks++;
        if (this.stunCooldown > 0) {
            this.stunCooldown--;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        LivingEntity target = this.target(level);
        boolean enraged = this.getHealth() < this.getMaxHealth() * 0.5F;
        if (enraged && !this.enraged) {
            this.enraged = true;                                     // the second phase: faster, fiercer
            this.playSound(ModSounds.WYRM_ROAR.get(), 6.0F, 1.1F);
        }
        Vec3 goal = this.position();
        double speed = 0.0;
        double grip = 0.15;
        switch (this.phase()) {
            case ASLEEP -> {
                this.setDeltaMovement(Vec3.ZERO);
                if (target != null && this.onPlatform(target)) {
                    this.wake(level);
                } else if (this.tickCount % 50 == 0) {
                    Vec3 snout = this.position().add(this.getLookAngle().scale(1.8)).add(0, 0.6, 0);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, snout.x, snout.y, snout.z, 3, 0.1, 0.05, 0.1, 0.01);
                    if (this.random.nextInt(3) == 0) {
                        this.playSound(ModSounds.WYRM_SNORE.get(), 1.2F, 0.9F + this.random.nextFloat() * 0.2F);
                    }
                }
                return;
            }
            case WAKING -> {
                // It lifts its head, uncoils off the nest and rises above it, roaring.
                goal = this.home.add(Math.cos(this.phaseTicks * 0.08) * 4.0, 7.0, Math.sin(this.phaseTicks * 0.08) * 4.0);
                speed = 0.3;
                if (this.phaseTicks == 25) {
                    this.playSound(ModSounds.WYRM_ROAR.get(), 6.0F, 0.85F);
                }
                if (this.phaseTicks > WAKING_TICKS) {
                    this.setPhase(Phase.CIRCLE);
                }
            }
            case CIRCLE -> {
                // Round the top of its spire, rising and dipping, never far from the nest.
                this.angle += this.enraged ? 0.045 : 0.032;
                double r = 15.0 + Math.sin(this.angle * 1.7) * 3.0;
                goal = this.home.add(Math.cos(this.angle) * r, 8.0 + Math.sin(this.angle * 2.3) * 3.0, Math.sin(this.angle) * r);
                speed = this.enraged ? 0.75 : 0.6;
                if (target == null) {
                    if (++this.idleTicks > 600) {
                        this.setPhase(Phase.RETURNING);                // no one left to fight: back to sleep
                    }
                } else {
                    this.idleTicks = 0;
                    int wait = this.enraged ? 70 + this.random.nextInt(40) : 110 + this.random.nextInt(60);
                    if (this.phaseTicks > wait) {
                        boolean breath = this.divesInRow >= 2 || this.random.nextInt(3) == 0;
                        this.divesInRow = breath ? 0 : this.divesInRow + 1;
                        this.setPhase(breath ? Phase.WIND_BREATH : Phase.WIND_DIVE);
                        this.playSound(ModSounds.WYRM_CHARGE.get(), 4.0F, this.phase() == Phase.WIND_DIVE ? 0.9F : 1.2F);
                    }
                }
            }
            case WIND_DIVE -> {
                // It stops, rears back and stares: its eyes flare and void gathers at its jaws. Then it dives.
                goal = this.position().add(0, 0.05, 0);
                speed = 0.05;
                if (target != null) {
                    this.lookAt(target.getEyePosition());
                    this.diveAt = new Vec3(target.getX(), Math.max(target.getY(), this.home.y), target.getZ());
                }
                if (this.tickCount % 3 == 0) {
                    Vec3 jaw = this.position().add(this.getLookAngle().scale(2.2)).add(0, 0.8, 0);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, jaw.x, jaw.y, jaw.z, 8, 0.4, 0.4, 0.4, 0.05);
                }
                if (this.phaseTicks > (this.enraged ? 22 : 32)) {
                    this.setPhase(this.diveAt != null ? Phase.DIVE : Phase.CIRCLE);
                }
            }
            case DIVE -> {
                goal = this.diveAt;
                speed = this.enraged ? 1.5 : 1.25;
                grip = 0.35;
                if (this.position().distanceToSqr(this.diveAt) < 2.5 || this.phaseTicks > 45) {
                    this.slam(level);
                    this.setPhase(Phase.GROUNDED);
                }
            }
            case GROUNDED -> {
                // Head down on the floor where it struck, dazed: the time to hit it.
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
                if (this.tickCount % 8 == 0) {
                    level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5, this.getZ(), 4, 0.5, 0.2, 0.5, 0.05);
                }
                if (this.phaseTicks > (this.enraged ? 35 : 50)) {
                    this.setPhase(Phase.CIRCLE);
                }
                this.moveTowards(this.position(), 0.0, grip);
                return;
            }
            case WIND_BREATH -> {
                // It pulls back above the platform, jaws opening, drawing breath.
                if (target != null) {
                    Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                    goal = target.position().add(away.scale(11.0)).add(0, 6.0, 0);
                    this.lookAt(target.getEyePosition());
                    this.breathFrom = this.position();
                    this.breathAt = new Vec3(target.getX(), Math.max(target.getY(), this.home.y), target.getZ());
                }
                speed = 0.45;
                if (this.tickCount % 3 == 0) {
                    Vec3 jaw = this.position().add(this.getLookAngle().scale(2.2)).add(0, 0.6, 0);
                    level.sendParticles(ParticleTypes.PORTAL, jaw.x, jaw.y, jaw.z, 10, 0.3, 0.3, 0.3, 0.4);
                }
                if (this.phaseTicks > 25) {
                    this.setPhase(this.breathAt != null ? Phase.BREATH : Phase.CIRCLE);
                    this.playSound(ModSounds.WYRM_BREATH.get(), 4.0F, 1.0F);
                }
            }
            case BREATH -> {
                // A line of void breath across the platform, from below it toward where you stood, and past.
                goal = this.position();
                speed = 0.0;
                this.lookAt(this.breathAt);
                int clouds = this.enraged ? 7 : 5;
                if (this.phaseTicks % 6 == 1 && this.phaseTicks / 6 < clouds) {
                    Vec3 flat = this.breathAt.subtract(this.breathFrom).multiply(1, 0, 1);
                    Vec3 dir = flat.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : flat.normalize();
                    Vec3 start = new Vec3(this.breathFrom.x, this.breathAt.y, this.breathFrom.z).add(dir.scale(Math.min(6.0, flat.length() * 0.4)));
                    this.breatheCloud(level, start.add(dir.scale(3.0 * (this.phaseTicks / 6))));
                }
                if (this.phaseTicks > clouds * 6 + 12) {
                    this.setPhase(Phase.CIRCLE);
                }
            }
            case STUNNED -> {
                goal = this.stunAt != null ? this.stunAt : this.position();
                speed = 0.7;
                grip = 0.3;
                if (this.position().distanceToSqr(goal) < 1.0) {
                    speed = 0.0;
                    if (this.tickCount % 10 == 0) {
                        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 1.5, this.getZ(), 6, 0.6, 0.3, 0.6, 0.1);
                    }
                }
                if (this.phaseTicks > STUN_TICKS) {
                    this.stunCooldown = STUN_COOLDOWN;
                    this.setPhase(Phase.CIRCLE);
                    this.playSound(ModSounds.WYRM_ROAR.get(), 5.0F, 1.15F);
                }
            }
            case RETURNING -> {
                goal = this.home;
                speed = 0.4;
                if (target != null) {
                    this.setPhase(Phase.CIRCLE);
                } else if (this.position().distanceToSqr(this.home) < 1.0) {
                    this.setPos(this.home.x, this.home.y, this.home.z);
                    this.setDeltaMovement(Vec3.ZERO);
                    this.trail.clear();                               // it curls up again
                    this.setPhase(Phase.ASLEEP);
                    return;
                }
            }
        }
        this.listenForBells(level);
        this.moveTowards(goal, speed, grip);
        boolean facing = this.phase() == Phase.WIND_DIVE || this.phase() == Phase.WIND_BREATH || this.phase() == Phase.BREATH;
        if (!facing && this.getDeltaMovement().lengthSqr() > 1.0E-3) {
            this.lookAt(this.position().add(this.getDeltaMovement()));
        }
        if (this.phase() != Phase.STUNNED && this.phase() != Phase.WAKING && this.tickCount % 4 == 0) {
            this.hurtTouching(level);
        }
    }

    // Steers toward goal at up to `speed` blocks a tick, turning its velocity a share `grip` of the way
    // each tick (so it swings round in curves rather than snapping). It never strays more than 26
    // blocks from its nest, nor dips below the platform.
    private void moveTowards(Vec3 goal, double speed, double grip) {
        Vec3 g = goal;
        Vec3 off = g.subtract(this.home).multiply(1, 0, 1);
        if (off.length() > 26.0) {
            g = this.home.add(off.normalize().scale(26.0)).add(0, g.y - this.home.y, 0);
        }
        if (g.y < this.home.y) {
            g = new Vec3(g.x, this.home.y, g.z);
        }
        Vec3 to = g.subtract(this.position());
        Vec3 want = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(Math.min(speed, to.length())) : Vec3.ZERO;
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.add(want.subtract(v).scale(grip)));
    }

    // Whoever is on the platform at the top of the spire (or just over it).
    private boolean onPlatform(Entity p) {
        double dx = p.getX() - this.home.x, dz = p.getZ() - this.home.z;
        return dx * dx + dz * dz < 14.0 * 14.0 && p.getY() > this.home.y - 3.0 && p.getY() < this.home.y + 20.0;
    }

    // The dive lands: the floor shakes, those beside it are hurt and thrown.
    private void slam(ServerLevel level) {
        Vec3 at = this.position();
        this.playSound(ModSounds.WYRM_SLAM.get(), 5.0F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 3, 1.0, 0.2, 1.0, 0.0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 60, 2.0, 0.3, 2.0, 0.08);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(3.5, 2.0, 3.5),
                p -> !p.isCreative() && !p.isSpectator())) {
            if (player.hurtServer(level, this.damageSources().mobAttack(this), this.enraged ? 12.0F : 9.0F)) {
                Vec3 away = player.position().subtract(at).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                player.push(away.x * 0.55, 0.3, away.z * 0.55);     // staggered, not flung off the tower
                if (player instanceof ServerPlayer sp) {
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                }
            }
        }
    }

    // One cloud of void breath, lying on the floor where it lands.
    private void breatheCloud(ServerLevel level, Vec3 at) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(this);
        cloud.setRadius(2.2F);
        cloud.setDuration(120);
        cloud.setRadiusPerTick(-0.006F);
        cloud.setWaitTime(4);
        cloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 0));
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.4, at.z, 30, 1.2, 0.2, 1.2, 0.02);
    }

    // Wakes: a roar, a wave of darkness over the top of the spire (like a Warden's), a burst of void
    // from the nest, then it rises (see WAKING above).
    public void wake(ServerLevel level) {
        if (this.phase() != Phase.ASLEEP) {
            return;
        }
        if (this.home == null) {
            this.home = this.position();
        }
        this.setPhase(Phase.WAKING);
        this.idleTicks = 0;
        this.playSound(ModSounds.WYRM_ROAR.get(), 6.0F, 0.75F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 160, 3.0, 1.0, 3.0, 0.15);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(40.0, 30.0, 40.0),
                p -> !p.isCreative() && !p.isSpectator())) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false), this);
        }
    }

    public boolean isAsleep() {
        return this.phase() == Phase.ASLEEP;
    }

    // For the trailer and screenshots: breathe at a spot, circle the top, or be stunned by the bell
    // nearest a spot, on cue.
    public void breatheAtOnCue(ServerLevel level, Vec3 at) {
        this.breathFrom = this.position();
        this.breathAt = at;
        this.setPhase(Phase.BREATH);
        this.playSound(ModSounds.WYRM_BREATH.get(), 4.0F, 1.0F);
    }

    public void circleNestOnCue(int ticks) {
        if (this.phase() != Phase.ASLEEP) {
            this.setPhase(Phase.CIRCLE);
        }
    }

    public void stunAtBellOnCue(ServerLevel level, BlockPos near) {
        this.findBells(level);
        BlockPos best = null;
        for (BlockPos p : this.bells) {
            if (best == null || p.distSqr(near) < best.distSqr(near)) {
                best = p;
            }
        }
        if (best != null && level.getBlockEntity(best) instanceof BellBlockEntity bell) {
            bell.onHit(net.minecraft.core.Direction.NORTH);
            level.playSound(null, best, SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.BLOCKS, 2.0F, 1.0F);
            this.stunCooldown = 0;
            this.stun(level, best);
        }
    }

    private void lookAt(Vec3 at) {
        Vec3 d = at.subtract(this.position());
        if (d.horizontalDistanceSqr() < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 10.0F));
        float pitch = (float) (-Mth.atan2(d.y, d.horizontalDistance()) * (180.0 / Math.PI));
        this.setXRot(Mth.approach(this.getXRot(), pitch, 6.0F));
    }

    // The player it fights: one at the top of its spire (on the platform or in the air above it). Those
    // still climbing the shaft below are left to climb.
    private @Nullable LivingEntity testTarget;

    // For the survey's fight test: fight this stand-in as if it were a player at the top.
    public void fightForTest(LivingEntity stand) {
        this.testTarget = stand;
    }

    private @Nullable LivingEntity target(ServerLevel level) {
        LivingEntity p = this.testTarget != null ? this.testTarget : level.getNearestPlayer(TARGETS, this);
        if (p == null || this.home == null || p instanceof Player pl && (pl.isCreative() || pl.isSpectator())) {
            this.setTarget(null);
            return null;
        }
        double dx = p.getX() - this.home.x, dz = p.getZ() - this.home.z;
        boolean atTop = dx * dx + dz * dz < 36.0 * 36.0 && p.getY() > this.home.y - 6.0 && p.getY() < this.home.y + 40.0;
        this.setTarget(atTop ? p : null);
        return atTop ? p : null;
    }

    // Its body hurts and throws whoever it touches; the tail swipes hardest.
    private void hurtTouching(ServerLevel level) {
        List<AABB> boxes = new ArrayList<>();
        boxes.add(this.getBoundingBox());
        for (SpireWyrmPart part : this.parts) {
            boxes.add(part.getBoundingBox());
        }
        for (int i = 0; i < boxes.size(); i++) {
            boolean tail = i > SEGMENTS - 3;
            for (Player player : level.getEntitiesOfClass(Player.class, boxes.get(i).inflate(0.3), p -> !p.isCreative() && !p.isSpectator())) {
                if (player.hurtServer(level, this.damageSources().mobAttack(this), i == 0 ? 7.0F : tail ? 6.0F : 4.0F)) {
                    Vec3 away = player.position().subtract(boxes.get(i).getCenter()).multiply(1.0, 0.0, 1.0);
                    away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                    player.push(away.x * (tail ? 1.2 : 0.7), 0.35, away.z * (tail ? 1.2 : 0.7));
                    if (player instanceof ServerPlayer sp) {
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                    }
                }
            }
        }
    }

    // ---- the bells: they hang on posts round the rim of the platform (and on the ledges below)

    private void findBells(ServerLevel level) {
        this.bells.clear();
        BlockPos h = BlockPos.containing(this.home);
        for (BlockPos p : BlockPos.betweenClosed(h.offset(-22, -6, -22), h.offset(22, 8, 22))) {
            if (level.getBlockEntity(p) instanceof BellBlockEntity) {
                this.bells.add(p.immutable());
            }
        }
    }

    private void listenForBells(ServerLevel level) {
        if (this.phase() == Phase.STUNNED || this.phase() == Phase.WAKING || this.stunCooldown > 0) {
            return;
        }
        for (Iterator<BlockPos> it = this.bells.iterator(); it.hasNext(); ) {
            BlockPos p = it.next();
            if (!(level.getBlockEntity(p) instanceof BellBlockEntity bell)) {
                it.remove();
                continue;
            }
            if (bell.shaking && bell.ticks <= 1 && this.near(Vec3.atCenterOf(p), 16.0)) {
                this.stun(level, p);
                return;
            }
        }
    }

    // Stunned by the ringing: it crashes onto the platform beside the bell and lies there.
    private void stun(ServerLevel level, BlockPos p) {
        Vec3 bellAt = Vec3.atCenterOf(p);
        Vec3 in = new Vec3(this.home.x - bellAt.x, 0, this.home.z - bellAt.z);
        in = in.lengthSqr() < 1.0E-4 ? Vec3.ZERO : in.normalize();
        this.stunAt = new Vec3(bellAt.x, this.home.y, bellAt.z).add(in.scale(3.0));
        this.setPhase(Phase.STUNNED);
        this.playSound(ModSounds.WYRM_HURT.get(), 5.0F, 0.6F);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(32.0))) {
            com.ggrantrichards.astralend.world.ModAdvancements.award(player, "ring_out");
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, bellAt.x, bellAt.y, bellAt.z, 1, 0, 0, 0, 0);
    }

    private boolean near(Vec3 at, double range) {
        if (this.position().distanceToSqr(at) < range * range) {
            return true;
        }
        for (SpireWyrmPart part : this.parts) {
            if (part.position().distanceToSqr(at) < range * range) {
                return true;
            }
        }
        return false;
    }

    // ---- damage

    public boolean hurtFromPart(ServerLevel level, SpireWyrmPart part, DamageSource source, float damage) {
        return this.hurtServer(level, source, damage);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.phase() == Phase.ASLEEP && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.wake(level);   // a blow to a sleeping wyrm lands in full, and wakes it
            return super.hurtServer(level, source, damage);
        }
        float scale = switch (this.phase()) {
            case STUNNED -> 1.5F;      // crashed by a bell
            case GROUNDED -> 1.25F;    // head down after a dive
            default -> 0.6F;           // flying, it shrugs off much of a blow
        };
        return super.hurtServer(level, source, damage * scale);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return super.doHurtTarget(level, target);
    }

    // ---- boss bar, sounds, saving

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        if (this.phase() == Phase.ASLEEP) {
            return null;
        }
        return ModSounds.WYRM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.WYRM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.WYRM_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.95F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.putDouble("HomeX", this.home.x);
            output.putDouble("HomeY", this.home.y);
            output.putDouble("HomeZ", this.home.z);
        }
        output.putBoolean("Asleep", this.phase() == Phase.ASLEEP);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
        super.readAdditionalSaveData(input);
        if ((input.contains("HomeY") ? input.getDouble("HomeY") : Double.NaN) == (input.contains("HomeY") ? input.getDouble("HomeY") : 0.0)) {
            this.home = new Vec3((input.contains("HomeX") ? input.getDouble("HomeX") : 0.0), (input.contains("HomeY") ? input.getDouble("HomeY") : 0.0), (input.contains("HomeZ") ? input.getDouble("HomeZ") : 0.0));
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        this.setPhase((input.contains("Asleep") ? input.getBoolean("Asleep") : true) ? Phase.ASLEEP : Phase.CIRCLE);
        this.trail.clear();
    }
}
