package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.EndChimesBlockEntity;
import java.util.ArrayList;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Curator: the keeper of a Leviathan's reliquary, a mini-boss. A hulking war-construct of the End's
// builders, 4.4 blocks tall (after beta.4, from Adam Smasher and Hearthstone's Curator): obsidian and
// gunmetal plating, massive pauldrons, one violet visor slit, its core in a glass case in its chest, a
// cannon for a right forearm, thrusters for feet - it hovers a hand's breadth off the floor. One to each
// Leviathan; it never respawns. It's dormant until someone walks into the reliquary, then:
//   - Core Cannon: at range it raises its arm (the visor and muzzle brighten, a rising whine), locks its
//     aim a moment before it fires, and fires a beam of force down the line. Step aside in that moment.
//     (Break its line of sight instead, and mind the Swap below.)
//   - Shoulder Charge: it crouches, thrusters flaring, and hurls itself straight at you. Sidestep: if it
//     hits a wall or a pylon instead it is stunned, and takes half again as much harm.
//   - Second phase, at half health: the case in its chest shatters, it calls the Hollowbacks of its
//     reliquary, and everything comes faster - but its bare core takes a quarter more harm.
//   - Phase Step: shoot at it and it teleports aside, and strikes where it reappears. Unless...
//   - Pylons: four End Chimes stand round the arena. Ring one (strike it) and for ten seconds nothing
//     can teleport near it - the Curator included, so near a still pylon arrows and tridents land. It
//     knows: it goes for still pylons first, and smashes them.
//   - Gravity Toll: every so often it slams down, and the ring of the blow lifts everyone near it
//     (two seconds of Levitation): over the floor's void gaps, deadly; for someone with a mace, height.
//   - Swap: lose its sight for three seconds and it trades places with you.
//   - Water: a splash of water staggers it for two seconds - it can't move, strike or teleport, and
//     takes half again as much harm.
// It drops the Curator's Core (the Starforge's Void Recall needs one) and a great deal of experience.
public class Curator extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_DORMANT = SynchedEntityData.defineId(Curator.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SLAM = SynchedEntityData.defineId(Curator.class, EntityDataSerializers.INT);
    // What it's doing (for the renderer): one of the ACT_ codes, and how many ticks of it are left.
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(Curator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_ACTION_T = SynchedEntityData.defineId(Curator.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED = SynchedEntityData.defineId(Curator.class, EntityDataSerializers.BOOLEAN);
    public static final int ACT_NONE = 0, ACT_AIM = 1, ACT_RECOIL = 2, ACT_CROUCH = 3, ACT_CHARGE = 4, ACT_PUNCH_LEFT = 5, ACT_PUNCH_RIGHT = 6;
    public static final int AIM_TICKS = 32, CROUCH_TICKS = 20, CHARGE_TICKS = 14, RECOIL_TICKS = 10, PUNCH_TICKS = 8;
    private static final double BEAM_RANGE = 24.0;
    public static final double ARENA = 26.0;     // its reach from home
    private final ServerBossEvent bossEvent = new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.PURPLE,
        BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos home;
    private int attackCooldown;
    private int slamCooldown = 160;
    private int unseen;
    private int stagger;
    private @Nullable BlockPos smashing;
    private int cannonCooldown = 80;
    private int chargeCooldown = 120;
    private Vec3 aim = Vec3.ZERO;          // where the cannon is locked on, or which way the charge goes
    private boolean leftNext;

    public Curator(EntityType<? extends Curator> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 300;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.ARMOR_TOUGHNESS, 4.0)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_DORMANT, true);
        builder.define(DATA_SLAM, 0);
        builder.define(DATA_ACTION, ACT_NONE);
        builder.define(DATA_ACTION_T, 0);
        builder.define(DATA_EXPOSED, false);
    }

    public int action() {
        return this.entityData.get(DATA_ACTION);
    }

    public int actionTicks() {
        return this.entityData.get(DATA_ACTION_T);
    }

    // Its second phase: the case in its chest shattered, the core bare.
    public boolean isExposed() {
        return this.entityData.get(DATA_EXPOSED);
    }

    // For the screenshots: awake (and, if asked, in its second phase), standing as in the fight.
    public void showAwake(boolean exposed) {
        this.entityData.set(DATA_DORMANT, false);
        this.entityData.set(DATA_EXPOSED, exposed);
    }

    private void act(int action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        this.entityData.set(DATA_ACTION_T, ticks);
    }

    public boolean isDormant() {
        return this.entityData.get(DATA_DORMANT);
    }

    // 0..1 while slamming down (for the renderer).
    public float slamProgress() {
        return this.entityData.get(DATA_SLAM) / 20.0F;
    }

    public boolean isStaggered() {
        return this.stagger > 0;
    }

    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    public @Nullable BlockPos home() {
        return this.home;
    }

    @Override
    protected void registerGoals() {
        // It's driven entirely from customServerAiStep.
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public void checkDespawn() {
    }

    // ---- being hurt

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isDormant()) {
            if (source.getEntity() instanceof Player) {
                this.wake(level);
            }
            return false;
        }
        // Water staggers it.
        var direct = source.getDirectEntity();
        if (direct != null && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType()).getPath().contains("potion")) {
            this.stagger = 40;
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 3.0, this.getZ(), 30, 0.6, 1.0, 0.6, 0.1);
            this.playSound(SoundEvents.ENDERMAN_HURT, 1.5F, 0.5F);
        }
        // Phase Step: a projectile makes it teleport aside - unless a still pylon is near (or it's busy charging).
        if (direct instanceof Projectile && this.stagger <= 0 && this.action() != ACT_CHARGE && this.phaseStep(level)) {
            return false;
        }
        float scaled = amount * (this.stagger > 0 ? 1.5F : 1.0F) * (this.isExposed() ? 1.25F : 1.0F);
        boolean hurt = super.hurtServer(level, source, scaled);
        if (hurt && !this.isExposed() && this.isAlive() && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            this.expose(level);
        }
        return hurt;
    }

    // Half its health gone: the case in its chest shatters, and it calls its reliquary's Hollowbacks.
    private void expose(ServerLevel level) {
        this.entityData.set(DATA_EXPOSED, true);
        Vec3 chest = this.position().add(0.0, 2.6, 0.0);
        level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.5F, 0.8F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, chest.x, chest.y, chest.z, 60, 0.6, 0.6, 0.6, 0.4);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, chest.x, chest.y, chest.z, 120, 0.4, 0.4, 0.4, 0.6);
        BlockPos centre = this.home != null ? this.home : this.blockPosition();
        int near = level.getEntitiesOfClass(Hollowback.class, this.getBoundingBox().inflate(ARENA)).size();
        for (int i = near; i < 3; i++) {
            BlockPos at = EndMobs.safeSpotNear(level, centre, 5, 12, this.random);
            var hollowback = ModEntities.HOLLOWBACK.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (at != null && hollowback != null) {
                hollowback.snapTo(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(hollowback);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 1.5, at.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.1);
            }
        }
    }

    private boolean canTeleport(ServerLevel level) {
        return !EndChimesBlockEntity.stillNear(level, this.position(), EndChimesBlockEntity.RANGE);
    }

    // Teleports 6-10 blocks aside, within the arena, and strikes whoever is nearest where it lands.
    private boolean phaseStep(ServerLevel level) {
        if (!this.canTeleport(level)) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 3.0, this.getZ(), 20, 0.5, 1.0, 0.5, 0.1);
            return false;
        }
        BlockPos centre = this.home != null ? this.home : this.blockPosition();
        BlockPos to = EndMobs.safeSpotNear(level, this.blockPosition(), 6, 10, this.random);
        if (to == null || to.distSqr(centre) > ARENA * ARENA) {
            return false;
        }
        Vec3 from = this.position();
        this.teleportTo(to.getX() + 0.5, to.getY() + 1.0, to.getZ() + 0.5);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y + 2.0, from.z, 40, 0.5, 1.5, 0.5, 0.1);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.5F, 0.6F);
        this.attackCooldown = 0;   // it strikes where it reappears
        return true;
    }

    private void wake(ServerLevel level) {
        if (!this.isDormant()) {
            return;
        }
        this.entityData.set(DATA_DORMANT, false);
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.6F);
        level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 3.0, this.getZ(), 120, 1.0, 2.0, 1.0, 0.2);
    }

    // ---- the fight

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.tickCount % 10 == 0) {
            this.updateBossBar(level);
        }
        BlockPos centre = this.home != null ? this.home : this.blockPosition();
        Player target = null;
        double best = ARENA * ARENA;
        for (Player p : level.players()) {
            double d = p.distanceToSqr(Vec3.atCenterOf(centre));
            if (!p.isSpectator() && !p.isCreative() && p.isAlive() && d < best) {
                best = d;
                target = p;
            }
        }
        if (this.isDormant()) {
            if (target != null && target.distanceToSqr(Vec3.atCenterOf(centre)) < 12.0 * 12.0) {
                this.wake(level);
            }
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.entityData.get(DATA_SLAM) > 0) {
            this.entityData.set(DATA_SLAM, this.entityData.get(DATA_SLAM) - 1);
        }
        if (this.stagger > 0) {
            this.stagger--;
            if (this.action() != ACT_NONE) {
                this.act(ACT_NONE, 0);
            }
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            return;
        }
        if (target == null) {
            // Back to the middle of its reliquary, slowly.
            this.moveToward(Vec3.atCenterOf(centre).add(0, 1.5, 0), 0.08);
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.attackCooldown = Math.max(0, this.attackCooldown - 1);

        // Pylons: a still one near the arena is a threat - go and smash it first.
        if (this.smashing == null && this.tickCount % 20 == 0) {
            this.smashing = this.findStillPylon(level, centre);
        }
        if (this.smashing != null) {
            if (!level.getBlockState(this.smashing).is(ModBlocks.END_CHIMES.get())) {
                this.smashing = null;
            } else if (this.position().distanceToSqr(Vec3.atCenterOf(this.smashing)) < 4.0 * 4.0) {
                level.destroyBlock(this.smashing, false, this);
                level.playSound(null, this.smashing, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
                this.smashing = null;
            } else {
                this.moveToward(Vec3.atCenterOf(this.smashing), 0.22);
                return;
            }
        }

        // Swap: out of its sight for three seconds, and it trades places with you.
        this.unseen = this.hasLineOfSight(target) ? 0 : this.unseen + 1;
        if (this.unseen > 60 && this.canTeleport(level)) {
            this.unseen = 0;
            Vec3 mine = this.position(), theirs = target.position();
            this.teleportTo(theirs.x, theirs.y, theirs.z);
            target.teleportTo(mine.x, mine.y, mine.z);
            level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.5F);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, mine.x, mine.y + 2.0, mine.z, 40, 0.5, 1.5, 0.5, 0.1);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, theirs.x, theirs.y + 1.0, theirs.z, 40, 0.5, 1.0, 0.5, 0.1);
        }

        // Gravity Toll: a slam whose ring lifts everyone near it.
        if (--this.slamCooldown <= 0 && this.distanceToSqr(target) < 10.0 * 10.0) {
            this.slamCooldown = 180 + this.random.nextInt(80);
            this.entityData.set(DATA_SLAM, 20);
            level.playSound(null, this.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 2.0F, 0.6F);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 200, 4.0, 0.3, 4.0, 0.3);
            for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(7.0, 3.0, 7.0))) {
                if (!p.isSpectator() && !p.isCreative()) {
                    p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40), this);
                    p.hurtServer(level, level.damageSources().mobAttack(this), 4.0F);
                }
            }
        }

        // Its attacks. Each is shown before it lands: the arm coming up, the crouch.
        float pace = this.isExposed() ? 0.75F : 1.0F;
        this.cannonCooldown--;
        this.chargeCooldown--;
        if (this.action() != ACT_NONE) {
            this.tickAction(level, target, pace);
        } else {
            double d2 = this.distanceToSqr(target);
            boolean sees = this.hasLineOfSight(target);
            if (this.cannonCooldown <= 0 && d2 > 7.0 * 7.0 && d2 < BEAM_RANGE * BEAM_RANGE && sees) {
                this.act(ACT_AIM, (int) (AIM_TICKS * pace));
                level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.0F, 0.8F);
            } else if (this.chargeCooldown <= 0 && d2 > 5.0 * 5.0 && d2 < 16.0 * 16.0 && sees) {
                this.act(ACT_CROUCH, (int) (CROUCH_TICKS * pace));
                level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
            } else {
                // Close in, hovering a hand's breadth off the floor, and strike - one fist, then the other.
                if (d2 > 3.2 * 3.2) {
                    this.moveToward(target.position().add(0.0, 0.2, 0.0), 0.25);
                } else {
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
                    if (this.attackCooldown == 0) {
                        this.attackCooldown = (int) (30 * pace);
                        this.leftNext = !this.leftNext;
                        this.act(this.leftNext ? ACT_PUNCH_LEFT : ACT_PUNCH_RIGHT, PUNCH_TICKS);
                        this.doHurtTarget(level, target);
                    }
                }
            }
        }
        // Never leaves its reliquary.
        if (this.position().distanceToSqr(Vec3.atCenterOf(centre)) > ARENA * ARENA) {
            this.moveToward(Vec3.atCenterOf(centre), 0.3);
        }
    }

    private void tickAction(ServerLevel level, Player target, float pace) {
        int t = this.actionTicks() - 1;
        this.entityData.set(DATA_ACTION_T, Math.max(0, t));
        switch (this.action()) {
            case ACT_AIM -> {
                // Stand and aim; the aim locks eight ticks before it fires - the moment to step aside.
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
                this.faceToward(target.position());
                if (t >= 8) {
                    this.aim = target.getEyePosition();
                }
                Vec3 muzzle = this.muzzle();
                level.sendParticles(t < 8 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.REVERSE_PORTAL, muzzle.x, muzzle.y, muzzle.z, 3, 0.15, 0.15, 0.15, 0.02);
                if (t <= 0) {
                    this.fireBeam(level);
                    this.act(ACT_RECOIL, RECOIL_TICKS);
                    this.cannonCooldown = (int) ((140 + this.random.nextInt(60)) * pace);
                }
            }
            case ACT_CROUCH -> {
                // Crouch, thrusters flaring; which way it goes is fixed as it springs.
                this.setDeltaMovement(this.getDeltaMovement().scale(0.4));
                this.faceToward(target.position());
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.1, this.getZ(), 6, 0.5, 0.05, 0.5, 0.15);
                if (t <= 0) {
                    Vec3 d = target.position().subtract(this.position());
                    this.aim = new Vec3(d.x, 0.0, d.z).normalize();
                    this.act(ACT_CHARGE, CHARGE_TICKS);
                    level.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.5F);
                }
            }
            case ACT_CHARGE -> {
                BlockPos centre = this.home != null ? this.home : this.blockPosition();
                double dy = Mth.clamp(target.getY() + 0.2 - this.getY(), -0.3, 0.3);
                this.setDeltaMovement(this.aim.x * 0.95, dy, this.aim.z * 0.95);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.4, 0.2, 0.4, 0.05);
                for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.6))) {
                    if (!p.isSpectator() && !p.isCreative()) {
                        p.hurtServer(level, level.damageSources().mobAttack(this), 12.0F);
                        p.knockback(2.2, -this.aim.x, -this.aim.z);
                        p.push(0.0, 0.45, 0.0);
                        p.hurtMarked = true;
                        this.endCharge(level, false);
                        return;
                    }
                }
                boolean pylon = !level.getBlockStates(this.getBoundingBox().inflate(0.4)).filter(b -> b.is(ModBlocks.END_CHIMES.get())).toList().isEmpty();
                if (this.horizontalCollision || pylon) {
                    this.endCharge(level, true);              // into a wall or a pylon: stunned
                } else if (t <= 0 || this.position().distanceToSqr(Vec3.atCenterOf(centre)) > ARENA * ARENA) {
                    this.endCharge(level, false);
                }
            }
            default -> {
                // Recoil and punches: just let them play out.
                this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
                if (t <= 0) {
                    this.act(ACT_NONE, 0);
                }
            }
        }
    }

    private void endCharge(ServerLevel level, boolean crashed) {
        this.setDeltaMovement(Vec3.ZERO);
        this.chargeCooldown = (int) ((160 + this.random.nextInt(80)) * (this.isExposed() ? 0.75F : 1.0F));
        if (crashed) {
            this.stagger = 50;
            level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.5F);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 2.5, this.getZ(), 40, 0.8, 1.0, 0.8, 0.2);
        }
        this.act(ACT_NONE, 0);
    }

    // The cannon's muzzle, roughly: out in front of its right shoulder, at chest height.
    private Vec3 muzzle() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        return this.position().add(0.0, 2.7, 0.0).add(forward.scale(1.9)).add(right.scale(0.9));
    }

    // The beam: a straight line of force from the muzzle to where it locked on, stopped by blocks; it
    // throws back and hurts whoever stands within a block of it.
    private void fireBeam(ServerLevel level) {
        Vec3 from = this.muzzle();
        Vec3 dir = this.aim.subtract(from);
        if (dir.lengthSqr() < 1.0E-4) {
            return;
        }
        dir = dir.normalize();
        Vec3 to = from.add(dir.scale(BEAM_RANGE));
        var hit = level.clip(new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        to = hit.getLocation();
        double length = to.distanceTo(from);
        for (double d = 1.0; d < length; d += 1.2) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 0.8F);
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(from, to).inflate(1.2),
                e -> e != this && !(e instanceof Hollowback))) {
            Vec3 c = e.position().add(0.0, e.getBbHeight() / 2.0, 0.0);
            double along = Mth.clamp(c.subtract(from).dot(dir), 0.0, length);
            if (c.distanceTo(from.add(dir.scale(along))) <= 1.2 && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))) {
                e.hurtServer(level, level.damageSources().mobAttack(this), 10.0F);
                e.knockback(1.6, -dir.x, -dir.z);
            }
        }
    }

    private void faceToward(Vec3 to) {
        Vec3 d = to.subtract(this.position());
        if (d.horizontalDistanceSqr() > 0.01) {
            float yaw = (float) (Math.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
        }
    }

    private @Nullable BlockPos findStillPylon(ServerLevel level, BlockPos centre) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-24, -8, -24), centre.offset(24, 16, 24))) {
            if (level.getBlockState(p).is(ModBlocks.END_CHIMES.get()) && level.getBlockEntity(p) instanceof EndChimesBlockEntity chime
                    && chime.isStill(level.getGameTime())) {
                found.add(p.immutable());
            }
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private void moveToward(Vec3 to, double speed) {
        Vec3 d = to.subtract(this.position());
        Vec3 v = d.lengthSqr() > 0.01 ? d.normalize().scale(Math.min(speed, d.length() * 0.1)) : Vec3.ZERO;
        this.setDeltaMovement(this.getDeltaMovement().scale(0.7).add(v.scale(0.3)));
        if (d.horizontalDistanceSqr() > 0.01) {
            float yaw = (float) (Math.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
            this.setYRot(yaw);
            this.yBodyRot = yaw;
        }
    }

    @Override
    public void travel(Vec3 input) {
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.91));
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
        if (this.level().isClientSide() && !this.isDormant() && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getY() + this.random.nextDouble() * 4.0,
                this.getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    // ---- the boss bar
    // Shown only while it's awake, and only to whoever is within 64 blocks. (It's tracked from 256
    // blocks off; resting in its reliquary, it shouldn't put its name on the screen of everyone near.)

    private void updateBossBar(ServerLevel level) {
        boolean awake = this.isAlive() && !this.isDormant();
        for (ServerPlayer player : level.players()) {
            boolean show = awake && player.distanceToSqr(this) < 64.0 * 64.0;
            boolean shown = this.bossEvent.getPlayers().contains(player);
            if (show && !shown) {
                this.bossEvent.addPlayer(player);
            } else if (!show && shown) {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    // ---- sounds and saving

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isDormant() ? null : SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Dormant", this.isDormant());
        output.putBoolean("Exposed", this.isExposed());
        if (this.home != null) {
            output.putLong("Home", this.home.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_DORMANT, input.getBooleanOr("Dormant", true));
        this.entityData.set(DATA_EXPOSED, input.getBooleanOr("Exposed", false));
        long home = input.getLongOr("Home", Long.MIN_VALUE);
        this.home = home == Long.MIN_VALUE ? null : BlockPos.of(home);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
