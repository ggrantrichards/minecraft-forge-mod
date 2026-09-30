package com.ggrantrichards.astralend.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Black Hole Guardian: a small, unstable singularity with a spinning accretion ring.
//
// It pulls nearby creatures and dropped items toward itself (it never breaks blocks), and after
// two or three minutes it collapses and is gone. Items it swallows are kept, not destroyed: it
// drops them all when it collapses or dies. It can only be hurt while its ring flickers (one
// second in every five), and only a kill drops a Singularity Shard. It can't be kept alive: its
// clock runs even if it is named. MobSpawning makes sure there's only ever one near a player.
public class BlackHoleGuardian extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_EXPOSED = SynchedEntityData.defineId(BlackHoleGuardian.class, EntityDataSerializers.BOOLEAN);
    public static final double PULL_RADIUS = 7.0;
    private static final int CYCLE = 100;
    private static final int EXPOSED_FROM = 80;

    private final List<ItemStack> swallowed = new ArrayList<>();
    private int lifeLeft;

    public BlackHoleGuardian(EntityType<? extends BlackHoleGuardian> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.lifeLeft = 2400 + this.random.nextInt(1200);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_EXPOSED, false);
    }

    public boolean isExposed() {
        return this.entityData.get(DATA_EXPOSED);
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
        this.setNoGravity(true);
        super.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                // Motes spiralling in.
                double a = this.random.nextDouble() * Math.PI * 2;
                double r = 1.5 + this.random.nextDouble() * 2.0;
                Vec3 p = this.position().add(Math.cos(a) * r, 0.45 + (this.random.nextDouble() - 0.5) * 0.6, Math.sin(a) * r);
                Vec3 v = this.position().add(0.0, 0.45, 0.0).subtract(p).scale(0.08);
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, v.x, v.y, v.z);
            }
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        this.entityData.set(DATA_EXPOSED, this.tickCount % CYCLE >= EXPOSED_FROM);
        this.drift(level);
        this.pull(level);
        if (--this.lifeLeft <= 0) {
            this.collapse(level);
        }
    }

    // Drifts slowly toward the nearest player, staying a little above their head.
    private void drift(ServerLevel level) {
        Player player = level.getNearestPlayer(this, 20.0);
        Vec3 motion = this.getDeltaMovement().scale(0.9);
        if (player != null && !player.isSpectator() && !player.isCreative()) {
            Vec3 want = player.position().add(0.0, 2.5, 0.0).subtract(this.position());
            if (want.lengthSqr() > 9.0) {
                motion = motion.add(want.normalize().scale(0.004));
            }
        } else {
            motion = motion.add((this.random.nextDouble() - 0.5) * 0.004, (this.random.nextDouble() - 0.5) * 0.002, (this.random.nextDouble() - 0.5) * 0.004);
        }
        this.setDeltaMovement(motion);
    }

    private void pull(ServerLevel level) {
        Vec3 centre = this.position().add(0.0, 0.45, 0.0);
        for (Entity entity : level.getEntities(this, this.getBoundingBox().inflate(PULL_RADIUS))) {
            if (entity instanceof BlackHoleGuardian || entity.isSpectator() || entity instanceof Player p && p.isCreative()) {
                continue;
            }
            Vec3 towards = centre.subtract(entity.position().add(0.0, entity.getBbHeight() / 2, 0.0));
            double distance = towards.length();
            if (distance > PULL_RADIUS || distance < 0.01) {
                continue;
            }
            if (entity instanceof ItemEntity item && distance < 1.2) {
                this.swallowed.add(item.getItem().copy());
                item.discard();
                this.playSound(SoundEvents.ITEM_PICKUP, 0.3F, 0.5F);
                continue;
            }
            double strength = 0.05 * (1.0 - distance / PULL_RADIUS);
            entity.setDeltaMovement(entity.getDeltaMovement().add(towards.normalize().scale(strength)));
            if (entity instanceof ServerPlayer player && this.tickCount % 2 == 0) {
                player.connection.send(new ClientboundSetEntityMotionPacket(player));
            }
            if (entity instanceof LivingEntity living && distance < 1.5 && this.tickCount % 20 == 0) {
                living.hurtServer(level, this.damageSources().mobAttack(this), 2.0F);
            }
        }
    }

    // Its time is up: it collapses and lets go of everything it swallowed. No shard.
    private void collapse(ServerLevel level) {
        this.releaseSwallowed(level);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.45, this.getZ(), 60, 0.4, 0.4, 0.4, 0.3);
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.45, this.getZ(), 30, 0.1, 0.1, 0.1, 0.25);
        this.playSound(SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1.5F, 0.6F);
        this.discard();
    }

    private void releaseSwallowed(ServerLevel level) {
        for (ItemStack stack : this.swallowed) {
            this.spawnAtLocation(level, stack, 0.4F);
        }
        this.swallowed.clear();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !this.isExposed()) {
            this.playSound(SoundEvents.SHULKER_BULLET_HURT, 0.6F, 0.6F);  // glances off
            return false;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.releaseSwallowed(level);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RESPAWN_ANCHOR_CHARGE;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.store("Swallowed", ItemStack.CODEC.listOf(), List.copyOf(this.swallowed));
        output.putInt("LifeLeft", this.lifeLeft);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.swallowed.clear();
        input.read("Swallowed", ItemStack.CODEC.listOf()).ifPresent(this.swallowed::addAll);
        this.lifeLeft = input.getIntOr("LifeLeft", 2400);
    }
}
