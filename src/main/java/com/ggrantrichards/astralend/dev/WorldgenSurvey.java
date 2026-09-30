package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.GravityAnomalyBlockEntity;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.world.AstralEndBiomeSource;
import com.ggrantrichards.astralend.world.GravityInversion;
import com.ggrantrichards.astralend.world.LowGravity;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

// Development tool, not part of normal play. When the server is started with
// -Dastralend.survey=true (./gradlew runServer -PastralendSurvey), this:
//   1. travels through a real End Portal (the way a stronghold's portal works) and checks the
//      arrival platform, the dragon fight and the central island,
//   2. draws a text map of the End's biomes and measures how much each biome covers,
//   3. generates an area inside every astral biome and counts every block, checking that each
//      biome has its stone, ores, plants and features,
//   4. checks low gravity (Nebula Fields) and gravity inversion (Dark Matter Belt),
// then prints SURVEY PASSED or SURVEY FAILED and stops the server. CI runs it on every push.
// tools/render_survey.py turns its maps into the pictures in the README.
public final class WorldgenSurvey {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[AstralEnd survey] ";
    private static final int MAP_RADIUS = 12000;
    private static final int MAP_STEP = 250;

    // Map symbol for each astral biome, and how much of the outer End it should cover (percent).
    private record BiomeInfo(ResourceKey<Biome> key, char symbol, String name, double minShare, double maxShare, int areaChunks) {}

    private static final List<BiomeInfo> ASTRAL = List.of(
        new BiomeInfo(ModBiomes.NEBULA_FIELDS, 'N', "Nebula Fields", 15, 45, 4),
        new BiomeInfo(ModBiomes.AURORA_SHALLOWS, 'A', "Aurora Shallows", 3, 25, 3),
        new BiomeInfo(ModBiomes.COMET_WAKE, 'W', "Comet Wake", 3, 25, 3),
        new BiomeInfo(ModBiomes.DARK_MATTER_BELT, 'D', "Dark Matter Belt", 2, 20, 3),
        new BiomeInfo(ModBiomes.SHATTERED_OBSERVATORY, 'R', "Shattered Observatory", 0.2, 6, 3),
        new BiomeInfo(ModBiomes.CORONA, 'C', "The Corona", 2, 20, 3));

    public static void register() {
        if (Boolean.getBoolean("astralend.survey")) {
            ServerStartedEvent.BUS.addListener(WorldgenSurvey::run);
        }
    }

    private static void run(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        List<String> failures = new ArrayList<>();
        try {
            endPortal(server, failures);
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
        // The screenshot client (dev/ClientScreenshots) opens this world next. Minecraft asks once per
        // world about "experimental settings" (any datapack-changed dimension counts); answer it here.
        if (server.getWorldData() instanceof net.minecraft.world.level.storage.PrimaryLevelData data) {
            data.withConfirmedWarning(true);
        }
        server.halt(false);
    }

    // ================================================================ 1. Getting to the End

    private static void endPortal(MinecraftServer server, List<String> failures) {
        ServerLevel overworld = server.overworld();
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) {
            failures.add("the End dimension doesn't exist");
            return;
        }
        if (!(end.getChunkSource().getGenerator().getBiomeSource() instanceof AstralEndBiomeSource)) {
            failures.add("the End isn't using the Astral End biome source (is data/minecraft/dimension/the_end.json loaded?)");
        }

        // Eyes of Ender lead to strongholds (the mod doesn't touch the Overworld; this just confirms it).
        BlockPos stronghold = overworld.findNearestMapStructure(StructureTags.EYE_OF_ENDER_LOCATED, BlockPos.ZERO, 100, false);
        log("Nearest stronghold (where Eyes of Ender lead): " + (stronghold == null ? "none found!" : stronghold.toShortString()));
        if (stronghold == null) {
            failures.add("no stronghold found for Eyes of Ender");
        }

        // Step through an End Portal exactly as the portal block does it: ask it for the destination,
        // then teleport. (A spectator can't do this in vanilla: spectators don't touch blocks, so
        // portals never trigger for them.)
        overworld.getChunk(0, 0);
        ArmorStand traveller = new ArmorStand(overworld, 0.5, 100, 0.5);
        overworld.addFreshEntity(traveller);
        TeleportTransition transition = ((Portal) Blocks.END_PORTAL).getPortalDestination(overworld, traveller, BlockPos.containing(0.5, 100, 0.5));
        Entity arrived = transition == null ? null : traveller.teleport(transition);
        if (arrived == null || arrived.level() != end) {
            failures.add("stepping through an End Portal didn't reach the End");
            return;
        }
        BlockPos at = arrived.blockPosition();
        int obsidian = 0;
        int clear = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (end.getBlockState(ServerLevel.END_SPAWN_POINT.offset(dx, -2, dz)).is(Blocks.OBSIDIAN)) {
                    obsidian++;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    if (end.getBlockState(ServerLevel.END_SPAWN_POINT.offset(dx, dy, dz)).isAir()) {
                        clear++;
                    }
                }
            }
        }
        end.getChunk(1, 1);
        int islandTop = end.getHeight(Heightmap.Types.WORLD_SURFACE, 30, 30);
        BlockState islandBlock = end.getBlockState(new BlockPos(30, islandTop - 1, 30));
        log(String.format("End Portal: arrived in the End at %s; obsidian platform %d/25 blocks, clear space above %d/75; "
                + "dragon fight %s; central island surface at (30, %d, 30) is %s",
            at.toShortString(), obsidian, clear, end.getDragonFight() != null ? "set up" : "MISSING", islandTop - 1,
            islandBlock.getBlock().getName().getString()));
        if (Math.abs(at.getX() - 100) > 1 || Math.abs(at.getZ()) > 1) {
            failures.add("arrived at " + at.toShortString() + " instead of the End spawn point (100, 50, 0)");
        }
        if (obsidian < 25 || clear < 75) {
            failures.add("the End arrival platform is incomplete (" + obsidian + "/25 obsidian, " + clear + "/75 air)");
        }
        if (end.getDragonFight() == null) {
            failures.add("the End has no dragon fight");
        }
        if (!islandBlock.is(Blocks.END_STONE)) {
            failures.add("the central island isn't vanilla End Stone at (30, 30): " + islandBlock);
        }
        arrived.discard();
    }

    // ================================================================ 2-4. Biomes

    private static void survey(MinecraftServer server, List<String> failures) {
        ServerLevel end = server.getLevel(Level.END);
        RandomState randomState = end.getChunkSource().randomState();
        ChunkGenerator generator = end.getChunkSource().getGenerator();
        BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(randomState);

        // ---- Biome map ----
        log("Biome map of the End, " + (2 * MAP_RADIUS) + " x " + (2 * MAP_RADIUS) + " blocks, north up, one character = "
            + MAP_STEP + " blocks:");
        log("  O central island   N Nebula Fields   A Aurora Shallows   W Comet Wake   D Dark Matter Belt   "
            + "R Shattered Observatory   C The Corona   H/M/B/s vanilla Highlands/Midlands/Barrens/Small Islands");
        Map<Character, Integer> outerCounts = new HashMap<>();
        Map<Character, Integer> runs = new HashMap<>();
        Map<Character, Integer> continued = new HashMap<>();
        Map<Character, List<BlockPos>> spots = new HashMap<>();
        int outerTotal = 0;
        BlockPos vanillaSpot = null;
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
                    outerCounts.merge(c, 1, Integer::sum);
                    // Coherence: how often a sample has the same biome just to its west.
                    runs.merge(c, 1, Integer::sum);
                    if (previous == c) {
                        continued.merge(c, 1, Integer::sum);
                    }
                }
                if (c == 'O' && distance > 1100) {
                    failures.add("central-island biome found " + (int) distance + " blocks out");
                }
                if (c != 'O' && distance < 1000) {
                    failures.add("non-central biome " + c + " found only " + (int) distance + " blocks from the centre");
                }
                if (distance > 1100 && distance < 9000) {
                    spots.computeIfAbsent(c, k -> new ArrayList<>()).add(new BlockPos(x, 64, z));
                }
                if (vanillaSpot == null && distance > 1500 && (c == 'H' || c == 'M')) {
                    vanillaSpot = new BlockPos(x, 64, z);
                }
                previous = c;
            }
            log("  " + row);
        }
        StringBuilder shares = new StringBuilder("Outer End (beyond 1,100 blocks):");
        for (char c : "NAWDRCHMBs".toCharArray()) {
            shares.append(String.format(" %c %.1f%%", c, 100.0 * outerCounts.getOrDefault(c, 0) / Math.max(1, outerTotal)));
        }
        log(shares.toString());
        for (BiomeInfo info : ASTRAL) {
            double share = 100.0 * outerCounts.getOrDefault(info.symbol(), 0) / Math.max(1, outerTotal);
            double coherence = 100.0 * continued.getOrDefault(info.symbol(), 0) / Math.max(1, runs.getOrDefault(info.symbol(), 0));
            log(String.format("%s: %.1f%% of the outer End, coherence %.0f%% (samples that continue the region of their western neighbour)",
                info.name(), share, coherence));
            if (share < info.minShare() || share > info.maxShare()) {
                failures.add(String.format("%s covers %.1f%% of the outer End (expected %.1f-%.0f%%)", info.name(), share,
                    info.minShare(), info.maxShare()));
            }
            double minCoherence = info.symbol() == 'R' ? 25 : 50;
            if (share > 0 && coherence < minCoherence) {
                failures.add(String.format("%s is scattered, not regions (coherence %.0f%%, expected %.0f%%+)", info.name(),
                    coherence, minCoherence));
            }
        }
        for (char vanilla : new char[] {'H', 'M', 's'}) {
            if (outerCounts.getOrDefault(vanilla, 0) == 0) {
                failures.add("vanilla biome " + vanilla + " no longer appears in the outer End");
            }
        }

        // ---- Generate an area of each astral biome and count what's there ----
        Map<Character, Area> areas = new LinkedHashMap<>();
        for (BiomeInfo info : ASTRAL) {
            BlockPos spot = bestSpot(spots.getOrDefault(info.symbol(), List.of()), end, generator, randomState);
            if (spot == null) {
                failures.add("no " + info.name() + " within 9,000 blocks of the centre");
                continue;
            }
            Area area = scan(end, info, spot);
            areas.put(info.symbol(), area);
            area.print();
        }

        // Minecraft 26.3 decides what "blocks motion" with block tags. If the mod's stone isn't in
        // them, heightmaps see its islands as air and plants, mobs and snow ignore them.
        for (Area area : areas.values()) {
            if (area.standOn != null) {
                int motion = end.getHeight(Heightmap.Types.MOTION_BLOCKING, area.standOn.getX(), area.standOn.getZ());
                BlockState ground = end.getBlockState(area.standOn.below());
                log(String.format("%s: surface at y=%d is %s; the MOTION_BLOCKING heightmap says %d", area.info.name(),
                    area.standOn.getY() - 1, ground.getBlock().getName().getString(), motion));
                if (motion < area.standOn.getY()) {
                    failures.add(area.info.name() + ": " + ground.getBlock().getName().getString()
                        + " doesn't block motion (is it missing from #minecraft:blocks_motion_in_heightmap?)");
                }
            }
        }
        writeViewpoints(areas, end);
        checkNebula(areas.get('N'), failures);
        checkAurora(areas.get('A'), failures);
        checkCometWake(areas.get('W'), failures);
        checkDarkMatter(areas.get('D'), end, failures);
        checkObservatory(areas.get('R'), failures);
        checkCorona(areas.get('C'), failures);
        checkLowGravity(areas.get('N'), vanillaSpot, end, failures);
        checkMobs(areas, end, failures);
    }

    // Every mob can be created in the End, in its home biome, with its attributes, and stays put
    // for a moment without errors; the ones with natural spawns have spawn rules.
    private static void checkMobs(Map<Character, Area> areas, ServerLevel end, List<String> failures) {
        record Home(net.minecraft.world.entity.EntityType<?> type, char biome) {}
        List<Home> homes = List.of(
            new Home(ModEntities.POLLINATOR.get(), 'N'), new Home(ModEntities.VOID_WANDERER.get(), 'R'),
            new Home(ModEntities.UMBRAL_STALKER.get(), 'D'), new Home(ModEntities.CINDER_MITE.get(), 'C'),
            new Home(ModEntities.CORONA_SKIMMER.get(), 'C'), new Home(ModEntities.BLACK_HOLE_GUARDIAN.get(), 'W'),
            new Home(ModEntities.DRIFT_WHALE.get(), 'A'), new Home(ModEntities.WATCHER.get(), 'R'));
        for (Home home : homes) {
            Area area = areas.get(home.biome());
            String name = home.type().getDescription().getString();
            if (area == null || area.standOn == null) {
                failures.add(name + ": no " + home.biome() + " area to test it in");
                continue;
            }
            var entity = home.type().create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (!(entity instanceof net.minecraft.world.entity.Mob mob)) {
                failures.add(name + ": couldn't be created");
                continue;
            }
            mob.snapTo(area.standOn.getX() + 0.5, area.standOn.getY() + 1.0, area.standOn.getZ() + 0.5, 0.0F, 0.0F);
            if (!end.addFreshEntity(mob)) {
                failures.add(name + ": couldn't be added to the End");
                continue;
            }
            try {
                for (int i = 0; i < 40; i++) {
                    mob.tick();
                }
            } catch (RuntimeException e) {
                LOGGER.error(TAG + name + " crashed while ticking", e);
                failures.add(name + ": crashed while ticking: " + e);
            }
            boolean hasHealth = mob.getAttribute(Attributes.MAX_HEALTH) != null && mob.getHealth() > 0;
            log(String.format("Mob %s: created in the %s at %s, health %.0f, still there after 40 ticks: %s", name, area.info.name(),
                mob.blockPosition().toShortString(), mob.getHealth(), !mob.isRemoved()));
            if (!hasHealth) {
                failures.add(name + ": has no health attribute");
            }
            mob.discard();
        }
    }

    // Two camera positions per biome for dev/ClientScreenshots: one looking across the land from
    // just above it, and one high aerial view. Written to astralend-shots.txt as "name x y z yaw pitch".
    private static void writeViewpoints(Map<Character, Area> areas, ServerLevel end) {
        List<String> lines = new ArrayList<>();
        for (Area area : areas.values()) {
            int top = area.surfaceHeightsIn.keySet().stream().max(Integer::compare).orElse(60);
            String slug = area.info.name().toLowerCase().replace("the ", "").replace(' ', '_');
            // Yaw -45 looks south-east (+x, +z), across the area from its north-west corner.
            lines.add(String.format("%s %d %d %d -45 22", slug, area.centre.getX() - 26, top + 10, area.centre.getZ() - 26));
            lines.add(String.format("%s_aerial %d %d %d -45 38", slug, area.centre.getX() - 64, top + 48, area.centre.getZ() - 64));
        }
        // One close-up of each mob, summoned in its home biome (see ClientScreenshots).
        mobShot(lines, areas.get('N'), end, "pollinator", 2.0, 1.0, 1.4);
        mobShot(lines, areas.get('R'), end, "void_wanderer", 2.6, 1.2, 1.7);
        mobShot(lines, areas.get('D'), end, "umbral_stalker angled", 5.0, 0.0, 2.2);
        mobShot(lines, areas.get('D'), end, "umbral_stalker lit,angled", 5.0, 0.0, 2.2);
        mobShot(lines, areas.get('C'), end, "cinder_mite", 1.5, 0.0, 0.7);
        mobShot(lines, areas.get('C'), end, "corona_skimmer", 3.0, 0.0, 1.6);
        mobShot(lines, areas.get('W'), end, "black_hole_guardian", 3.0, 1.6, 2.0);
        mobShot(lines, areas.get('A'), end, "drift_whale side", 11.0, 7.0, 7.5);
        mobShot(lines, areas.get('R'), end, "watcher", 6.0, 0.0, 2.2);
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("astralend-shots.txt"), lines);
            log("Wrote " + lines.size() + " screenshot viewpoints to astralend-shots.txt");
        } catch (java.io.IOException e) {
            LOGGER.error(TAG + "couldn't write viewpoints", e);
        }
    }

    // A viewpoint "mob_<name> x y z yaw pitch <entity> mobX mobY mobZ [lit]": the camera (eyes `eye`
    // blocks above the ground) `distance` blocks north-west of the mob, which stands `lift` blocks
    // above the spot of land the survey stood on in that area.
    private static void mobShot(List<String> lines, Area area, ServerLevel end, String mob, double distance, double lift, double eye) {
        if (area == null) {
            return;
        }
        String[] parts = mob.split(" ");
        if (area.standOn == null) {
            return;
        }
        int x = area.standOn.getX();
        int z = area.standOn.getZ();
        int ground = area.standOn.getY();
        double mobY = ground + lift;
        double d = distance / Math.sqrt(2.0);
        double eyeY = ground + eye;
        double camY = eyeY - 1.62;  // a teleport places the feet; the eyes are 1.62 higher
        double pitch = Math.toDegrees(Math.atan2(eyeY - (mobY + 0.5), distance));
        String name = "mob_" + parts[0] + (parts.length > 1 ? "_" + parts[1].split(",")[0] : "");
        lines.add(String.format(java.util.Locale.ROOT, "%s %.1f %.1f %.1f -45 %.1f astralend:%s %.1f %.1f %.1f%s", name,
            x + 0.5 - d, camY, z + 0.5 - d, pitch, parts[0], x + 0.5, mobY, z + 0.5, parts.length > 1 ? " " + parts[1] : ""));
    }

    // Among the candidate spots nearest the centre, the one with the most land (by terrain height,
    // without generating chunks).
    private static BlockPos bestSpot(List<BlockPos> candidates, ServerLevel end, ChunkGenerator generator, RandomState randomState) {
        List<BlockPos> sorted = new ArrayList<>(candidates);
        sorted.sort((a, b) -> Long.compare(distSqr(a), distSqr(b)));
        BlockPos best = null;
        int bestLand = -1;
        for (BlockPos spot : sorted.subList(0, Math.min(sorted.size(), 40))) {
            int land = 0;
            for (int ox = -48; ox <= 48; ox += 24) {
                for (int oz = -48; oz <= 48; oz += 24) {
                    int h = generator.getBaseHeight(spot.getX() + ox, spot.getZ() + oz, Heightmap.Types.WORLD_SURFACE_WG, end, randomState);
                    if (h > end.getMinY() + 1) {
                        land++;
                    }
                }
            }
            if (land > bestLand) {
                bestLand = land;
                best = spot;
            }
            if (land >= 15) {
                break; // 15 of 25 sample points on land is plenty
            }
        }
        return best;
    }

    // What was found in a generated area. "in" counts only columns whose biome is the surveyed one.
    private static final class Area {
        final BiomeInfo info;
        final BlockPos centre;
        final Map<Block, Integer> in = new HashMap<>();
        final Map<Block, Integer> out = new HashMap<>();
        final Map<Block, Integer> topsIn = new HashMap<>();
        final Map<Integer, Integer> surfaceHeightsIn = new HashMap<>();
        final List<BlockPos> anomalies = new ArrayList<>();
        final List<String> profiles = new ArrayList<>();
        int decayingLeaves;
        BlockPos standOn;
        int columnsIn;
        int landColumnsIn;
        char[][] topView;

        Area(BiomeInfo info, BlockPos centre) {
            this.info = info;
            this.centre = centre;
        }

        int in(Block block) {
            return this.in.getOrDefault(block, 0);
        }

        int out(Block block) {
            return this.out.getOrDefault(block, 0);
        }

        int tops(Block block) {
            return this.topsIn.getOrDefault(block, 0);
        }

        double landChunks() {
            return this.landColumnsIn / 256.0;
        }

        void print() {
            log("Top-down view of " + this.info.name() + " around " + this.centre.getX() + ", " + this.centre.getZ() + " ("
                + this.topView.length + " x " + this.topView.length + " blocks, north up; lower case = outside the biome):");
            for (char[] row : this.topView) {
                log("  |" + new String(row) + "|");
            }
            List<Map.Entry<Block, Integer>> found = new ArrayList<>(this.in.entrySet());
            found.sort((a, b) -> b.getValue() - a.getValue());
            StringBuilder line = new StringBuilder();
            for (Map.Entry<Block, Integer> e : found) {
                line.append(", ").append(e.getKey().getName().getString()).append(' ').append(e.getValue());
            }
            log(String.format("%s: %d columns in the biome, %d with land (~%.0f chunks). Blocks found%s", this.info.name(),
                this.columnsIn, this.landColumnsIn, this.landChunks(), line));
            this.profiles.forEach(p -> log(this.info.name() + " column " + p));
        }
    }

    private static Area scan(ServerLevel end, BiomeInfo info, BlockPos spot) {
        Area area = new Area(info, spot);
        int radius = info.areaChunks();
        int cx = spot.getX() >> 4;
        int cz = spot.getZ() >> 4;
        int size = (2 * radius + 1) * 16;
        area.topView = new char[size][size];
        log("Generating " + (2 * radius + 1) * (2 * radius + 1) + " chunks of " + info.name() + " around " + spot.getX() + ", "
            + spot.getZ() + "...");
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                LevelChunk chunk = end.getChunk(cx + dx, cz + dz);
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int bx = ((cx + dx) << 4) + x;
                        int bz = ((cz + dz) << 4) + z;
                        boolean inside = end.getBiome(new BlockPos(bx, 64, bz)).is(info.key());
                        if (inside) {
                            area.columnsIn++;
                        }
                        Block firstSeen = null;
                        Block top = null;
                        boolean sample = inside && x == 8 && z == 8 && (dx + dz) % 2 == 0 && area.profiles.size() < 8;
                        StringBuilder profile = sample ? new StringBuilder(bx + "," + bz + " solid at y:") : null;
                        int runTop = Integer.MIN_VALUE;
                        for (int y = end.getMaxY(); y >= end.getMinY(); y--) {
                            BlockPos pos = new BlockPos(bx, y, bz);
                            BlockState state = chunk.getBlockState(pos);
                            if (profile != null) {
                                // Record solid runs as "top-bottom" pairs.
                                if (!state.isAir() && runTop == Integer.MIN_VALUE) {
                                    runTop = y;
                                } else if (state.isAir() && runTop != Integer.MIN_VALUE) {
                                    profile.append(' ').append(runTop).append('-').append(y + 1);
                                    runTop = Integer.MIN_VALUE;
                                }
                            }
                            if (state.isAir()) {
                                continue;
                            }
                            Block block = state.getBlock();
                            (inside ? area.in : area.out).merge(block, 1, Integer::sum);
                            if (firstSeen == null) {
                                firstSeen = block;
                            }
                            if (block == ModBlocks.GRAVITY_ANOMALY.get()) {
                                area.anomalies.add(pos);
                            }
                            if (block == ModBlocks.NULLWOOD_LEAVES.get()
                                && state.getValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE) >= 7) {
                                area.decayingLeaves++;
                            }
                            if (top == null && state.isSolidRender()) {
                                top = block;
                                if (inside) {
                                    area.surfaceHeightsIn.merge(y, 1, Integer::sum);
                                    if (area.standOn == null) {
                                        area.standOn = pos.above();
                                    }
                                }
                            }
                        }
                        if (profile != null) {
                            if (runTop != Integer.MIN_VALUE) {
                                profile.append(' ').append(runTop).append('-').append(end.getMinY());
                            }
                            profile.append(", heightmap ").append(end.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz));
                            area.profiles.add(profile.toString());
                        }
                        char symbol = topSymbol(firstSeen);
                        area.topView[((dz + radius) << 4) + z][((dx + radius) << 4) + x] =
                            inside ? symbol : Character.toLowerCase(symbol);
                        if (inside && top != null) {
                            area.landColumnsIn++;
                            area.topsIn.merge(top, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        return area;
    }

    // ---------------------------------------------------------------- per-biome checks

    private static boolean enoughLand(Area area, List<String> failures) {
        if (area == null) {
            return false;
        }
        if (area.landColumnsIn < 300) {
            failures.add("the surveyed " + area.info.name() + " area has almost no land (" + area.landColumnsIn + " columns)");
            return false;
        }
        return true;
    }

    private static void atLeast(Area area, Block block, double perChunk, List<String> failures) {
        int n = area.in(block);
        if (n < Math.max(1, area.landChunks() * perChunk)) {
            failures.add(String.format("%s: too little %s (%d for ~%.0f chunks of land, expected %.2f+/chunk)", area.info.name(),
                block.getName().getString(), n, area.landChunks(), perChunk));
        }
    }

    // The biome's own stone should replace End Stone; a little End Stone at blurred borders is fine.
    private static void replacesEndStone(Area area, Block stone, List<String> failures) {
        int own = area.in(stone);
        int endStone = area.in(Blocks.END_STONE);
        log(String.format("%s: %s inside %d, End Stone inside %d, %s outside the biome %d", area.info.name(),
            stone.getName().getString(), own, endStone, stone.getName().getString(), area.out(stone)));
        if (endStone > 0.02 * Math.max(1, own)) {
            failures.add(String.format("%s: %d End Stone blocks left inside the biome, next to %d %s", area.info.name(), endStone,
                own, stone.getName().getString()));
        }
    }

    // Big landmasses: at least this share of the biome's columns should have land.
    private static void landCoverage(Area area, double minPercent, List<String> failures) {
        double land = 100.0 * area.landColumnsIn / Math.max(1, area.columnsIn);
        log(String.format("%s: land under %.0f%% of the biome", area.info.name(), land));
        if (land < minPercent) {
            failures.add(String.format("%s: only %.0f%% of it is land (expected big islands, %.0f%%+)", area.info.name(), land, minPercent));
        }
    }

    private static void checkNebula(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        landCoverage(area, 45, failures);
        atLeast(area, ModBlocks.LUMINE.log().get(), 2, failures);       // giant Lumineshroom stems
        atLeast(area, ModBlocks.LUMINE_CAP.get(), 8, failures);
        atLeast(area, ModBlocks.NEBULA_TUFTS.get(), 20, failures);
        atLeast(area, ModBlocks.NEBULA_TENDRIL.get(), 1, failures);
        // Where the ground itself is the top block (not a giant mushroom's cap), it should be moss.
        int mossTops = area.tops(ModBlocks.NEBULA_MOSS.get());
        int groundTops = mossTops + area.tops(Blocks.END_STONE);
        if (mossTops < 0.9 * groundTops) {
            failures.add("Nebula Fields: only " + mossTops + " of " + groundTops + " bare-ground columns are topped with Nebula Moss");
        }
        int chorus = area.in(Blocks.CHORUS_PLANT) + area.in(Blocks.CHORUS_FLOWER);
        if (chorus > 0) {
            failures.add("Nebula Fields: " + chorus + " chorus blocks grew (they need bare End Stone)");
        }
        if (area.out(ModBlocks.NEBULA_MOSS.get()) > 0.01 * Math.max(1, area.in(ModBlocks.NEBULA_MOSS.get()))) {
            failures.add("Nebula Moss generated outside Nebula Fields (more than border blending explains)");
        }
        atLeast(area, ModBlocks.LUMINESHROOM.get(), 2, failures);
        atLeast(area, ModBlocks.STARBLOOM.get(), 1, failures);
        atLeast(area, ModBlocks.ASTRAL_VINE.get(), 0.3, failures);
    }

    private static void checkAurora(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.PALE_END_STONE.get(), failures);
        landCoverage(area, 40, failures);
        atLeast(area, ModBlocks.SHIMMER_GRASS.get(), 15, failures);
        atLeast(area, ModBlocks.SHIMMER_REED.get(), 1, failures);
        atLeast(area, ModBlocks.PALE_END_STONE_BRICKS.get(), 0.05, failures);  // standing stones
        // Flat shelves: most of the land should be topped at the same height.
        int flat = 0;
        for (int y = 52; y <= 55; y++) {
            flat += area.surfaceHeightsIn.getOrDefault(y, 0);
        }
        double flatShare = 100.0 * flat / Math.max(1, area.landColumnsIn);
        log(String.format("Aurora Shallows: %.0f%% of the land is a flat shelf topped at y=52-55; Leviathan Bone ribs: %d blocks",
            flatShare, area.in(ModBlocks.LEVIATHAN_BONE_BLOCK.get())));
        if (flatShare < 60) {
            failures.add(String.format("Aurora Shallows aren't flat shelves: only %.0f%% of the land is topped at y=52-55", flatShare));
        }
    }

    private static void checkCometWake(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.CHARRED_END_STONE.get(), failures);
        atLeast(area, ModBlocks.STARDUST_BLOCK.get(), 1, failures);
        atLeast(area, ModBlocks.COMET_ICE.get(), 0.5, failures);
        atLeast(area, ModBlocks.METEORIC_IRON_ORE.get(), 0.02, failures);   // comet cores
        atLeast(area, ModBlocks.COMET_CRESS.get(), 0.2, failures);
    }

    private static void checkDarkMatter(Area area, ServerLevel end, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.UMBRAL_STONE.get(), failures);
        atLeast(area, ModBlocks.VOID_CRYSTAL_ORE.get(), 0.5, failures);
        atLeast(area, ModBlocks.NULLWOOD.log().get(), 15, failures);
        atLeast(area, ModBlocks.NULLWOOD_LEAVES.get(), 40, failures);
        atLeast(area, ModBlocks.NULL_LANTERN.get(), 2, failures);
        int leaves = area.in(ModBlocks.NULLWOOD_LEAVES.get());
        log(String.format("Dark Matter Belt: %d of %d Nullwood leaves are too far from a log and would decay", area.decayingLeaves, leaves));
        if (area.decayingLeaves > 0.05 * Math.max(1, leaves)) {
            failures.add("Dark Matter Belt: " + area.decayingLeaves + " Nullwood leaves would decay (trees would lose their leaves)");
        }
        log("Dark Matter Belt: " + area.anomalies.size() + " gravity inversion pockets");
        if (area.anomalies.isEmpty()) {
            failures.add("Dark Matter Belt: no gravity inversion pockets generated");
            return;
        }
        // Put a test mob in the middle of a pocket and let the Gravity Anomaly do its work.
        BlockPos core = area.anomalies.get(0);
        if (!(end.getBlockEntity(core) instanceof GravityAnomalyBlockEntity)) {
            failures.add("the Gravity Anomaly at " + core.toShortString() + " has no block entity");
        }
        var centre = GravityAnomalyBlockEntity.centre(core);
        ArmorStand probe = new ArmorStand(end, centre.x, centre.y - 0.5, centre.z);
        end.addFreshEntity(probe);
        GravityAnomalyBlockEntity.invertInside(end, core);
        double inside = probe.getAttributeValue(Attributes.GRAVITY);
        log(String.format("Gravity in the pocket under %s: %.4f (vanilla 0.0800; inverted is negative)", core.toShortString(), inside));
        if (!GravityInversion.isInverted(probe) || Math.abs(inside - 0.08 * (1 + GravityInversion.GRAVITY_CHANGE)) > 0.001) {
            failures.add("gravity wasn't inverted inside a pocket (" + inside + ")");
        }
        probe.discard();
    }

    private static void checkObservatory(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        landCoverage(area, 40, failures);
        atLeast(area, ModBlocks.OBSERVATORY_QUARTZ.get(), 1, failures);
        atLeast(area, ModBlocks.OBSERVATORY_QUARTZ_PILLAR.get(), 0.3, failures);
        atLeast(area, ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get(), 0.05, failures);
        atLeast(area, ModBlocks.STARBLOOM.get(), 0.5, failures);
    }

    private static void checkCorona(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        int scorchedTops = area.tops(ModBlocks.SUNSCORCHED_STONE.get()) + area.tops(ModBlocks.SOLARITE_ORE.get());
        if (scorchedTops < 0.8 * area.landColumnsIn) {
            failures.add("The Corona: only " + scorchedTops + " of " + area.landColumnsIn + " land columns are topped with Sunscorched Stone");
        }
        replacesEndStone(area, ModBlocks.CHARRED_END_STONE.get(), failures);
        landCoverage(area, 45, failures);
        atLeast(area, ModBlocks.PETRIFIED_STALK.get(), 2, failures);
        atLeast(area, ModBlocks.SOLARITE_ORE.get(), 1, failures);
        atLeast(area, ModBlocks.FLAREBLOOM.get(), 2, failures);
        atLeast(area, ModBlocks.SUNSCALE_LICHEN.get(), 15, failures);
        atLeast(area, ModBlocks.ASHEN_LUMINESHROOM.get(), 0.2, failures);
        int chorus = area.in(Blocks.CHORUS_PLANT) + area.in(Blocks.CHORUS_FLOWER);
        if (chorus > 0) {
            failures.add("The Corona: " + chorus + " chorus blocks grew there");
        }
    }

    private static void checkLowGravity(Area nebula, BlockPos vanillaSpot, ServerLevel end, List<String> failures) {
        if (nebula == null || nebula.standOn == null) {
            failures.add("found no Nebula Fields surface to test low gravity on");
            return;
        }
        if (vanillaSpot == null) {
            failures.add("found no vanilla outer biome to test normal gravity in");
            return;
        }
        ArmorStand probe = new ArmorStand(end, nebula.standOn.getX() + 0.5, nebula.standOn.getY(), nebula.standOn.getZ() + 0.5);
        LowGravity.update(probe);
        double inside = probe.getAttributeValue(Attributes.GRAVITY);
        end.getChunk(vanillaSpot.getX() >> 4, vanillaSpot.getZ() >> 4);
        probe.setPos(vanillaSpot.getX() + 0.5, 70, vanillaSpot.getZ() + 0.5);
        LowGravity.update(probe);
        double outside = probe.getAttributeValue(Attributes.GRAVITY);
        log(String.format("Gravity inside Nebula Fields: %.4f; after moving to %d, %d (a vanilla biome): %.4f (vanilla is 0.0800)",
            inside, vanillaSpot.getX(), vanillaSpot.getZ(), outside));
        if (Math.abs(inside - 0.08 * (1 + LowGravity.GRAVITY_CHANGE)) > 0.001) {
            failures.add("low gravity not applied inside Nebula Fields (" + inside + ")");
        }
        if (Math.abs(outside - 0.08) > 0.001) {
            failures.add("low gravity not removed after leaving Nebula Fields (" + outside + ")");
        }
    }

    // ---------------------------------------------------------------- symbols

    private static final Map<Block, Character> TOP_SYMBOLS = new HashMap<>();

    // What you'd see looking straight down (tools/render_survey.py has the colour for each).
    private static char topSymbol(Block block) {
        if (block == null) return ' ';
        if (TOP_SYMBOLS.isEmpty()) {
            TOP_SYMBOLS.put(Blocks.END_STONE, 'E');
            TOP_SYMBOLS.put(Blocks.CHORUS_PLANT, 'Y');
            TOP_SYMBOLS.put(Blocks.CHORUS_FLOWER, 'Y');
            TOP_SYMBOLS.put(ModBlocks.NEBULA_MOSS.get(), 'M');
            TOP_SYMBOLS.put(ModBlocks.LUMINESHROOM.get(), 'L');
            TOP_SYMBOLS.put(ModBlocks.STARBLOOM.get(), 'S');
            TOP_SYMBOLS.put(ModBlocks.ASTRAL_VINE.get(), 'V');
            TOP_SYMBOLS.put(ModBlocks.ASTRAL_VINE_PLANT.get(), 'V');
            TOP_SYMBOLS.put(ModBlocks.PALE_END_STONE.get(), 'P');
            TOP_SYMBOLS.put(ModBlocks.SHIMMER_GRASS.get(), 'G');
            TOP_SYMBOLS.put(ModBlocks.PALE_END_STONE_BRICKS.get(), 'B');
            TOP_SYMBOLS.put(ModBlocks.PALE_END_STONE_TILES.get(), 'B');
            TOP_SYMBOLS.put(ModBlocks.LEVIATHAN_BONE_BLOCK.get(), 'J');
            TOP_SYMBOLS.put(ModBlocks.CHARRED_END_STONE.get(), 'H');
            TOP_SYMBOLS.put(ModBlocks.COMET_ICE.get(), 'I');
            TOP_SYMBOLS.put(ModBlocks.STARDUST_BLOCK.get(), 'T');
            TOP_SYMBOLS.put(ModBlocks.METEORIC_IRON_ORE.get(), 'O');
            TOP_SYMBOLS.put(ModBlocks.UMBRAL_STONE.get(), 'U');
            TOP_SYMBOLS.put(ModBlocks.VOID_CRYSTAL_ORE.get(), 'X');
            TOP_SYMBOLS.put(ModBlocks.GRAVITY_ANOMALY.get(), 'Z');
            TOP_SYMBOLS.put(ModBlocks.OBSERVATORY_QUARTZ.get(), 'Q');
            TOP_SYMBOLS.put(ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get(), 'Q');
            TOP_SYMBOLS.put(ModBlocks.OBSERVATORY_QUARTZ_PILLAR.get(), 'K');
            TOP_SYMBOLS.put(ModBlocks.SUNSCORCHED_STONE.get(), 'C');
            TOP_SYMBOLS.put(ModBlocks.SOLARITE_ORE.get(), 'R');
            TOP_SYMBOLS.put(ModBlocks.FLAREBLOOM.get(), 'F');
            TOP_SYMBOLS.put(ModBlocks.SUNSCALE_LICHEN.get(), 'N');
            TOP_SYMBOLS.put(ModBlocks.ASHEN_LUMINESHROOM.get(), 'A');
            TOP_SYMBOLS.put(ModBlocks.PETRIFIED_ASTRAL_VINE.get(), 'D');
            TOP_SYMBOLS.put(ModBlocks.POLLINATOR_HUSK.get(), 'W');
            TOP_SYMBOLS.put(ModBlocks.LUMINE_CAP.get(), 'L');
            TOP_SYMBOLS.put(ModBlocks.NEBULA_TUFTS.get(), 'M');
            TOP_SYMBOLS.put(ModBlocks.NULLWOOD_LEAVES.get(), 'V');
            TOP_SYMBOLS.put(ModBlocks.NULLWOOD.log().get(), 'U');
            TOP_SYMBOLS.put(ModBlocks.SHIMMER_REED.get(), 'G');
            TOP_SYMBOLS.put(ModBlocks.PETRIFIED_STALK.get(), 'D');
        }
        return TOP_SYMBOLS.getOrDefault(block, '#');
    }

    private static char symbol(Holder<Biome> biome) {
        if (biome.is(ModBiomes.NEBULA_FIELDS)) return 'N';
        if (biome.is(ModBiomes.AURORA_SHALLOWS)) return 'A';
        if (biome.is(ModBiomes.COMET_WAKE)) return 'W';
        if (biome.is(ModBiomes.DARK_MATTER_BELT)) return 'D';
        if (biome.is(ModBiomes.SHATTERED_OBSERVATORY)) return 'R';
        if (biome.is(ModBiomes.CORONA)) return 'C';
        if (biome.is(Biomes.THE_END)) return 'O';
        if (biome.is(Biomes.END_HIGHLANDS)) return 'H';
        if (biome.is(Biomes.END_MIDLANDS)) return 'M';
        if (biome.is(Biomes.END_BARRENS)) return 'B';
        if (biome.is(Biomes.SMALL_END_ISLANDS)) return 's';
        return '?';
    }

    private static long distSqr(BlockPos pos) {
        return (long) pos.getX() * pos.getX() + (long) pos.getZ() * pos.getZ();
    }

    private static void log(String line) {
        LOGGER.info(TAG + line);
    }

    private WorldgenSurvey() {}
}
