package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.world.AstralEndBiomeSource;
import com.ggrantrichards.astralend.world.LowGravity;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

// Development tool, not part of normal play. When the server is started with
// -Dastralend.survey=true (./gradlew runServer -PastralendSurvey), this:
//   1. draws a text map of the End's biomes and measures how much is Nebula Fields,
//   2. generates a 9x9-chunk area inside Nebula Fields and counts every block,
//   3. checks that low gravity is applied inside Nebula Fields and not outside,
// then prints SURVEY PASSED or SURVEY FAILED and stops the server. CI runs it on every push.
public final class WorldgenSurvey {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[AstralEnd survey] ";
    private static final int MAP_RADIUS = 6000;
    private static final int MAP_STEP = 150;
    private static final int AREA_CHUNKS = 4; // survey (2*4+1)^2 = 81 chunks

    public static void register() {
        if (Boolean.getBoolean("astralend.survey")) {
            ServerStartedEvent.BUS.addListener(WorldgenSurvey::run);
        }
    }

    private static void run(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        List<String> failures = new ArrayList<>();
        try {
            survey(server, failures);
        } catch (RuntimeException e) {
            LOGGER.error(TAG + "crashed", e);
            failures.add("survey crashed: " + e);
        }
        if (failures.isEmpty()) {
            log("SURVEY PASSED");
        } else {
            failures.forEach(f -> log("FAIL: " + f));
            log("SURVEY FAILED");
        }
        server.halt(false);
    }

    private static void survey(MinecraftServer server, List<String> failures) {
        ServerLevel end = server.getLevel(Level.END);
        Climate.Sampler sampler = end.getChunkSource().randomState().sampler();
        BiomeResolver biomes = end.getChunkSource().getGenerator().getBiomeSource().createResolver(sampler);

        // ---- 1. Biome map ----
        log("Biome map of the End, " + (2 * MAP_RADIUS) + " x " + (2 * MAP_RADIUS) + " blocks, north up, one character = "
            + MAP_STEP + " blocks:");
        log("  O = central island (vanilla)   N = Nebula Fields   H/M/B/s = vanilla Highlands/Midlands/Barrens/Small Islands");
        Map<String, Integer> outerCounts = new HashMap<>();
        int outerTotal = 0;
        BlockPos bestNebula = null;
        double bestErosion = Double.NEGATIVE_INFINITY;
        for (int z = -MAP_RADIUS; z <= MAP_RADIUS; z += MAP_STEP * 2) {
            StringBuilder row = new StringBuilder();
            for (int x = -MAP_RADIUS; x <= MAP_RADIUS; x += MAP_STEP) {
                Holder<Biome> biome = biomes.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z));
                char c = symbol(biome);
                row.append(c);
                double distance = Math.sqrt((double) x * x + (double) z * z);
                if (distance > 1100) {
                    outerTotal++;
                    outerCounts.merge(String.valueOf(c), 1, Integer::sum);
                }
                if (c == 'O' && distance > 1100) {
                    failures.add("central-island biome found " + (int) distance + " blocks out");
                }
                if (c != 'O' && distance < 1000) {
                    failures.add("non-central biome " + c + " found only " + (int) distance + " blocks from the centre");
                }
                // Remember the Nebula Fields spot with the most island mass (vanilla's island noise), within 4,000 blocks.
                if (c == 'N' && distance < 4000) {
                    double erosion = sampler.erosion().sampleValue(x, 64, z);
                    if (erosion > bestErosion) {
                        bestErosion = erosion;
                        bestNebula = new BlockPos(x, 64, z);
                    }
                }
            }
            log("  " + row);
        }
        int nebula = outerCounts.getOrDefault("N", 0);
        double nebulaShare = 100.0 * nebula / Math.max(1, outerTotal);
        log(String.format("Outer End (beyond 1,100 blocks): Nebula Fields %.1f%%, Highlands %.1f%%, Midlands %.1f%%, Barrens %.1f%%, Small Islands %.1f%%",
            nebulaShare, pct(outerCounts, "H", outerTotal), pct(outerCounts, "M", outerTotal), pct(outerCounts, "B", outerTotal),
            pct(outerCounts, "s", outerTotal)));
        if (nebulaShare < 15 || nebulaShare > 45) {
            failures.add(String.format("Nebula Fields cover %.1f%% of the outer End (expected 15-45%%)", nebulaShare));
        }
        for (String vanilla : new String[] {"H", "M", "s"}) {
            if (outerCounts.getOrDefault(vanilla, 0) == 0) {
                failures.add("vanilla biome " + vanilla + " no longer appears in the outer End");
            }
        }
        if (bestNebula == null) {
            failures.add("no Nebula Fields within 4,000 blocks of the centre");
            return;
        }

        // ---- 2. Generate chunks inside Nebula Fields and count blocks ----
        int cx = bestNebula.getX() >> 4;
        int cz = bestNebula.getZ() >> 4;
        log("Generating " + (2 * AREA_CHUNKS + 1) * (2 * AREA_CHUNKS + 1) + " chunks around " + bestNebula.getX() + ", "
            + bestNebula.getZ() + " (Nebula Fields, most island-dense spot found)...");
        Map<Block, Integer> counts = new HashMap<>();
        int nebulaColumns = 0;
        int columnsWithLand = 0;
        int mossTops = 0;
        int stoneTops = 0;
        BlockPos standOn = null;
        for (int dx = -AREA_CHUNKS; dx <= AREA_CHUNKS; dx++) {
            for (int dz = -AREA_CHUNKS; dz <= AREA_CHUNKS; dz++) {
                LevelChunk chunk = end.getChunk(cx + dx, cz + dz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int bx = ((cx + dx) << 4) + x;
                        int bz = ((cz + dz) << 4) + z;
                        boolean inNebula = end.getBiome(new BlockPos(bx, 64, bz)).is(ModBiomes.NEBULA_FIELDS);
                        if (inNebula) {
                            nebulaColumns++;
                        }
                        Block top = null;
                        for (int y = end.getMaxY(); y >= end.getMinY(); y--) {
                            BlockState state = chunk.getBlockState(new BlockPos(bx, y, bz));
                            if (state.isAir()) {
                                continue;
                            }
                            counts.merge(state.getBlock(), 1, Integer::sum);
                            if (top == null && state.isSolidRender()) {
                                top = state.getBlock();
                                if (inNebula && standOn == null && top == ModBlocks.NEBULA_MOSS.get()) {
                                    standOn = new BlockPos(bx, y + 1, bz);
                                }
                            }
                        }
                        if (top != null && inNebula) {
                            columnsWithLand++;
                            if (top == ModBlocks.NEBULA_MOSS.get()) {
                                mossTops++;
                            } else if (top == Blocks.END_STONE) {
                                stoneTops++;
                            }
                        }
                    }
                }
            }
        }
        int moss = counts.getOrDefault(ModBlocks.NEBULA_MOSS.get(), 0);
        int shrooms = counts.getOrDefault(ModBlocks.LUMINESHROOM.get(), 0);
        int blooms = counts.getOrDefault(ModBlocks.STARBLOOM.get(), 0);
        int vineTips = counts.getOrDefault(ModBlocks.ASTRAL_VINE.get(), 0);
        int vineBody = counts.getOrDefault(ModBlocks.ASTRAL_VINE_PLANT.get(), 0);
        int chorus = counts.getOrDefault(Blocks.CHORUS_PLANT, 0) + counts.getOrDefault(Blocks.CHORUS_FLOWER, 0);
        log("Columns in Nebula Fields: " + nebulaColumns + " of " + (2 * AREA_CHUNKS + 1) * (2 * AREA_CHUNKS + 1) * 256
            + ", with land: " + columnsWithLand);
        log("Top block of Nebula Fields land: Nebula Moss " + mossTops + ", bare End Stone " + stoneTops);
        log("Blocks: End Stone " + counts.getOrDefault(Blocks.END_STONE, 0) + ", Nebula Moss " + moss + ", Lumineshroom " + shrooms
            + ", Starbloom " + blooms + ", Astral Vine tips " + vineTips + " (+" + vineBody + " vine body), chorus " + chorus);
        double chunksWithLand = columnsWithLand / 256.0;
        if (columnsWithLand < 500) {
            failures.add("the surveyed Nebula Fields area has almost no land (" + columnsWithLand + " columns)");
        } else {
            if (mossTops < 0.9 * columnsWithLand) {
                failures.add("only " + mossTops + " of " + columnsWithLand + " land columns are topped with Nebula Moss (expected 90%+)");
            }
            if (shrooms < chunksWithLand * 0.5) {
                failures.add("too few Lumineshrooms: " + shrooms + " for ~" + (int) chunksWithLand + " chunks of land");
            }
            if (blooms < chunksWithLand * 0.3) {
                failures.add("too few Starblooms: " + blooms + " for ~" + (int) chunksWithLand + " chunks of land");
            }
            if (vineTips < chunksWithLand * 0.1) {
                failures.add("too few Astral Vines: " + vineTips + " for ~" + (int) chunksWithLand + " chunks of land");
            }
            if (shrooms > chunksWithLand * 40 || blooms > chunksWithLand * 40) {
                failures.add("plants are far too dense (" + shrooms + " Lumineshrooms, " + blooms + " Starblooms)");
            }
        }

        // ---- 3. Low gravity ----
        if (standOn != null) {
            ArmorStand probe = new ArmorStand(end, standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5);
            LowGravity.update(probe);
            double inside = probe.getAttributeValue(Attributes.GRAVITY);
            probe.setPos(1500.5, 70, 0.5);
            boolean outsideIsNebula = end.getBiome(probe.blockPosition()).is(ModBiomes.NEBULA_FIELDS);
            LowGravity.update(probe);
            double outside = probe.getAttributeValue(Attributes.GRAVITY);
            log(String.format("Gravity inside Nebula Fields: %.4f, at (1500, 0) %s: %.4f (vanilla is 0.0800)", inside,
                outsideIsNebula ? "[also Nebula Fields]" : "[not Nebula Fields]", outside));
            if (Math.abs(inside - 0.08 * (1 + LowGravity.GRAVITY_CHANGE)) > 0.001) {
                failures.add("low gravity not applied inside Nebula Fields (" + inside + ")");
            }
            if (!outsideIsNebula && Math.abs(outside - 0.08) > 0.001) {
                failures.add("low gravity not removed outside Nebula Fields (" + outside + ")");
            }
        } else {
            failures.add("found no Nebula Moss surface to test gravity on");
        }
    }

    private static char symbol(Holder<Biome> biome) {
        if (biome.is(ModBiomes.NEBULA_FIELDS)) return 'N';
        if (biome.is(Biomes.THE_END)) return 'O';
        if (biome.is(Biomes.END_HIGHLANDS)) return 'H';
        if (biome.is(Biomes.END_MIDLANDS)) return 'M';
        if (biome.is(Biomes.END_BARRENS)) return 'B';
        if (biome.is(Biomes.SMALL_END_ISLANDS)) return 's';
        return '?';
    }

    private static double pct(Map<String, Integer> counts, String key, int total) {
        return 100.0 * counts.getOrDefault(key, 0) / Math.max(1, total);
    }

    private static void log(String line) {
        LOGGER.info(TAG + line);
    }

    private WorldgenSurvey() {}
}
