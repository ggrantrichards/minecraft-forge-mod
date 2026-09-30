package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
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
        RandomState randomState = end.getChunkSource().randomState();
        ChunkGenerator generator = end.getChunkSource().getGenerator();
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(randomState);

        // ---- 1. Biome map ----
        log("Biome map of the End, " + (2 * MAP_RADIUS) + " x " + (2 * MAP_RADIUS) + " blocks, north up, one character = "
            + MAP_STEP + " blocks:");
        log("  O = central island (vanilla)   N = Nebula Fields   H/M/B/s = vanilla Highlands/Midlands/Barrens/Small Islands");
        Map<String, Integer> outerCounts = new HashMap<>();
        int outerTotal = 0;
        List<BlockPos> nebulaSpots = new ArrayList<>();
        BlockPos vanillaSpot = null;
        int nebulaRuns = 0;
        int nebulaWithNebulaNeighbour = 0;
        for (int z = -MAP_RADIUS; z <= MAP_RADIUS; z += MAP_STEP * 2) {
            StringBuilder row = new StringBuilder();
            char previous = ' ';
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
                if (c == 'N' && distance < 4000) {
                    nebulaSpots.add(new BlockPos(x, 64, z));
                }
                if (vanillaSpot == null && distance > 1500 && (c == 'H' || c == 'M')) {
                    vanillaSpot = new BlockPos(x, 64, z);
                }
                // Coherence: how often a Nebula Fields sample has Nebula Fields just to its west.
                if (c == 'N' && distance > 1100) {
                    nebulaRuns++;
                    if (previous == 'N') {
                        nebulaWithNebulaNeighbour++;
                    }
                }
                previous = c;
            }
            log("  " + row);
        }
        int nebula = outerCounts.getOrDefault("N", 0);
        double nebulaShare = 100.0 * nebula / Math.max(1, outerTotal);
        log(String.format("Outer End (beyond 1,100 blocks): Nebula Fields %.1f%%, Highlands %.1f%%, Midlands %.1f%%, Barrens %.1f%%, Small Islands %.1f%%",
            nebulaShare, pct(outerCounts, "H", outerTotal), pct(outerCounts, "M", outerTotal), pct(outerCounts, "B", outerTotal),
            pct(outerCounts, "s", outerTotal)));
        double coherence = 100.0 * nebulaWithNebulaNeighbour / Math.max(1, nebulaRuns);
        log(String.format("Nebula Fields coherence: %.0f%% of samples continue the region of their western neighbour (%d blocks away)",
            coherence, MAP_STEP));
        if (coherence < 50) {
            failures.add(String.format("Nebula Fields are scattered, not regions (coherence %.0f%%, expected 50%%+)", coherence));
        }
        if (nebulaShare < 15 || nebulaShare > 45) {
            failures.add(String.format("Nebula Fields cover %.1f%% of the outer End (expected 15-45%%)", nebulaShare));
        }
        for (String vanilla : new String[] {"H", "M", "s"}) {
            if (outerCounts.getOrDefault(vanilla, 0) == 0) {
                failures.add("vanilla biome " + vanilla + " no longer appears in the outer End");
            }
        }
        // Pick the Nebula Fields spot nearest the centre that has plenty of islands, using the
        // terrain generator's surface height (no chunks generated yet).
        nebulaSpots.sort((a, b) -> Long.compare(distSqr(a), distSqr(b)));
        BlockPos bestNebula = null;
        int bestLand = -1;
        for (BlockPos spot : nebulaSpots) {
            int land = 0;
            for (int ox = -64; ox <= 64; ox += 32) {
                for (int oz = -64; oz <= 64; oz += 32) {
                    int h = generator.getBaseHeight(spot.getX() + ox, spot.getZ() + oz, Heightmap.Types.WORLD_SURFACE_WG, end, randomState);
                    if (h > end.getMinY() + 1) {
                        land++;
                    }
                }
            }
            if (land > bestLand) {
                bestLand = land;
                bestNebula = spot;
            }
            if (land >= 15) {
                break; // 15 of 25 sample points on land is plenty
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
            + bestNebula.getZ() + " (the nearest Nebula Fields spot with plenty of islands)...");
        Map<Block, Integer> counts = new HashMap<>();
        Map<Block, Integer> nebulaCounts = new HashMap<>();
        int nebulaColumns = 0;
        int columnsWithLand = 0;
        int mossTops = 0;
        int stoneTops = 0;
        BlockPos standOn = null;
        int size = (2 * AREA_CHUNKS + 1) * 16;
        char[][] topView = new char[size][size];
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
                        Block firstSeen = null;
                        for (int y = end.getMaxY(); y >= end.getMinY(); y--) {
                            BlockState state = chunk.getBlockState(new BlockPos(bx, y, bz));
                            if (state.isAir()) {
                                continue;
                            }
                            counts.merge(state.getBlock(), 1, Integer::sum);
                            if (firstSeen == null) {
                                firstSeen = state.getBlock();
                            }
                            if (inNebula) {
                                nebulaCounts.merge(state.getBlock(), 1, Integer::sum);
                            }
                            if (top == null && state.isSolidRender()) {
                                top = state.getBlock();
                                if (inNebula && standOn == null && top == ModBlocks.NEBULA_MOSS.get()) {
                                    standOn = new BlockPos(bx, y + 1, bz);
                                }
                            }
                        }
                        topView[((dz + AREA_CHUNKS) << 4) + z][((dx + AREA_CHUNKS) << 4) + x] = topSymbol(firstSeen, inNebula);
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
        int moss = nebulaCounts.getOrDefault(ModBlocks.NEBULA_MOSS.get(), 0);
        int shrooms = nebulaCounts.getOrDefault(ModBlocks.LUMINESHROOM.get(), 0);
        int blooms = nebulaCounts.getOrDefault(ModBlocks.STARBLOOM.get(), 0);
        int vineTips = nebulaCounts.getOrDefault(ModBlocks.ASTRAL_VINE.get(), 0);
        int vineBody = nebulaCounts.getOrDefault(ModBlocks.ASTRAL_VINE_PLANT.get(), 0);
        int chorus = nebulaCounts.getOrDefault(Blocks.CHORUS_PLANT, 0) + nebulaCounts.getOrDefault(Blocks.CHORUS_FLOWER, 0);
        int mossOutside = counts.getOrDefault(ModBlocks.NEBULA_MOSS.get(), 0) - moss;
        int chorusOutside = counts.getOrDefault(Blocks.CHORUS_PLANT, 0) + counts.getOrDefault(Blocks.CHORUS_FLOWER, 0) - chorus;
        log("Columns in Nebula Fields: " + nebulaColumns + " of " + (2 * AREA_CHUNKS + 1) * (2 * AREA_CHUNKS + 1) * 256
            + ", with land: " + columnsWithLand);
        log("Top block of Nebula Fields land: Nebula Moss " + mossTops + ", bare End Stone " + stoneTops);
        double chunksWithLand = columnsWithLand / 256.0;
        log("Top-down view of the surveyed area (" + size + " x " + size + " blocks, north up), what you'd see from above:");
        log("  ' ' void  e/E End Stone (outside/inside Nebula Fields)  m Nebula Moss  L Lumineshroom  S Starbloom  V Astral Vine  c chorus");
        for (char[] row : topView) {
            log("  |" + new String(row) + "|");
        }
        log(String.format("Inside Nebula Fields (~%.0f chunks of land): Nebula Moss %d, Lumineshroom %d (%.1f/chunk), Starbloom %d (%.1f/chunk), "
            + "Astral Vines %d (+%d body blocks, %.1f/chunk), chorus %d", chunksWithLand, moss, shrooms, shrooms / chunksWithLand, blooms,
            blooms / chunksWithLand, vineTips, vineBody, vineTips / chunksWithLand, chorus));
        log("Outside Nebula Fields in the same area (vanilla biomes): Nebula Moss " + mossOutside + ", chorus " + chorusOutside);
        // Minecraft blurs biome borders by a block or two, so a little moss just across the border
        // is normal. Much more than that would mean the surface rule is wrong.
        if (mossOutside > 0.01 * Math.max(1, moss)) {
            failures.add(mossOutside + " Nebula Moss blocks generated outside Nebula Fields (more than border blending explains)");
        }
        if (chorus > 0) {
            failures.add(chorus + " chorus blocks grew inside Nebula Fields (they need bare End Stone)");
        }
        if (columnsWithLand < 500) {
            failures.add("the surveyed Nebula Fields area has almost no land (" + columnsWithLand + " columns)");
        } else {
            if (mossTops < 0.9 * columnsWithLand) {
                failures.add("only " + mossTops + " of " + columnsWithLand + " land columns are topped with Nebula Moss (expected 90%+)");
            }
            if (shrooms < chunksWithLand * 2) {
                failures.add("too few Lumineshrooms: " + shrooms + " for ~" + (int) chunksWithLand + " chunks of land");
            }
            if (blooms < chunksWithLand * 1) {
                failures.add("too few Starblooms: " + blooms + " for ~" + (int) chunksWithLand + " chunks of land");
            }
            if (vineTips < chunksWithLand * 0.3) {
                failures.add("too few Astral Vines: " + vineTips + " for ~" + (int) chunksWithLand + " chunks of land");
            }
            if (shrooms > chunksWithLand * 25 || blooms > chunksWithLand * 25) {
                failures.add("plants are far too dense (" + shrooms + " Lumineshrooms, " + blooms + " Starblooms)");
            }
        }

        // ---- 3. Low gravity ----
        if (standOn != null) {
            ArmorStand probe = new ArmorStand(end, standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5);
            LowGravity.update(probe);
            double inside = probe.getAttributeValue(Attributes.GRAVITY);
            if (vanillaSpot == null) {
                failures.add("found no vanilla outer biome to test normal gravity in");
                return;
            }
            end.getChunk(vanillaSpot.getX() >> 4, vanillaSpot.getZ() >> 4);
            probe.setPos(vanillaSpot.getX() + 0.5, 70, vanillaSpot.getZ() + 0.5);
            boolean outsideIsNebula = end.getBiome(probe.blockPosition()).is(ModBiomes.NEBULA_FIELDS);
            LowGravity.update(probe);
            double outside = probe.getAttributeValue(Attributes.GRAVITY);
            log(String.format("Gravity inside Nebula Fields: %.4f; after moving to %d, %d (%s): %.4f (vanilla is 0.0800)", inside,
                vanillaSpot.getX(), vanillaSpot.getZ(), outsideIsNebula ? "Nebula Fields?!" : "a vanilla biome", outside));
            if (Math.abs(inside - 0.08 * (1 + LowGravity.GRAVITY_CHANGE)) > 0.001) {
                failures.add("low gravity not applied inside Nebula Fields (" + inside + ")");
            }
            if (outsideIsNebula || Math.abs(outside - 0.08) > 0.001) {
                failures.add("low gravity not removed after leaving Nebula Fields (" + outside + ")");
            }
        } else {
            failures.add("found no Nebula Moss surface to test gravity on");
        }
    }

    private static char topSymbol(Block block, boolean inNebula) {
        if (block == null) return ' ';
        if (block == ModBlocks.NEBULA_MOSS.get()) return 'm';
        if (block == ModBlocks.LUMINESHROOM.get()) return 'L';
        if (block == ModBlocks.STARBLOOM.get()) return 'S';
        if (block == ModBlocks.ASTRAL_VINE.get() || block == ModBlocks.ASTRAL_VINE_PLANT.get()) return 'V';
        if (block == Blocks.CHORUS_PLANT || block == Blocks.CHORUS_FLOWER) return 'c';
        if (block == Blocks.END_STONE) return inNebula ? 'E' : 'e';
        return '#';
    }

    private static long distSqr(BlockPos pos) {
        return (long) pos.getX() * pos.getX() + (long) pos.getZ() * pos.getZ();
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
