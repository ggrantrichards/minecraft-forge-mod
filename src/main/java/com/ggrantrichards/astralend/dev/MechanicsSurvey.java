package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.entity.DriftWhale;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.entity.UmbralStalker;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
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
        holder.moveTo(standOn.getX() + 40.5, standOn.getY() + 30, standOn.getZ() + 0.5, 0.0F, 0.0F);
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
        DriftWhale whale = ModEntities.DRIFT_WHALE.get().create(end);
        if (whale != null) {
            whale.moveTo(standOn.getX() + 0.5, standOn.getY() + 12.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(whale);
            float before = whale.getHealth();
            whale.hurt(end.damageSources().generic(), 10.0F);
            log.accept(String.format("mechanics: Drift Whale health %.0f, after a hit %.0f", before, whale.getHealth()));
            if (!(whale.getHealth() < before) || before < 300.0F) {
                failures.add("mechanics: a Drift Whale should have a lot of health and take damage");
            }
            whale.discard();
        }

        // ---- the Umbral Stalker fears fire
        UmbralStalker stalker = ModEntities.UMBRAL_STALKER.get().create(end);
        if (stalker != null) {
            stalker.moveTo(standOn.getX() + 0.5, standOn.getY() + 1.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(stalker);
            boolean calm = !stalker.isAfraid();
            stalker.hurt(end.damageSources().onFire(), 1.0F);
            log.accept(String.format("mechanics: Umbral Stalker calm at first: %s, afraid after a burn: %s", calm, stalker.isAfraid()));
            if (!calm || !stalker.isAfraid()) {
                failures.add("mechanics: fire should frighten an Umbral Stalker");
            }
            stalker.discard();
        }

        // ---- the Spire Wyrm sleeps until disturbed, and a blow wakes it
        com.ggrantrichards.astralend.entity.SpireWyrm wyrm = ModEntities.SPIRE_WYRM.get().create(end);
        if (wyrm != null) {
            wyrm.moveTo(standOn.getX() + 0.5, standOn.getY() + 30.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(wyrm);
            boolean asleep = wyrm.isAsleep();
            wyrm.hurt(end.damageSources().generic(), 1.0F);
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
        com.ggrantrichards.astralend.entity.SpireWyrm wyrm = ModEntities.SPIRE_WYRM.get().create(end);
        if (wyrm == null) {
            failures.add("Spire Wyrm fight: couldn't create one");
            return;
        }
        wyrm.moveTo(nest.getX() + 0.5, nest.getY(), nest.getZ() + 0.5, 0.0F, 0.0F);
        end.addFreshEntity(wyrm);
        // A stand-in for the player: an armour stand where they'd be standing.
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(end, 0, 0, 0);
        stand.moveTo(nest.getX() + 6.5, nest.getY(), nest.getZ() + 0.5, 0.0F, 0.0F);
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
        String[] mobs = {"pollinator", "umbral_stalker", "drift_whale", "watcher", "spire_wyrm",
                         "hushback", "hydragrip", "mirror_wretch", "hollowback", "curator", "shardback"};
        net.minecraft.server.level.ServerPlayer player = standIn(end);
        player.moveTo(standOn.getX() + 0.5, standOn.getY(), standOn.getZ() - 4.5, 0.0F, 30.0F);
        StringBuilder report = new StringBuilder("spawn eggs:");
        for (int k = 0; k < mobs.length; k++) {
            String mob = mobs[k];
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("astralend", mob + "_spawn_egg"));
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("astralend", mob));
            if (item.isEmpty() || type.isEmpty()) {
                failures.add("spawn egg: no egg or no mob for " + mob);
                continue;
            }
            // Seven to a row, two rows: within the 48 x 12 blocks round the stand-in that are always loaded.
            BlockPos ground = standOn.below().offset((k % 7) * 7 - 21, 0, (k / 7) * 12);
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
                player.moveTo(c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 0.0F, 60.0F);   // inside the ring, as a player would stand
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
        DriftWhale whale = ModEntities.DRIFT_WHALE.get().create(end);
        if (whale != null) {
            whale.moveTo(standOn.getX() + 0.5, standOn.getY() + 14.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
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
            // No two alike: a pod's whales differ in size and pace, and they play.
            DriftWhale calf = ModEntities.DRIFT_WHALE.get().create(end);
            if (calf != null) {
                calf.moveTo(standOn.getX() + 12.5, standOn.getY() + 14.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
                end.addFreshEntity(calf);
                whale.setSize(1.15F);
                calf.setSize(0.6F);
                calf.playForTest(end);
                int play = calf.play();
                net.minecraft.world.phys.Vec3 from = calf.position();
                for (int i = 0; i < 40; i++) {
                    calf.tick();
                }
                double moved = calf.position().distanceTo(from);
                log.accept(String.format("systems: Drift Whales - sizes %.2f and %.2f (calf: %s), tempos %.2f and %.2f; the calf played (%d) and moved %.1f blocks",
                    whale.getScale(), calf.getScale(), calf.isCalf(), whale.tempo(), calf.tempo(), play, moved));
                if (Math.abs(whale.getScale() - calf.getScale()) < 0.3 || !calf.isCalf() || whale.isCalf() || whale.tempo() == calf.tempo()
                        || play == DriftWhale.PLAY_NONE || moved < 0.5) {
                    failures.add("systems: Drift Whales should each have their own size and pace, and play");
                }
                calf.discard();
            }
            whale.discard();
        }
        // The Hollow Thorn carves a pumpkin into a Hollow Lantern, and carves the next face.
        BlockPos pumpkin = standOn.above(2).east(6);
        end.setBlockAndUpdate(pumpkin, Blocks.PUMPKIN.defaultBlockState());
        net.minecraft.server.level.ServerPlayer carver = standIn(end);
        carver.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.ggrantrichards.astralend.ModItems.HOLLOW_THORN.get()));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pumpkin), net.minecraft.core.Direction.NORTH, pumpkin, false);
        var use = new net.minecraft.world.item.context.UseOnContext(carver, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        com.ggrantrichards.astralend.ModItems.HOLLOW_THORN.get().useOn(use);
        net.minecraft.world.level.block.state.BlockState carved = end.getBlockState(pumpkin);
        com.ggrantrichards.astralend.ModItems.HOLLOW_THORN.get().useOn(use);
        net.minecraft.world.level.block.state.BlockState again = end.getBlockState(pumpkin);
        boolean lantern = carved.is(ModBlocks.HOLLOW_LANTERN.get()) && again.is(ModBlocks.HOLLOW_LANTERN.get())
            && again.getValue(com.ggrantrichards.astralend.block.HollowLanternBlock.FACE) == 1;
        log.accept("systems: a Hollow Thorn on a pumpkin carves " + carved.getBlock().getName().getString()
            + (lantern ? ", and again the next face" : " (and again: " + again + ")"));
        if (!lantern) {
            failures.add("systems: a Hollow Thorn should carve a pumpkin into a Hollow Lantern, then its next face");
        }
        end.setBlockAndUpdate(pumpkin, Blocks.AIR.defaultBlockState());
    }

    // Lets the next blow land at once (26.3 keeps the hurt cooldown private; Forge runs on Mojang's names).
    private static void clearHurtCooldown(net.minecraft.world.entity.Entity entity) {
        for (Class<?> c = entity.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("invulnerableTime");
                f.setAccessible(true);
                f.setInt(entity, 0);
                return;
            } catch (ReflectiveOperationException | RuntimeException e) {
                // not declared here: look further up
            }
        }
        for (int i = 0; i < 11; i++) {      // no such field: let the cooldown run out (a blow lands once it's 10 or less)
            entity.tick();
        }
    }

    // A column with nothing in it from the sky to the void, near `from` (the outer End has plenty).
    private static BlockPos voidColumn(ServerLevel end, BlockPos from) {
        for (int r = 48; r <= 320; r += 24) {
            for (int k = 0; k < 12; k++) {
                double a = k * Math.PI / 6.0;
                int x = from.getX() + (int) Math.round(Math.cos(a) * r), z = from.getZ() + (int) Math.round(Math.sin(a) * r);
                if (end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) <= end.getMinBuildHeight() + 1) {
                    return new BlockPos(x, from.getY(), z);
                }
            }
        }
        return from.offset(0, 0, 400);
    }

    // Loot: what 26.3's own loot tables look like (printed from the game's data, to settle the format), and
    // the mod's tables doing what they say - counts, the cooked-if-on-fire, the double slab - by killing real
    // mobs and breaking a real block. A key the game doesn't know is ignored without a word, so only rolling
    // the tables shows it.
    static void checkLoot(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        for (String vanilla : new String[] {"loot_table/entities/sheep.json", "loot_table/chests/end_city_treasure.json",
                                            "loot_table/blocks/stone_slab.json"}) {
            var res = end.getServer().getResourceManager().getResource(net.minecraft.resources.ResourceLocation.withDefaultNamespace(vanilla));
            if (res.isPresent()) {
                try (var reader = res.get().openAsReader()) {
                    String text = reader.lines().map(String::trim).collect(java.util.stream.Collectors.joining(" "));
                    log.accept("loot format: vanilla " + vanilla + ": " + text.substring(0, Math.min(1400, text.length())));
                } catch (java.io.IOException e) {
                    log.accept("loot format: couldn't read " + vanilla + ": " + e);
                }
            } else {
                log.accept("loot format: no " + vanilla);
            }
        }
        BlockPos at = standOn.offset(-90, 0, 0);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                end.setBlock(at.offset(dx, -1, dz), Blocks.END_STONE.defaultBlockState(), 3);
                for (int up = 0; up <= 5; up++) {
                    end.setBlock(at.offset(dx, up, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        net.minecraft.server.level.ServerPlayer player = standIn(end);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 4.5, 0.0F, 0.0F);
        // Hushback: 1-2 Raw Flanks (the count), cooked if it dies burning (a condition).
        int flanks = 0, most = 0;
        for (int i = 0; i < 8; i++) {
            int n = killFor(end, ModEntities.HUSHBACK.get(), at, player, false, com.ggrantrichards.astralend.ModItems.HUSHBACK_FLANK.get());
            flanks += n;
            most = Math.max(most, n);
        }
        int cooked = killFor(end, ModEntities.HUSHBACK.get(), at, player, true, com.ggrantrichards.astralend.ModItems.COOKED_HUSHBACK_FLANK.get());
        log.accept(String.format("loot: Hushback - %d flanks from 8 (most from one: %d); burning, cooked flanks: %d", flanks, most, cooked));
        if (most < 2 || cooked < 1) {
            failures.add("loot: a Hushback should drop 1-2 flanks, cooked when it dies burning (most " + most + ", cooked " + cooked + ")");
        }
        // Shardback: 1-2 Glimmer Clusters.
        int clusters = 0, mostClusters = 0;
        for (int i = 0; i < 8; i++) {
            int n = killFor(end, ModEntities.SHARDBACK.get(), at, player, false, ModBlocks.GLIMMER_CLUSTER.get().asItem());
            clusters += n;
            mostClusters = Math.max(mostClusters, n);
        }
        log.accept(String.format("loot: Shardback - %d Glimmer Clusters from 8 (most from one: %d)", clusters, mostClusters));
        if (mostClusters < 2 || clusters < 8) {
            failures.add("loot: a Shardback should drop 1-2 Glimmer Clusters (most " + mostClusters + ", total " + clusters + ")");
        }
        // The Curator: exactly two Starforge Templates, and its Core.
        int templates = killFor(end, ModEntities.CURATOR.get(), at, player, false, com.ggrantrichards.astralend.ModItems.STARFORGE_TEMPLATE.get());
        log.accept("loot: Curator - Starforge Templates: " + templates);
        if (templates != 2) {
            failures.add("loot: the Curator should drop two Starforge Templates, dropped " + templates);
        }
        // A double slab drops two.
        var slab = ModBlocks.NIGHTSTONE_SLAB.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        int slabs = 0;
        for (var stack : net.minecraft.world.level.block.Block.getDrops(slab, end, at, null)) {
            slabs += stack.getCount();
        }
        log.accept("loot: a double Nightstone slab drops " + slabs);
        if (slabs != 2) {
            failures.add("loot: a double slab should drop 2 slabs, dropped " + slabs);
        }
    }

    // Kills a fresh mob of `type` (with the stand-in's blow, burning if asked) and counts the `item` it drops.
    private static int killFor(ServerLevel end, net.minecraft.world.entity.EntityType<?> type, BlockPos at,
                               net.minecraft.server.level.ServerPlayer player, boolean burning, net.minecraft.world.item.Item item) {
        var mob = (net.minecraft.world.entity.LivingEntity) type.create(end);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        end.addFreshEntity(mob);
        if (burning) {
            mob.setRemainingFireTicks(200);
        }
        for (int hit = 0; hit < 3 && mob.isAlive(); hit++) {      // (the first may only wake a dormant Curator)
            mob.setHealth(1.0F);
            clearHurtCooldown(mob);
            mob.hurt(end.damageSources().playerAttack(player), 50.0F);
        }
        int n = 0;
        for (var drop : end.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mob.getBoundingBox().inflate(6.0))) {
            if (drop.getItem().is(item)) {
                n += drop.getItem().getCount();
            }
            drop.discard();
        }
        mob.discard();
        return n;
    }

    // The web of life: every thread between the End's creatures, each made to happen and checked.
    // (The Sunstorm's are in WorldgenSurvey.testEclipse, the Leviathan circle in testLeviathan.)
    static void checkWebOfLife(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        BlockPos at = standOn.offset(90, 0, 0);
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                end.setBlock(at.offset(dx, -1, dz), Blocks.END_STONE.defaultBlockState(), 3);
                for (int up = 0; up <= 6; up++) {
                    end.setBlock(at.offset(dx, up, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        java.util.List<net.minecraft.world.entity.Entity> made = new java.util.ArrayList<>();
        java.util.function.BiFunction<net.minecraft.world.entity.EntityType<?>, Vec3, net.minecraft.world.entity.Entity> put = (type, pos) -> {
            var e = type.create(end);
            e.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
            end.addFreshEntity(e);
            made.add(e);
            return e;
        };
        Vec3 o = Vec3.atBottomCenterOf(at);
        java.util.function.BiConsumer<String, Boolean> check = (what, ok) -> {
            log.accept("web of life: " + what + ": " + ok);
            if (!ok) {
                failures.add("web of life: " + what);
            }
        };
        try {
            // Hushbacks: a lookout sees an endermite and chimes; the herd shies away.
            var lookout = (com.ggrantrichards.astralend.entity.Hushback) put.apply(ModEntities.HUSHBACK.get(), o);
            var mate = (com.ggrantrichards.astralend.entity.Hushback) put.apply(ModEntities.HUSHBACK.get(), o.add(2, 0, 0));
            var mite = put.apply(net.minecraft.world.entity.EntityType.ENDERMITE, o.add(0, 0, 6));
            lookout.standLookout(200);
            var threat = lookout.threatNear(end);
            if (threat != null) {
                lookout.raiseAlarm(end, threat.position());
            }
            check.accept("a Hushback lookout spots an endermite and the herd shies away", lookout.isLookout() == false && threat == mite && mate.isAlarmed());
            // Hydragrip: goes for the endermite pestering the herd, and lounges on a Hushback's back.
            var grip = (com.ggrantrichards.astralend.entity.Hydragrip) put.apply(ModEntities.HYDRAGRIP.get(), o.add(-8, 0, 0));
            boolean guards = grip.pesteringMite() == mite;
            for (int i = 0; i < 100 && grip.getTarget() == null; i++) {
                grip.tick();
            }
            check.accept("a Hydragrip goes for the endermite pestering the herd", guards && grip.getTarget() == mite);
            mite.discard();
            grip.setTarget(null);
            grip.moveTo(mate.getX(), mate.getY(), mate.getZ() + 1.0, 0.0F, 0.0F);
            grip.lounge(mate);
            check.accept("a Hydragrip lounges on a Hushback's back", grip.isPassenger() && grip.getVehicle() == mate);
            grip.stopRiding();
            // Hydragrip broods play tag; an axolotl joins in.
            var kidA = (com.ggrantrichards.astralend.entity.Hydragrip) put.apply(ModEntities.HYDRAGRIP.get(), o.add(-4, 0, 6));
            var kidB = (com.ggrantrichards.astralend.entity.Hydragrip) put.apply(ModEntities.HYDRAGRIP.get(), o.add(-2, 0, 6));
            kidA.setAge(-24000);
            kidB.setAge(-24000);
            kidA.startPlay(kidB, true);
            boolean paired = kidB.isPlaying() && kidB.playMate() == kidA && !kidB.isIt();
            kidA.tag();
            check.accept("Hydragrip young play tag (caught, the other is it)", paired && !kidA.isIt() && kidB.isIt());
            var axolotl = (net.minecraft.world.entity.LivingEntity) put.apply(net.minecraft.world.entity.EntityType.AXOLOTL, o.add(-6, 0, 8));
            grip.startPlay(axolotl, false);
            for (int i = 0; i < 20; i++) {
                grip.tick();
            }
            check.accept("a Hydragrip plays with an axolotl", grip.isPlaying() && grip.playMate() == axolotl);
            // Hollowback: someone holding Breathcomb makes it falter.
            var player = standIn(end);
            player.moveTo(o.x + 4, o.y, o.z - 8, 0.0F, 0.0F);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(ModBlocks.BREATHCOMB.get()));
            var hollow = (com.ggrantrichards.astralend.entity.Hollowback) put.apply(ModEntities.HOLLOWBACK.get(), o.add(4, 0, -12));
            hollow.setTarget(player);
            for (int i = 0; i < 6 && !hollow.isHesitating(); i++) {
                hollow.tick();
            }
            check.accept("a Hollowback falters before someone holding Breathcomb", hollow.isHesitating());
            hollow.discard();
            // Umbral Stalker: hunts an EnderMan when no player is about; keeps clear of Pollinators and a woken Shardback.
            var stalker = (com.ggrantrichards.astralend.entity.UmbralStalker) put.apply(ModEntities.UMBRAL_STALKER.get(), o.add(10, 0, 10));
            var enderman = put.apply(net.minecraft.world.entity.EntityType.ENDERMAN, o.add(10, 0, 2));
            for (int i = 0; i < 100 && stalker.getTarget() == null; i++) {
                stalker.tick();
            }
            check.accept("an Umbral Stalker hunts an EnderMan", stalker.getTarget() == enderman);
            stalker.setTarget(null);
            var shard = (com.ggrantrichards.astralend.entity.Shardback) put.apply(ModEntities.SHARDBACK.get(), o.add(13, 0, 13));
            boolean ignoresHidden = stalker.shunned() == null;
            shard.hurt(end.damageSources().generic(), 1.0F);     // woken
            check.accept("an Umbral Stalker keeps clear of a woken Shardback (not a hidden one)", ignoresHidden && stalker.shunned() == shard);
            shard.discard();
            var glow = put.apply(ModEntities.POLLINATOR.get(), o.add(12, 1, 11));
            check.accept("an Umbral Stalker keeps clear of a Pollinator's light", stalker.shunned() == glow);
            glow.discard();
            stalker.discard();
            // A wolf gives a hidden Stalker away (a fresh one, out of reach of the EnderMan).
            var lurker = (com.ggrantrichards.astralend.entity.UmbralStalker) put.apply(ModEntities.UMBRAL_STALKER.get(), o.add(-14, 0, 14));
            lurker.tick();
            boolean hidden = !lurker.isRevealed();
            var wolf = (net.minecraft.world.entity.Mob) put.apply(net.minecraft.world.entity.EntityType.WOLF, o.add(-14, 0, 6));
            var sensed = com.ggrantrichards.astralend.world.WebOfLife.wolfSenses(end, wolf, true);
            lurker.tick();
            check.accept("a wolf growls at a hidden Umbral Stalker and gives it away", hidden && sensed == lurker && lurker.isRevealed());
            lurker.discard();
            wolf.discard();
            // Shardback: an EnderMan within six blocks of its geode sets it off.
            enderman.moveTo(o.x + 10, o.y, o.z + 2, 0.0F, 0.0F);       // (if the Stalker's blow sent it blinking off)
            var ambusher = (com.ggrantrichards.astralend.entity.Shardback) put.apply(ModEntities.SHARDBACK.get(), o.add(10, 0, -2));
            for (int i = 0; i < 100 && ambusher.getTarget() == null; i++) {
                ambusher.tick();
            }
            check.accept("a Shardback ambushes an EnderMan that comes close", ambusher.getTarget() == enderman && !ambusher.isHidden());
            ambusher.discard();
            // Endermen won't meet the Watcher's eye.
            var watcher = put.apply(ModEntities.WATCHER.get(), o.add(10, 3, -14));
            enderman.moveTo(o.x + 10, o.y, o.z - 4, 180.0F, 0.0F);
            boolean turned = com.ggrantrichards.astralend.world.WebOfLife.shunWatcher(end, (net.minecraft.world.entity.monster.EnderMan) enderman);
            Vec3 facing = enderman.getLookAngle();
            check.accept("an EnderMan turns its back on the Watcher", turned && facing.z > 0.5);
            watcher.discard();
            // Mirror Wretch: eats endermites; hums at whales, which answer and then keep away, silent.
            var wretch = (com.ggrantrichards.astralend.entity.MirrorWretch) put.apply(ModEntities.MIRROR_WRETCH.get(), o.add(-12, 0, -12));
            var meal = put.apply(net.minecraft.world.entity.EntityType.ENDERMITE, o.add(-10, 0, -12));
            for (int i = 0; i < 150 && wretch.getTarget() == null; i++) {
                wretch.tick();
            }
            check.accept("a Mirror Wretch goes for an endermite", wretch.getTarget() == meal);
            meal.discard();
            var whale = (com.ggrantrichards.astralend.entity.DriftWhale) put.apply(ModEntities.DRIFT_WHALE.get(), o.add(-12, 20, 20));
            boolean heard = wretch.mimicWhales(end);
            for (int i = 0; i < 60 && !whale.isWary(); i++) {
                whale.tick();
            }
            check.accept("whales answer a Mirror Wretch's false hum, then fall silent and keep away", heard && whale.isWary());
            wretch.discard();
            whale.discard();
            // Pollinators drift in a passing whale's wake.
            var lead = (com.ggrantrichards.astralend.entity.DriftWhale) put.apply(ModEntities.DRIFT_WHALE.get(), o.add(0, 12, 0));
            var drifter = (com.ggrantrichards.astralend.entity.EndPollinator) put.apply(ModEntities.POLLINATOR.get(), o.add(4, 2, 4));
            Vec3 wake = drifter.wakeSpot(lead);
            check.accept("a Pollinator drifts behind a passing whale's tail", drifter.passingWhale() == lead
                && wake.subtract(lead.position()).dot(lead.getLookAngle()) < 0.0);
            // A pod: a calf strays after the Pollinator; its mother calls, comes and nudges it back.
            var calf = (com.ggrantrichards.astralend.entity.DriftWhale) put.apply(ModEntities.DRIFT_WHALE.get(), o.add(4, 12, 4));
            calf.setSize(0.6F);
            calf.follow(lead, new Vec3(6, 0, 0));
            calf.startStray(end);
            boolean chases = calf.isStraying() && calf.strayingAfter() == drifter;
            calf.moveTo(o.x + 30, o.y + 12, o.z + 4, 0.0F, 0.0F);
            var fetched = lead.checkCalves(end);
            lead.nudgeBack(end, calf);
            check.accept("a whale calf chases a Pollinator, and its mother fetches it back", chases && fetched == calf && !calf.isStraying());
            // The flock turns as the leader turned, a beat late.
            for (int i = 0; i < 25; i++) {
                lead.setYRot(0.0F);
                lead.tick();
            }
            float before = 0.0F;
            lead.setYRot(90.0F);
            lead.tick();
            float late = calf.flockHeading(lead);
            check.accept("a pod turns as its leader turned, a beat late", Math.abs(net.minecraft.util.Mth.wrapDegrees(late - before)) < 30.0F
                && Math.abs(net.minecraft.util.Mth.wrapDegrees(lead.pastYaw(0) - (before + 90.0F))) < 30.0F);
            // The pod sleeps, hanging nose-up.
            lead.sleepPod(end, 400);
            for (int i = 0; i < 40; i++) {
                calf.tick();
            }
            check.accept("a pod sleeps together, hanging nose-up", lead.isAsleep() && calf.isAsleep() && calf.getXRot() < -20.0F);
        } finally {
            made.forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    // The distillation's new mobs, doing what they're for (not only appearing): the Hushback is shorn and
    // its herd scatters when one is hurt; the Hydragrip eats an endermite and coughs up a pearl, and
    // leaves players alone; the Mirror Wretch knows when you're at an edge and throws you outward; the
    // Hollowback blinks aside when struck; the Curator sleeps through anything but a player, wakes for
    // one, is staggered by a splash of water, and can't Phase Step away from an arrow by a rung pylon.
    static void checkNewMobs(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        net.minecraft.server.level.ServerPlayer player = standIn(end);
        BlockPos at = standOn.offset(30, 0, 0);
        for (int dx = -14; dx <= 14; dx++) {          // a floor to stand on and blink about
            for (int dz = -14; dz <= 14; dz++) {
                end.setBlock(at.offset(dx, -1, dz), Blocks.END_STONE.defaultBlockState(), 3);
                for (int up = 0; up <= 5; up++) {
                    end.setBlock(at.offset(dx, up, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() - 6.5, 0.0F, 0.0F);
        var shears = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS);
        // Hushback: shorn; hurt one and the herd blinks away.
        java.util.List<com.ggrantrichards.astralend.entity.Hushback> herd = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            var h = ModEntities.HUSHBACK.get().create(end);
            h.moveTo(at.getX() + 0.5 + i * 2, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(h);
            herd.add(h);
        }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shears);
        herd.get(0).mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean shorn = herd.get(0).isSheared();
        Vec3 before1 = herd.get(1).position(), before2 = herd.get(2).position();
        herd.get(0).hurt(end.damageSources().playerAttack(player), 1.0F);
        boolean scattered = herd.get(1).position().distanceTo(before1) > 4.0 || herd.get(2).position().distanceTo(before2) > 4.0;
        herd.forEach(net.minecraft.world.entity.Entity::discard);
        log.accept(String.format("new mobs: Hushback - shorn: %s; the herd blinked away when one was hurt: %s", shorn, scattered));
        if (!shorn || !scattered) {
            failures.add("new mobs: a Hushback should be shorn with shears, and its herd should blink away when one is hurt");
        }
        // Hydragrip: kills an endermite, a pearl comes up; won't go for players.
        var grip = ModEntities.HYDRAGRIP.get().create(end);
        var mite = net.minecraft.world.entity.EntityType.ENDERMITE.create(end);
        grip.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 0.0F, 0.0F);
        mite.moveTo(at.getX() + 1.5, at.getY(), at.getZ() + 4.5, 0.0F, 0.0F);
        end.addFreshEntity(grip);
        end.addFreshEntity(mite);
        mite.setHealth(2.0F);           // one blow (3) to kill it: hurt cooldowns don't come into it
        grip.doHurtTarget(end, mite);
        boolean pearl = !end.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, grip.getBoundingBox().inflate(4.0),
            e -> e.getItem().is(net.minecraft.world.item.Items.ENDER_PEARL)).isEmpty();
        boolean leavesPlayers = !grip.canAttack(player);
        log.accept(String.format("new mobs: Hydragrip - endermite killed: %s, pearl: %s; leaves players alone: %s", !mite.isAlive(), pearl, leavesPlayers));
        if (mite.isAlive() || !pearl || !leavesPlayers) {
            failures.add("new mobs: a Hydragrip should kill an endermite and cough up a pearl, and not attack players");
        }
        grip.discard();
        mite.discard();
        // Mirror Wretch: an edge is an edge; its blow throws you away from it.
        BlockPos ledge = voidColumn(end, at).atY(at.getY() + 10);   // one block of stone over open void
        end.setBlock(ledge.below(), Blocks.END_STONE.defaultBlockState(), 3);
        player.moveTo(ledge.getX() + 0.5, ledge.getY(), ledge.getZ() + 0.5, 0.0F, 0.0F);
        boolean edge = com.ggrantrichards.astralend.entity.MirrorWretch.atEdgeForTest(end, player);
        player.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        boolean ground = com.ggrantrichards.astralend.entity.MirrorWretch.atEdgeForTest(end, player);
        end.setBlock(ledge.below(), Blocks.AIR.defaultBlockState(), 3);
        var wretch = ModEntities.MIRROR_WRETCH.get().create(end);
        wretch.moveTo(at.getX() - 1.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        end.addFreshEntity(wretch);
        player.setDeltaMovement(Vec3.ZERO);
        wretch.throwOutward(player);    // (what its blow does once it lands)
        boolean thrown = player.getDeltaMovement().x > 0.2;
        wretch.discard();
        log.accept(String.format("new mobs: Mirror Wretch - on a ledge counts as an edge: %s, open ground: %s; its blow throws you outward: %s",
            edge, ground, thrown));
        if (!edge || ground || !thrown) {
            failures.add("new mobs: a Mirror Wretch should know a ledge from open ground, and throw a player away from it");
        }
        // Hollowback: struck, it blinks aside (1 in 3) - a dozen blows, and it will have.
        boolean blinked = false;
        for (int i = 0; i < 16 && !blinked; i++) {      // a fresh one each time (no hurt cooldown); 1 in 3 blinks
            var hollow = ModEntities.HOLLOWBACK.get().create(end);
            hollow.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 8.5, 0.0F, 0.0F);
            end.addFreshEntity(hollow);
            Vec3 was = hollow.position();
            hollow.hurt(end.damageSources().playerAttack(player), 0.5F);
            blinked = hollow.position().distanceTo(was) > 2.5;
            hollow.discard();
        }
        log.accept("new mobs: Hollowback - blinked aside when struck: " + blinked);
        if (!blinked) {
            failures.add("new mobs: a Hollowback should blink aside when struck");
        }
        // The Curator.
        var curator = ModEntities.CURATOR.get().create(end);
        curator.moveTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        curator.setHome(at);
        end.addFreshEntity(curator);
        boolean sleptThrough = !curator.hurt(end.damageSources().generic(), 5.0F) && curator.isDormant();
        curator.hurt(end.damageSources().playerAttack(player), 1.0F);
        boolean woke = !curator.isDormant();
        var potion = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("splash_potion")).map(t -> t.create(end)).orElse(null);
        boolean staggered = false;
        if (potion != null) {
            potion.moveTo(curator.getX(), curator.getY() + 3.0, curator.getZ(), 0.0F, 0.0F);
            clearHurtCooldown(curator);
            curator.hurt(end.damageSources().indirectMagic(potion, player), 1.0F);
            staggered = curator.isStaggered();
        }
        // A rung pylon close by: an arrow lands instead of being dodged.
        BlockPos chime = at.offset(3, 4, 0);
        end.setBlock(chime.above(), Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
        end.setBlock(chime, ModBlocks.END_CHIMES.get().defaultBlockState(), 3);
        if (end.getBlockEntity(chime) instanceof com.ggrantrichards.astralend.block.EndChimesBlockEntity bell) {
            bell.ring(end);
        }
        var arrow = (net.minecraft.world.entity.projectile.AbstractArrow) net.minecraft.world.entity.EntityType.ARROW.create(end);
        Vec3 standing = curator.position();
        float health = curator.getHealth();
        for (int i = 0; i < 45; i++) {
            curator.tick();             // let the stagger wear off
        }
        curator.moveTo(standing.x, standing.y, standing.z, 0.0F, 0.0F);
        clearHurtCooldown(curator);
        curator.hurt(end.damageSources().arrow(arrow, player), 6.0F);
        boolean landed = curator.getHealth() < health && curator.position().distanceTo(standing) < 1.0;
        // Half its health gone: the case shatters and it turns over for the upside-down second phase.
        clearHurtCooldown(curator);
        curator.hurt(end.damageSources().playerAttack(player), curator.getMaxHealth() * 0.7F);
        boolean exposed = curator.isExposed() && curator.isAlive();
        log.accept(String.format("new mobs: Curator - at %.0f of %.0f health, in its second phase: %s", curator.getHealth(), curator.getMaxHealth(), exposed));
        if (!exposed) {
            failures.add("new mobs: the Curator should enter its second phase at half health");
        }
        end.setBlock(chime, Blocks.AIR.defaultBlockState(), 3);
        end.setBlock(chime.above(), Blocks.AIR.defaultBlockState(), 3);
        curator.discard();
        log.accept(String.format("new mobs: Curator - slept through a non-player hit: %s; woke for a player: %s; staggered by a splash: %s; "
            + "an arrow by a rung pylon landed: %s", sleptThrough, woke, staggered, landed));
        if (!sleptThrough || !woke || !staggered || !landed) {
            failures.add("new mobs: the Curator should sleep through all but a player, wake for one, stagger in water, and take arrows by a rung pylon");
        }
    }

    static void checkContent(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        var mite = ModEntities.POLLINATOR.get().create(end);
        if (mite != null) {
            mite.moveTo(standOn.getX() + 0.5, standOn.getY() + 1.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
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
        float bounce = com.ggrantrichards.astralend.block.VoidgelBlock.RESTITUTION;
        float moss = ModBlocks.CHORUS_MOSS.get().getSpeedFactor();
        log.accept(String.format("content: Voidgel bounce %.2f, Chorus Moss speed %.2f", bounce, moss));
        if (bounce <= 0.0F || moss >= 1.0F) {
            failures.add("content: Voidgel should bounce and Chorus Moss should slow you a little");
        }
    }

    private MechanicsSurvey() {}
}
