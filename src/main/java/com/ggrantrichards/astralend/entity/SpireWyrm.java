package com.ggrantrichards.astralend.entity;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Spire Wyrm: a serpent kin of the Ender Dragon that nests atop a Resonant Spire, the mini-boss
// of the Glimmering Crags.
//
// It sleeps coiled on its nest until someone climbs near it or strikes it. Then it wakes: a roar, a
// wave of darkness through the spire, and it rises off the nest, uncoiling, as its boss bar appears.
//
// It coils up and down its spire, weaving out through the wall openings and back in, its long
// body (a chain of SpireWyrmParts) following the path its head flies. In a fight it:
//   - coils past you - its body hurts and throws whoever it touches, its tail most of all;
//   - lunges straight through the shaft at you;
//   - hovers and breathes: clouds of lingering void breath that harm whoever stands in them.
// Flying, it shrugs off half of every blow. But the spire's balconies hang with bells, and the
// Wyrm can't bear their ringing: ring one (strike it, or shoot it) while the Wyrm passes close and
// it is stunned - it crashes onto the ledge by the bell and lies there for six seconds, taking
// half again as much damage. Then it shakes it off, and no bell stuns it again for fifteen seconds.
//
// Killed, it drops the Resonant Key that opens the Resonance Vault at the top of the spire, and a
// Rift Pearl.
public class SpireWyrm extends Monster {
    public static final int SEGMENTS = 12;
    public static final double SEGMENT_LENGTH = 1.5;
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(SpireWyrm.class, EntityDataSerializers.INT);
    private static final TargetingConditions TARGETS = TargetingConditions.forCombat().range(64.0).ignoreLineOfSight();
    // How far down from the nest the spire goes, and its radii (inside the shaft / outside the walls).
    public static final double SPIRE_DEPTH = 76.0;
    private static final double INNER = 6.0, OUTER = 17.0;
    private static final int STUN_TICKS = 120, STUN_COOLDOWN = 300;

    public enum Phase { COIL, LUNGE, BREATH, STUNNED, ASLEEP, WAKING }
    private static final int WAKING_TICKS = 70;
    private static final double WAKE_RANGE = 22.0;

    private final SpireWyrmPart[] parts = new SpireWyrmPart[SEGMENTS];
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(),
        BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(false);
    private @Nullable Vec3 home;
    private double angle;
    private int phaseTicks;
    private int stunCooldown;
    private @Nullable Vec3 stunAt;
    private @Nullable Vec3 lungeAt;
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
        if (this.tickCount % 600 == 1) {
            this.findBells(level);
        }
        this.phaseTicks++;
        if (this.stunCooldown > 0) {
            this.stunCooldown--;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        Player target = this.target(level);
        if (this.phase() == Phase.ASLEEP) {
            this.setDeltaMovement(Vec3.ZERO);
            if (target != null && target.distanceToSqr(this.home) < WAKE_RANGE * WAKE_RANGE) {
                this.wake(level);
            } else if (this.tickCount % 40 == 0) {
                Vec3 snout = this.position().add(this.getLookAngle().scale(1.8)).add(0, 0.6, 0);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, snout.x, snout.y, snout.z, 3, 0.1, 0.05, 0.1, 0.01);
            }
            return;
        }
        if (this.phase() == Phase.WAKING) {
            // Rising off the nest, uncoiling, still roaring.
            Vec3 up = this.home.add(0, 10.0, 0).add(Math.cos(this.phaseTicks * 0.09) * 3.0, 0, Math.sin(this.phaseTicks * 0.09) * 3.0);
            Vec3 to = up.subtract(this.position());
            this.setDeltaMovement(to.lengthSqr() > 1.0E-4 ? to.normalize().scale(Math.min(0.35, to.length())) : Vec3.ZERO);
            this.lookAt(this.position().add(this.getDeltaMovement()).add(0, 0.2, 0));
            if (this.phaseTicks == 30) {
                this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 5.0F, 0.75F);
            }
            if (this.phaseTicks > WAKING_TICKS) {
                this.setPhase(Phase.COIL);
            }
            return;
        }
        this.listenForBells(level);
        Vec3 goal;
        double speed;
        switch (this.phase()) {
            case STUNNED -> {
                goal = this.stunAt != null ? this.stunAt : this.position();
                speed = 0.6;
                if (this.position().distanceToSqr(goal) < 1.0) {
                    speed = 0.0;
                    if (this.tickCount % 10 == 0) {
                        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 2.0, this.getZ(), 6, 0.6, 0.3, 0.6, 0.1);
                    }
                }
                if (this.phaseTicks > STUN_TICKS) {
                    this.stunCooldown = STUN_COOLDOWN;
                    this.setPhase(Phase.COIL);
                    this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 1.4F);
                }
            }
            case LUNGE -> {
                goal = this.lungeAt != null ? this.lungeAt : this.coilPoint();
                speed = 1.05;
                if (this.position().distanceToSqr(goal) < 4.0 || this.phaseTicks > 50) {
                    this.setPhase(Phase.COIL);
                }
            }
            case BREATH -> {
                if (target == null || this.phaseTicks > 70) {
                    this.setPhase(Phase.COIL);
                    goal = this.coilPoint();
                    speed = 0.5;
                } else {
                    Vec3 from = target.position().add(this.position().subtract(target.position()).normalize().scale(10.0)).add(0, 4, 0);
                    goal = from;
                    speed = 0.45;
                    this.lookAt(target.getEyePosition());
                    if (this.phaseTicks > 20 && this.phaseTicks % 12 == 0) {
                        this.breathe(level, target);
                    }
                }
            }
            default -> {
                goal = this.coilPoint();
                speed = target != null ? 0.6 : 0.35;
                if (target != null && this.phaseTicks > 140 + this.random.nextInt(80)) {
                    if (this.random.nextBoolean()) {
                        this.lungeAt = target.getEyePosition().add(target.getDeltaMovement().scale(10.0))
                            .add(target.getEyePosition().subtract(this.position()).normalize().scale(6.0));
                        this.setPhase(Phase.LUNGE);
                        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 1.1F);
                    } else {
                        this.setPhase(Phase.BREATH);
                    }
                }
            }
        }
        Vec3 to = goal.subtract(this.position());
        Vec3 want = to.lengthSqr() > 1.0E-4 ? to.normalize().scale(Math.min(speed, to.length())) : Vec3.ZERO;
        Vec3 v = this.getDeltaMovement();
        this.setDeltaMovement(v.add(want.subtract(v).scale(this.phase() == Phase.LUNGE ? 0.3 : 0.12)));
        if (this.phase() != Phase.BREATH && this.getDeltaMovement().lengthSqr() > 1.0E-3) {
            this.lookAt(this.position().add(this.getDeltaMovement()));
        }
        if (this.phase() != Phase.STUNNED && this.tickCount % 4 == 0) {
            this.hurtTouching(level);
        }
    }

    // Wakes: whoever disturbed it hears it first. A roar, a wave of darkness through the spire (like a
    // Warden's), a burst of void from the nest, then it rises (see WAKING above).
    public void wake(ServerLevel level) {
        if (this.phase() != Phase.ASLEEP) {
            return;
        }
        if (this.home == null) {
            this.home = this.position();
        }
        this.setPhase(Phase.WAKING);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.6F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.0, this.getZ(), 160, 3.0, 1.0, 3.0, 0.15);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(40.0, SPIRE_DEPTH, 40.0),
                p -> !p.isCreative() && !p.isSpectator())) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false), this);
        }
    }

    public boolean isAsleep() {
        return this.phase() == Phase.ASLEEP;
    }

    // For the trailer: breathe at a spot, or be stunned by the bell nearest it, on cue.
    public void breatheAtOnCue(ServerLevel level, Vec3 at) {
        this.setPhase(Phase.BREATH);
        this.lookAt(at);
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(this);
        cloud.setRadius(2.5F);
        cloud.setDuration(80);
        cloud.setRadiusPerTick(-0.005F);
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 60, 1.5, 0.3, 1.5, 0.02);
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 0.5, at.z, 80, 1.5, 0.3, 1.5, 0.3);
        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 0.8F);
    }

    public void stunAtNearestBellOnCue(ServerLevel level) {
        this.findBells(level);
        BlockPos best = null;
        for (BlockPos p : this.bells) {
            if (best == null || p.distSqr(this.blockPosition()) < best.distSqr(this.blockPosition())) {
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

    // Round and round the spire, rising and falling; every quarter turn it slips out through an
    // opening in the wall or back in.
    private Vec3 coilPoint() {
        Vec3 h = this.home != null ? this.home : this.position();
        this.angle += this.phase() == Phase.COIL ? 0.035 : 0.0;
        int quarter = Mth.floor(this.angle / (Math.PI / 2.0));
        double r = (quarter & 1) == 0 ? INNER : OUTER;
        double y = h.y - 10.0 - (SPIRE_DEPTH - 22.0) * (0.5 + 0.5 * Math.sin(this.tickCount * 0.006));
        return new Vec3(h.x + Math.cos(this.angle) * r, y, h.z + Math.sin(this.angle) * r);
    }

    private void lookAt(Vec3 at) {
        Vec3 d = at.subtract(this.position());
        if (d.horizontalDistanceSqr() < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(Mth.approachDegrees(this.getYRot(), yaw, 12.0F));
        this.setXRot((float) (-Mth.atan2(d.y, d.horizontalDistance()) * (180.0 / Math.PI)));
    }

    private @Nullable Player target(ServerLevel level) {
        Player p = level.getNearestPlayer(TARGETS, this);
        if (p == null || this.home == null || p.isCreative() || p.isSpectator()) {
            return null;
        }
        double dx = p.getX() - this.home.x, dz = p.getZ() - this.home.z;
        boolean inSpire = dx * dx + dz * dz < 30.0 * 30.0 && p.getY() > this.home.y - SPIRE_DEPTH - 6 && p.getY() < this.home.y + 24;
        if (inSpire) {
            this.setTarget(p);
            return p;
        }
        this.setTarget(null);
        return null;
    }

    // Lingering void breath where the target stands (and a little ahead of it).
    private void breathe(ServerLevel level, Player target) {
        Vec3 at = target.position().add(target.getDeltaMovement().scale(6.0));
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        cloud.setOwner(this);
        cloud.setRadius(2.5F);
        cloud.setDuration(140);
        cloud.setRadiusPerTick(-0.005F);
        cloud.setWaitTime(10);
        cloud.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 0));
        level.addFreshEntity(cloud);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 40, 1.5, 0.3, 1.5, 0.02);
        this.playSound(SoundEvents.ENDER_DRAGON_SHOOT, 2.0F, 0.8F);
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
                if (player.hurtServer(level, this.damageSources().mobAttack(this), i == 0 ? 8.0F : tail ? 7.0F : 5.0F)) {
                    Vec3 away = player.position().subtract(boxes.get(i).getCenter()).multiply(1.0, 0.0, 1.0).normalize();
                    player.push(away.x * (tail ? 1.4 : 0.8), 0.4, away.z * (tail ? 1.4 : 0.8));
                    if (player instanceof ServerPlayer sp) {
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
                    }
                }
            }
        }
    }

    // ---- the bells

    private void findBells(ServerLevel level) {
        this.bells.clear();
        BlockPos h = BlockPos.containing(this.home);
        for (BlockPos p : BlockPos.betweenClosed(h.offset(-16, (int) -SPIRE_DEPTH - 4, -16), h.offset(16, 6, 16))) {
            if (level.getBlockEntity(p) instanceof BellBlockEntity) {
                this.bells.add(p.immutable());
            }
        }
    }

    private void listenForBells(ServerLevel level) {
        if (this.phase() == Phase.STUNNED || this.stunCooldown > 0) {
            return;
        }
        for (Iterator<BlockPos> it = this.bells.iterator(); it.hasNext(); ) {
            BlockPos p = it.next();
            if (!(level.getBlockEntity(p) instanceof BellBlockEntity bell)) {
                it.remove();
                continue;
            }
            if (bell.shaking && bell.ticks <= 1 && this.near(Vec3.atCenterOf(p), 10.0)) {
                this.stun(level, p);
                return;
            }
        }
    }

    // Stunned: it drops onto the ledge beside the bell, toward the middle of the shaft.
    private void stun(ServerLevel level, BlockPos p) {
        Vec3 bellAt = Vec3.atCenterOf(p);
        Vec3 in = this.home != null ? new Vec3(this.home.x - bellAt.x, 0, this.home.z - bellAt.z).normalize() : Vec3.ZERO;
        this.stunAt = bellAt.add(in.scale(1.5)).add(0, -0.4, 0);
        this.setPhase(Phase.STUNNED);
        this.playSound(SoundEvents.ENDER_DRAGON_HURT, 3.0F, 0.6F);
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
        float scale = this.phase() == Phase.STUNNED ? 1.5F : 0.5F;
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
        return this.random.nextInt(3) == 0 ? SoundEvents.ENDER_DRAGON_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.home != null) {
            output.putDouble("HomeX", this.home.x);
            output.putDouble("HomeY", this.home.y);
            output.putDouble("HomeZ", this.home.z);
        }
        output.putBoolean("Asleep", this.phase() == Phase.ASLEEP);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getDoubleOr("HomeY", Double.NaN) == input.getDoubleOr("HomeY", 0.0)) {
            this.home = new Vec3(input.getDoubleOr("HomeX", 0.0), input.getDoubleOr("HomeY", 0.0), input.getDoubleOr("HomeZ", 0.0));
        }
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
        this.setPhase(input.getBooleanOr("Asleep", true) ? Phase.ASLEEP : Phase.COIL);
        this.trail.clear();
    }
}
