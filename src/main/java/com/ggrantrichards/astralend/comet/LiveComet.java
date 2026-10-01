package com.ggrantrichards.astralend.comet;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.world.ModAdvancements;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A live comet (Phase 7): a glowing rock with a long tail that crosses the outer End on a slow
// arc, a minute or two long. Nothing about it is made of blocks while it flies.
//
//   - Fly through its tail with an elytra and it carries you along (a speed boost) and you catch
//     Stardust Motes.
//   - Hit its head and chunks break off: Stardust, Comet Ice, now and then raw Meteoric Iron. A
//     comet only has so much to give (HITS hits).
//   - Some comets end their arc by striking an island. Only then does it touch the world: a small
//     crater (under 40 blocks, see CometImpact) with a Comet Core in it, and some loot.
//
// Comets aren't saved: if the area unloads, the comet is simply gone. CometShowers decides when
// and where they appear.
public class LiveComet extends Mob {
    public static final int HITS = 6;
    private static final int MOTE_CAP = 16;

    private Vec3 start = Vec3.ZERO;
    private Vec3 control = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;
    private int duration;
    private int age;
    private boolean landfall;
    private boolean launched;
    private int hitsLeft = HITS;
    private int motesLeft = MOTE_CAP;
    // Where the head has been lately (one point every 2 ticks, about 6 seconds): the tail.
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final Map<UUID, Integer> riding = new HashMap<>();

    public LiveComet(EntityType<? extends LiveComet> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    // Sets it on its way: a quadratic arc from start through control to end, in `duration` ticks.
    public void launch(Vec3 start, Vec3 control, Vec3 end, int duration, boolean landfall) {
        this.start = start;
        this.control = control;
        this.end = end;
        this.duration = Math.max(40, duration);
        this.landfall = landfall;
        this.launched = true;
        this.age = 0;
        this.snapTo(start.x, start.y, start.z, 0.0F, 0.0F);
        this.face(this.direction(0.0));
    }

    public boolean isLandfall() {
        return this.landfall;
    }

    public int hitsLeft() {
        return this.hitsLeft;
    }

    private Vec3 at(double t) {
        double u = 1.0 - t;
        return this.start.scale(u * u).add(this.control.scale(2 * u * t)).add(this.end.scale(t * t));
    }

    private Vec3 direction(double t) {
        Vec3 d = this.control.subtract(this.start).scale(2 * (1 - t)).add(this.end.subtract(this.control).scale(2 * t));
        return d.lengthSqr() < 1.0E-6 ? new Vec3(0, 0, 1) : d.normalize();
    }

    // Which way the head is going (also known on the client, from its rotation).
    public Vec3 heading() {
        return Vec3.directionFromRotation(this.getXRot(), this.getYRot());
    }

    private void face(Vec3 d) {
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel level && this.launched) {
            this.age++;
            double t = Math.min(1.0, this.age / (double) this.duration);
            Vec3 next = this.at(t);
            this.setDeltaMovement(next.subtract(this.position()));
            this.face(this.direction(t));
            if (this.age % 2 == 0) {
                this.trail.addFirst(this.position());
                while (this.trail.size() > 60) {
                    this.trail.removeLast();
                }
            }
            this.tailRiders(level);
            if (this.age >= this.duration) {
                if (this.landfall) {
                    CometImpact.strike(level, BlockPos.containing(this.end), this.random, this);
                }
                this.discard();
                return;
            }
        } else if (!this.level().isClientSide() && !this.launched) {
            this.setDeltaMovement(Vec3.ZERO);   // a comet that was never launched (summoned) just hangs there
        }
        super.aiStep();
        if (this.level().isClientSide()) {
            this.tailParticles();
        }
    }

    // The long tail: sparks and motes shed behind the head, drifting back along its path.
    private void tailParticles() {
        Vec3 back = this.heading().scale(-1.0);
        Vec3 head = this.position().add(0, this.getBbHeight() * 0.5, 0);
        RandomSource r = this.random;
        for (int i = 0; i < 6; i++) {
            Vec3 p = head.add(back.scale(r.nextDouble() * 2.5)).add((r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 1.2,
                (r.nextDouble() - 0.5) * 1.2);
            Vec3 v = back.scale(0.04 + r.nextDouble() * 0.08);
            this.level().addParticle(i % 3 == 0 ? ParticleTypes.SNOWFLAKE : ParticleTypes.END_ROD, p.x, p.y, p.z, v.x, v.y, v.z);
        }
        if (r.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, head.x, head.y, head.z, (r.nextDouble() - 0.5) * 0.2,
                (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2);
        }
    }

    // Anyone gliding through the tail is swept along with the comet and catches Stardust Motes.
    private void tailRiders(ServerLevel level) {
        if (this.trail.isEmpty()) {
            return;
        }
        Vec3 heading = this.heading();
        for (ServerPlayer player : level.players()) {
            if (!player.isFallFlying() || player.distanceToSqr(this) > 64 * 64) {
                continue;
            }
            boolean inTail = false;
            Vec3 at = player.position();
            for (Vec3 p : this.trail) {
                if (p.distanceToSqr(at) < 16.0) {
                    inTail = true;
                    break;
                }
            }
            if (!inTail) {
                continue;
            }
            Vec3 v = player.getDeltaMovement().add(heading.scale(0.1));
            if (v.length() > 2.4) {
                v = v.normalize().scale(2.4);
            }
            player.setDeltaMovement(v);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            int time = this.riding.merge(player.getUUID(), 1, Integer::sum);
            if (time % 30 == 1 && this.motesLeft > 0) {
                this.motesLeft--;
                ItemStack mote = new ItemStack(ModItems.STARDUST_MOTE.get());
                if (!player.getInventory().add(mote)) {
                    net.minecraft.world.level.block.Block.popResource(level, player.blockPosition(), mote);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
                ModAdvancements.award(player, "catch_a_falling_star");
            }
        }
    }

    // A hit knocks a chunk off the head.
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.getEntity() instanceof Player && this.hitsLeft > 0) {
            this.hitsLeft--;
            this.knockChunk(level);
        }
        return false;
    }

    public void knockChunk(ServerLevel level) {
        RandomSource r = this.random;
        int roll = r.nextInt(20);
        ItemStack chunk = roll < 3 ? new ItemStack(ModItems.RAW_METEORIC_IRON.get())
            : roll < 11 ? new ItemStack(ModBlocks.STARDUST_BLOCK.get(), 1 + r.nextInt(2))
            : new ItemStack(ModBlocks.COMET_ICE.get(), 1 + r.nextInt(2));
        Vec3 c = this.position().add(0, this.getBbHeight() * 0.5, 0);
        ItemEntity item = new ItemEntity(level, c.x, c.y, c.z, chunk);
        item.setDeltaMovement((r.nextDouble() - 0.5) * 0.4, 0.2 + r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.4);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.NEUTRAL, 1.5F, 0.7F);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 20, 0.4, 0.4, 0.4, 0.15);
        if (this.hitsLeft == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 1.5F, 0.5F);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 192 * 192;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 320 * 320;
    }

    @Override
    protected @Nullable net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F;
    }
}
