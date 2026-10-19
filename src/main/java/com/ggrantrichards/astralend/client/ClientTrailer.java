package com.ggrantrichards.astralend.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// Development tool, not part of normal play. With -Dastralend.trailer=true (./gradlew runClient
// -PastralendTrailer) the game opens the world the worldgen survey generated and films the trailer:
// for each shot in astralend-trailer.txt (tools/trailer/scenes.txt) it goes to the survey's viewpoint,
// flies the whole of the shot's camera path once first, waiting at points along it until every chunk the
// camera will see has arrived and been drawn, then moves the camera along the path one step per game
// tick, saving every tick as a frame (screenshots/trailer/NN_<shot>_FFFF.png). The game runs at a few
// ticks a second while it films, so every frame is drawn in full however slow the renderer is; played
// back at 24 frames a second, everything moves at about its real speed. The fight scenes are played in
// survival (TrailerFight), and the sounds the world makes are written down frame by frame (TrailerSound)
// so the edit can mix them in. tools/trailer/compose.py cuts it all together.
public final class ClientTrailer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[AstralEnd trailer] ";
    // The game's speed while filming is set per shot from how fast that view draws, so that there are
    // several fresh pictures for every tick (never faster than this, never slower than a tick in 4 s).
    private static final float MAX_RATE = Float.parseFloat(System.getProperty("astralend.trailerRate", "4"));
    // Only the shots whose names contain this are filmed (-Dastralend.trailerOnly=fight, to try a part).
    private static final String ONLY = System.getProperty("astralend.trailerOnly", "");

    record Scene(int index, String shot, int frames, Map<String, String> opts, Map<Integer, List<String>> events) {
        double num(String key, double fallback) {
            return opts.containsKey(key) ? Double.parseDouble(opts.get(key)) : fallback;
        }

        boolean has(String key) {
            return opts.containsKey(key);
        }

        boolean fight() {
            return opts.containsKey("fight");
        }

        boolean third() {
            return "third".equals(opts.get("cam"));
        }
    }

    private enum Phase { WAIT, SETTLE, PRELOAD, ARM, RECORD }

    private static final List<Scene> SCENES = new ArrayList<>();
    private static int sceneIndex = -1;
    private static Phase phase = Phase.WAIT;
    private static int ticks;
    private static int frame;
    private static int waypoint;
    private static boolean done;
    private static long recordStart;
    private static int renders;           // pictures drawn so far (counted as each one sets its camera angles)
    private static int rendersAtGrab;
    private static int repeats;           // frames saved with no new picture since the last one
    private static float rate;
    private static boolean cameraHooked;

    // the shot being filmed: its starting eye position and angles, and what it looks at
    private static Vec3 eye0;
    private static float yaw0;
    private static float pitch0;
    private static Vec3 focus;
    private static Entity mob;            // on the server
    private static Vec3 mob0;
    private static float roll;
    // the third-person camera: where it is and where it looks, set each tick and used by every picture
    private static Vec3 camPos;
    private static float camYaw;
    private static float camPitch;
    private static Vec3 camFocus;         // what it follows, smoothed
    private static Vec3 camBase;          // which way it starts from its focus (flat, unit length)
    private static Method setPosition;

    public static void register() {
        if (Boolean.getBoolean("astralend.trailer")) {
            TickEvent.ClientTickEvent.Post.BUS.addListener(ClientTrailer::onTick);
            ViewportEvent.ComputeCameraAngles.BUS.addListener(event -> renders++);
            TrailerSound.register();
        }
    }

    // Registered once the game is running, so that it's the last to see the camera (after the
    // upside-down view's own turn of it): the trailer's camera has the final word.
    private static void hookCamera() {
        if (cameraHooked) {
            return;
        }
        cameraHooked = true;
        ViewportEvent.ComputeCameraAngles.BUS.addListener(ClientTrailer::onCamera);
    }

    private static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (sceneIndex < 0 || sceneIndex >= SCENES.size() || phase == Phase.WAIT) {
            return;
        }
        Scene scene = SCENES.get(sceneIndex);
        if (scene.third() && camPos != null) {
            moveCamera(event.getCamera(), camPos);
            event.setYaw(camYaw);
            event.setPitch(camPitch);
            event.setRoll(roll);
        } else if (!scene.fight() && roll != 0.0F) {
            event.setRoll(roll);
        }
    }

    private static void moveCamera(Camera camera, Vec3 to) {
        try {
            if (setPosition == null) {
                setPosition = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                setPosition.setAccessible(true);
            }
            setPosition.invoke(camera, to);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error(TAG + "couldn't place the camera", e);
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
                hookCamera();
                ClientScreenshots.load(minecraft);
                loadScenes(minecraft);
                // no autosaves while filming: their "Saving world" note would be in the picture
                server.execute(() -> server.getAllLevels().forEach(level -> level.noSave = true));
                File dir = new File(minecraft.gameDirectory, "screenshots/trailer");
                dir.mkdirs();
                TrailerSound.open(new File(dir, "sounds.txt"));
                TrailerHud.hide(minecraft);
                next(minecraft, server);
            }
            case SETTLE -> {
                Scene scene = SCENES.get(sceneIndex);
                ticks++;
                if (!scene.fight()) {
                    if (ticks == 40 && shot(scene).mob() != null) {
                        server.execute(() -> ClientScreenshots.summon(server.getLevel(Level.END), shot(scene)));
                    }
                    if (ticks == 60) {
                        mob = shot(scene).mob() != null ? ClientScreenshots.summoned : null;
                        mob0 = mob != null ? mob.position() : null;
                        if (mob instanceof Mob m && scene.has("ai")) {
                            server.execute(() -> m.setNoAi(false));   // let it go about its business
                        }
                    }
                    if (ticks >= 60) {
                        findFocus(minecraft, scene);
                    }
                }
                pose(minecraft, server, scene, 0);
                if (ticks >= 80 && inEnd(minecraft)) {
                    phase = Phase.PRELOAD;
                    waypoint = 0;
                    ticks = 0;
                }
            }
            case PRELOAD -> {
                // Fly the path once, waiting at each point until everything in view has come and been
                // drawn - so nothing appears while the shot is filmed. Then back to the start.
                Scene scene = SCENES.get(sceneIndex);
                int points = waypoints(scene);
                int f = waypoint >= points ? 0 : Math.round((float) waypoint * (scene.frames() - 1) / Math.max(1, points - 1));
                pose(minecraft, server, scene, f);
                ticks++;
                boolean ready = ticks >= 6 && chunksArrived(minecraft) && minecraft.levelRenderer.hasRenderedAllSections();
                if (ready || ticks >= 400) {
                    if (!ready) {
                        LOGGER.warn(TAG + "{}: the view at frame {} still wasn't all drawn after 20 s", scene.shot(), f);
                    }
                    if (waypoint >= points) {
                        server.execute(() -> server.tickRateManager().setTickRate(2.0F));
                        rendersAtGrab = renders;
                        recordStart = System.nanoTime();
                        phase = Phase.ARM;
                        ticks = 0;
                    } else {
                        waypoint++;
                        ticks = 0;
                    }
                }
            }
            case ARM -> {
                // Measure how fast this view draws (for about two seconds at 2 ticks a second), then
                // film at a quarter of that.
                Scene scene = SCENES.get(sceneIndex);
                pose(minecraft, server, scene, 0);
                ++ticks;
                if (ticks == 5) {
                    double seconds = (System.nanoTime() - recordStart) / 1e9;
                    double fps = (renders - rendersAtGrab) / Math.max(0.1, seconds);
                    rate = (float) Mth.clamp(fps / 4.0, 0.25, MAX_RATE);
                    float r = rate;
                    server.execute(() -> {
                        server.tickRateManager().setFrozen(false);
                        server.tickRateManager().setTickRate(r);
                    });
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
                TrailerSound.at(scene.index(), frame);
                for (String what : scene.events().getOrDefault(frame, List.of())) {
                    BlockPos here = BlockPos.containing(eye0);
                    server.execute(() -> ClientScreenshots.event(server.getLevel(Level.END), here, what));
                }
                if (scene.fight()) {
                    TrailerFight.tick(minecraft, server, scene, frame);
                }
                pose(minecraft, server, scene, frame);
                TrailerSound.camera(scene.index(), frame, cameraPosition(minecraft, scene), cameraYaw(minecraft, scene));
                frame++;
            }
        }
    }

    private static boolean inEnd(Minecraft minecraft) {
        return minecraft.level != null && minecraft.level.dimension() == Level.END;
    }

    // How many points along the path to stop at before filming: one every 16 frames of a moving shot;
    // a fight, or a camera that circles something close, needs only its start.
    private static int waypoints(Scene scene) {
        if (scene.fight() || scene.third()) {
            return 0;
        }
        boolean moves = scene.has("fwd") || scene.has("side") || scene.has("up") || scene.has("orbit") || scene.has("track")
            || scene.has("yaw") || scene.has("pitch");
        return moves ? Math.max(2, scene.frames() / 16 + 1) : 0;
    }

    // Every chunk the camera can see has come from the server.
    private static boolean chunksArrived(Minecraft minecraft) {
        Vec3 at = minecraft.gameRenderer.getMainCamera().position();
        int cx = Mth.floor(at.x) >> 4, cz = Mth.floor(at.z) >> 4;
        int r = Math.max(2, minecraft.options.getEffectiveRenderDistance() - 2);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= r * r && !minecraft.level.getChunkSource().hasChunk(cx + dx, cz + dz)) {
                    return false;
                }
            }
        }
        return true;
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
                if (words[0].equals("card") || words[0].equals("logo") || words[0].equals("vo") || words[0].equals("music")) {
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
                Scene scene = new Scene(index++, words[0], Integer.parseInt(words[1]), opts, events);
                String name = scene.shot() + (scene.fight() ? " fight " + opts.get("fight") : "");
                if (ONLY.isEmpty() || name.contains(ONLY)) {
                    SCENES.add(scene);
                }
            }
            LOGGER.info(TAG + "{} shots to film{}", SCENES.size(), ONLY.isEmpty() ? "" : " (only those with \"" + ONLY + "\")");
        } catch (IOException e) {
            LOGGER.error(TAG + "couldn't read {}", file, e);
        }
    }

    private static void next(Minecraft minecraft, IntegratedServer server) {
        TrailerSound.at(-1, -1);
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
        focus = null;
        mob = null;
        mob0 = null;
        roll = 0.0F;
        camPos = null;
        camFocus = null;
        camBase = null;
        eye0 = new Vec3(base.x(), base.y() + 1.62, base.z());
        yaw0 = base.yaw();
        pitch0 = base.pitch();
        if (scene.fight()) {
            // Played in survival, in the Leviathan's reliquary (the shot is only where to find it).
            server.execute(() -> server.tickRateManager().setFrozen(true));
            TrailerFight.setUp(minecraft, server, scene, base);
        } else {
            TrailerFight.release(minecraft);
            TrailerHud.hide(minecraft);
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
            if (scene.third()) {
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        }
        phase = Phase.SETTLE;
        ticks = 0;
        LOGGER.info(TAG + "setting up {} ({} frames)", scene.shot() + (scene.fight() ? " (" + scene.opts().get("fight") + ")" : ""),
            scene.frames());
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
        if (scene.has("orbit") || "hit".equals(scene.opts().get("focus"))) {
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

    // roll=a:b turns the view from a to b degrees between frames rollFrom and rollTo (the whole shot by
    // default); roll=b is 0:b.
    private static float rollAt(Scene scene, int f) {
        if (!scene.has("roll")) {
            return 0.0F;
        }
        String[] r = scene.opts().get("roll").split(":");
        double from = r.length > 1 ? Double.parseDouble(r[0]) : 0.0, to = Double.parseDouble(r[r.length - 1]);
        double start = scene.num("rollFrom", 0), end = scene.num("rollTo", scene.frames() - 1);
        double t = Mth.clamp((f - start) / Math.max(1.0, end - start), 0.0, 1.0);
        t = t * t * (3 - 2 * t);
        return (float) Mth.lerp(t, from, to);
    }

    private static void pose(Minecraft minecraft, IntegratedServer server, Scene scene, int f) {
        double t = scene.frames() > 1 ? (double) f / (scene.frames() - 1) : 0.0;
        double e = ease(scene, t);
        roll = rollAt(scene, f);
        if (scene.third()) {
            thirdPerson(minecraft, scene, e);
            return;
        }
        if (scene.fight()) {
            return;   // first person in a fight: the player's own view (TrailerFight turns it)
        }
        Vec3 forward = Vec3.directionFromRotation(0.0F, yaw0);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);   // facing south (+z), your right is -x
        Vec3 eye;
        float yaw = yaw0;
        float pitch = pitch0;
        if (scene.has("pitch")) {
            String[] p = scene.opts().get("pitch").split(":");
            pitch = (float) Mth.lerp(e, Double.parseDouble(p[0]), Double.parseDouble(p[1]));
        }
        Vec3 aim = null;
        if (scene.has("orbit") && focus != null) {
            Vec3 flat = new Vec3(eye0.x - focus.x, 0, eye0.z - focus.z);
            double radius = flat.length() - scene.num("fwd", 0) * e;
            double angle = Math.atan2(flat.z, flat.x) + Math.toRadians(scene.num("orbit", 0)) * e;
            eye = new Vec3(focus.x + Math.cos(angle) * radius, eye0.y + scene.num("up", 0) * e, focus.z + Math.sin(angle) * radius);
            aim = focus;
        } else if (scene.has("track") && mob0 != null) {
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
        if (aim != null) {
            Vec3 d = aim.subtract(eye);
            yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        }

        LocalPlayer player = minecraft.player;
        Vec3 feet = eye.subtract(0, player.getEyeHeight(), 0);
        player.snapTo(feet.x, feet.y, feet.z, yaw, pitch);
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
        player.setDeltaMovement(Vec3.ZERO);
    }

    // The cinematic third-person camera: it follows its subject (the fight's two, or the player), from
    // `dist` blocks off and `height` up, starting `angle` degrees round from its first side (the line
    // across the fight; behind the player otherwise), and circles `orbit` degrees over the shot,
    // drawing back `pull` and rising `rise`.
    private static void thirdPerson(Minecraft minecraft, Scene scene, double e) {
        Vec3 subject = scene.fight() ? TrailerFight.focus(minecraft) : minecraft.player.position().add(0.0, scene.num("look", 1.1), 0.0);
        if (subject == null) {
            return;
        }
        camFocus = camFocus == null ? subject : camFocus.lerp(subject, scene.num("follow", 0.25));
        if (camBase == null) {
            Vec3 b = scene.fight() ? TrailerFight.across(minecraft) : Vec3.directionFromRotation(0.0F, minecraft.player.getYRot()).scale(-1.0);
            camBase = b == null || b.horizontalDistanceSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : new Vec3(b.x, 0.0, b.z).normalize();
        }
        double angle = Math.toRadians(scene.num("angle", 0) + scene.num("orbit", 0) * e);
        Vec3 dir = new Vec3(camBase.x * Math.cos(angle) - camBase.z * Math.sin(angle), 0.0, camBase.x * Math.sin(angle) + camBase.z * Math.cos(angle));
        double dist = scene.num("dist", 6.0) + scene.num("pull", 0.0) * e;
        camPos = camFocus.add(dir.scale(dist)).add(0.0, scene.num("height", 1.5) + scene.num("rise", 0.0) * e, 0.0);
        Vec3 d = camFocus.subtract(camPos);
        camYaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        camPitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
    }

    private static Vec3 cameraPosition(Minecraft minecraft, Scene scene) {
        return scene.third() && camPos != null ? camPos : minecraft.player.getEyePosition();
    }

    private static float cameraYaw(Minecraft minecraft, Scene scene) {
        return scene.third() && camPos != null ? camYaw : minecraft.player.getYRot();
    }

    // ---- frames

    private static void grab(Minecraft minecraft, Scene scene, int f) {
        String name = String.format("trailer/%02d_%s_%04d.png", scene.index(), scene.shot(), f);
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
    }

    private static void finish(Minecraft minecraft, IntegratedServer server) {
        if (server != null) {
            server.execute(() -> {
                server.tickRateManager().setFrozen(false);
                server.tickRateManager().setTickRate(20.0F);
            });
        }
        TrailerFight.release(minecraft);
        TrailerSound.close();
        LOGGER.info(TAG + "ASTRALEND TRAILER DONE");
        done = true;
        minecraft.execute(minecraft::stop);
    }

    private ClientTrailer() {}
}
