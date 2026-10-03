package com.ggrantrichards.astralend.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.entity.UmbralStalker;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.Sunstorms;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// Development tool, not part of normal play. With -Dastralend.screenshots=true
// (./gradlew runClient -PastralendScreenshots), the game opens the world the worldgen survey
// generated, flies to each viewpoint the survey chose (astralend-shots.txt: "name x y z yaw pitch"),
// waits for the chunks to draw, saves screenshots/astralend-<name>.png and quits. CI uses it to
// show what every biome actually looks like.
public final class ClientScreenshots {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SETTLE_TICKS = 260;      // time for chunks to load and draw after a teleport
    private static final int MAX_WAIT_TICKS = 900;

    // A viewpoint; mob shots also name a mob to summon (with its AI off) at mobX/Y/Z, facing the
    // camera ("display" instead of a mob builds a small display of the Phase 5 flare blocks there).
    // The optional last word lists styles, comma-separated: "lit" puts a Spore Lantern beside it,
    // "side" turns it side-on, "angled" three-quarters on, "flare=<phase>" sets the Sunstorm
    // phase for the shot (calm, warning, flare, afterglow), "clear" removes every mob nearby.
    record Shot(String name, double x, double y, double z, float yaw, float pitch,
                        String mob, double mobX, double mobY, double mobZ, String style) {}

    static final List<Shot> SHOTS = new ArrayList<>();
    private static int index = -1;
    private static int ticks;
    private static int ticksWithoutWorld;
    private static boolean done;
    static Entity summoned;
    private static BlockPos lantern;

    public static void register() {
        if (Boolean.getBoolean("astralend.screenshots")) {
            TickEvent.ClientTickEvent.Post.BUS.addListener(ClientScreenshots::onTick);
        }
    }

    private static void onTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (done) {
            return;
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null || server.getPlayerList().getPlayers().isEmpty()) {
            // Still loading; if nothing happens for 3 minutes, photograph whatever is on screen and quit.
            if (++ticksWithoutWorld == 3600) {
                grab(minecraft, "stuck");
                LOGGER.error("[AstralEnd screenshots] the world never opened");
                finish(minecraft);
            }
            return;
        }
        ticks++;
        if (index == -1) {
            if (ticks < 60) {
                return;
            }
            load(minecraft);
            index = 0;
            ticks = 0;
            if (SHOTS.isEmpty()) {
                LOGGER.error("[AstralEnd screenshots] no viewpoints in astralend-shots.txt");
                finish(minecraft);
                return;
            }
            teleport(server, SHOTS.get(0));
            return;
        }
        if (index >= SHOTS.size()) {
            if (ticks > 40) {
                finish(minecraft);
            }
            return;
        }
        boolean inEnd = minecraft.level != null && minecraft.level.dimension() == Level.END;
        // Mob shots: summon the mob once the area has loaded around the camera.
        Shot current = SHOTS.get(index);
        if (current.mob() != null && ticks == 100) {
            server.execute(() -> summon(server.getLevel(Level.END), current));
        }
        boolean drawn = minecraft.levelRenderer.hasRenderedAllSections();
        if ((inEnd && ticks >= SETTLE_TICKS && drawn) || ticks >= MAX_WAIT_TICKS) {
            Shot shot = SHOTS.get(index);
            grab(minecraft, shot.name());
            LOGGER.info("[AstralEnd screenshots] {} after {} ticks (all chunks drawn: {})", shot.name(), ticks, drawn);
            index++;
            ticks = 0;
            if (index < SHOTS.size()) {
                teleport(server, SHOTS.get(index));
            }
        }
    }

    static void load(Minecraft minecraft) {
        File file = new File(minecraft.gameDirectory, "astralend-shots.txt");
        try {
            for (String line : Files.readAllLines(file.toPath())) {
                String[] p = line.trim().split("\\s+");
                if (p.length == 6 || p.length == 7) {
                    SHOTS.add(new Shot(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                        Float.parseFloat(p[4]), Float.parseFloat(p[5]), null, 0, 0, 0, p.length == 7 ? p[6] : ""));
                } else if (p.length >= 10) {
                    SHOTS.add(new Shot(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                        Float.parseFloat(p[4]), Float.parseFloat(p[5]), p[6], Double.parseDouble(p[7]), Double.parseDouble(p[8]),
                        Double.parseDouble(p[9]), p.length > 10 ? p[10] : ""));
                }
            }
        } catch (IOException e) {
            LOGGER.error("[AstralEnd screenshots] couldn't read {}", file, e);
        }
    }

    static void teleport(IntegratedServer server, Shot shot) {
        // Phase 8: "armor" photographs the player (in third person, wearing upgraded gear); "forge" opens
        // the Astral Forge's screen.
        Minecraft minecraft = Minecraft.getInstance();
        boolean showPlayer = shot.style().contains("armor");
        minecraft.options.setCameraType(showPlayer ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT : net.minecraft.client.CameraType.FIRST_PERSON);
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ServerLevel end = server.getLevel(Level.END);
            player.closeContainer();
            server.tickRateManager().setFrozen(false);
            player.getInventory().clearContent();
            player.setGameMode(GameType.SPECTATOR);
            player.teleport(new TeleportTransition(end, new Vec3(shot.x(), shot.y(), shot.z()), Vec3.ZERO, shot.yaw(), shot.pitch(),
                TeleportTransition.DO_NOTHING));
            if (showPlayer || shot.style().contains("forge")) {
                StarforgeShots.setUp(server, end, player, shot.x(), shot.y(), shot.z(), shot.yaw(), shot.style());
            }
            // Clear away the previous shot's mob and lantern.
            if (summoned != null) {
                summoned.discard();
                summoned = null;
            }
            if (lantern != null) {
                end.setBlock(lantern, Blocks.AIR.defaultBlockState(), 3);
                lantern = null;
            }
            // A fixed flare phase for the shot: the first shot settles it to calm (so a flare can't start
            // in the middle of the biome shots), and the Phase 5 shots ask for the phase they show.
            Sunstorms flares = Sunstorms.get(end);
            for (String style : shot.style().split(",")) {
                event(end, BlockPos.containing(shot.x(), shot.y(), shot.z()), style);
            }
            if (index == 0) {
                flares.setPhase(end, FlarePhase.CALM, end.getRandom());
            }
        });
    }

    // One of a shot's styles that changes the world: flare=<phase>, clear, bridges or wake (see
    // the comments in each). `here` is the camera's position.
    static void event(ServerLevel end, BlockPos here, String style) {
        Sunstorms flares = Sunstorms.get(end);
        if (style.startsWith("flare=")) {
            flares.setPhase(end, FlarePhase.valueOf(style.substring(6).toUpperCase(java.util.Locale.ROOT)), end.getRandom());
        }
        if (style.equals("clear")) {
            // A still life: no wandering Endermen in the picture.
            for (Entity other : end.getEntities((Entity) null, new AABB(here).inflate(32.0),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                other.discard();
            }
        }
        if (style.equals("bridges")) {
            // A Constellation Maze with all its bridges lit (they stay lit 20 s, long enough).
            for (int id = 0; id < 4; id++) {
                com.ggrantrichards.astralend.maze.MazeScoping.lightBridges(end, here.offset(24, -18, 24), id);
            }
        }
        if (style.startsWith("wyrm_")) {
            // The Spire Wyrm nearest the camera, on cue: wake it, have it breathe just ahead of the
            // camera, or ring the bell nearest it and stun it.
            List<com.ggrantrichards.astralend.entity.SpireWyrm> wyrms = end.getEntitiesOfClass(com.ggrantrichards.astralend.entity.SpireWyrm.class,
                new AABB(here).inflate(64.0, 100.0, 64.0));
            if (wyrms.isEmpty()) {
                LOGGER.error("[AstralEnd screenshots] no Spire Wyrm near {} for {}", here.toShortString(), style);
                return;
            }
            com.ggrantrichards.astralend.entity.SpireWyrm wyrm = wyrms.get(0);
            switch (style) {
                case "wyrm_wake" -> wyrm.wake(end);
                case "wyrm_breath" -> {
                    // between the camera and the Wyrm, five blocks out: breathed at whoever is filming
                    wyrm.wake(end);
                    net.minecraft.world.phys.Vec3 camera = net.minecraft.world.phys.Vec3.atCenterOf(here);
                    wyrm.breatheAtOnCue(end, camera.add(wyrm.position().subtract(camera).normalize().scale(5.0)));
                }
                case "wyrm_circle" -> {
                    wyrm.wake(end);
                    wyrm.circleNestOnCue(400);
                }
                case "wyrm_stun" -> {
                    wyrm.wake(end);
                    wyrm.stunAtBellOnCue(end, here);
                }
                default -> LOGGER.error("[AstralEnd screenshots] unknown event {}", style);
            }
        }
        if (style.equals("wake")) {
            // A Rift Maw, woken: light its eyes and open its throat.
            BlockPos throat = null;
            for (BlockPos p : BlockPos.betweenClosed(here.offset(-40, -24, -40), here.offset(40, 16, 40))) {
                var state = end.getBlockState(p);
                if (state.is(ModBlocks.MAW_EYE.get())) {
                    end.setBlock(p, state.setValue(com.ggrantrichards.astralend.maw.MawEyeBlock.LIT, true), 2);
                } else if (throat == null && state.is(ModBlocks.MAW_THROAT.get())) {
                    throat = p.immutable();
                }
            }
            if (throat != null) {
                com.ggrantrichards.astralend.maw.MawNetwork.wake(end, throat, null);
            }
        }
    }

    static void summon(ServerLevel end, Shot shot) {
        BlockPos at = BlockPos.containing(shot.mobX(), shot.mobY(), shot.mobZ());
        end.getChunk(at.getX() >> 4, at.getZ() >> 4);  // make sure it's loaded
        if (shot.mob().endsWith("display")) {
            FlareDisplay.build(end, at);
            return;
        }
        // Nothing else of ours in the picture (a mob left over from an earlier shot, say).
        for (Entity other : end.getEntities((Entity) null, new AABB(at).inflate(24.0),
                e -> BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals(AstralEnd.MODID))) {
            other.discard();
        }
        BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(shot.mob())).ifPresentOrElse(type -> {
            Entity entity = type.create(end, EntitySpawnReason.COMMAND);
            if (entity == null) {
                return;
            }
            // 135: facing the camera (north-west); "side": in profile; "angled": three-quarters toward it
            float yaw = shot.style().contains("side") ? 45.0F : shot.style().contains("angled") ? 100.0F : 135.0F;
            entity.snapTo(shot.mobX(), shot.mobY(), shot.mobZ(), yaw, 0.0F);
            entity.setYHeadRot(yaw);
            entity.setNoGravity(true);
            // A big mob can clip into a wall and take damage (and flash red): move it to the nearest
            // spot where it fits.
            search:
            for (int r = 1; r <= 3 && !end.noCollision(entity); r++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        entity.snapTo(shot.mobX() + dx * 0.5, shot.mobY(), shot.mobZ() + dz * 0.5, yaw, 0.0F);
                        if (end.noCollision(entity)) {
                            break search;
                        }
                    }
                }
            }
            if (!end.noCollision(entity)) {
                entity.snapTo(shot.mobX(), shot.mobY(), shot.mobZ(), yaw, 0.0F);
            }
            if (entity instanceof UmbralStalker stalker) {
                stalker.showAsHunting();   // otherwise, with no prey, there'd be nothing to see
            }
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                mob.yBodyRot = yaw;
            }
            end.addFreshEntity(entity);
            summoned = entity;
            if (shot.style().contains("lit")) {
                lantern = at.east();
                end.setBlock(lantern, ModBlocks.SPORE_LANTERN.get().defaultBlockState(), 3);
            }
        }, () -> LOGGER.error("[AstralEnd screenshots] unknown mob {}", shot.mob()));
    }

    private static void grab(Minecraft minecraft, String name) {
        Screenshot.grab(minecraft.gameDirectory, "astralend-" + name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
            message -> LOGGER.info("[AstralEnd screenshots] saved {}", name));
    }

    private static void finish(Minecraft minecraft) {
        LOGGER.info("[AstralEnd screenshots] ASTRALEND SCREENSHOTS DONE");
        done = true;
        minecraft.execute(minecraft::stop);
    }

    private ClientScreenshots() {}
}
