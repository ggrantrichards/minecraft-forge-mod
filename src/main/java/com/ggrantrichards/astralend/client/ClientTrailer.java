package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.comet.LiveComet;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// Development tool, not part of normal play. With -Dastralend.trailer=true (./gradlew runClient
// -PastralendTrailer) the game opens the world the worldgen survey generated and films the trailer:
// for each shot in astralend-trailer.txt (tools/trailer/scenes.txt) it flies to the survey's viewpoint,
// waits for the chunks to draw, then moves the camera along the shot's path one step per game tick,
// saving every tick as a frame (screenshots/trailer/NN_<shot>_FFFF.png). The game runs at a few ticks a
// second while it films, so every frame is drawn in full however slow the renderer is; played back at
// 24 frames a second, everything moves at about its real speed. tools/trailer/compose.py cuts the
// frames together with the titles and the score.
public final class ClientTrailer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[AstralEnd trailer] ";
    // The game's speed while filming is set per shot from how fast that view draws, so that there is
    // always a fresh picture for every tick (never more than this, never less than a tick in 3 s).
    private static final float MAX_RATE = Float.parseFloat(System.getProperty("astralend.trailerRate", "5"));

    private record Scene(int index, String shot, int frames, Map<String, String> opts, Map<Integer, List<String>> events) {
        double num(String key, double fallback) {
            return opts.containsKey(key) ? Double.parseDouble(opts.get(key)) : fallback;
        }
    }

    private enum Phase { WAIT, SETTLE, ARM, RECORD }

    private static final List<Scene> SCENES = new ArrayList<>();
    private static int sceneIndex = -1;
    private static Phase phase = Phase.WAIT;
    private static int ticks;
    private static int frame;
    private static boolean done;
    private static long recordStart;
    private static int renders;           // pictures drawn so far (counted as each one sets its camera angles)
    private static int rendersAtGrab;
    private static int repeats;           // frames saved with no new picture since the last one
    private static float rate;

    // the shot being filmed: its starting eye position and angles, and what it looks at
    private static Vec3 eye0;
    private static float yaw0;
    private static float pitch0;
    private static Vec3 focus;
    private static Entity mob;            // on the server
    private static Vec3 mob0;
    private static Vec3 aim;              // smoothed look target (the comet)
    private static volatile Vec3[] comet; // start, control, end
    private static float roll;

    public static void register() {
        if (Boolean.getBoolean("astralend.trailer")) {
            TickEvent.ClientTickEvent.Post.BUS.addListener(ClientTrailer::onTick);
            ViewportEvent.ComputeCameraAngles.BUS.addListener(event -> {
                renders++;
                if (phase == Phase.RECORD || phase == Phase.ARM || phase == Phase.SETTLE) {
                    event.setRoll(roll);
                }
            });
        }
    }

    private static void onTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (done) {
            return;
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null || server.getPlayerList().getPlayers().isEmpty()) {
            if (++ticks == 3600) {
                LOGGER.error(TAG + "the world never opened");
                finish(minecraft, server);
            }
            return;
        }
        switch (phase) {
            case WAIT -> {
                if (++ticks < 60) {
                    return;
                }
                ClientScreenshots.load(minecraft);
                loadScenes(minecraft);
                new File(minecraft.gameDirectory, "screenshots/trailer").mkdirs();
                hideHud(minecraft);
                next(minecraft, server);
            }
            case SETTLE -> {
                Scene scene = SCENES.get(sceneIndex);
                ticks++;
                if (ticks == 40 && shot(scene).mob() != null) {
                    server.execute(() -> ClientScreenshots.summon(server.getLevel(Level.END), shot(scene)));
                }
                if (ticks == 60) {
                    mob = shot(scene).mob() != null ? ClientScreenshots.summoned : null;
                    mob0 = mob != null ? mob.position() : null;
                }
                if (ticks >= 60) {
                    findFocus(minecraft, scene);
                }
                pose(minecraft, server, scene, 0);
                boolean inEnd = minecraft.level != null && minecraft.level.dimension() == Level.END;
                if ((inEnd && ticks >= 140 && minecraft.levelRenderer.hasRenderedAllSections()) || ticks >= 700) {
                    server.execute(() -> server.tickRateManager().setTickRate(2.0F));
                    rendersAtGrab = renders;
                    recordStart = System.nanoTime();
                    phase = Phase.ARM;
                    ticks = 0;
                }
            }
            case ARM -> {
                // Measure how fast this view draws (for about two seconds at 2 ticks a second), then
                // film at a third of that.
                Scene scene = SCENES.get(sceneIndex);
                pose(minecraft, server, scene, 0);
                ++ticks;
                if (ticks == 5) {
                    double seconds = (System.nanoTime() - recordStart) / 1e9;
                    double fps = (renders - rendersAtGrab) / Math.max(0.1, seconds);
                    rate = (float) Mth.clamp(fps / 3.0, 0.34, MAX_RATE);
                    float r = rate;
                    server.execute(() -> server.tickRateManager().setTickRate(r));
                    LOGGER.info(TAG + "{} draws at {} fps; filming at {} ticks a second", scene.shot(),
                        String.format("%.1f", fps), String.format("%.2f", rate));
                }
                if (ticks >= 8) {
                    phase = Phase.RECORD;
                    frame = 0;
                    repeats = 0;
                    rendersAtGrab = renders;
                    recordStart = System.nanoTime();
                }
            }
            case RECORD -> {
                Scene scene = SCENES.get(sceneIndex);
                if (frame > 0) {
                    if (renders == rendersAtGrab) {
                        repeats++;
                    }
                    rendersAtGrab = renders;
                    grab(minecraft, scene, frame - 1);
                }
                if (frame == scene.frames()) {
                    double seconds = (System.nanoTime() - recordStart) / 1e9;
                    LOGGER.info(TAG + "filmed {} ({} frames in {} s; {} repeated)", scene.shot(), scene.frames(),
                        String.format("%.1f", seconds), repeats);
                    server.execute(() -> server.tickRateManager().setTickRate(20.0F));
                    next(minecraft, server);
                    return;
                }
                for (String what : scene.events().getOrDefault(frame, List.of())) {
                    BlockPos here = BlockPos.containing(eye0);
                    server.execute(() -> ClientScreenshots.event(server.getLevel(Level.END), here, what));
                }
                if (frame == 0 && scene.opts().containsKey("comet")) {
                    launchComet(server, scene);
                }
                pose(minecraft, server, scene, frame);
                frame++;
            }
        }
    }

    // ---- the shots

    private static ClientScreenshots.Shot shot(Scene scene) {
        for (ClientScreenshots.Shot s : ClientScreenshots.SHOTS) {
            if (s.name().equals(scene.shot())) {
                return s;
            }
        }
        return null;
    }

    private static void loadScenes(Minecraft minecraft) {
        File file = new File(minecraft.gameDirectory, "astralend-trailer.txt");
        try {
            int index = 0;
            for (String raw : Files.readAllLines(file.toPath())) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] words = line.split("\\|")[0].trim().split("\\s+");
                if (words[0].equals("card") || words[0].equals("logo")) {
                    continue;
                }
                Map<String, String> opts = new HashMap<>();
                Map<Integer, List<String>> events = new HashMap<>();
                for (int i = 2; i < words.length; i++) {
                    String w = words[i];
                    if (w.contains("@")) {
                        String[] e = w.split("@");
                        events.computeIfAbsent(Integer.parseInt(e[1]), k -> new ArrayList<>()).add(e[0]);
                    } else if (w.contains("=")) {
                        String[] kv = w.split("=", 2);
                        opts.put(kv[0], kv[1]);
                    } else {
                        opts.put(w, "");
                    }
                }
                SCENES.add(new Scene(index++, words[0], Integer.parseInt(words[1]), opts, events));
            }
            LOGGER.info(TAG + "{} shots to film", SCENES.size());
        } catch (IOException e) {
            LOGGER.error(TAG + "couldn't read {}", file, e);
        }
    }

    private static void next(Minecraft minecraft, IntegratedServer server) {
        sceneIndex++;
        while (sceneIndex < SCENES.size() && shot(SCENES.get(sceneIndex)) == null) {
            LOGGER.error(TAG + "no viewpoint called {}; skipping it", SCENES.get(sceneIndex).shot());
            sceneIndex++;
        }
        if (sceneIndex >= SCENES.size()) {
            finish(minecraft, server);
            return;
        }
        Scene scene = SCENES.get(sceneIndex);
        ClientScreenshots.Shot base = shot(scene);
        // The shot's own styles happen as it's set up, except the ones this scene times itself; the
        // scene's own flare= and clear join them.
        List<String> styles = new ArrayList<>();
        for (String style : base.style().split(",")) {
            if (!style.isEmpty() && scene.events().values().stream().noneMatch(list -> list.contains(style))) {
                styles.add(style);
            }
        }
        scene.opts().keySet().stream().filter(k -> k.equals("clear")).forEach(styles::add);
        for (String raw : List.of(scene.opts().getOrDefault("flare", ""))) {
            if (!raw.isEmpty()) {
                styles.add("flare=" + raw);
            }
        }
        ClientScreenshots.Shot setUp = new ClientScreenshots.Shot(base.name(), base.x(), base.y(), base.z(), base.yaw(), base.pitch(),
            base.mob(), base.mobX(), base.mobY(), base.mobZ(), String.join(",", styles));
        ClientScreenshots.teleport(server, setUp);
        eye0 = new Vec3(base.x(), base.y() + 1.62, base.z());
        yaw0 = base.yaw();
        pitch0 = base.pitch();
        focus = null;
        mob = null;
        mob0 = null;
        aim = null;
        comet = null;
        roll = 0.0F;
        phase = Phase.SETTLE;
        ticks = 0;
        LOGGER.info(TAG + "setting up {} ({} frames)", scene.shot(), scene.frames());
    }

    // What an orbit circles and looks at: the shot's mob, or the ground the view first meets.
    private static void findFocus(Minecraft minecraft, Scene scene) {
        if (focus != null) {
            return;
        }
        if (mob != null && !"hit".equals(scene.opts().get("focus"))) {
            Entity seen = minecraft.level.getEntity(mob.getId());
            Entity which = seen != null ? seen : mob;
            focus = which.getBoundingBox().getCenter();
            return;
        }
        if (scene.opts().containsKey("orbit") || "hit".equals(scene.opts().get("focus"))) {
            if (!minecraft.levelRenderer.hasRenderedAllSections() && ticks < 300) {
                return;   // wait until the ground is there to hit
            }
            Vec3 dir = Vec3.directionFromRotation(pitch0, yaw0);
            HitResult hit = minecraft.level.clip(new ClipContext(eye0, eye0.add(dir.scale(220.0)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, minecraft.player));
            focus = hit.getType() == HitResult.Type.MISS ? eye0.add(dir.scale(40.0)) : hit.getLocation();
        }
    }

    // ---- the camera

    private static double ease(Scene scene, double t) {
        return switch (scene.opts().getOrDefault("ease", "both")) {
            case "none" -> t;
            case "in" -> t * t;
            case "out" -> 1 - (1 - t) * (1 - t);
            default -> 0.35 * t + 0.65 * (t * t * (3 - 2 * t));
        };
    }

    private static void pose(Minecraft minecraft, IntegratedServer server, Scene scene, int f) {
        double t = scene.frames() > 1 ? (double) f / (scene.frames() - 1) : 0.0;
        double e = ease(scene, t);
        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw0);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);   // facing south (+z), your right is -x
        Vec3 eye = eye0;
        float yaw = yaw0;
        float pitch = pitch0;
        if (scene.opts().containsKey("pitch")) {
            String[] p = scene.opts().get("pitch").split(":");
            pitch = (float) Mth.lerp(e, Double.parseDouble(p[0]), Double.parseDouble(p[1]));
        }

        if (scene.opts().containsKey("comet")) {
            Vec3 target = cometAt(f);
            if (target != null) {
                aim = aim == null ? target : aim.lerp(target, 0.22);
            }
        }

        if (scene.opts().containsKey("orbit") && focus != null) {
            Vec3 flat = new Vec3(eye0.x - focus.x, 0, eye0.z - focus.z);
            double radius = flat.length() - scene.num("fwd", 0) * e;
            double angle = Math.atan2(flat.z, flat.x) + Math.toRadians(scene.num("orbit", 0)) * e;
            eye = new Vec3(focus.x + Math.cos(angle) * radius, eye0.y + scene.num("up", 0) * e, focus.z + Math.sin(angle) * radius);
            aim = focus;
        } else if (scene.opts().containsKey("track") && mob0 != null) {
            Vec3 heading = Vec3.directionFromRotation(0.0F, mob.getYRot());
            Vec3 step = heading.scale(scene.num("track", 0) * f);
            eye = eye0.add(step);
            Vec3 mobAt = mob0.add(step);
            server.execute(() -> mob.setPos(mobAt));
            Entity seen = minecraft.level.getEntity(mob.getId());
            if (seen != null) {
                seen.snapTo(mobAt.x, mobAt.y, mobAt.z, seen.getYRot(), seen.getXRot());
            }
            yaw = yaw0 + (float) (scene.num("yaw", 0) * e);
        } else {
            eye = eye0.add(forward.scale(scene.num("fwd", 0) * e)).add(right.scale(scene.num("side", 0) * e))
                .add(0, scene.num("up", 0) * e, 0);
            yaw = yaw0 + (float) (scene.num("yaw", 0) * e);
        }
        if (aim != null && (scene.opts().containsKey("orbit") || scene.opts().containsKey("comet"))) {
            Vec3 d = aim.subtract(eye);
            yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        }
        roll = (float) (scene.num("roll", 0) * e);

        LocalPlayer player = minecraft.player;
        Vec3 feet = eye.subtract(0, player.getEyeHeight(), 0);
        player.snapTo(feet.x, feet.y, feet.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
        player.setDeltaMovement(Vec3.ZERO);
    }

    // ---- the live comet: it comes in high from the far left, crosses the sky and lands ahead

    private static void launchComet(IntegratedServer server, Scene scene) {
        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw0);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);   // facing south (+z), your right is -x
        server.execute(() -> {
            ServerLevel end = server.getLevel(Level.END);
            Vec3 ahead = eye0.add(forward.scale(44.0));
            int ground = end.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(ahead.x), Mth.floor(ahead.z));
            boolean land = ground > end.getMinY() + 8;
            Vec3 stop = new Vec3(Mth.floor(ahead.x) + 0.5, land ? ground - 1 : eye0.y - 14, Mth.floor(ahead.z) + 0.5);
            Vec3 start = stop.add(forward.scale(60.0)).add(right.scale(-70.0)).add(0, 80.0, 0);
            Vec3 control = start.add(stop).scale(0.5).add(right.scale(-12.0)).add(0, 26.0, 0);
            comet = new Vec3[] {start, control, stop};
            LiveComet c = ModEntities.LIVE_COMET.get().create(end, EntitySpawnReason.EVENT);
            if (c != null) {
                c.launch(start, control, stop, (int) (scene.frames() * 0.78), land);
                end.addFreshEntity(c);
            }
        });
    }

    private static Vec3 cometAt(int f) {
        if (comet == null) {
            return null;
        }
        Scene scene = SCENES.get(sceneIndex);
        double t = Math.min(1.0, f / (scene.frames() * 0.78));
        Vec3 p = comet[0].scale((1 - t) * (1 - t)).add(comet[1].scale(2 * (1 - t) * t)).add(comet[2].scale(t * t));
        return t >= 1.0 ? p.add(0, 3.0, 0) : p;
    }

    // ---- frames

    private static void grab(Minecraft minecraft, Scene scene, int f) {
        String name = String.format("trailer/%02d_%s_%04d.png", scene.index(), scene.shot(), f);
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
    }

    private static void hideHud(Minecraft minecraft) {
        TrailerHud.hide(minecraft);
    }

    private static void finish(Minecraft minecraft, IntegratedServer server) {
        if (server != null) {
            server.execute(() -> server.tickRateManager().setTickRate(20.0F));
        }
        LOGGER.info(TAG + "ASTRALEND TRAILER DONE");
        done = true;
        minecraft.execute(minecraft::stop);
    }

    private ClientTrailer() {}
}
