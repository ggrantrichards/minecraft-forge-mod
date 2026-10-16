package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.block.UpwellCoreBlockEntity;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.world.AstralEndBiomeSource;
import com.ggrantrichards.astralend.world.Upwells;
import com.ggrantrichards.astralend.world.LowGravity;
import com.ggrantrichards.astralend.world.EclipseLayout;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

// Development tool, not part of normal play. When the server is started with
// -Dastralend.survey=true (./gradlew runServer -PastralendSurvey), this:
//   1. travels through a real End Portal (the way a stronghold's portal works) and checks the
//      arrival platform, the dragon fight and the central island,
//   2. draws a text map of the End's biomes and measures how much each biome covers,
//   3. generates an area inside every astral biome and counts every block, checking that each
//      biome has its stone, ores, plants and features,
//   4. checks low gravity (Nebula Fields) and the Upwells (Deep Hollows),
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
        new BiomeInfo(ModBiomes.NEBULA_FIELDS, 'N', "Nebula Fields", 8, 45, 4),
        new BiomeInfo(ModBiomes.GLIMMERING_CRAGS, 'G', "Glimmering Crags", 1, 15, 3),
        new BiomeInfo(ModBiomes.ASHEN_BARRENS, 'A', "Ashen Barrens", 3, 25, 3),
        new BiomeInfo(ModBiomes.DEEP_HOLLOWS, 'D', "Deep Hollows", 2, 25, 3),
        // The Eclipse: rare, and small (360 blocks across) - one golden meadow.
        new BiomeInfo(ModBiomes.ECLIPSE, 'E', "The Gilded Reach", 0.04, 4, 3),
        // The Crags' variant: patches inside their territory.
        new BiomeInfo(ModBiomes.SHATTERED_CRAGS, 'X', "Shattered Crags", 0.1, 8, 3));

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
        // Stop once the server has ticked a while. The survey generates hundreds of chunks before the
        // first tick; halting with all their follow-up work still queued runs that work inline while
        // stopping, which overflows the stack and loses the save (and with it the answer above).
        surveyed = server;
        TickEvent.ServerTickEvent.Post.BUS.addListener(WorldgenSurvey::stopWhenSettled);
    }

    private static MinecraftServer surveyed;
    private static int ticksAfterSurvey;

    private static void stopWhenSettled(TickEvent.ServerTickEvent.Post event) {
        if (surveyed != null && ++ticksAfterSurvey == 200) {
            log("ten seconds of ticks after the survey; stopping (stopping saves the world)");
            surveyed.halt(false);
        }
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
        log("  O central island   N Nebula Fields   A Ashen Barrens   D Deep Hollows   "
            + "E The Gilded Reach   "
            + "G Glimmering Crags   X Shattered Crags   "
            + "H/M/B/s vanilla Highlands/Midlands/Barrens/Small Islands");
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
        for (char c : "NATDVEGXHMBs".toCharArray()) {
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
            // (Variants are patches inside their parents' territories, so they're allowed to be smaller.)
            double minCoherence = info.symbol() == 'X' ? 35 : 50;
            boolean small = info.symbol() == 'E';   // the Eclipse is too small for the map's grid
            if (share > 0 && coherence < minCoherence && !small) {
                failures.add(String.format("%s is scattered, not regions (coherence %.0f%%, expected %.0f%%+)", info.name(),
                    coherence, minCoherence));
            }
        }
        for (char vanilla : new char[] {'H', 'M', 's'}) {
            if (outerCounts.getOrDefault(vanilla, 0) == 0) {
                failures.add("vanilla biome " + vanilla + " no longer appears in the outer End");
            }
        }

        // The vanilla noises the regions are drawn from (compared with the 1.21.1 port's).
        var noises = end.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.NOISE);
        for (String name : new String[] {"temperature", "vegetation", "ridge", "continentalness", "erosion"}) {
            var params = noises.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(name));
            if (params != null) {
                log("noise minecraft:" + name + ": " + params);
            }
        }
        // ---- Generate an area of each astral biome and count what's there ----
        // The Eclipse is surveyed at a known spot of the nearest one, between the Wellspring and the rim.
        EclipseLayout.Eclipse eclipse = EclipseLayout.nearest(0, 0, EclipseLayout.sampler(randomState), 3);
        if (eclipse == null) {
            failures.add("no Eclipse within 3 cells of the centre");
        } else {
            log(String.format("Nearest Eclipse: centre %.0f, %.0f, turned %.0f degrees", eclipse.x(), eclipse.z(),
                Math.toDegrees(eclipse.angle())));
            spots.put('E', List.of(BlockPos.containing(eclipse.worldX(0.5, 0.3), 64, eclipse.worldZ(0.5, 0.3))));
        }
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
        Map<String, BlockPos> structures = checkStructures(areas, end, failures);
        testLeviathan(end, failures);   // the distillation, phase 5
        if (areas.get('N') != null && areas.get('N').standOn != null) {
            EXTRA_SHOTS.addAll(StarforgeSurvey.check(end, areas.get('N').standOn, failures, WorldgenSurvey::log));   // Phase 8
            PolishSurvey.check(end, areas.get('N').standOn, failures, WorldgenSurvey::log);         // Phase 9
            MechanicsSurvey.check(end, areas.get('N').standOn, failures, WorldgenSurvey::log);      // Phase 10
            MechanicsSurvey.checkContent(end, areas.get('N').standOn, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkSystems(end, areas.get('N').standOn, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkNewMobs(end, areas.get('N').standOn, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkWebOfLife(end, areas.get('N').standOn, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkLoot(end, areas.get('N').standOn, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkSpawnEggs(end, areas.get('N').standOn, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkHandBuiltPortal(end, failures, WorldgenSurvey::log);
            MechanicsSurvey.checkWyrmFight(end, failures, WorldgenSurvey::log);
        }
        checkCaves(areas.get('D'), areas.get('X'), end, failures);   // (before the viewpoints: it adds the caves' shots)
        writeViewpoints(areas, end, structures);
        // Cut-outs for the website's dioramas: a piece of each biome, and each structure.
        for (Area area : areas.values()) {
            if (area.standOn != null) {
                Dioramas.biome(end, "biome_" + area.info.name().toLowerCase(java.util.Locale.ROOT).replace("the ", "").replace(' ', '_'), area.standOn);
            }
        }
        for (String s : List.of("comet_crater", "shattered_observatory", "solar_array")) {
            BlockPos marker = structures.getOrDefault(s + "_hot", structures.get(s));
            if (marker != null) {
                Dioramas.around(end, s, marker, 34, 26, 40);
            }
        }
        checkNebula(areas.get('N'), failures);
        checkAurora(areas.get('A'), failures);
        checkDeepHollows(areas.get('D'), end, failures);
        checkEclipse(areas.get('E'), failures);
        if (eclipse != null) {
            testEclipse(end, eclipse, failures);
        }
        checkCrags(areas.get('G'), end, failures);
        checkShatteredCrags(areas.get('X'), failures);
        checkLowGravity(areas.get('N'), vanillaSpot, end, failures);
        checkMobs(areas, end, failures);
    }

    // Every mob can be created in the End, in its home biome, with its attributes, and stays put
    // for a moment without errors; the ones with natural spawns have spawn rules.
    private static void checkMobs(Map<Character, Area> areas, ServerLevel end, List<String> failures) {
        record Home(net.minecraft.world.entity.EntityType<?> type, char biome) {}
        List<Home> homes = List.of(
            new Home(ModEntities.POLLINATOR.get(), 'N'),
            new Home(ModEntities.UMBRAL_STALKER.get(), 'G'),
            new Home(ModEntities.DRIFT_WHALE.get(), 'A'), new Home(ModEntities.WATCHER.get(), 'N'),
            new Home(ModEntities.SPIRE_WYRM.get(), 'G'), new Home(ModEntities.HUSHBACK.get(), 'E'),
            new Home(ModEntities.HYDRAGRIP.get(), 'E'), new Home(ModEntities.MIRROR_WRETCH.get(), 'A'));
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

    // A sky view from just above an area's centre, looking the given way. Written as
    // "name x y z yaw pitch style".
    private static void skyShot(List<String> lines, Area area, String name, float yaw, float pitch, String style) {
        if (area == null) {
            return;
        }
        // From above the area's highest ground, so no cliff can be in the way.
        int top = area.surfaceHeightsIn.keySet().stream().max(Integer::compare).orElse(60);
        BlockPos at = new BlockPos(area.centre.getX(), top + 12, area.centre.getZ());
        lines.add(String.format(java.util.Locale.ROOT, "%s %d %d %d %.1f %.1f %s", name, at.getX(), at.getY(), at.getZ(), yaw, pitch, style));
    }

    // Two camera positions per biome for dev/ClientScreenshots: one looking across the land from
    // just above it, and one high aerial view. Written to astralend-shots.txt as "name x y z yaw pitch".
    private static void writeViewpoints(Map<Character, Area> areas, ServerLevel end, Map<String, BlockPos> structures) {
        List<String> lines = new ArrayList<>();
        for (Area area : areas.values()) {
            int top = area.surfaceHeightsIn.keySet().stream().max(Integer::compare).orElse(60);
            String slug = area.info.name().toLowerCase().replace("the ", "").replace(' ', '_');
            EclipseLayout.Eclipse eclipse = area.info.key() == ModBiomes.ECLIPSE
                ? EclipseLayout.nearest(area.centre.getX(), area.centre.getZ(), EclipseLayout.sampler(end.getChunkSource().randomState()), 1) : null;
            if (eclipse != null) {
                // The Eclipse: from the spot's outer side, looking in across the disc (from anywhere else the
                // view runs off its edge into the void).
                double dx = eclipse.x() - area.centre.getX(), dz = eclipse.z() - area.centre.getZ(), len = Math.max(1.0, Math.hypot(dx, dz));
                double yaw = Math.toDegrees(Math.atan2(-dx, dz));
                for (int k = 0; k < 2; k++) {
                    double back = k == 0 ? 30 : 70;
                    int x = (int) Math.round(area.centre.getX() - dx / len * back), z = (int) Math.round(area.centre.getZ() - dz / len * back);
                    int ground = Math.max(end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), (int) com.ggrantrichards.astralend.world.feature.EclipseTerrainFeature.TOP);
                    lines.add(String.format(java.util.Locale.ROOT, "%s%s %d %d %d %.1f %d", slug, k == 0 ? "" : "_aerial", x, ground + (k == 0 ? 8 : 40), z,
                        yaw, k == 0 ? 16 : 34));
                }
                continue;
            }
            // Yaw -45 looks south-east (+x, +z), across the land from north-west of a spot on it, at the
            // height most of the land's surface is at (not its highest point: a Leviathan floating over the
            // Ashen Barrens put the camera in the sky, looking at the Leviathan instead of the biome).
            int typical = area.surfaceHeightsIn.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(top);
            BlockPos anchor = area.standOn != null ? area.standOn : area.centre;
            lines.add(String.format("%s %d %d %d -45 20", slug, anchor.getX() - 22, typical + 9, anchor.getZ() - 22));
            lines.add(String.format("%s_aerial %d %d %d -45 36", slug, anchor.getX() - 60, typical + 46, anchor.getZ() - 60));
        }
        // One close-up of each mob, summoned in its home biome (see ClientScreenshots).
        mobShot(lines, areas.get('N'), end, "pollinator", 2.0, 1.0, 1.4);
        mobShot(lines, areas.get('D'), end, "umbral_stalker angled", 5.5, 0.0, 1.9);
        mobShot(lines, areas.get('D'), end, "umbral_stalker lit,angled", 5.5, 0.0, 1.9);
        mobShot(lines, areas.get('A'), end, "drift_whale side", 11.0, 7.0, 7.5);
        mobShot(lines, areas.get('N'), end, "watcher", 6.5, 0.5, 2.6);
        mobShot(lines, areas.get('G'), end, "spire_wyrm", 16.0, 6.0, 5.0);
        mobShot(lines, areas.get('E'), end, "hushback", 3.2, 0.5, 1.4);
        mobShot(lines, areas.get('E'), end, "hydragrip angled", 3.4, 0.0, 1.2);
        mobShot(lines, areas.get('A'), end, "mirror_wretch angled", 5.0, 0.0, 1.6);
        mobShot(lines, areas.get('A'), end, "mirror_wretch reared,angled", 6.0, 0.0, 2.0);
        mobShot(lines, areas.get('A'), end, "hollowback", 3.2, 0.5, 1.4);
        mobShot(lines, areas.get('A'), end, "hollowback reared,angled", 3.0, 0.5, 1.2);   // its jaw open on its fangs
        mobShot(lines, areas.get('A'), end, "curator", 8.0, 0.5, 3.2);
        mobShot(lines, areas.get('A'), end, "curator awake,angled", 7.0, 0.5, 2.8);
        mobShot(lines, areas.get('A'), end, "curator exposed,awake", 6.0, 0.5, 2.8);
        mobShot(lines, areas.get('D'), end, "shardback angled", 3.4, 0.0, 1.4);
        // Phase 5: the sky and Sunstorms.
        skyShot(lines, areas.get('A'), "sky_ashen_barrens", -45.0F, -32.0F, "flare=calm");
        skyShot(lines, areas.get('N'), "sky_nebula_fields", -45.0F, -32.0F, "flare=calm");
        // (The astral sky shows only over the Eclipse: up at its star - south-west, 62 degrees up -
        // brightening before a Sunstorm, with the gold clouds; and in a Sunstorm, the sky gone gold.)
        skyShot(lines, areas.get('E'), "sky_eclipse_star", 125.0F, -40.0F, "flare=warning");
        skyShot(lines, areas.get('E'), "sky_eclipse_flare", -45.0F, -18.0F, "flare=flare");
        mobShot(lines, areas.get('E'), end, "display flare=flare", 5.5, 0.0, 2.6);
        // Phase 6: each structure, from above and to the north-west of the block the survey found in it.
        structureShot(lines, structures, "comet_crater", 18.0, 17.0, 2.0);
        structureShot(lines, structures, "comet_crater_hot", 18.0, 17.0, 2.0);
        structureShot(lines, structures, "shattered_observatory", 46.0, 26.0, 4.0);
        structureShot(lines, structures, "solar_array", 34.0, 36.0, 4.0);
        structureShot(lines, structures, "solar_array_room", 3.0, 2.2, 0.0);
        borderShot(lines, areas.get('N'), end);
        checkBlend(areas.get('N'), end);
        lines.addAll(EXTRA_SHOTS);
        try {
            java.nio.file.Files.write(java.nio.file.Path.of("astralend-shots.txt"), lines);
            log("Wrote " + lines.size() + " screenshot viewpoints to astralend-shots.txt");
        } catch (java.io.IOException e) {
            LOGGER.error(TAG + "couldn't write viewpoints", e);
        }
    }

    // Extra screenshot lines worked out by the structure tests (they need the structure's layout).
    private static final List<String> EXTRA_SHOTS = new ArrayList<>();

    // The Eclipse, the golden meadow: mostly gold Living Turf (moss by its water, bare End Stone outcrops),
    // Breathcomb, giant Lumineshrooms to shelter under, and its accents in moderation - a few clumps of
    // Flareblooms, Sunscale Lichen on the outcrops. Nothing of the old hell half.
    private static void checkEclipse(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        atLeast(area, Blocks.END_STONE, 400, failures);
        int turf = area.tops(ModBlocks.LIVING_TURF.get()), moss = area.tops(Blocks.MOSS_BLOCK), stone = area.tops(Blocks.END_STONE);
        log(String.format("The Gilded Reach: of %d land columns, %d topped with Living Turf, %d with moss, %d with End Stone",
            area.landColumnsIn, turf, moss, stone));
        if (turf < area.landColumnsIn * 0.45) {
            failures.add("The Gilded Reach should be a meadow: only " + turf + " of " + area.landColumnsIn + " land columns are Living Turf");
        }
        atLeast(area, ModBlocks.BREATHCOMB.get(), 1, failures);
        atLeast(area, ModBlocks.LUMINE_CAP.get(), 5, failures);
        atLeast(area, ModBlocks.FLAREBLOOM.get(), 0.3, failures);
        atLeast(area, ModBlocks.SUNSCALE_LICHEN.get(), 1, failures);
        int hell = area.in(ModBlocks.SUNSCORCHED_STONE.get()) + area.in(ModBlocks.CHARRED_END_STONE.get()) + area.in(Blocks.OBSIDIAN);
        log("The Gilded Reach: " + hell + " blocks of the old hell half (Sunscorched Stone, Charred End Stone, Obsidian)");
        if (hell > 40) {
            failures.add("The Gilded Reach: " + hell + " blocks of Sunscorched Stone, Charred End Stone or Obsidian (the hell half is gone)");
        }
    }

    // The Eclipse's landmarks: the Wellspring (a basin of water on a bed of End Stone), and the whole disc one
    // biome.
    private static void testEclipse(ServerLevel end, EclipseLayout.Eclipse e, List<String> failures) {
        double wx = e.worldX(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        double wz = e.worldZ(EclipseLayout.WELLSPRING[0], EclipseLayout.WELLSPRING[1]);
        int cx = (int) Math.floor(wx) >> 4, cz = (int) Math.floor(wz) >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                end.getChunk(cx + dx, cz + dz);
            }
        }
        int water = 0, bed = 0, glass = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double r = EclipseLayout.WELLSPRING_RADIUS * EclipseLayout.RADIUS - 1.5;
        for (int x = (int) (wx - r); x <= (int) (wx + r); x++) {
            for (int z = (int) (wz - r); z <= (int) (wz + r); z++) {
                if ((x - wx) * (x - wx) + (z - wz) * (z - wz) > r * r) {
                    continue;
                }
                for (int y = end.getMinY(); y < end.getMinY() + 128; y++) {
                    BlockState state = end.getBlockState(p.set(x, y, z));
                    if (state.is(Blocks.WATER)) {
                        water++;
                    } else if (state.is(ModBlocks.ASTRAL_GLASS.get())) {
                        glass++;
                    } else if (state.is(ModBlocks.PALE_END_STONE.get()) || state.is(Blocks.END_STONE)) {
                        bed++;
                    }
                }
            }
        }
        log(String.format("The Wellspring at %.0f, %.0f: %d water, on a bed of %d End Stone (%d glass)", wx, wz, water, bed, glass));
        if (water < 2000 || bed < 1500 || glass > 0) {
            failures.add("the Wellspring should be a basin of water on a bed of End Stone and Pale End Stone (" + water + " water, "
                + bed + " bed, " + glass + " glass)");
        }
        // The whole disc is the Eclipse (the biome map is warped near its rim, so test well inside).
        var resolver = end.getChunkSource().getGenerator().getBiomeSource().createUncachedResolver(end.getChunkSource().randomState());
        int right = 0, total = 0;
        for (double u = -0.7; u <= 0.71; u += 0.35) {
            for (double v = -0.7; v <= 0.71; v += 0.35) {
                if (u * u + v * v > 0.55) {
                    continue;
                }
                Holder<Biome> b = resolver.getNoiseBiome(QuartPos.fromBlock((int) e.worldX(u, v)), QuartPos.fromBlock(64),
                    QuartPos.fromBlock((int) e.worldZ(u, v)));
                total++;
                if (b.is(ModBiomes.ECLIPSE)) {
                    right++;
                }
            }
        }
        log("The Eclipse: " + right + " of " + total + " test points across the disc are the Eclipse");
        if (right < total) {
            failures.add("the Eclipse should be one biome across its disc: " + (total - right) + " of " + total + " test points aren't");
        }
        testSunstormLife(end, e, wx, wz, failures);
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "eclipse_overview %.1f %d %.1f %.1f 50.0", e.x() - 190, 190, e.z() - 190, -45.0));
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "eclipse_wellspring %.1f %d %.1f %.1f 25.0", wx - 26, 72, wz - 26, -45.0));
    }

    // The web of life in a Sunstorm, in the Gilded Reach itself: Hushbacks find cover, Hydragrips find
    // the water, pods swim out of the Reach; and endermites creep after the herd.
    private static void testSunstormLife(ServerLevel end, EclipseLayout.Eclipse e, double wx, double wz, List<String> failures) {
        var storms = com.ggrantrichards.astralend.flare.Sunstorms.get(end);
        var was = storms.phase();
        double r = EclipseLayout.WELLSPRING_RADIUS * EclipseLayout.RADIUS + 8.0;
        int mx = (int) Math.floor(wx + r), mz = (int) Math.floor(wz);
        BlockPos meadow = new BlockPos(mx, end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, mx, mz), mz);
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        List<BlockPos> roof = new ArrayList<>();
        try {
            storms.setPhase(end, com.ggrantrichards.astralend.flare.FlarePhase.FLARE, end.getRandom());
            boolean stormy = com.ggrantrichards.astralend.entity.EndMobs.stormy(end, meadow);
            // A roof four blocks off: cover the herd can find.
            BlockPos shelter = meadow.offset(0, 0, 5);
            shelter = new BlockPos(shelter.getX(), end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, shelter.getX(), shelter.getZ()), shelter.getZ());
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos b = shelter.offset(dx, 3, dz);
                    if (end.getBlockState(b).isAir()) {
                        end.setBlock(b, Blocks.END_STONE.defaultBlockState(), 3);
                        roof.add(b);
                    }
                }
            }
            BlockPos cover = com.ggrantrichards.astralend.entity.EndMobs.coverNear(end, meadow, 12, end.getRandom());
            BlockPos water = com.ggrantrichards.astralend.entity.EndMobs.waterNear(end, meadow, 16);
            var whale = ModEntities.DRIFT_WHALE.get().create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            whale.snapTo(meadow.getX() + 0.5, meadow.getY() + 30, meadow.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(whale);
            made.add(whale);
            var out = whale.outOfTheStorm(end);
            boolean outward = out != null && out.x * (whale.getX() - e.x()) + out.z * (whale.getZ() - e.z()) > 0.0;
            var herd = ModEntities.HUSHBACK.get().create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            herd.snapTo(meadow.getX() + 0.5, meadow.getY(), meadow.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(herd);
            made.add(herd);
            var mite = net.minecraft.world.entity.EntityTypes.ENDERMITE.create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            mite.snapTo(meadow.getX() + 3.5, meadow.getY(), meadow.getZ() + 0.5, 0.0F, 0.0F);
            end.addFreshEntity(mite);
            made.add(mite);
            boolean pesters = com.ggrantrichards.astralend.world.WebOfLife.pesterHerd(end, mite);
            log(String.format("web of life in a Sunstorm (at %s): stormy %s; Hushbacks find cover %s; Hydragrips find the water %s; "
                + "pods swim out of the Reach %s; endermites pester the herd %s", meadow.toShortString(), stormy, cover, water, outward, pesters));
            if (!stormy || cover == null || water == null || !outward || !pesters) {
                failures.add("web of life: in a Sunstorm Hushbacks should find cover, Hydragrips water and pods the way out of the Reach; "
                    + "endermites should pester the herd (" + stormy + ", " + cover + ", " + water + ", " + outward + ", " + pesters + ")");
            }
        } finally {
            made.forEach(net.minecraft.world.entity.Entity::discard);
            roof.forEach(b -> end.setBlock(b, Blocks.AIR.defaultBlockState(), 3));
            storms.setPhase(end, was, end.getRandom());
        }
    }

    // The Leviathan: found the way /locate finds it; its skull built, with the reliquary's pedestal, the
    // four End Chimes pylons, the Maw Eyes in the sockets and the Curator (dormant, deaf to all but a
    // player); the dragon egg set on the pedestal lights the eyes. Then views: the whole skeleton from
    // off its flank, and the reliquary from its door.
    private static void testLeviathan(ServerLevel end, List<String> failures) {
        var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
            net.minecraft.resources.Identifier.fromNamespaceAndPath(com.ggrantrichards.astralend.AstralEnd.MODID, "leviathan"));
        BlockPos start = end.findNearestMapStructure(tag, new BlockPos(1200, 64, 0), 200, false);
        if (start == null) {
            failures.add("leviathan: none found within 3200 blocks");
            return;
        }
        var registry = end.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        com.ggrantrichards.astralend.world.structure.LeviathanPiece body = null;
        com.ggrantrichards.astralend.world.structure.LeviathanHeadPiece head = null;
        for (var entry : end.getChunk(start.getX() >> 4, start.getZ() >> 4).getAllStarts().entrySet()) {
            var key = registry.getKey(entry.getKey());
            if (key != null && key.getPath().equals("leviathan")) {
                for (var piece : entry.getValue().getPieces()) {
                    if (piece instanceof com.ggrantrichards.astralend.world.structure.LeviathanPiece lp) {
                        body = lp;
                    } else if (piece instanceof com.ggrantrichards.astralend.world.structure.LeviathanHeadPiece hp) {
                        head = hp;
                    }
                }
            }
        }
        if (body == null || head == null) {
            failures.add("leviathan: found a start at " + start.toShortString() + " but not both its pieces (body " + (body != null)
                + ", skull " + (head != null) + ")");
            return;
        }
        // Generate all of it.
        var box = body.getBoundingBox();
        var skullBox = head.getBoundingBox();
        for (int cx = Math.min(box.minX(), skullBox.minX()) >> 4; cx <= Math.max(box.maxX(), skullBox.maxX()) >> 4; cx++) {
            for (int cz = Math.min(box.minZ(), skullBox.minZ()) >> 4; cz <= Math.max(box.maxZ(), skullBox.maxZ()) >> 4; cz++) {
                end.getChunk(cx, cz);
            }
        }
        BlockPos pedestal = body.pedestal(), arena = body.arena(), heart = body.heart(), throat = head.throat();
        // The whole skeleton, cut out for the website's diorama.
        Dioramas.dump(end, "leviathan", new BlockPos(Math.min(box.minX(), skullBox.minX()), Math.min(box.minY(), skullBox.minY()),
            Math.min(box.minZ(), skullBox.minZ())), new BlockPos(Math.max(box.maxX(), skullBox.maxX()), Math.max(box.maxY(), skullBox.maxY()),
            Math.max(box.maxZ(), skullBox.maxZ())));
        testMaze(end, body.mazeStart(), failures);
        // A Drift Whale passing through: once round the skeleton, humming low.
        var passing = ModEntities.DRIFT_WHALE.get().create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        passing.snapTo(heart.getX() + 0.5, heart.getY() + 2.0, heart.getZ() + 0.5, 0.0F, 0.0F);
        end.addFreshEntity(passing);
        boolean circles = passing.noticeLeviathan(end) && passing.isCircling();
        passing.discard();
        log("web of life: a Drift Whale passing a Leviathan circles it once: " + circles);
        if (!circles) {
            failures.add("web of life: a Drift Whale passing a Leviathan should circle it once");
        }
        // The reliquary in the cathedral: the pedestal, four pylons with their chimes, the lungs over it.
        int chimes = 0, breathcomb = 0;
        for (BlockPos p : BlockPos.betweenClosed(arena.offset(-28, -4, -28), arena.offset(28, 24, 28))) {
            BlockState state = end.getBlockState(p);
            if (state.is(ModBlocks.END_CHIMES.get())) {
                chimes++;
            } else if (state.is(ModBlocks.BREATHCOMB.get())) {
                breathcomb++;
            }
        }
        // The heart, beating in its muscle.
        int heartBlocks = 0, muscle = 0;
        for (BlockPos p : BlockPos.betweenClosed(heart.offset(-7, -7, -7), heart.offset(7, 7, 7))) {
            BlockState state = end.getBlockState(p);
            if (state.is(ModBlocks.LEVIATHAN_HEART.get())) {
                heartBlocks++;
            } else if (state.is(ModBlocks.LEVIATHAN_FLESH.get())) {
                muscle++;
            }
        }
        boolean hasPedestal = end.getBlockState(pedestal).is(ModBlocks.LEVIATHAN_PEDESTAL.get());
        var curators = end.getEntitiesOfClass(com.ggrantrichards.astralend.entity.Curator.class, new net.minecraft.world.phys.AABB(arena).inflate(24));
        log(String.format("leviathan: start %s; pedestal %s (%s); %d End Chimes, %d Breathcomb in the lungs; heart: %d Leviathan Heart in %d "
            + "muscle; %d Curator(s)%s", start.toShortString(), pedestal.toShortString(), hasPedestal ? "built" : "MISSING", chimes, breathcomb,
            heartBlocks, muscle, curators.size(), curators.isEmpty() ? "" : curators.get(0).isDormant() ? ", dormant" : ", AWAKE"));
        if (!hasPedestal || chimes != 8 || breathcomb < 150 || heartBlocks < 40 || muscle < 40) {
            failures.add(String.format("leviathan: expected the pedestal, 8 End Chimes (4 pylons on the floor, 4 hanging from the ceiling), a lung's Breathcomb (150+) and a heart (40+ each); got %s, %d, %d, %d, %d",
                hasPedestal, chimes, breathcomb, heartBlocks, muscle));
        }
        if (curators.size() != 1) {
            failures.add("leviathan: expected one Curator in the reliquary, found " + curators.size());
        } else {
            var curator = curators.get(0);
            boolean hurt = curator.hurtServer(end, end.damageSources().generic(), 10.0F);
            if (!curator.isDormant() || hurt || curator.home() == null) {
                failures.add("leviathan: the Curator should be dormant, unhurt by anything but a player, and know its home");
            }
        }
        // The garrison: two shulkers on each tower's roof but the maze's (one such tower at least), still there once the chunks are live.
        var shulkers = end.getEntitiesOfClass(net.minecraft.world.entity.monster.Shulker.class, net.minecraft.world.phys.AABB.of(box).inflate(8));
        log("leviathan: " + shulkers.size() + " shulkers on the towers");
        if (shulkers.size() < 2) {
            failures.add("leviathan: expected 2+ shulkers on the towers, found " + shulkers.size());
        }
        // The maze on the last tower: its start, a spyglass, and bridges for all four constellations.
        BlockPos maze = body.mazeStart();
        int[] bridges = new int[4];
        boolean spyglass = false;
        for (BlockPos p : BlockPos.betweenClosed(maze.offset(-40, -4, -40), maze.offset(40, 20, 40))) {
            BlockState state = end.getBlockState(p);
            if (state.is(ModBlocks.STAR_BRIDGE.get())) {
                bridges[state.getValue(com.ggrantrichards.astralend.maze.StarBridgeBlock.CONSTELLATION)]++;
            } else if (state.is(Blocks.BARREL) && end.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
                spyglass |= barrel.getItem(13).is(net.minecraft.world.item.Items.SPYGLASS);
            }
        }
        boolean mazeStart = end.getBlockState(maze).is(ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get());
        log(String.format("leviathan: the maze on its tower: start %s (%s), spyglass %s, bridges %s", maze.toShortString(),
            mazeStart ? "a star inlay" : "MISSING", spyglass, java.util.Arrays.toString(bridges)));
        if (!mazeStart || !spyglass || java.util.Arrays.stream(bridges).anyMatch(n -> n < 8)) {
            failures.add("leviathan: the maze on its tower needs its start, a spyglass and bridges for all four constellations");
        }
        // The skull: four dark eyes, the throat, bone.
        int dark = 0;
        for (BlockPos eye : head.eyes()) {
            if (end.getBlockState(eye).is(ModBlocks.MAW_EYE.get()) && !end.getBlockState(eye).getValue(com.ggrantrichards.astralend.maw.MawEyeBlock.LIT)) {
                dark++;
            }
        }
        int skullBone = 0, variants = 0;
        for (BlockPos p : BlockPos.betweenClosed(throat.offset(-40, -24, -40), throat.offset(40, 30, 40))) {
            BlockState state = end.getBlockState(p);
            if (state.is(ModBlocks.LEVIATHAN_BONE_BLOCK.get())) {
                skullBone++;
            } else if (state.is(ModBlocks.WEATHERED_LEVIATHAN_BONE.get()) || state.is(ModBlocks.LICHENED_LEVIATHAN_BONE.get())) {
                skullBone++;
                variants++;
            }
        }
        log("leviathan: " + variants + " of its skull's " + skullBone + " bone blocks weathered or lichened");
        if (variants < skullBone / 10 || variants > skullBone * 2 / 3) {
            failures.add("leviathan: its bone should be broken up with weathered and lichened bone (" + variants + " of " + skullBone + ")");
        }
        boolean hasThroat = end.getBlockState(throat).is(ModBlocks.MAW_THROAT.get());
        log(String.format("leviathan: skull - %d dark eyes, throat %s (%s), %d bone round it", dark, throat.toShortString(),
            hasThroat ? "built" : "MISSING", skullBone));
        if (dark != 4 || !hasThroat || skullBone < 4000) {
            failures.add(String.format("leviathan: expected 4 dark eyes, the throat and 4000+ bone round the skull; got %d, %s, %d", dark, hasThroat, skullBone));
        }
        // Nothing of the land reaches into it: no End Stone or ground cover anywhere in its bounds.
        int ground = 0;
        for (var b : List.of(box, skullBox)) {
            for (int x = b.minX(); x <= b.maxX(); x += 2) {
                for (int z = b.minZ(); z <= b.maxZ(); z += 2) {
                    for (int y = b.minY(); y <= b.maxY(); y++) {
                        BlockState state = end.getBlockState(new BlockPos(x, y, z));
                        if (state.is(Blocks.END_STONE) || state.is(ModBlocks.PALE_END_STONE.get()) || state.is(ModBlocks.CHORUS_MOSS.get())
                                || state.is(ModBlocks.LIVING_TURF.get()) || state.is(Blocks.CHORUS_PLANT)) {
                            ground++;
                        }
                    }
                }
            }
        }
        log("leviathan: " + ground + " blocks of land inside its bounds (every other column)");
        if (ground > 0) {
            failures.add("leviathan: land reaches into it (" + ground + " blocks of End Stone or ground cover in its bounds)");
        }
        // The egg on the pedestal lights the skull's four eyes; then the Maw wakes into a wormhole.
        if (hasPedestal) {
            com.ggrantrichards.astralend.block.LeviathanPedestalBlock.enshrine(end, pedestal);
            int lit = 0;
            for (BlockPos eye : head.eyes()) {
                BlockState state = end.getBlockState(eye);
                if (state.is(ModBlocks.MAW_EYE.get()) && state.getValue(com.ggrantrichards.astralend.maw.MawEyeBlock.LIT)) {
                    lit++;
                }
            }
            var maw = com.ggrantrichards.astralend.maw.MawNetwork.wake(end, throat, null);
            boolean wormhole = end.getBlockState(throat).is(ModBlocks.WORMHOLE.get());
            log(String.format("leviathan: the egg enshrined lit %d of 4 eyes; the Maw %s, wormhole %s%s", lit, maw != null ? "woke" : "DIDN'T WAKE",
                wormhole ? "open" : "MISSING", maw == null ? "" : ", exit " + maw.exit().toShortString() + " facing "
                + net.minecraft.core.Direction.from2DDataValue(maw.facing())));
            if (lit != 4 || maw == null || !wormhole) {
                failures.add("leviathan: the egg should light the skull's 4 eyes and the Maw then wake; lit " + lit + ", woke " + (maw != null));
            }
        }
        // Views: the whole of it from high off its flank; the skull from in front of its mouth; the hall
        // in the skull; the reliquary; the heart from under it; the maze.
        double dx = throat.getX() - arena.getX(), dz = throat.getZ() - arena.getZ(), len = Math.hypot(dx, dz);
        double ax = dx / len, az = dz / len;                // along the body, toward the head
        BlockPos mid = new BlockPos((int) Math.round(arena.getX() + ax * 8), throat.getY(), (int) Math.round(arena.getZ() + az * 8));
        BlockPos cam = mid.offset((int) Math.round(-az * 120 - ax * 30), 40, (int) Math.round(ax * 120 - az * 30));   // (within the render distance)
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan %d.5 %d %d.5 %.1f 18.0 clear", cam.getX(), cam.getY(), cam.getZ(),
            yawTo(cam, mid)));
        BlockPos front = throat.offset((int) Math.round(ax * 95 - az * 55), 22, (int) Math.round(az * 95 + ax * 55));
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan_maw %d.5 %d %d.5 %.1f 8.0 wake=%d;%d;%d", front.getX(), front.getY(),
            front.getZ(), yawTo(front, throat), throat.getX(), throat.getY(), throat.getZ()));
        // The skull's face, side on and a little in front, close enough to read.
        BlockPos face = head.frame(46, com.ggrantrichards.astralend.world.structure.LeviathanShape.b() + 18,
            com.ggrantrichards.astralend.world.structure.LeviathanShape.SKULL_W + 46);
        BlockPos faceAt = head.frame(0, com.ggrantrichards.astralend.world.structure.LeviathanShape.b() + 6,
            com.ggrantrichards.astralend.world.structure.LeviathanShape.SKULL_W + 24);
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan_skull %d.5 %d %d.5 %.1f 12.0 clear", face.getX(), face.getY(),
            face.getZ(), yawTo(face, faceAt)));
        BlockPos hall = head.hall().above();
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan_maw_hall %d.5 %d %d.5 %.1f -12.0", hall.getX(), hall.getY(),
            hall.getZ(), yawTo(hall, throat)));
        BlockPos nave = pedestal.offset((int) Math.round(-ax * 6), 3, (int) Math.round(-az * 6));
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan_reliquary %d.5 %d %d.5 %.1f 8.0", nave.getX(), nave.getY(),
            nave.getZ(), yawTo(nave, arena)));
        BlockPos under = pedestal.offset((int) Math.round(-ax * 9), 1, (int) Math.round(-az * 9));
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan_heart %d.5 %d %d.5 %.1f -38.0", under.getX(), under.getY(),
            under.getZ(), yawTo(under, heart)));
        BlockPos mazeCam = maze.offset((int) Math.round(-az * 34 - ax * 20), 16, (int) Math.round(ax * 34 - az * 20));
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_leviathan_maze %d.5 %d %d.5 %.1f 20.0 bridges=%d;%d;%d", mazeCam.getX(),
            mazeCam.getY(), mazeCam.getZ(), yawTo(mazeCam, maze), maze.getX(), maze.getY(), maze.getZ()));
    }

    // The yaw (Minecraft's: 0 = south, 90 = west) that looks from `from` toward `to`.
    private static double yawTo(BlockPos from, BlockPos to) {
        return Math.toDegrees(Math.atan2(-(to.getX() - from.getX()), to.getZ() - from.getZ()));
    }

    // A Constellation Maze: four platforms (the stars in their floors, where you stand to sight a
    // constellation through a spyglass), bridges for all four constellations - each lighting when its
    // constellation is sighted from its platform - and the spyglass in the barrel on the first. Then a
    // view of the whole maze with its bridges lit, from above its edge.
    private static boolean quartz(BlockState state) {
        return state.is(ModBlocks.OBSERVATORY_QUARTZ.get()) || state.is(ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get());
    }

    // The Constellation Maze on a Leviathan's last tower (since beta.5 the only one): its four starred
    // platforms, the spyglass at the start, and a bridge that lights for each constellation.
    private static void testMaze(ServerLevel end, BlockPos bridge, List<String> failures) {
        List<BlockPos> spots = new ArrayList<>();
        boolean spyglass = false;
        for (BlockPos p : BlockPos.betweenClosed(bridge.offset(-48, -12, -48), bridge.offset(48, 12, 48))) {
            var state = end.getBlockState(p);
            // A platform's star: set in polished Nightstone (the dais's stars are in quartz).
            if (state.is(ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get()) && !quartz(end.getBlockState(p.east())) && !quartz(end.getBlockState(p.west()))) {
                spots.add(p.immutable());
            }
            if (state.is(Blocks.BARREL) && end.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity barrel) {
                for (int i = 0; i < barrel.getContainerSize(); i++) {
                    spyglass |= barrel.getItem(i).is(net.minecraft.world.item.Items.SPYGLASS);
                }
            }
        }
        StringBuilder counts = new StringBuilder();
        long x = 0, y = 0, z = 0;
        int answered = 0;
        for (BlockPos spot : spots) {
            for (int id = 0; id < 4; id++) {
                int lit = com.ggrantrichards.astralend.maze.MazeScoping.lightBridges(end, spot, id);
                if (lit > 0) {
                    answered |= 1 << id;
                }
                counts.append(' ').append(id).append(':').append(lit);
            }
            counts.append(" |");
            x += spot.getX();
            y += spot.getY();
            z += spot.getZ();
        }
        log("constellation_maze: " + spots.size() + " platforms; spyglass in the barrel: " + spyglass + "; bridges lit per constellation:" + counts);
        if (answered != 15) {
            failures.add("constellation_maze: not every constellation has a bridge that lights");
        }
        if (!spyglass) {
            failures.add("constellation_maze: no spyglass in the barrel on the first platform");
        }
        if (spots.size() != 4) {
            failures.add("constellation_maze: expected 4 platforms, found " + spots.size());
            return;
        }
        BlockPos centre = new BlockPos((int) (x / 4), (int) (y / 4), (int) (z / 4));
        double d = 34.0 / Math.sqrt(2.0);
        double pitch = Math.toDegrees(Math.atan2(20.0, 34.0));
        EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "structure_constellation_maze %.1f %d %.1f -45 %.1f bridges",
            centre.getX() + 0.5 - d, centre.getY() + 18, centre.getZ() + 0.5 - d, pitch));
    }

    // The caves: under the Deep Hollows, the Hollow Roots (glowcaps, Starsap, fireflies); under the
    // Shattered Crags, the Crystal Vaults (geodes of Glimmer Crystal, each with its Shardback).
    private static void checkCaves(Area roots, Area vaults, ServerLevel end, List<String> failures) {
        if (roots != null) {
            int glowcaps = 0, sap = 0, chunks = 0;
            BlockPos cap = null;
            for (int cx = -4; cx <= 4; cx++) {
                for (int cz = -4; cz <= 4; cz++) {
                    var chunk = end.getChunk((roots.centre.getX() >> 4) + cx, (roots.centre.getZ() >> 4) + cz);
                    chunks++;
                    for (BlockPos p : BlockPos.betweenClosed(chunk.getPos().getMinBlockX(), 10, chunk.getPos().getMinBlockZ(),
                            chunk.getPos().getMaxBlockX(), com.ggrantrichards.astralend.world.AstralEndBiomeSource.ROOTS_BELOW, chunk.getPos().getMaxBlockZ())) {
                        BlockState state = end.getBlockState(p);
                        if (state.is(ModBlocks.HOLLOW_GLOWCAP.get())) {
                            glowcaps++;
                            if (cap == null || glowcaps % 17 == 0) {
                                cap = p.immutable();
                            }
                        } else if (state.is(ModBlocks.STARSAP_ORE.get())) {
                            sap++;
                        }
                    }
                }
            }
            var biome = end.getBiome(roots.centre.atY(30)).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            log(String.format("caves: the Hollow Roots (%s at y=30 under the Deep Hollows' centre): %d glowcaps, %d Starsap ore in %d chunks",
                biome, glowcaps, sap, chunks));
            if (cap != null) {
                // A view along the cave, from a few blocks off a glowcap, at eye height.
                BlockPos eye = caveEye(end, cap);
                Dioramas.cave(end, "cave_hollow_roots", cap);
                EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "biome_hollow_roots %d.5 %d %d.5 %.1f 10.0", eye.getX(), eye.getY(), eye.getZ(),
                    yawTo(eye, cap)));
            }
            if (!biome.equals("astralend:hollow_roots") || glowcaps + sap == 0) {
                failures.add("caves: expected the Hollow Roots under the Deep Hollows, with glowcaps and Starsap");
            }
        }
        if (vaults != null) {
            int crystal = 0;
            BlockPos geode = null;
            for (int cx = -4; cx <= 4; cx++) {
                for (int cz = -4; cz <= 4; cz++) {
                    var chunk = end.getChunk((vaults.centre.getX() >> 4) + cx, (vaults.centre.getZ() >> 4) + cz);
                    for (BlockPos p : BlockPos.betweenClosed(chunk.getPos().getMinBlockX(), 4, chunk.getPos().getMinBlockZ(),
                            chunk.getPos().getMaxBlockX(), com.ggrantrichards.astralend.world.AstralEndBiomeSource.VAULTS_BELOW, chunk.getPos().getMaxBlockZ())) {
                        if (end.getBlockState(p).is(ModBlocks.GLIMMER_CRYSTAL.get())) {
                            crystal++;
                            if (geode == null) {
                                geode = p.immutable();
                            }
                        }
                    }
                }
            }
            var biome = end.getBiome(vaults.centre.atY(20)).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            var hunters = end.getEntitiesOfClass(com.ggrantrichards.astralend.entity.Shardback.class,
                new net.minecraft.world.phys.AABB(vaults.centre).inflate(80, 80, 80));
            log(String.format("caves: the Crystal Vaults (%s at y=20 under the Shattered Crags' centre): %d Glimmer Crystal in geodes, %d Shardbacks (%d hidden)",
                biome, crystal, hunters.size(), hunters.stream().filter(com.ggrantrichards.astralend.entity.Shardback::isHidden).count()));
            if (geode != null) {
                BlockPos at = geode;
                BlockPos eye = caveEye(end, at);
                Dioramas.cave(end, "cave_crystal_vaults", at);
                EXTRA_SHOTS.add(String.format(java.util.Locale.ROOT, "biome_crystal_vaults %d.5 %d %d.5 %.1f 15.0", eye.getX(), eye.getY(), eye.getZ(),
                    yawTo(eye, at)));
            }
            if (!biome.equals("astralend:crystal_vaults") || crystal == 0) {
                failures.add("caves: expected the Crystal Vaults under the Shattered Crags, with geodes");
            }
        }
    }

    // Somewhere to stand in a cave a few blocks from `at`: two blocks of air (cave or not), looked for
    // round it; else just above it.
    private static BlockPos caveEye(ServerLevel end, BlockPos at) {
        for (int r = 5; r >= 2; r--) {
            for (int[] d : new int[][] {{r, 0}, {-r, 0}, {0, r}, {0, -r}, {r, r}, {-r, -r}}) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = at.offset(d[0], dy, d[1]);
                    if (end.getBlockState(p).isAir() && end.getBlockState(p.above()).isAir()) {
                        return p;               // (the shot's position is where its feet are)
                    }
                }
            }
        }
        return at.above();
    }

    // Borders blend (BorderBlendFeature): along lines across the edge of the Nebula Fields, the top
    // block - its Chorus Moss, or the neighbour's ground - should switch back and forth, not once.
    // Logged rather than failed: whether a border crosses land at all depends on the seed.
    private static void checkBlend(Area area, ServerLevel end) {
        if (area == null) {
            return;
        }
        var here = end.getBiome(area.centre);
        for (int dx = 4; dx < 4000; dx += 4) {
            BlockPos edge = area.centre.offset(dx, 0, 0);
            if (end.getBiome(edge).equals(here)) {
                continue;
            }
            int rows = 0, switches = 0;
            for (int dz = -96; dz <= 96; dz += 3) {
                int run = 0;
                Boolean last = null;
                int changes = 0;
                for (int ex = -24; ex <= 24; ex++) {
                    BlockPos column = edge.offset(ex, 0, dz);
                    int top = end.getChunk(column).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                        column.getX() & 15, column.getZ() & 15);
                    BlockState state = end.getBlockState(column.atY(top));
                    if (state.isAir()) {
                        continue;
                    }
                    run++;
                    boolean moss = state.is(ModBlocks.CHORUS_MOSS.get());
                    if (last != null && moss != last) {
                        changes++;
                    }
                    last = moss;
                }
                if (run >= 30 && changes > 0) {
                    rows++;
                    switches += changes;
                }
            }
            log(String.format("border blend: at %s, %d rows across it on land, %.1f switches of ground per row (a hard border: 1)",
                edge.toShortString(), rows, rows == 0 ? 0.0 : switches / (double) rows));
            return;
        }
    }

    // For the playtests: where to start a flight east across the edge of the Nebula Fields - 100 blocks
    // short of the first place east of its centre where the biome changes, 30 above the ground.
    private static void borderShot(List<String> lines, Area area, ServerLevel end) {
        if (area == null) {
            return;
        }
        var here = end.getBiome(area.centre);
        for (int dx = 4; dx < 4000; dx += 4) {
            BlockPos p = area.centre.offset(dx, 0, 0);
            var there = end.getBiome(p);
            if (!there.equals(here)) {
                int x = p.getX() - 100;
                int y = Math.max(70, end.getChunkSource().getGenerator().getBaseHeight(x, p.getZ(),
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, end, end.getChunkSource().randomState())) + 30;
                log("border: " + here.unwrapKey().map(k -> k.identifier().toString()).orElse("?") + " gives way to "
                    + there.unwrapKey().map(k -> k.identifier().toString()).orElse("?") + " at " + p.toShortString());
                lines.add(String.format(java.util.Locale.ROOT, "pt_border %d.5 %d %d.5 -90 15", x, y, p.getZ()));
                return;
            }
        }
    }

    // A viewpoint "structure_<name> x y z yaw pitch": `distance` blocks north-west of the found block and
    // `height` above it, looking at a point `aimLift` above that block.
    private static void structureShot(List<String> lines, Map<String, BlockPos> structures, String name, double distance,
                                      double height, double aimLift) {
        BlockPos at = structures.get(name);
        if (at == null) {
            return;
        }
        double d = distance / Math.sqrt(2.0);
        double eyeY = at.getY() + height;
        double pitch = Math.toDegrees(Math.atan2(eyeY - (at.getY() + aimLift), distance));
        lines.add(String.format(java.util.Locale.ROOT, "structure_%s %.1f %.1f %.1f -45 %.1f", name,
            at.getX() + 0.5 - d, eyeY - 1.62, at.getZ() + 0.5 - d, pitch));
    }

    // Phase 6: each structure is found the way /locate finds it, generated, and looked for by a block
    // only it has: a crater's loot chest, an observatory's vault, the Array's pedestal, a
    // maze's star-bridge. Returns where each was found (for screenshots); a hot crater is reported separately.
    private static Map<String, BlockPos> checkStructures(Map<Character, Area> areas, ServerLevel end, List<String> failures) {
        Map<String, BlockPos> found = new LinkedHashMap<>();
        record Wanted(String name, char biome, Block marker) {}
        for (Wanted w : List.of(new Wanted("comet_crater", 'A', Blocks.CHEST),
                                new Wanted("shattered_observatory", 'N', Blocks.VAULT),
                                new Wanted("solar_array", 'E', ModBlocks.COLLECTOR_PEDESTAL.get()))) {
            Area area = areas.get(w.biome());
            BlockPos from = area != null ? area.centre : BlockPos.ZERO;
            net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.STRUCTURE,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(com.ggrantrichards.astralend.AstralEnd.MODID, w.name()));
            List<BlockPos> starts = new ArrayList<>();
            BlockPos search = from;
            // The nearest few, so a hot crater turns up too: each search starts past the last one found.
            for (int i = 0; i < (w.name().equals("comet_crater") ? 6 : 1); i++) {
                BlockPos start = end.findNearestMapStructure(tag, search, 100, false);
                if (start == null || starts.contains(start)) {
                    break;
                }
                starts.add(start);
                search = start.offset(400, 0, 0);
            }
            if (starts.isEmpty()) {
                failures.add(w.name() + ": no structure found within 1600 blocks of " + from.toShortString());
                continue;
            }
            int placed = 0;
            for (BlockPos start : starts) {
                BlockPos marker = null;
                boolean hot = false;
                for (int cx = -4; cx <= 4; cx++) {
                    for (int cz = -4; cz <= 4; cz++) {
                        end.getChunk((start.getX() >> 4) + cx, (start.getZ() >> 4) + cz);
                    }
                }
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
                for (int x = start.getX() - 56; x <= start.getX() + 56 && marker == null; x++) {
                    for (int z = start.getZ() - 56; z <= start.getZ() + 56 && marker == null; z++) {
                        for (int y = end.getMinY(); y < end.getMinY() + 256; y++) {
                            BlockState state = end.getBlockState(p.set(x, y, z));
                            if (state.is(w.marker()) && (w.marker() != ModBlocks.VOID_CRYSTAL_BLOCK.get()
                                    || end.getBlockState(p.east()).is(ModBlocks.ASTRAL_GLASS.get()))) {
                                marker = p.immutable();
                                break;
                            }
                        }
                    }
                }
                if (marker == null) {
                    log(String.format("%s: start at %s, but nothing of it was built", w.name(), start.toShortString()));
                    // Why not: does its template load, and what did the start chunk record?
                    var registry = end.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
                    for (int cx = -1; cx <= 1; cx++) {
                        for (int cz = -1; cz <= 1; cz++) {
                            var chunk = end.getChunk((start.getX() >> 4) + cx, (start.getZ() >> 4) + cz);
                            chunk.getAllStarts().forEach((structure, st) -> {
                                var key = registry.getKey(structure);
                                if (key != null && key.getNamespace().equals(com.ggrantrichards.astralend.AstralEnd.MODID)) {
                                    log("  start of " + key + " in chunk " + chunk.getPos() + ": valid " + st.isValid() + ", "
                                        + st.getPieces().size() + " piece(s), box " + st.getBoundingBox());
                                    for (var piece : st.getPieces()) {
                                        log("    piece " + piece.getClass().getSimpleName() + " " + piece.getBoundingBox());
                                        var b = piece.getBoundingBox();
                                        Map<Block, Integer> inBox = new HashMap<>();
                                        for (BlockPos q : BlockPos.betweenClosed(b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ())) {
                                            inBox.merge(end.getBlockState(q).getBlock(), 1, Integer::sum);
                                        }
                                        StringBuilder top = new StringBuilder();
                                        inBox.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(6)
                                            .forEach(e -> top.append(' ').append(e.getKey().getName().getString()).append('=').append(e.getValue()));
                                        log("    blocks in it:" + top);
                                    }
                                }
                            });
                        }
                    }
                    continue;
                }
                placed++;
                String loot = "";
                if (end.getBlockEntity(marker) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity chest) {
                    loot = ", chest loot " + (chest.getLootTable() == null ? "none" : chest.getLootTable().identifier());
                    if (chest.getLootTable() == null) {
                        failures.add(w.name() + ": its chest at " + marker.toShortString() + " has no loot table");
                    }
                }
                if (w.name().equals("comet_crater")) {
                    for (BlockPos q : BlockPos.betweenClosed(marker.offset(-10, 0, -10), marker.offset(10, 10, 10))) {
                        if (end.getBlockState(q).is(ModBlocks.RAW_SOLARITE_BLOCK.get())) {
                            hot = true;
                            break;
                        }
                    }
                }
                String key = w.name() + (hot ? "_hot" : "");
                log(String.format("%s: built at %s (start %s)%s%s", w.name(), marker.toShortString(), start.toShortString(),
                    hot ? ", still hot" : "", loot));
                found.putIfAbsent(key, marker);
                if (w.name().equals("solar_array")) {
                    // Inside the control room too: the console, seen from a corner of the room.
                    found.putIfAbsent("solar_array_room", marker.above());
                }
            }
            if (placed == 0) {
                failures.add(w.name() + ": found " + starts.size() + " start(s) but none was built");
            }
        }
        return found;
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
        double d = distance / Math.sqrt(2.0);
        BlockPos spot = clearSpot(end, area.standOn, d, lift, eye);
        int x = spot.getX();
        int z = spot.getZ();
        int ground = spot.getY();
        double mobY = ground + lift;
        double eyeY = ground + eye;
        double camY = eyeY - 1.62;  // a teleport places the feet; the eyes are 1.62 higher
        double pitch = Math.toDegrees(Math.atan2(eyeY - (mobY + 0.5), distance));
        String name = "mob_" + parts[0] + (parts.length > 1 ? "_" + parts[1].split(",")[0].replace("flare=", "") : "");
        lines.add(String.format(java.util.Locale.ROOT, "%s %.1f %.1f %.1f -45 %.1f astralend:%s %.1f %.1f %.1f%s", name,
            x + 0.5 - d, camY, z + 0.5 - d, pitch, parts[0], x + 0.5, mobY, z + 0.5, parts.length > 1 ? " " + parts[1] : ""));
    }

    // A spot near `from` where the camera (d blocks north-west on each axis, `eye` above the ground)
    // sees the mob (`lift` above it) with nothing solid in between: no pillar, tree or wall in front.
    private static BlockPos clearSpot(ServerLevel end, BlockPos from, double d, double lift, double eye) {
        for (int r = 0; r <= 24; r += 4) {
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;   // this ring only
                    }
                    int x = from.getX() + dx, z = from.getZ() + dz;
                    int ground = end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    if (ground < end.getMinY() + 8 || Math.abs(ground - from.getY()) > 12) {
                        continue;
                    }
                    net.minecraft.world.phys.Vec3 mob = new net.minecraft.world.phys.Vec3(x + 0.5, ground + lift + 0.5, z + 0.5);
                    net.minecraft.world.phys.Vec3 cam = new net.minecraft.world.phys.Vec3(x + 0.5 - d, ground + eye, z + 0.5 - d);
                    // the centre of the picture and a fan round it: a pillar beside the line still fills the frame
                    net.minecraft.world.phys.Vec3 across = new net.minecraft.world.phys.Vec3(0.7071, 0, -0.7071);
                    boolean clear = true;
                    for (net.minecraft.world.phys.Vec3 aim : List.of(mob, mob.add(across.scale(2.5)), mob.add(across.scale(-2.5)),
                            mob.add(0, 1.8, 0), mob.add(0, -0.3, 0))) {
                        clear &= clearBetween(end, cam, aim);
                    }
                    if (clear) {
                        return new BlockPos(x, ground, z);
                    }
                }
            }
        }
        return from;
    }

    private static boolean clearBetween(ServerLevel end, net.minecraft.world.phys.Vec3 a, net.minecraft.world.phys.Vec3 b) {
        int steps = (int) Math.ceil(a.distanceTo(b) * 4.0);
        for (int i = 0; i <= steps - 2; i++) {   // up to just short of the mob itself
            BlockPos p = BlockPos.containing(a.lerp(b, (double) i / steps));
            if (!end.getBlockState(p).getCollisionShape(end, p).isEmpty()) {
                return false;
            }
        }
        // and room for the camera's own head
        return end.getBlockState(BlockPos.containing(a)).getCollisionShape(end, BlockPos.containing(a)).isEmpty();
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
        final Map<Block, Integer> firstIn = new HashMap<>();      // the first block of any kind from the sky down
        final Map<Integer, Integer> surfaceHeightsIn = new HashMap<>();
        final List<BlockPos> anomalies = new ArrayList<>();
        final List<BlockPos> endStoneIn = new ArrayList<>();   // where End Stone was left inside (the first few thousand)
        final List<String> profiles = new ArrayList<>();
        int decayingLeaves;
        int caveAir;                                         // sealed cave air (the Crags' root hollows)
        final List<BlockPos> caveFloors = new ArrayList<>();  // cave air with rock under it
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

        int firsts(Block block) {
            return this.firstIn.getOrDefault(block, 0);
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

    private static ServerLevel LAST_END;

    private static Area scan(ServerLevel end, BiomeInfo info, BlockPos spot) {
        LAST_END = end;
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
                            if (state.is(Blocks.CAVE_AIR) && inside) {
                                area.caveAir++;
                                if (area.caveFloors.size() < 400 && !chunk.getBlockState(pos.below()).isAir()
                                    && chunk.getBlockState(pos.above()).isAir()) {
                                    area.caveFloors.add(pos);
                                }
                            }
                            if (state.isAir()) {
                                continue;
                            }
                            Block block = state.getBlock();
                            boolean here = inside;
                            if (block == Blocks.END_STONE) {
                                // Judged by the biome at the block itself: the game's biome lookup jitters a
                                // few blocks in every direction, so a cliff at a border mixes the two stones.
                                here = end.getBiome(pos).is(info.key());
                            }
                            (here ? area.in : area.out).merge(block, 1, Integer::sum);
                            if (here && block == Blocks.END_STONE && area.endStoneIn.size() < 4000) {
                                area.endStoneIn.add(pos);
                            }
                            if (firstSeen == null) {
                                firstSeen = block;
                            }
                            if (block == ModBlocks.UPWELL_CORE.get()) {
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
                        if (inside && firstSeen != null) {
                            area.firstIn.merge(firstSeen, 1, Integer::sum);
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
            // Where it is: its heights, how many columns, the biome at the block itself, and what's round a few.
            Map<Integer, Integer> byY = new java.util.TreeMap<>();
            java.util.Set<Long> columns = new java.util.HashSet<>();
            for (BlockPos p : area.endStoneIn) {
                byY.merge(p.getY() / 8 * 8, 1, Integer::sum);
                columns.add(((long) p.getX() << 32) ^ (p.getZ() & 0xffffffffL));
            }
            log(String.format("%s: the End Stone, by height (in 8s): %s; in %d columns", area.info.name(), byY, columns.size()));
            ServerLevel level = LAST_END;
            for (int i = 0; i < area.endStoneIn.size() && i < 6 * 97; i += 97) {
                BlockPos p = area.endStoneIn.get(i);
                if (level != null) {
                    log(String.format("  End Stone at %s: biome there %s, above %s, below %s, beside %s", p.toShortString(),
                        level.getBiome(p).unwrapKey().map(k -> k.identifier().toString()).orElse("?"),
                        level.getBlockState(p.above()).getBlock().getName().getString(),
                        level.getBlockState(p.below()).getBlock().getName().getString(),
                        level.getBlockState(p.east()).getBlock().getName().getString()));
                }
            }
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
        landCoverage(area, 30, failures);
        atLeast(area, ModBlocks.NEBULA_TUFTS.get(), 4, failures);           // a scatter, no longer a carpet
        atLeast(area, ModBlocks.NEBULA_BLADDER.get(), 0.5, failures);       // Drift Bladders floating over the moss
        atLeast(area, ModBlocks.NEBULA_TENDRIL.get(), 1, failures);
        // Oases: water standing on the surface.
        int water = area.firsts(Blocks.WATER) + area.firsts(Blocks.LILY_PAD);
        log(String.format("Nebula Fields: %d columns of oasis water (%.1f per chunk)", water, water / Math.max(1.0, area.landChunks())));
        if (water < area.landChunks()) {
            failures.add("Nebula Fields: hardly any oases (" + water + " columns of water)");
        }
        // Where the ground itself is the top block, it should be moss.
        int mossTops = area.tops(ModBlocks.CHORUS_MOSS.get());
        int groundTops = mossTops + area.tops(Blocks.END_STONE);
        if (mossTops < 0.9 * groundTops) {
            failures.add("Nebula Fields: only " + mossTops + " of " + groundTops + " bare-ground columns are topped with Nebula Moss");
        }
        // After beta.4 (an emission nebula): reed groves of Astral Vines, their tips glowing pale blue,
        // a few Lumineshrooms; no giant Lumineshrooms or Starblooms (the Eclipse's).
        atLeast(area, ModBlocks.ASTRAL_VINE.get(), 2, failures);
        atLeast(area, ModBlocks.ASTRAL_VINE_PLANT.get(), 8, failures);
        atLeast(area, ModBlocks.LUMINESHROOM.get(), 0.5, failures);
        if (area.in(ModBlocks.LUMINE_CAP.get()) > 0 || area.in(ModBlocks.STARBLOOM.get()) > 0) {
            failures.add("Nebula Fields: giant Lumineshrooms or Starblooms grew there (they're the Eclipse's now)");
        }
    }

    private static void checkAurora(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.PALE_END_STONE.get(), failures);
        landCoverage(area, 25, failures);
        atLeast(area, ModBlocks.SHIMMER_GRASS.get(), 15, failures);
        atLeast(area, ModBlocks.SHIMMER_REED.get(), 1, failures);
        atLeast(area, ModBlocks.PALE_END_STONE_BRICKS.get(), 0.05, failures);  // standing stones
        atLeast(area, ModBlocks.MEMORY_MOSS.get(), 0.5, failures);
        atLeast(area, ModBlocks.LEVIATHAN_BONE_BLOCK.get(), 1, failures);     // a graveyard: bones everywhere
        // Solarite (since the golden-meadow rework): veins in the pale stone, and raw Solarite glowing at the
        // bottom of the scorch fissures.
        atLeast(area, ModBlocks.SOLARITE_ORE.get(), 0.5, failures);
        atLeast(area, ModBlocks.RAW_SOLARITE_BLOCK.get(), 0.3, failures);
        atLeast(area, ModBlocks.SUNSCORCHED_STONE.get(), 0.3, failures);   // the baked halo round each fissure
        // Flat shelves: most of the land should be topped at the same height.
        int flat = 0;
        for (int y = 52; y <= 55; y++) {
            flat += area.surfaceHeightsIn.getOrDefault(y, 0);
        }
        double flatShare = 100.0 * flat / Math.max(1, area.landColumnsIn);
        log(String.format("Ashen Barrens: %.0f%% of the land is a flat shelf topped at y=52-55; Leviathan Bone ribs: %d blocks",
            flatShare, area.in(ModBlocks.LEVIATHAN_BONE_BLOCK.get())));
        if (flatShare < 60) {
            failures.add(String.format("Ashen Barrens aren't flat shelves: only %.0f%% of the land is topped at y=52-55", flatShare));
        }
    }

    // Phase 10: Nightstone peaks grown through with Glimmer Crystal. The distillation: root caves inside
    // the bigger islands, sealed (no daylight reaches them), where Void Crystal grows and the Umbral
    // Stalker - and only the Stalker - can spawn.
    private static void checkCrags(Area area, ServerLevel end, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.NIGHTSTONE.get(), failures);
        atLeast(area, ModBlocks.GLIMMER_CRYSTAL.get(), 10, failures);
        atLeast(area, ModBlocks.GLIMMER_CLUSTER.get(), 1, failures);
        atLeast(area, ModBlocks.VOID_CRYSTAL_ORE.get(), 0.05, failures);
        log(String.format("Glimmering Crags: %d blocks of root cave (%.0f per chunk of land), %d cave floors", area.caveAir,
            area.caveAir / Math.max(1.0, area.landChunks()), area.caveFloors.size()));
        if (area.caveAir < 40 * Math.max(1.0, area.landChunks()) / 4) {
            failures.add(String.format("Glimmering Crags: hardly any root caves (%d blocks of cave air)", area.caveAir));
        }
        // The Stalker's spawn rule, asked where the game would ask it: dark cave floors yes, open ground no.
        var stalker = ModEntities.UMBRAL_STALKER.get();
        int caveYes = 0, tried = 0;
        for (BlockPos floor : area.caveFloors) {
            if (end.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, floor) > 0) {
                continue;
            }
            tried++;
            if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(stalker, end,
                    net.minecraft.world.entity.EntitySpawnReason.NATURAL, floor, end.getRandom())) {
                caveYes++;
            }
            if (tried >= 40) {
                break;
            }
        }
        boolean openNo = area.standOn == null || !net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(stalker, end,
            net.minecraft.world.entity.EntitySpawnReason.NATURAL, area.standOn, end.getRandom());
        log(String.format("Umbral Stalker spawn rule: %d of %d dark cave floors allow it; open ground %s", caveYes, tried,
            openNo ? "doesn't" : "DOES"));
        if (caveYes == 0 || !openNo) {
            failures.add("Umbral Stalker: it should spawn on dark cave floors in the Crags (" + caveYes + "/" + tried
                + ") and never on open ground (" + (openNo ? "ok" : "it does") + ")");
        }
    }

    // The Crags' variant: fragments, mostly void between them.
    private static void checkShatteredCrags(Area area, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.NIGHTSTONE.get(), failures);
        double land = 100.0 * area.landColumnsIn / Math.max(1, area.columnsIn);
        log(String.format("Shattered Crags: land under %.0f%% of the biome", land));
        if (land > 55) {
            failures.add(String.format("Shattered Crags: %.0f%% of it is land (expected fragments with wide gaps, under 55%%)", land));
        }
    }

    // The Deep Hollows (with the Hollow Isles folded in after beta.4): dark Nullwood forest on ring
    // islands, Hollow Vines hanging to the ground, Mimic Plants, Voidgel and Upwells beneath.
    private static void checkDeepHollows(Area area, ServerLevel end, List<String> failures) {
        if (!enoughLand(area, failures)) {
            return;
        }
        replacesEndStone(area, ModBlocks.NIGHTSTONE.get(), failures);
        atLeast(area, ModBlocks.VOIDGEL.get(), 0.5, failures);
        atLeast(area, ModBlocks.NULLWOOD.log().get(), 25, failures);
        atLeast(area, ModBlocks.HOLLOW_VINES_PLANT.get(), 3, failures);
        atLeast(area, ModBlocks.MIMIC_PLANT.get(), 0.3, failures);
        atLeast(area, ModBlocks.NULLWOOD_LEAVES.get(), 40, failures);
        atLeast(area, ModBlocks.NULL_LANTERN.get(), 2, failures);
        int leaves = area.in(ModBlocks.NULLWOOD_LEAVES.get());
        log(String.format("Deep Hollows: %d of %d Nullwood leaves are too far from a log and would decay", area.decayingLeaves, leaves));
        if (area.decayingLeaves > 0.05 * Math.max(1, leaves)) {
            failures.add("Deep Hollows: " + area.decayingLeaves + " Nullwood leaves would decay (trees would lose their leaves)");
        }
        log("Deep Hollows: " + area.anomalies.size() + " Upwells");
        if (area.anomalies.isEmpty()) {
            failures.add("Deep Hollows: no Upwells generated");
            return;
        }
        // Put a test mob in the middle of a pocket and let the Upwell Core do its work.
        BlockPos core = area.anomalies.get(0);
        if (!(end.getBlockEntity(core) instanceof UpwellCoreBlockEntity)) {
            failures.add("the Upwell Core at " + core.toShortString() + " has no block entity");
        }
        var centre = UpwellCoreBlockEntity.centre(core);
        ArmorStand probe = new ArmorStand(end, centre.x, centre.y - 0.5, centre.z);
        end.addFreshEntity(probe);
        UpwellCoreBlockEntity.invertInside(end, core);
        double inside = probe.getAttributeValue(Attributes.GRAVITY);
        log(String.format("Gravity in the pocket under %s: %.4f (vanilla 0.0800; inverted is negative)", core.toShortString(), inside));
        if (!Upwells.isInverted(probe) || Math.abs(inside - 0.08 * (1 + Upwells.GRAVITY_CHANGE)) > 0.001) {
            failures.add("gravity wasn't inverted inside a pocket (" + inside + ")");
        }
        probe.discard();
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
            TOP_SYMBOLS.put(ModBlocks.CHORUS_MOSS.get(), 'M');
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
            TOP_SYMBOLS.put(Blocks.BLUE_ICE, 'I');
            TOP_SYMBOLS.put(ModBlocks.STARDUST_BLOCK.get(), 'T');
            TOP_SYMBOLS.put(ModBlocks.METEORIC_IRON_ORE.get(), 'O');
            TOP_SYMBOLS.put(ModBlocks.NIGHTSTONE.get(), 'U');
            TOP_SYMBOLS.put(ModBlocks.VOID_CRYSTAL_ORE.get(), 'X');
            TOP_SYMBOLS.put(ModBlocks.UPWELL_CORE.get(), 'Z');
            TOP_SYMBOLS.put(ModBlocks.OBSERVATORY_QUARTZ.get(), 'Q');
            TOP_SYMBOLS.put(ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get(), 'Q');
            TOP_SYMBOLS.put(ModBlocks.OBSERVATORY_QUARTZ_PILLAR.get(), 'K');
            TOP_SYMBOLS.put(ModBlocks.SUNSCORCHED_STONE.get(), 'C');
            TOP_SYMBOLS.put(ModBlocks.SOLARITE_ORE.get(), 'R');
            TOP_SYMBOLS.put(ModBlocks.FLAREBLOOM.get(), 'F');
            TOP_SYMBOLS.put(ModBlocks.SUNSCALE_LICHEN.get(), 'N');
            TOP_SYMBOLS.put(ModBlocks.ASHEN_LUMINESHROOM.get(), 'A');
            TOP_SYMBOLS.put(ModBlocks.LUMINE_CAP.get(), 'L');
            TOP_SYMBOLS.put(ModBlocks.NEBULA_TUFTS.get(), 'M');
            TOP_SYMBOLS.put(ModBlocks.NULLWOOD_LEAVES.get(), 'V');
            TOP_SYMBOLS.put(ModBlocks.NULLWOOD.log().get(), 'U');
            TOP_SYMBOLS.put(ModBlocks.SHIMMER_REED.get(), 'G');
        }
        return TOP_SYMBOLS.getOrDefault(block, '#');
    }

    private static char symbol(Holder<Biome> biome) {
        if (biome.is(ModBiomes.NEBULA_FIELDS)) return 'N';
        if (biome.is(ModBiomes.ASHEN_BARRENS)) return 'A';
        if (biome.is(ModBiomes.DEEP_HOLLOWS)) return 'D';
        if (biome.is(ModBiomes.ECLIPSE)) return 'E';
        if (biome.is(ModBiomes.GLIMMERING_CRAGS)) return 'G';
        if (biome.is(ModBiomes.SHATTERED_CRAGS)) return 'X';
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

    static void logLine(String line) {
        log(line);
    }

    private static void log(String line) {
        LOGGER.info(TAG + line);
    }

    private WorldgenSurvey() {}
}
