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
                         "hushback", "hydragrip", "mirror_wretch", "hollowback", "curator", "gloamgrub", "shardback"};
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
            // No two alike: a pod's whales differ in size and pace, and they play.
            DriftWhale calf = ModEntities.DRIFT_WHALE.get().create(end, EntitySpawnReason.COMMAND);
            if (calf != null) {
                calf.snapTo(standOn.getX() + 12.5, standOn.getY() + 14.0, standOn.getZ() + 0.5, 0.0F, 0.0F);
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
                if (end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) <= end.getMinY() + 1) {
                    return new BlockPos(x, from.getY(), z);
                }
            }
        }
        return from.offset(0, 0, 400);
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
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() - 6.5, 0.0F, 0.0F);
        var shears = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SHEARS);
        // Hushback: shorn; hurt one and the herd blinks away.
        java.util.List<com.ggrantrichards.astralend.entity.Hushback> herd = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            var h = ModEntities.HUSHBACK.get().create(end, EntitySpawnReason.COMMAND);
            h.snapTo(at.getX() + 0.5 + i * 2, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(h);
            herd.add(h);
        }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, shears);
        herd.get(0).mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean shorn = herd.get(0).isSheared();
        Vec3 before1 = herd.get(1).position(), before2 = herd.get(2).position();
        herd.get(0).hurtServer(end, end.damageSources().playerAttack(player), 1.0F);
        boolean scattered = herd.get(1).position().distanceTo(before1) > 4.0 || herd.get(2).position().distanceTo(before2) > 4.0;
        herd.forEach(net.minecraft.world.entity.Entity::discard);
        log.accept(String.format("new mobs: Hushback - shorn: %s; the herd blinked away when one was hurt: %s", shorn, scattered));
        if (!shorn || !scattered) {
            failures.add("new mobs: a Hushback should be shorn with shears, and its herd should blink away when one is hurt");
        }
        // Hydragrip: kills an endermite, a pearl comes up; won't go for players.
        var grip = ModEntities.HYDRAGRIP.get().create(end, EntitySpawnReason.COMMAND);
        var mite = net.minecraft.world.entity.EntityTypes.ENDERMITE.create(end, EntitySpawnReason.COMMAND);
        grip.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 4.5, 0.0F, 0.0F);
        mite.snapTo(at.getX() + 1.5, at.getY(), at.getZ() + 4.5, 0.0F, 0.0F);
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
        player.snapTo(ledge.getX() + 0.5, ledge.getY(), ledge.getZ() + 0.5, 0.0F, 0.0F);
        boolean edge = com.ggrantrichards.astralend.entity.MirrorWretch.atEdgeForTest(end, player);
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        boolean ground = com.ggrantrichards.astralend.entity.MirrorWretch.atEdgeForTest(end, player);
        end.setBlock(ledge.below(), Blocks.AIR.defaultBlockState(), 3);
        var wretch = ModEntities.MIRROR_WRETCH.get().create(end, EntitySpawnReason.COMMAND);
        wretch.snapTo(at.getX() - 1.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
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
            var hollow = ModEntities.HOLLOWBACK.get().create(end, EntitySpawnReason.COMMAND);
            hollow.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 8.5, 0.0F, 0.0F);
            end.addFreshEntity(hollow);
            Vec3 was = hollow.position();
            hollow.hurtServer(end, end.damageSources().playerAttack(player), 0.5F);
            blinked = hollow.position().distanceTo(was) > 2.5;
            hollow.discard();
        }
        log.accept("new mobs: Hollowback - blinked aside when struck: " + blinked);
        if (!blinked) {
            failures.add("new mobs: a Hollowback should blink aside when struck");
        }
        // The Curator.
        var curator = ModEntities.CURATOR.get().create(end, EntitySpawnReason.COMMAND);
        curator.snapTo(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 0.0F, 0.0F);
        curator.setHome(at);
        end.addFreshEntity(curator);
        boolean sleptThrough = !curator.hurtServer(end, end.damageSources().generic(), 5.0F) && curator.isDormant();
        curator.hurtServer(end, end.damageSources().playerAttack(player), 1.0F);
        boolean woke = !curator.isDormant();
        var potion = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(
            net.minecraft.resources.Identifier.withDefaultNamespace("splash_potion")).map(t -> t.create(end, EntitySpawnReason.COMMAND)).orElse(null);
        boolean staggered = false;
        if (potion != null) {
            potion.snapTo(curator.getX(), curator.getY() + 3.0, curator.getZ(), 0.0F, 0.0F);
            clearHurtCooldown(curator);
            curator.hurtServer(end, end.damageSources().indirectMagic(potion, player), 1.0F);
            staggered = curator.isStaggered();
        }
        // A rung pylon close by: an arrow lands instead of being dodged.
        BlockPos chime = at.offset(3, 4, 0);
        end.setBlock(chime.above(), Blocks.PURPUR_BLOCK.defaultBlockState(), 3);
        end.setBlock(chime, ModBlocks.END_CHIMES.get().defaultBlockState(), 3);
        if (end.getBlockEntity(chime) instanceof com.ggrantrichards.astralend.block.EndChimesBlockEntity bell) {
            bell.ring(end);
        }
        var arrow = (net.minecraft.world.entity.projectile.arrow.AbstractArrow) net.minecraft.world.entity.EntityTypes.ARROW.create(end, EntitySpawnReason.COMMAND);
        Vec3 standing = curator.position();
        float health = curator.getHealth();
        for (int i = 0; i < 45; i++) {
            curator.tick();             // let the stagger wear off
        }
        curator.snapTo(standing.x, standing.y, standing.z, 0.0F, 0.0F);
        clearHurtCooldown(curator);
        curator.hurtServer(end, end.damageSources().arrow(arrow, player), 6.0F);
        boolean landed = curator.getHealth() < health && curator.position().distanceTo(standing) < 1.0;
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
