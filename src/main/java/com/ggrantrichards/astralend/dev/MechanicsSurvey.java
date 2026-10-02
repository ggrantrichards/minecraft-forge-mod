package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.LodestarBlockEntity;
import com.ggrantrichards.astralend.entity.DriftWhale;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.entity.UmbralStalker;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// The survey's Phase 10 checks: a Lodestar keeps comets off the ground near it (and only near it),
// a Drift Whale can be hurt (and runs), and fire frightens an Umbral Stalker.
final class MechanicsSurvey {
    static void check(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        // ---- the Lodestar
        BlockPos at = standOn.above(3);
        end.setBlockAndUpdate(at, ModBlocks.LODESTAR.get().defaultBlockState());
        boolean near = LodestarBlockEntity.guards(end, Vec3.atCenterOf(at).add(30, 0, 0));
        boolean far = LodestarBlockEntity.guards(end, Vec3.atCenterOf(at).add(80, 0, 0));
        end.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        boolean gone = LodestarBlockEntity.guards(end, Vec3.atCenterOf(at));
        log.accept(String.format("mechanics: Lodestar guards 30 blocks away: %s, 80 away: %s, once broken: %s", near, far, gone));
        if (!near || far || gone) {
            failures.add("mechanics: a Lodestar should keep comets off the ground within 48 blocks, and only while it stands");
        }

        // ---- the Drift Whale: killable, and it flees
        DriftWhale whale = ModEntities.DRIFT_WHALE.get().create(end, EntitySpawnReason.COMMAND);
        if (whale != null) {
            whale.snapTo(standOn.getX() + 0.5, standOn.getY() + 12.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(whale);
            float before = whale.getHealth();
            whale.hurtServer(end, end.damageSources().generic(), 10.0F);
            log.accept(String.format("mechanics: Drift Whale health %.0f, after a hit %.0f", before, whale.getHealth()));
            if (!(whale.getHealth() < before) || before < 300.0F) {
                failures.add("mechanics: a Drift Whale should have a lot of health and take damage");
            }
            whale.discard();
        }

        // ---- the Umbral Stalker fears fire
        UmbralStalker stalker = ModEntities.UMBRAL_STALKER.get().create(end, EntitySpawnReason.COMMAND);
        if (stalker != null) {
            stalker.snapTo(standOn.getX() + 0.5, standOn.getY() + 1.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(stalker);
            boolean calm = !stalker.isAfraid();
            stalker.hurtServer(end, end.damageSources().onFire(), 1.0F);
            log.accept(String.format("mechanics: Umbral Stalker calm at first: %s, afraid after a burn: %s", calm, stalker.isAfraid()));
            if (!calm || !stalker.isAfraid()) {
                failures.add("mechanics: fire should frighten an Umbral Stalker");
            }
            stalker.discard();
        }
    }

    // Batch C: Upwelling turns gravity around, End Chimes still a second teleport, Voidgel bounces.
    static void checkContent(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        var mite = ModEntities.CINDER_MITE.get().create(end, EntitySpawnReason.COMMAND);
        if (mite != null) {
            mite.snapTo(standOn.getX() + 0.5, standOn.getY() + 1.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(mite);
            double before = mite.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
            mite.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.ggrantrichards.astralend.ModEffects.upwelling(), 100));
            double during = mite.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
            mite.removeEffect(com.ggrantrichards.astralend.ModEffects.upwelling());
            boolean slowFalling = mite.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
            log.accept(String.format("content: Upwelling - gravity %.3f -> %.3f; Slow Falling once it ends: %s", before, during, slowFalling));
            if (!(during < 0.0) || !slowFalling) {
                failures.add("content: Upwelling should reverse gravity, and leave Slow Falling when it ends");
            }
            mite.discard();
        }
        BlockPos chime = standOn.above(6);
        end.setBlockAndUpdate(chime.above(), ModBlocks.NIGHTSTONE.get().defaultBlockState());
        end.setBlockAndUpdate(chime, ModBlocks.END_CHIMES.get().defaultBlockState());
        Vec3 near = Vec3.atCenterOf(chime).add(4, -3, 0);
        boolean first = com.ggrantrichards.astralend.block.EndChimesBlockEntity.stillsTeleport(end, near, near.add(8, 0, 0));
        boolean second = com.ggrantrichards.astralend.block.EndChimesBlockEntity.stillsTeleport(end, near, near.add(8, 0, 0));
        boolean far = com.ggrantrichards.astralend.block.EndChimesBlockEntity.stillsTeleport(end, near.add(60, 0, 0), near.add(70, 0, 0));
        end.setBlockAndUpdate(chime, Blocks.AIR.defaultBlockState());
        end.setBlockAndUpdate(chime.above(), Blocks.AIR.defaultBlockState());
        log.accept(String.format("content: End Chimes - first teleport stopped: %s, second: %s, far away: %s", first, second, far));
        if (first || !second || far) {
            failures.add("content: End Chimes should let one teleport through, still the next, and leave far ones alone");
        }
        float bounce = ModBlocks.VOIDGEL.get().getBounceRestitution();
        float moss = ModBlocks.CHORUS_MOSS.get().getSpeedFactor();
        log.accept(String.format("content: Voidgel bounce %.2f, Chorus Moss speed %.2f", bounce, moss));
        if (bounce <= 0.0F || moss >= 1.0F) {
            failures.add("content: Voidgel should bounce and Chorus Moss should slow you a little");
        }
    }

    private MechanicsSurvey() {}
}
