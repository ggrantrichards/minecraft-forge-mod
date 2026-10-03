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

        // ---- the Spire Wyrm sleeps until disturbed, and a blow wakes it
        com.ggrantrichards.astralend.entity.SpireWyrm wyrm = ModEntities.SPIRE_WYRM.get().create(end, EntitySpawnReason.COMMAND);
        if (wyrm != null) {
            wyrm.snapTo(standOn.getX() + 0.5, standOn.getY() + 30.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(wyrm);
            boolean asleep = wyrm.isAsleep();
            wyrm.hurtServer(end, end.damageSources().generic(), 1.0F);
            log.accept(String.format("mechanics: Spire Wyrm asleep at first: %s, %s after a blow", asleep, wyrm.phase()));
            if (!asleep || wyrm.isAsleep()) {
                failures.add("mechanics: a Spire Wyrm should sleep on its nest until something wakes it");
            }
            wyrm.discard();
        }
    }

    // Every spawn egg, used on the ground the way a player uses it: does the mob appear, and is it
    // still there (and, for the Umbral Stalker, visible) a couple of seconds later?
    static void checkSpawnEggs(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        net.minecraft.server.level.ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(end);
        player.snapTo(standOn.getX() + 0.5, standOn.getY(), standOn.getZ() - 4.5, 0.0F, 30.0F);
        String[] mobs = {"pollinator", "void_wanderer", "umbral_stalker", "cinder_mite", "void_ray", "hollow", "drift_whale",
            "watcher", "live_comet", "spire_wyrm"};
        StringBuilder report = new StringBuilder("spawn eggs:");
        for (int k = 0; k < mobs.length; k++) {
            String mob = mobs[k];
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("astralend", mob + "_spawn_egg"));
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("astralend", mob));
            if (item.isEmpty() || type.isEmpty()) {
                failures.add("spawn egg: no egg or no mob for " + mob);
                continue;
            }
            BlockPos ground = standOn.below().offset((k % 5) * 12 - 24, 0, (k / 5) * 12);
            end.setBlock(ground, Blocks.END_STONE.defaultBlockState(), 3);
            for (int up = 1; up <= 6; up++) {
                end.setBlock(ground.above(up), Blocks.AIR.defaultBlockState(), 3);
            }
            net.minecraft.world.item.ItemStack egg = new net.minecraft.world.item.ItemStack(item.get());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, egg);
            net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(ground).inflate(6.0);
            int before = end.getEntities(type.get(), around, e -> true).size();
            var result = egg.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), net.minecraft.core.Direction.UP, ground, false)));
            var spawned = end.getEntities(type.get(), around, e -> true);
            int made = spawned.size() - before;
            String state = "";
            if (made > 0) {
                net.minecraft.world.entity.Entity e = spawned.get(spawned.size() - 1);
                for (int i = 0; i < 40 && !e.isRemoved(); i++) {
                    e.tick();
                }
                state = e.isRemoved() ? " but gone within 2 s" : "";
                if (e instanceof UmbralStalker stalker) {
                    state += stalker.isRevealed() ? " (visible)" : " (invisible)";
                }
                e.discard();
            }
            report.append(String.format(" %s %s%s;", mob, made > 0 ? "appeared" : "NOTHING (" + result + ")", state));
            boolean fleeting = mob.equals("watcher") || mob.equals("live_comet");   // they leave by design
            if (made <= 0 || (state.contains("gone") && !fleeting)) {
                failures.add("spawn egg: the " + mob + " egg doesn't give a mob you can see" + state);
            }
        }
        log.accept(report.toString());
    }

    // An End portal built by hand: twelve frames round a 3x3 hole, eyes put in with a player's hand.
    // Facing in (as when you place them standing inside the ring) it must light; facing out it
    // mustn't (vanilla's rule) - so a portal that won't light is the frames, not the mod.
    static void checkHandBuiltPortal(ServerLevel end, List<String> failures, Consumer<String> log) {
        net.minecraft.server.level.ServerLevel overworld = end.getServer().overworld();
        net.minecraft.server.level.ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(overworld);
        StringBuilder report = new StringBuilder("hand-built End portal:");
        for (boolean inward : new boolean[] {true, false}) {
            BlockPos c = new BlockPos(inward ? 20 : 40, 200, 20);   // in the sky over spawn: nothing in the way
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -1, -3), c.offset(3, 2, 3))) {
                overworld.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
            java.util.List<BlockPos> frames = new java.util.ArrayList<>();
            for (int i = -1; i <= 1; i++) {
                place(overworld, c.offset(i, 0, -2), net.minecraft.core.Direction.SOUTH, inward, frames);
                place(overworld, c.offset(i, 0, 2), net.minecraft.core.Direction.NORTH, inward, frames);
                place(overworld, c.offset(-2, 0, i), net.minecraft.core.Direction.EAST, inward, frames);
                place(overworld, c.offset(2, 0, i), net.minecraft.core.Direction.WEST, inward, frames);
            }
            for (BlockPos f : frames) {
                net.minecraft.world.item.ItemStack eye = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_EYE);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, eye);
                eye.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(f).add(0, 0.5, 0), net.minecraft.core.Direction.UP, f, false)));
            }
            int eyes = 0;
            for (BlockPos f : frames) {
                if (overworld.getBlockState(f).getValue(net.minecraft.world.level.block.EndPortalFrameBlock.HAS_EYE)) {
                    eyes++;
                }
            }
            int portal = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 0, 1))) {
                if (overworld.getBlockState(p).is(Blocks.END_PORTAL)) {
                    portal++;
                }
            }
            report.append(String.format(" facing %s: %d/12 eyes in, %d/9 portal blocks;", inward ? "in" : "out", eyes, portal));
            if (eyes != 12 || (inward ? portal != 9 : portal != 0)) {
                failures.add("End portal: a hand-built frame facing " + (inward ? "in" : "out") + " gave " + eyes + " eyes and "
                    + portal + " portal blocks");
            }
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -1, -3), c.offset(3, 2, 3))) {
                overworld.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        log.accept(report.toString());
    }

    private static void place(ServerLevel level, BlockPos at, net.minecraft.core.Direction in, boolean inward, java.util.List<BlockPos> frames) {
        level.setBlock(at, Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(
            net.minecraft.world.level.block.EndPortalFrameBlock.FACING, inward ? in : in.getOpposite()), 3);
        frames.add(at);
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
