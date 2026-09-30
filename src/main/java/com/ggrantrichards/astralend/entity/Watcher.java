package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.item.StarChartItem;
import com.ggrantrichards.astralend.world.ModAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// The Watcher: a floating stone sentinel with one great eye, at the edge of sight on the Shattered
// Observatory's islands. It hangs in the air, perfectly still but for the shards circling it.
//
// It appears only when you're alone, only far away, and only for a few seconds. Come close and it
// is simply gone. Look at it through a spyglass and it vanishes too - but a Star Chart appears in
// your inventory, and you get the advancement "Something Looks Back". It never attacks and can't
// be reached. When it goes, it comes apart into starlight with a crystal chime. (MobSpawning decides where and when it appears.)
public class Watcher extends Mob {
    public static final int LIFETIME = 240;
    private static final double TOO_CLOSE = 24.0;

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }
        this.vanish(level);
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Player nearest = level.getNearestPlayer(this, 160.0);
        if (nearest == null || this.tickCount > LIFETIME) {
            this.vanish(level);
            return;
        }
        // Always facing whoever is nearest.
        Vec3 to = nearest.position().subtract(this.position());
        float yaw = (float) (Mth.atan2(to.z, to.x) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        if (this.distanceToSqr(nearest) < TOO_CLOSE * TOO_CLOSE) {
            this.vanish(level);
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isScoping() && this.isLookedAtBy(player)) {
                player.getInventory().add(StarChartItem.create(level, this.blockPosition()));
                ModAdvancements.award(player, "something_looks_back");
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
                this.vanish(level);
                return;
            }
        }
    }

    private boolean isLookedAtBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 to = this.position().add(0.0, this.getBbHeight() * 0.6, 0.0).subtract(player.getEyePosition());
        double distance = to.length();
        return distance < 160.0 && view.dot(to.normalize()) > 0.997 && player.hasLineOfSight(this);
    }

    private void vanish(ServerLevel level) {
        if (this.isRemoved()) {
            return;
        }
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.6, this.getZ(), 40, 0.4, 0.9, 0.4, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.6, this.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
        level.playSound(null, this.getX(), this.getY() + 1.6, this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.5F, 0.5F);
        this.discard();
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, net.minecraft.world.level.block.state.BlockState onState, BlockPos pos) {
    }
}
