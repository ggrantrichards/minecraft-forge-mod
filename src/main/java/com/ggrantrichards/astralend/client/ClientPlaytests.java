package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.entity.DriftWhale;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.entity.SpireWyrm;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// Scripted playtests in the real game (CI runs them; see .github/scripts/playtests.sh). Each one
// sets a scene up the way a player would meet it - in survival, with real items - then plays it with
// real key presses and camera moves, a picture every few ticks and a line of log for what happened.
// Judged by eye afterwards: they show how something feels to play, which the survey's checks can't.
//
//   whale_ride     mount a Drift Whale with a Chorus Flower on a Stick and steer it: forward, a turn,
//                  a climb (first person, then from behind)
//   wyrm_fight     step onto a Resonant Spire's top in survival: the Wyrm wakes and fights; the camera
//                  keeps it in view
//   corona_drop    teleport into the Corona from high up, as with /tp, and look round as you land
//   upwelling      drink Upwelling under a ledge: fall up onto its underside, walk on it, turn, jump off
//   biome_border   fly east out of the Nebula Fields across its border: does the sky shift gently,
//                  do the biomes interlock?
//   maw_hall       stand in the hall inside a Rift Maw's skull and look all round it
//   remnant_flight fly in toward the Remnant from far off: the picture of it in the sky gives way to
//                  the real thing
public final class ClientPlaytests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[AstralEnd playtest] ";

    private record Test(String name, int ticks, int every) {}

    private static final List<Test> TESTS = List.of(
        new Test("whale_ride", 300, 10),
        new Test("wyrm_fight", 700, 10),
        new Test("corona_drop", 260, 13),
        new Test("upwelling", 320, 10),
        new Test("biome_border", 360, 12),
        new Test("maw_hall", 240, 12),
        new Test("remnant_flight", 400, 16));

    private static int index = -1;
    private static int ticks;
    private static int waiting;
    private static boolean done;
    private static Entity subject;    // on the server: the whale or the Wyrm under test

    public static void register() {
        if (Boolean.getBoolean("astralend.playtests")) {
            TickEvent.ClientTickEvent.Post.BUS.addListener(ClientPlaytests::onTick);
        }
    }

    private static void onTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (done) {
            return;
        }
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (minecraft.player == null || server == null || server.getPlayerList().getPlayers().isEmpty()) {
            if (++waiting == 3600) {
                LOGGER.error(TAG + "the world never opened");
                finish(minecraft);
            }
            return;
        }
        if (index == -1) {
            if (++waiting < 80) {
                return;
            }
            ClientScreenshots.load(minecraft);
            new File(minecraft.gameDirectory, "screenshots/playtest").mkdirs();
            start(minecraft, server, 0);
            return;
        }
        Test test = TESTS.get(index);
        ticks++;
        if (ticks < 0) {
            return;       // settling: chunks loading round the new place
        }
        try {
            play(minecraft, server, test, ticks);
        } catch (RuntimeException e) {
            LOGGER.error(TAG + test.name() + " failed at tick " + ticks, e);
            ticks = test.ticks();
        }
        if (ticks % test.every() == 0) {
            Screenshot.grab(minecraft.gameDirectory, String.format("playtest/%s_%04d.png", test.name(), ticks),
                minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
        }
        if (ticks >= test.ticks()) {
            release(minecraft);
            LOGGER.info(TAG + "{} done", test.name());
            if (index + 1 < TESTS.size()) {
                start(minecraft, server, index + 1);
            } else {
                finish(minecraft);
            }
        }
    }

    private static ClientScreenshots.Shot shot(String name) {
        for (ClientScreenshots.Shot s : ClientScreenshots.SHOTS) {
            if (s.name().equals(name)) {
                return s;
            }
        }
        return null;
    }

    private static void start(Minecraft minecraft, IntegratedServer server, int which) {
        index = which;
        ticks = -200;     // ten seconds for the place to load before the test plays
        Test test = TESTS.get(which);
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        LOGGER.info(TAG + "setting up {}", test.name());
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            ServerLevel end = server.getLevel(Level.END);
            if (subject != null) {
                subject.discard();
                subject = null;
            }
            player.stopRiding();
            player.getInventory().clearContent();
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.removeAllEffects();
            switch (test.name()) {
                case "whale_ride" -> {
                    ClientScreenshots.Shot s = shot("mob_drift_whale_side");
                    Vec3 at = s != null ? new Vec3(s.mobX(), s.mobY() + 6, s.mobZ()) : new Vec3(0, 120, 0);
                    put(player, end, at.add(-4, 0, 0), -90, 10);
                    DriftWhale whale = ModEntities.DRIFT_WHALE.get().create(end, EntitySpawnReason.COMMAND);
                    whale.snapTo(at.x, at.y, at.z, -90, 0);
                    whale.setPersistenceRequired();
                    end.addFreshEntity(whale);
                    subject = whale;
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHORUS_FLOWER_ON_A_STICK.get()));
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 2000, 0, false, false));
                }
                case "wyrm_fight" -> {
                    ClientScreenshots.Shot s = shot("pt_wyrm_nest");
                    if (s == null) {
                        LOGGER.error(TAG + "no Resonant Spire nest in astralend-shots.txt");
                        return;
                    }
                    Vec3 nest = new Vec3(s.x(), s.y(), s.z());
                    put(player, end, nest.add(8.5, 0, 0.5), 90, 0);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
                    player.getInventory().add(new ItemStack(Items.BOW));
                    player.getInventory().add(new ItemStack(Items.ARROW, 64));
                    // Kept alive so the whole fight plays (the damage still shows, as red flashes).
                    player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 2000, 3, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2000, 2, false, false));
                    end.getEntitiesOfClass(SpireWyrm.class, new net.minecraft.world.phys.AABB(nest, nest).inflate(40))
                        .stream().findFirst().ifPresent(w -> subject = w);
                }
                case "upwelling" -> {
                    // A ledge to hang from: a 9x9 roof of Nightstone 6 blocks over the Nebula Fields.
                    ClientScreenshots.Shot s = shot("nebula_fields");
                    BlockPos ground = s != null ? BlockPos.containing(s.x(), s.y(), s.z()) : new BlockPos(0, 80, 400);
                    ground = end.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, ground);
                    for (BlockPos p : BlockPos.betweenClosed(ground.offset(-4, 6, -4), ground.offset(4, 6, 4))) {
                        end.setBlockAndUpdate(p, com.ggrantrichards.astralend.ModBlocks.NIGHTSTONE.get().defaultBlockState());
                    }
                    for (BlockPos p : BlockPos.betweenClosed(ground.offset(-4, 0, -4), ground.offset(4, 5, 4))) {
                        end.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                    put(player, end, Vec3.atBottomCenterOf(ground), 0, 0);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.alchemy.PotionContents
                        .createItemStack(Items.POTION, com.ggrantrichards.astralend.ModEffects.UPWELLING_POTION.getHolder().orElseThrow()));
                }
                case "biome_border", "remnant_flight" -> {
                    ClientScreenshots.Shot s = shot(test.name().equals("biome_border") ? "pt_border" : "structure_remnant");
                    if (s == null) {
                        LOGGER.error(TAG + "nowhere to start {} in astralend-shots.txt", test.name());
                        return;
                    }
                    double back = test.name().equals("remnant_flight") ? 260 : 0;     // from about 300 blocks out
                    double yaw = Math.toRadians(s.yaw());
                    put(player, end, new Vec3(s.x() + Math.sin(yaw) * back, s.y() + 10, s.z() - Math.cos(yaw) * back), s.yaw(), 5);
                    player.setGameMode(GameType.CREATIVE);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
                case "maw_hall" -> {
                    ClientScreenshots.Shot s = shot("structure_rift_maw_hall");
                    if (s == null) {
                        LOGGER.error(TAG + "no Rift Maw hall in astralend-shots.txt");
                        return;
                    }
                    put(player, end, new Vec3(s.x(), s.y() - 1.6, s.z()), s.yaw(), 0);
                }
                case "corona_drop" -> {
                    ClientScreenshots.Shot s = shot("corona");
                    Vec3 at = s != null ? new Vec3(s.x() + 26, 140, s.z() + 26) : new Vec3(-2750, 140, -1000);
                    put(player, end, at, 0, 20);
                }
                default -> { }
            }
        });
    }

    private static void put(ServerPlayer player, ServerLevel end, Vec3 at, float yaw, float pitch) {
        player.teleport(new TeleportTransition(end, at, Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
    }

    // One tick of the test: what the player presses and where they look.
    private static void play(Minecraft minecraft, IntegratedServer server, Test test, int t) {
        Player player = minecraft.player;
        var keys = minecraft.options;
        switch (test.name()) {
            case "whale_ride" -> {
                if (t == 1) {
                    server.execute(() -> {
                        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
                        if (subject != null) {
                            p.startRiding(subject);
                        }
                    });
                }
                keys.keyUp.setDown(t > 10 && t < 260);
                keys.keyJump.setDown(t >= 180 && t < 230);
                if (t >= 100 && t < 150) {
                    player.setYRot(player.getYRot() + 1.2F);          // a turn to the right
                }
                player.setXRot(t < 180 ? 5.0F : -15.0F);
                if (t == 200) {
                    minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (t % 20 == 0) {
                    Entity v = player.getVehicle();
                    LOGGER.info(TAG + "whale_ride t={} riding={} at {} speed {} rider eye {}", t, v != null,
                        v != null ? v.position().toString() : "-", v != null ? String.format("%.3f", v.getDeltaMovement().length()) : "-",
                        player.getEyePosition().toString());
                }
            }
            case "wyrm_fight" -> {
                Entity w = subject != null ? minecraft.level.getEntity(subject.getId()) : null;
                if (w != null) {
                    Vec3 d = w.position().add(0, 1, 0).subtract(player.getEyePosition());
                    float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
                    float pitch = (float) -Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()));
                    player.setYRot(Mth.approachDegrees(player.getYRot(), yaw, 8.0F));
                    player.setXRot(Mth.approach(player.getXRot(), Mth.clamp(pitch, -60, 60), 6.0F));
                }
                if (t % 20 == 0 && subject instanceof SpireWyrm wyrm) {
                    LOGGER.info(TAG + "wyrm_fight t={} phase={} health={} at {} player health {}", t, wyrm.phase(),
                        String.format("%.0f", wyrm.getHealth()), wyrm.position(), String.format("%.1f", player.getHealth()));
                }
                if (t % 40 == 20 && subject instanceof SpireWyrm wyrm && wyrm.position().distanceTo(player.position()) < 6) {
                    server.execute(() -> wyrm.hurtServer((ServerLevel) wyrm.level(), wyrm.damageSources().playerAttack(
                        server.getPlayerList().getPlayers().get(0)), 7.0F));   // a sword hit when it's in reach
                }
            }
            case "upwelling" -> {
                if (t == 5) {
                    keys.keyUse.setDown(true);                       // drink it
                }
                if (t == 50) {
                    keys.keyUse.setDown(false);
                }
                keys.keyUp.setDown(t > 120 && t < 220);              // walk along the ceiling
                if (t >= 160 && t < 200) {
                    player.setYRot(player.getYRot() + 2.0F);
                }
                keys.keyJump.setDown(t >= 240 && t < 246);          // and drop off it
                if (t == 280) {
                    minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                if (t % 20 == 0) {
                    LOGGER.info(TAG + "upwelling t={} y={} on ground {} gravity {} upside down {}", t, String.format("%.2f", player.getY()),
                        player.onGround(), player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY),
                        UpsideDown.isFlipped(player));
                }
            }
            case "biome_border", "remnant_flight" -> {
                keys.keyUp.setDown(true);
                keys.keySprint.setDown(test.name().equals("biome_border"));
                if (t % 30 == 0) {
                    LOGGER.info(TAG + "{} t={} at {} biome {}", test.name(), t, player.blockPosition().toShortString(),
                        minecraft.level.getBiome(player.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("?"));
                }
            }
            case "maw_hall" -> {
                player.setYRot(player.getYRot() + 1.5F);
                player.setXRot(10.0F * Mth.sin(t * 0.05F));
            }
            case "corona_drop" -> {
                player.setYRot(player.getYRot() + 2.0F);              // look all the way round as you fall
                player.setXRot(t < 120 ? 25.0F : 5.0F);
                if (t % 26 == 0) {
                    LOGGER.info(TAG + "corona_drop t={} at {} on ground {} biome {}", t, player.position(), player.onGround(),
                        minecraft.level.getBiome(player.blockPosition()).unwrapKey().map(k -> k.identifier().toString()).orElse("?"));
                }
            }
            default -> { }
        }
    }

    private static void release(Minecraft minecraft) {
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keyUse.setDown(false);
        minecraft.options.keySprint.setDown(false);
        minecraft.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static void finish(Minecraft minecraft) {
        LOGGER.info(TAG + "ASTRALEND PLAYTESTS DONE");
        done = true;
        minecraft.execute(minecraft::stop);
    }

    private ClientPlaytests() {}
}
