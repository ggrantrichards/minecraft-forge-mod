package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
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
// Also: a Bottled Flare burns in a furnace for as long as it takes to smelt 120 items.
public final class MobSpawning {
    private static final int CHECK_EVERY = 200;  // ticks
    public static final ResourceKey<Structure> OBSERVATORY = ResourceKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "shattered_observatory"));

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(MobSpawning::onLevelTick);
        FurnaceFuelBurnTimeEvent.BUS.addListener(event -> {
            if (event.getItemStack().is(ModItems.BOTTLED_FLARE.get())) {
                event.setBurnTime(24000);   // a captured Sunstorm: 120 items, more than a lava bucket
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
            // The Watcher: only around the Shattered Observatory, and only when you're alone.
            if (random.nextInt(12) == 0 && isOuterEnd(player) && none(level, player, Watcher.class, 256)
                && level.players().stream().noneMatch(other -> other != player && other.distanceToSqr(player) < 96.0 * 96.0)) {
                spawnWatcher(level, player, random);
            }
        }
    }

    private static boolean isOuterEnd(ServerPlayer player) {
        return player.getX() * player.getX() + player.getZ() * player.getZ() > 1000.0 * 1000.0;
    }

    private static boolean none(ServerLevel level, ServerPlayer player, Class<? extends Mob> type, double radius) {
        return level.getEntitiesOfClass(type, new AABB(player.blockPosition()).inflate(radius)).isEmpty();
    }

    private static void spawnWhale(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 64.0 + random.nextDouble() * 32.0;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = Mth.clamp(player.getY() + 10.0 + random.nextDouble() * 25.0, 30.0, 140.0);
            if (trySpawn(level, ModEntities.DRIFT_WHALE.get(), x, y, z)) {
                return;
            }
        }
    }

    private static void spawnWatcher(ServerLevel level, ServerPlayer player, RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            // Somewhere ahead of the player, near the edge of sight, in the observatory's ruins.
            double angle = Math.toRadians(player.getYRot() + 90.0 + (random.nextDouble() - 0.5) * 120.0);
            double distance = 40.0 + random.nextDouble() * 32.0;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y <= level.getMinY() + 1
                || !level.structureManager().getStructureWithPieceAt(new BlockPos(x, y - 1, z), holder -> holder.is(OBSERVATORY)).isValid()) {
                continue;
            }
            if (trySpawn(level, ModEntities.WATCHER.get(), x + 0.5, y, z + 0.5)) {
                return;
            }
        }
    }

    private static <T extends Mob> boolean trySpawn(ServerLevel level, EntityType<T> type, double x, double y, double z) {
        T mob = type.create(level, EntitySpawnReason.NATURAL);
        if (mob == null) {
            return false;
        }
        mob.snapTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob) || !level.isLoaded(mob.blockPosition())) {
            return false;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.NATURAL, null);
        return level.addFreshEntity(mob);
    }

    private MobSpawning() {}
}
