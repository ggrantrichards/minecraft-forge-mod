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

// The Curator: the keeper of a Leviathan's skull reliquary. An enderman-like construct 4.5 blocks tall,
// of obsidian plating inlaid with purpur, with no face - one slit of violet light - hovering a little
// off the floor. One to each Leviathan; it never respawns. It's dormant until someone walks into the
// reliquary, then:
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
    public static final double ARENA = 26.0;     // its reach from home
    private final ServerBossEvent bossEvent = new ServerBossEvent(java.util.UUID.randomUUID(), this.getDisplayName(), BossEvent.BossBarColor.PURPLE,
        BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos home;
    private int attackCooldown;
    private int slamCooldown = 160;
    private int unseen;
    private int stagger;
    private @Nullable BlockPos smashing;

    public Curator(EntityType<? extends Curator> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.xpReward = 300;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 300.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
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
        // Phase Step: a projectile makes it teleport aside - unless a still pylon is near.
        if (direct instanceof Projectile && this.stagger <= 0 && this.phaseStep(level)) {
            return false;
        }
        return super.hurtServer(level, source, this.stagger > 0 ? amount * 1.5F : amount);
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

        // Close in, hovering a little off the floor, and strike.
        Vec3 aim = target.position().add(0.0, 0.5, 0.0);
        if (this.distanceToSqr(target) > 3.0 * 3.0) {
            this.moveToward(aim, 0.25);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
            if (this.attackCooldown == 0) {
                this.attackCooldown = 30;
                this.doHurtTarget(level, target);
            }
        }
        // Never leaves its reliquary.
        if (this.position().distanceToSqr(Vec3.atCenterOf(centre)) > ARENA * ARENA) {
            this.moveToward(Vec3.atCenterOf(centre), 0.3);
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
        return this.isDormant() ? null : SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Dormant", this.isDormant());
        if (this.home != null) {
            output.putLong("Home", this.home.asLong());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_DORMANT, input.getBooleanOr("Dormant", true));
        long home = input.getLongOr("Home", Long.MIN_VALUE);
        this.home = home == Long.MIN_VALUE ? null : BlockPos.of(home);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
