package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.entity.DriftWhale;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.entity.Watcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.LogicalSide;

// Places the mobs that must never crowd a player: the Drift Whale and the Watcher. Biome spawn
// lists can't promise "at most one near you", so these are placed here instead, checked for each
// player in the End a few times a minute.
//
// Also: a Bottled Flare burns in a furnace for as long as it takes to smelt 120 items, and Starsap 12.
public final class MobSpawning {
    private static final int CHECK_EVERY = 200;  // ticks
    public static final ResourceKey<Structure> OBSERVATORY = ResourceKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "shattered_observatory"));

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(MobSpawning::onLevelTick);
        FurnaceFuelBurnTimeEvent.BUS.addListener(event -> {
            if (event.getItemStack().is(ModItems.BOTTLED_FLARE.get())) {
                event.setBurnTime(24000);   // a captured Sunstorm: 120 items, more than a lava bucket
            } else if (event.getItemStack().is(ModItems.STARSAP.get())) {
                event.setBurnTime(2400);    // old amber burns long: 12 items, half again a piece of coal
            } else if (event.getItemStack().is(ModBlocks.STARSAP_BLOCK.get().asItem())) {
                event.setBurnTime(24000);
            }
        });
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.side() != LogicalSide.SERVER || !(event.level() instanceof ServerLevel level) || level.dimension() != Level.END
            || level.getGameTime() % CHECK_EVERY != 0) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            // Drift Whales: most common over the Ashen Barrens, rare elsewhere in the outer End.
            int whaleChance = biome.is(ModBiomes.ASHEN_BARRENS) ? 6 : 30;
            if (isOuterEnd(player) && random.nextInt(whaleChance) == 0 && none(level, player, DriftWhale.class, 192)) {
                spawnWhale(level, player, random);
            }
            // The Watcher: over the Ashen Barrens and round the Shattered Observatory, only when you're
            // alone - and soon again after you've looked straight at one, further along the way.
            boolean recent = level.getGameTime() - RECENT.getOrDefault(player.getUUID(), -10000L) < 1200;
            // Arriving where the Watchers were leading, with one seen in the last five minutes.
            BlockPos led = GUIDE.get(player.getUUID());
            if (led != null && level.getGameTime() - RECENT.getOrDefault(player.getUUID(), -10000L) < 6000
                && led.distSqr(player.blockPosition().atY(led.getY())) < 80.0 * 80.0) {
                com.ggrantrichards.astralend.world.ModAdvancements.award(player, "led_by_the_watcher");
            }
            int watcherChance = recent ? 1 : biome.is(ModBiomes.ASHEN_BARRENS) ? 5 : 12;
            if (random.nextInt(watcherChance) == 0 && isOuterEnd(player) && none(level, player, Watcher.class, 256)
                && level.players().stream().noneMatch(other -> other != player && other.distanceToSqr(player) < 96.0 * 96.0)) {
                spawnWatcher(level, player, random, biome.is(ModBiomes.ASHEN_BARRENS) || recent);
            }
        }
    }

    private static boolean isOuterEnd(ServerPlayer player) {
        return player.getX() * player.getX() + player.getZ() * player.getZ() > 1000.0 * 1000.0;
    }

    private static boolean none(ServerLevel level, ServerPlayer player, Class<? extends Mob> type, double radius) {
        return level.getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(radius)).isEmpty();
    }

    // A pod of three to five: the first is its leader (the biggest), the rest swim round it; often one
    // is a calf, which keeps close by its mother, the leader.
    private static void spawnWhale(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 64.0 + random.nextDouble() * 32.0;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = Mth.clamp(player.getY() + 10.0 + random.nextDouble() * 25.0, 30.0, 140.0);
            DriftWhale leader = trySpawn(level, ModEntities.DRIFT_WHALE.get(), x, y, z);
            if (leader == null) {
                continue;
            }
            leader.setSize(1.05F + random.nextFloat() * 0.15F);
            int pod = 2 + random.nextInt(3);
            boolean calf = random.nextInt(2) == 0;
            for (int i = 0; i < pod; i++) {
                net.minecraft.world.phys.Vec3 station = new net.minecraft.world.phys.Vec3((random.nextDouble() - 0.5) * 30.0,
                    (random.nextDouble() - 0.5) * 8.0, 10.0 + random.nextDouble() * 16.0);
                DriftWhale follower = trySpawn(level, ModEntities.DRIFT_WHALE.get(), x + station.x, y + station.y, z + station.z);
                if (follower != null) {
                    follower.setSize(calf && i == 0 ? 0.55F + random.nextFloat() * 0.1F : 0.8F + random.nextFloat() * 0.25F);
                    follower.follow(leader, station);
                }
            }
            return;
        }
    }

    // When each player last looked straight at a Watcher (so the next appears soon, further along).
    private static final java.util.Map<java.util.UUID, Long> RECENT = new java.util.HashMap<>();
    // Where the Watcher leads each player: the nearest leviathan's remains, looked up now and then.
    private static final java.util.Map<java.util.UUID, BlockPos> GUIDE = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, BlockPos> GUIDE_FROM = new java.util.HashMap<>();
    public static final net.minecraft.tags.TagKey<Structure> REMAINS = net.minecraft.tags.TagKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "leviathan_remains"));

    public static void watcherSeen(ServerPlayer player) {
        RECENT.put(player.getUUID(), player.level().getGameTime());
    }

    private static @org.jspecify.annotations.Nullable BlockPos guideFor(ServerLevel level, ServerPlayer player) {
        BlockPos from = GUIDE_FROM.get(player.getUUID());
        if (from == null || from.distSqr(player.blockPosition()) > 400.0 * 400.0) {
            BlockPos found = level.findNearestMapStructure(REMAINS, player.blockPosition(), 48, false);
            GUIDE_FROM.put(player.getUUID(), player.blockPosition());
            if (found == null) {
                GUIDE.remove(player.getUUID());
            } else {
                GUIDE.put(player.getUUID(), found);
            }
        }
        return GUIDE.get(player.getUUID());
    }

    private static void spawnWatcher(ServerLevel level, ServerPlayer player, RandomSource random, boolean guiding) {
        BlockPos guide = guiding ? guideFor(level, player) : null;
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle;
            if (guide != null) {
                // Toward the remains, give or take 20 degrees.
                angle = Math.atan2(guide.getZ() - player.getZ(), guide.getX() - player.getX()) + (random.nextDouble() - 0.5) * 0.7;
            } else {
                // Somewhere ahead of the player, near the edge of sight.
                angle = Math.toRadians(player.getYRot() + 90.0 + (random.nextDouble() - 0.5) * 120.0);
            }
            double distance = 40.0 + random.nextDouble() * 32.0;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            boolean ground = y > level.getMinY() + 1;
            if (!guiding && (!ground
                || !level.structureManager().getStructureWithPieceAt(new BlockPos(x, y - 1, z), holder -> holder.is(OBSERVATORY)).isValid())) {
                continue;
            }
            double hover = ground ? y + 2.0 : player.getY() + 4.0;
            if (trySpawn(level, ModEntities.WATCHER.get(), x + 0.5, hover, z + 0.5) != null) {
                return;
            }
        }
    }

    private static <T extends Mob> @org.jspecify.annotations.Nullable T trySpawn(ServerLevel level, EntityType<T> type, double x, double y, double z) {
        T mob = type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) {
            return null;
        }
        mob.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob) || !level.isLoaded(mob.blockPosition())) {
            return null;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, null);
        return level.addFreshEntity(mob) ? mob : null;
    }

    private MobSpawning() {}
}
