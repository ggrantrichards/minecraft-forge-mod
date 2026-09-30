package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.ModAdvancements;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Drift Whale: a huge, translucent whale that swims slowly through the void, the last small
// descendant of the Star Leviathans.
//
// It follows long, lazy paths and never touches ground. When a player comes close it turns toward
// them, slows down and hums. Nothing hurts it. Its reward is an advancement chain, which can't be
// farmed: see one (Something Big Out There), be near while it hums (Whale Song), follow one for a
// minute while flying (Tagalong). Very rarely it sheds a glowing Whalelight Scale as it hums.
public class DriftWhale extends Mob {
    private static final EntityDataAccessor<Boolean> DATA_HUMMING = SynchedEntityData.defineId(DriftWhale.class, EntityDataSerializers.BOOLEAN);
    private static final double CRUISE_SPEED = 0.12;
    private static final double NEAR_PLAYER = 24.0;

    private @Nullable Vec3 target;
    private int humCooldown = 100;
    private int humTicks;
    private final Map<UUID, Integer> followers = new HashMap<>();

    public DriftWhale(EntityType<? extends DriftWhale> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 200.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUMMING, false);
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
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // Hits pass through it.
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Player near = level.getNearestPlayer(this, NEAR_PLAYER);
        if (near != null && near.isSpectator()) {
            near = null;
        }
        Vec3 velocity;
        if (near != null) {
            // Turn toward the player and slow down.
            Vec3 toPlayer = near.getEyePosition().subtract(this.position().add(0.0, 1.0, 0.0));
            velocity = toPlayer.lengthSqr() > 100.0 ? toPlayer.normalize().scale(0.03) : Vec3.ZERO;
            this.faceDirection(toPlayer, 2.0F);
            this.hum(level);
        } else {
            if (this.target == null || this.position().distanceToSqr(this.target) < 36.0 || this.horizontalCollision || this.random.nextInt(1200) == 0) {
                this.target = this.pickTarget(level);
            }
            Vec3 toTarget = this.target != null ? this.target.subtract(this.position()) : Vec3.ZERO;
            velocity = toTarget.lengthSqr() > 1.0 ? toTarget.normalize().scale(CRUISE_SPEED) : Vec3.ZERO;
            this.faceDirection(velocity, 1.0F);
        }
        this.setDeltaMovement(this.getDeltaMovement().scale(0.9).add(velocity.scale(0.1)));
        if (this.humTicks > 0 && --this.humTicks == 0) {
            this.entityData.set(DATA_HUMMING, false);
        }
        if (this.tickCount % 20 == 0) {
            this.checkAdvancements(level);
        }
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
        if (this.random.nextInt(25) == 0) {
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
    public void checkDespawn() {
        if (!this.isPersistenceRequired() && this.level().getNearestPlayer(this, 192.0) == null) {
            this.discard();
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return true;
    }
}
