package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
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

// The survey's Phase 10 checks: a Lodestar sends you back to it (the End's respawn point),
// a Drift Whale can be hurt (and runs), and fire frightens an Umbral Stalker.
final class MechanicsSurvey {
    static void check(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        // ---- the Lodestar: used in the End, it holds you; die there and you're sent back to it
        BlockPos at = standOn.above(3);
        end.setBlockAndUpdate(at, ModBlocks.LODESTAR.get().defaultBlockState());
        net.minecraft.server.level.ServerPlayer holder = standIn(end);
        com.ggrantrichards.astralend.world.LodestarBindings.get(end).bind(holder, at);
        holder.snapTo(standOn.getX() + 40.5, standOn.getY() + 30, standOn.getZ() + 0.5, 0.0F, 0.0F);
        boolean sent = com.ggrantrichards.astralend.world.LodestarBindings.sendBack(holder);
        boolean beside = holder.blockPosition().distSqr(at) < 9.0;
        end.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        boolean gone = com.ggrantrichards.astralend.world.LodestarBindings.sendBack(holder);
        log.accept(String.format("mechanics: Lodestar sends you back: %s, beside it: %s (at %s); once broken: %s", sent, beside,
            holder.blockPosition().toShortString(), gone));
        if (!sent || !beside || gone) {
            failures.add("mechanics: a Lodestar should send a player back to stand beside it, and only while it stands");
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

    // The Spire Wyrm's fight, a minute of it against a stand-in player on the platform beside its
    // nest (in the open sky, so nothing is in the way): it must wake, circle, wind up and dive, lie
    // grounded, wind up and breathe - and never stray more than 26 blocks from its nest nor sink below
    // the platform.
    static void checkWyrmFight(ServerLevel end, List<String> failures, Consumer<String> log) {
        BlockPos nest = new BlockPos(0, 180, 400);
        end.getChunk(nest.getX() >> 4, nest.getZ() >> 4);
        com.ggrantrichards.astralend.entity.SpireWyrm wyrm = ModEntities.SPIRE_WYRM.get().create(end, EntitySpawnReason.COMMAND);
        if (wyrm == null) {
            failures.add("Spire Wyrm fight: couldn't create one");
            return;
        }
        wyrm.snapTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, 0.0F, 0.0F);
        end.addFreshEntity(wyrm);
        // A stand-in for the player: an armour stand where they'd be standing.
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(end, 0, 0, 0);
        stand.snapTo(nest.getX() + 6.5, nest.getY(), nest.getZ() + 0.5, 0.0F, 0.0F);
        wyrm.fightForTest(stand);
        java.util.Map<String, Integer> seen = new java.util.LinkedHashMap<>();
        double far = 0, low = 0;
        String last = "";
        try {
            for (int i = 0; i < 1200; i++) {
                stand.setHealth(stand.getMaxHealth());
                wyrm.tick();
                String phase = wyrm.phase().name();
                if (!phase.equals(last)) {
                    seen.merge(phase, 1, Integer::sum);
                    last = phase;
                }
                far = Math.max(far, Math.sqrt(Math.pow(wyrm.getX() - nest.getX() - 0.5, 2) + Math.pow(wyrm.getZ() - nest.getZ() - 0.5, 2)));
                low = Math.max(low, nest.getY() - wyrm.getY());
            }
        } catch (RuntimeException e) {
            failures.add("Spire Wyrm fight: crashed: " + e);
        }
        log.accept(String.format("Spire Wyrm fight: phases %s; furthest from the nest %.1f blocks, lowest %.1f below it", seen, far, low));
        for (String must : List.of("WAKING", "CIRCLE", "WIND_DIVE", "DIVE", "GROUNDED", "WIND_BREATH", "BREATH")) {
            if (!seen.containsKey(must)) {
                failures.add("Spire Wyrm fight: never went through " + must);
            }
        }
        if (far > 28.0 || low > 1.0) {      // (it lies on the floor, a little below where it nests)
            failures.add(String.format("Spire Wyrm fight: strayed %.1f blocks from its nest or %.1f below it", far, low));
        }
        wyrm.discard();
    }

    // Every spawn egg, used on the ground the way a player uses it: does the mob appear, and is it
    // still there (and, for the Umbral Stalker, visible) a couple of seconds later?
    static void checkSpawnEggs(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        String[] mobs = {"pollinator", "void_wanderer", "umbral_stalker", "void_ray", "drift_whale", "watcher", "spire_wyrm",
                         "hushback", "hydragrip", "mirror_wretch", "hollowback", "curator"};
        net.minecraft.server.level.ServerPlayer player = standIn(end);
        player.snapTo(standOn.getX() + 0.5, standOn.getY(), standOn.getZ() - 4.5, 0.0F, 30.0F);
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
            // Six to a row, two rows: within the 48 x 12 blocks round the stand-in that are always loaded.
            BlockPos ground = standOn.below().offset((k % 6) * 9 - 22, 0, (k / 6) * 12);
            end.setBlock(ground, Blocks.END_STONE.defaultBlockState(), 3);
            for (int up = 1; up <= 6; up++) {
                end.setBlock(ground.above(up), Blocks.AIR.defaultBlockState(), 3);
            }
            net.minecraft.world.item.ItemStack egg = new net.minecraft.world.item.ItemStack(item.get());
            net.minecraft.world.phys.AABB around = new net.minecraft.world.phys.AABB(ground).inflate(6.0);
            int before = end.getEntities(type.get(), around, e -> true).size();
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, egg);
            var result = egg.useOn(use(player, ground));
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
            boolean fleeting = mob.equals("watcher");   // it leaves by design
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
        net.minecraft.server.level.ServerPlayer player = standIn(overworld);
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
                player.snapTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 0.0F, 60.0F);   // inside the ring, as a player would stand
                eye.useOn(use(player, f));
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

    // The item in the stand-in's hand, used on the top of a block.
    private static net.minecraft.world.item.context.UseOnContext use(net.minecraft.server.level.ServerPlayer player, BlockPos on) {
        return new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(on).add(0, 0.5, 0), net.minecraft.core.Direction.UP, on, false));
    }

    // A player for the survey's hands: a real ServerPlayer, never joined to the game (no
    // connection, not in the player list), so nothing is sent anywhere.
    private static net.minecraft.server.level.ServerPlayer standIn(ServerLevel level) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("astralend-survey".getBytes()), "AstralSurvey");
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
            net.minecraft.server.level.ClientInformation.createDefault());
        return player;
    }

    private static void place(ServerLevel level, BlockPos at, net.minecraft.core.Direction in, boolean inward, java.util.List<BlockPos> frames) {
        level.setBlock(at, Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(
            net.minecraft.world.level.block.EndPortalFrameBlock.FACING, inward ? in : in.getOpposite()), 3);
        frames.add(at);
    }

    // Batch C: Upwelling turns gravity around, End Chimes still a second teleport, Voidgel bounces.
    // The distillation's systems: Memory Moss remembers when trodden or brushed, it's edible
    // (Remembering), and a Drift Whale sung to by someone takes Chorus Fruit from them alone - tamed.
    static void checkSystems(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        BlockPos moss = standOn.above(4);
        end.setBlockAndUpdate(moss, ModBlocks.MEMORY_MOSS.get().defaultBlockState());
        com.ggrantrichards.astralend.block.MemoryMossBlock.remember(end, moss, end.getBlockState(moss));
        int echo = end.getBlockState(moss).getValue(com.ggrantrichards.astralend.block.MemoryMossBlock.ECHO);
        end.setBlockAndUpdate(moss, Blocks.AIR.defaultBlockState());
        boolean edible = new net.minecraft.world.item.ItemStack(ModBlocks.MEMORY_MOSS.get()).has(net.minecraft.core.component.DataComponents.FOOD);
        log.accept("systems: Memory Moss remembers (echo " + echo + "), edible: " + edible);
        if (echo != 3 || !edible) {
            failures.add("systems: Memory Moss should start remembering when disturbed, and be edible");
        }
        DriftWhale whale = ModEntities.DRIFT_WHALE.get().create(end, EntitySpawnReason.COMMAND);
        if (whale != null) {
            whale.snapTo(standOn.getX() + 0.5, standOn.getY() + 14.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(whale);
            net.minecraft.server.level.ServerPlayer singer = standIn(end);
            boolean early = whale.tryTame(end, singer);
            whale.hearSong(end, singer, standOn);
            net.minecraft.server.level.ServerPlayer other = new net.minecraft.server.level.ServerPlayer(end.getServer(), end,
                new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("astralend-other".getBytes()), "AstralOther"),
                net.minecraft.server.level.ClientInformation.createDefault());
            boolean stranger = whale.tryTame(end, other);
            boolean tamed = whale.tryTame(end, singer) && whale.isTamed();
            log.accept(String.format("systems: Drift Whale - fed before any song tames it: %s; fed by someone else: %s; sung to and fed: %s",
                early, stranger, tamed));
            if (early || stranger || !tamed) {
                failures.add("systems: a Drift Whale should be tamed only by Chorus Fruit from whoever just sang to it");
            }
            whale.discard();
        }
    }

    static void checkContent(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        var mite = ModEntities.POLLINATOR.get().create(end, EntitySpawnReason.COMMAND);
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
