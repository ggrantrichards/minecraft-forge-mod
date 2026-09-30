package com.ggrantrichards.astralend.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
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
    // camera. The optional last word: "lit" puts a Spore Lantern beside it, "side" turns it side-on.
    private record Shot(String name, double x, double y, double z, float yaw, float pitch,
                        String mob, double mobX, double mobY, double mobZ, String style) {}

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static int index = -1;
    private static int ticks;
    private static int ticksWithoutWorld;
    private static boolean done;
    private static Entity summoned;
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

    private static void load(Minecraft minecraft) {
        File file = new File(minecraft.gameDirectory, "astralend-shots.txt");
        try {
            for (String line : Files.readAllLines(file.toPath())) {
                String[] p = line.trim().split("\\s+");
                if (p.length == 6) {
                    SHOTS.add(new Shot(p[0], Double.parseDouble(p[1]), Double.parseDouble(p[2]), Double.parseDouble(p[3]),
                        Float.parseFloat(p[4]), Float.parseFloat(p[5]), null, 0, 0, 0, ""));
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

    private static void teleport(IntegratedServer server, Shot shot) {
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ServerLevel end = server.getLevel(Level.END);
            player.setGameMode(GameType.SPECTATOR);
            player.teleport(new TeleportTransition(end, new Vec3(shot.x(), shot.y(), shot.z()), Vec3.ZERO, shot.yaw(), shot.pitch(),
                TeleportTransition.DO_NOTHING));
            // Clear away the previous shot's mob and lantern.
            if (summoned != null) {
                summoned.discard();
                summoned = null;
            }
            if (lantern != null) {
                end.setBlock(lantern, Blocks.AIR.defaultBlockState(), 3);
                lantern = null;
            }
        });
    }

    private static void summon(ServerLevel end, Shot shot) {
        BlockPos at = BlockPos.containing(shot.mobX(), shot.mobY(), shot.mobZ());
        end.getChunk(at.getX() >> 4, at.getZ() >> 4);  // make sure it's loaded
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
            float yaw = shot.style().equals("side") ? 45.0F : 135.0F;  // 135: facing the camera (north-west)
            entity.snapTo(shot.mobX(), shot.mobY(), shot.mobZ(), yaw, 0.0F);
            entity.setYHeadRot(yaw);
            entity.setNoGravity(true);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setPersistenceRequired();
                mob.yBodyRot = yaw;
            }
            end.addFreshEntity(entity);
            summoned = entity;
            if (shot.style().equals("lit")) {
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
