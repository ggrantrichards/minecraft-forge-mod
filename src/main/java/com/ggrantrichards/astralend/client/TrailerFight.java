package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModEffects;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.entity.Curator;
import com.ggrantrichards.astralend.entity.Hollowback;
import com.ggrantrichards.astralend.world.UpwellingEffect;
import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

// The trailer's fight with the Curator (dev tool, with ClientTrailer): played for real, in survival, in a
// Leviathan's reliquary - the player in enchanted netherite with a sword, the HUD up in first person -
// with the keys pressed, the view turned and the blows struck on cue, frame by frame, and the Curator
// on cue too (Curator.playOnCue): it makes the attacks it's told to, when it's told to. Each beat is its
// own shot, set up afresh (or, with "cont", carrying on from the one before it):
//   wake     walking up to it asleep in its reliquary; it wakes
//   charge   it crouches and charges; a sidestep, and it hits a pylon instead
//   strike   it's stunned: sprint in and strike, jumping for critical hits
//   beam     its cannon: the whine, the lock, the beam, and a dive aside
//   flip     the blow that breaks its case: the reliquary turns over and the player falls up
//   ceiling  fighting it upside down, on the ceiling
//   finish   the last blow; it falls, and gravity comes back
// The floor is only sure in the middle of the arena (LeviathanShape.gap), so every beat keeps to it:
// positions are (across, along) in blocks from the Curator's home, "along" toward the pedestal.
final class TrailerFight {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[AstralEnd trailer] ";

    private static Curator curator;       // on the server
    private static int curatorId = -1;
    private static BlockPos home;
    private static Vec3 centre;           // the middle of the floor block under its home
    private static Vec3 along = new Vec3(0, 0, 1);
    private static Vec3 across = new Vec3(1, 0, 0);
    private static String beat = "";
    private static boolean mapped;
    private static volatile boolean ready;    // the beat is set up (the Curator found and staged)
    private static boolean cont;
    private static Vec3 near;

    // ---- setting up a beat (on the client, as its shot begins)

    static void setUp(Minecraft minecraft, IntegratedServer server, ClientTrailer.Scene scene, ClientScreenshots.Shot near) {
        beat = scene.opts().get("fight");
        release(minecraft);
        minecraft.options.setCameraType(scene.third() ? CameraType.THIRD_PERSON_BACK : CameraType.FIRST_PERSON);
        if (scene.third()) {
            TrailerHud.hide(minecraft);
        } else {
            TrailerHud.show(minecraft);
        }
        cont = scene.has("cont");
        TrailerFight.near = new Vec3(near.x(), near.y(), near.z());
        ready = false;
        retry(server);
    }

    // Set up again until it works: the first time, the Curator's chunk may not have loaded yet.
    static void retry(IntegratedServer server) {
        server.execute(() -> {
            try {
                ready = prepare(server, beat, cont, near);
            } catch (RuntimeException e) {
                LOGGER.error(TAG + "couldn't set up the fight's {} beat", beat, e);
            }
        });
    }

    static boolean ready() {
        return ready;
    }

    private static boolean prepare(IntegratedServer server, String beat, boolean cont, Vec3 near) {
        ServerLevel end = server.getLevel(Level.END);
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        if (curator == null || curator.isRemoved()) {
            if (!find(end, near)) {
                // go there, so its chunks (and the Curator) load; ClientTrailer tries again shortly
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(end, near.x, near.y, near.z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
                return false;
            }
        }
        curatorId = curator.getId();
        // The garrison, and anything left lying about from the beat before, out of the way.
        for (Entity e : end.getEntities((Entity) null, new AABB(home).inflate(40.0),
                e -> e instanceof Hollowback || e instanceof ItemEntity || e instanceof ExperienceOrb || e instanceof Projectile
                    || e instanceof net.minecraft.world.entity.monster.Enderman)) {
            e.discard();
        }
        if (!cont) {
            gear(player);
        }
        player.setHealth(player.getMaxHealth());
        // (the reliquary is dark: seen as a player with a night vision potion would see it)
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 900, 0, false, false, false));
        switch (beat) {
            case "wake" -> stage(end, player, 0.0, 4.5, 0.0, -4.0, true, false, 300.0F, 5.0);
            case "charge" -> {
                // in line between the Curator and a pylon behind the player (at -6, +7), so that the
                // charge, dodged, carries on into the pylon
                stage(end, player, -1.5, 1.5, 2.55, -3.45, false, false, 280.0F, 9.0);
            }
            case "strike" -> {
                stage(end, player, 0.0, 4.0, 0.0, -1.5, false, false, 235.0F, 2.5);
                curator.cueStagger(80);
            }
            case "beam" -> stage(end, player, 0.0, 4.5, 0.0, -5.0, false, false, 200.0F, 9.5);
            case "flip" -> stage(end, player, 0.0, 2.2, 0.0, -1.0, false, false, 156.0F, 2.4);
            case "ceiling" -> {
                // carrying on from the flip: on the ceiling, upside down, its case broken
                if (player.getEffect(ModEffects.upwelling()) == null) {
                    Vec3 under = at(0.0, 2.0, 13.0);
                    player.teleportTo(end, under.x, under.y, under.z, java.util.Set.of(), yawTo(under, at(0.0, -1.0, 0.0)), 0.0F, true);
                    UpwellingEffect.softLanding(player);
                    player.addEffect(new MobEffectInstance(ModEffects.upwelling(), 600, 0, false, true));
                }
                curator.showAwake(true);
                curator.setHealth(120.0F);
                curator.playOnCue(2.4);
            }
            case "finish" -> {
                curator.showAwake(true);
                curator.setHealth(5.0F);
                curator.playOnCue(2.4);
            }
            default -> LOGGER.error(TAG + "no fight beat called {}", beat);
        }
        LOGGER.info(TAG + "fight {}: player at {}, Curator at {} ({} health{})", beat, player.blockPosition().toShortString(),
            curator.blockPosition().toShortString(), (int) curator.getHealth(), curator.isExposed() ? ", exposed" : "");
        return true;
    }

    // The Curator nearest the reliquary's viewpoint, its home, and which way its Leviathan lies.
    private static boolean find(ServerLevel end, Vec3 near) {
        List<Curator> found = end.getEntitiesOfClass(Curator.class, new AABB(near, near).inflate(64.0));
        if (found.isEmpty()) {
            LOGGER.info(TAG + "no Curator near {} yet", BlockPos.containing(near).toShortString());
            return false;
        }
        curator = found.get(0);
        home = curator.home() != null ? curator.home() : curator.blockPosition();
        centre = new Vec3(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
        BlockPos pedestal = null;
        for (BlockPos p : BlockPos.betweenClosed(home.offset(-32, -3, -32), home.offset(32, 3, 32))) {
            if (end.getBlockState(p).is(ModBlocks.LEVIATHAN_PEDESTAL.get())
                    && (pedestal == null || p.distSqr(home) < pedestal.distSqr(home))) {
                pedestal = p.immutable();
            }
        }
        if (pedestal != null) {
            Vec3 d = new Vec3(pedestal.getX() - home.getX(), 0.0, pedestal.getZ() - home.getZ());
            if (d.lengthSqr() > 1.0) {
                along = d.normalize();
                across = new Vec3(-along.z, 0.0, along.x);
            }
        }
        LOGGER.info(TAG + "fight: the Curator's home is {}, its pedestal {}", home.toShortString(),
            pedestal == null ? "not found" : pedestal.toShortString());
        if (!mapped) {
            mapped = true;
            // The floor round it, as the fight sees it: # floor, . a gap, C home, P a pylon.
            LOGGER.info(TAG + "fight: the reliquary's floor (across -10..10 left to right, along -12..12 down; # floor, . gap, P pylon):");
            for (int d = -12; d <= 12; d++) {
                StringBuilder map = new StringBuilder(String.format("fight floor %4d ", d));
                for (int u = -10; u <= 10; u++) {
                    BlockPos p = BlockPos.containing(at(u, d, 0.0));
                    char c = end.getBlockState(p).is(net.minecraft.world.level.block.Blocks.OBSIDIAN) ? 'P'
                        : u == 0 && d == 0 ? 'C' : end.getBlockState(p.below()).isAir() ? '.' : '#';
                    map.append(c);
                }
                LOGGER.info(TAG + map);
            }
        }
        return true;
    }

    private static Vec3 at(double u, double d, double up) {
        return centre.add(across.scale(u)).add(along.scale(d)).add(0.0, up, 0.0);
    }

    private static float yawTo(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static void stage(ServerLevel end, ServerPlayer player, double pu, double pd, double cu, double cd, boolean dormant,
            boolean exposed, float health, double hold) {
        player.removeEffect(ModEffects.upwelling());
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.LEVITATION);
        Vec3 p = at(pu, pd, 0.0), c = at(cu, cd, 0.1);
        player.teleportTo(end, p.x, p.y, p.z, java.util.Set.of(), yawTo(p, c), -12.0F, true);
        player.setDeltaMovement(Vec3.ZERO);
        curator.cueReset(dormant, exposed, health);
        curator.playOnCue(hold);
        float yaw = yawTo(c, p);
        curator.snapTo(c.x, c.y, c.z, yaw, 0.0F);
        curator.setYHeadRot(yaw);
        curator.yBodyRot = yaw;
    }

    // Enchanted netherite and a sword, a hotbar like anyone's, a shield; kept alive (unseen Resistance and
    // Regeneration), so the fight plays to its end and every hit still shows.
    private static void gear(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.removeAllEffects();
        HolderLookup.RegistryLookup<Enchantment> enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        player.setItemSlot(EquipmentSlot.HEAD, enchanted(Items.NETHERITE_HELMET, enchantments, Enchantments.PROTECTION, 4));
        player.setItemSlot(EquipmentSlot.CHEST, enchanted(Items.NETHERITE_CHESTPLATE, enchantments, Enchantments.PROTECTION, 4));
        player.setItemSlot(EquipmentSlot.LEGS, enchanted(Items.NETHERITE_LEGGINGS, enchantments, Enchantments.PROTECTION, 4));
        player.setItemSlot(EquipmentSlot.FEET, enchanted(Items.NETHERITE_BOOTS, enchantments, Enchantments.FEATHER_FALLING, 4));
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        player.setItemInHand(InteractionHand.MAIN_HAND, enchanted(Items.NETHERITE_SWORD, enchantments, Enchantments.SHARPNESS, 5));
        ItemStack[] hotbar = {enchanted(Items.BOW, enchantments, Enchantments.POWER, 5), new ItemStack(Items.GOLDEN_APPLE, 12),
            new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.COOKED_BEEF, 41), new ItemStack(ModItems.RIFT_PEARL.get()),
            new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.COBBLED_DEEPSLATE, 64), new ItemStack(Items.TORCH, 48)};
        int selected = player.getInventory().findSlotMatchingItem(player.getMainHandItem());
        int slot = 0;
        for (ItemStack stack : hotbar) {
            if (slot == selected) {
                slot++;
            }
            if (slot < 9) {
                player.getInventory().setItem(slot++, stack);
            }
        }
        player.getInventory().setItem(9, new ItemStack(Items.ARROW, 64));
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.setExperienceLevels(30);
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 900, 3, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 900, 1, false, false, false));
    }

    private static ItemStack enchanted(net.minecraft.world.item.Item item, HolderLookup.RegistryLookup<Enchantment> enchantments,
            ResourceKey<Enchantment> enchantment, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantments.getOrThrow(enchantment), level);
        if (enchantment != Enchantments.UNBREAKING && item != Items.BOW) {
            stack.enchant(enchantments.getOrThrow(Enchantments.UNBREAKING), 3);
        }
        return stack;
    }

    // ---- playing a beat, a tick at a time (on the client, before the frame's camera is set)

    static void tick(Minecraft minecraft, IntegratedServer server, ClientTrailer.Scene scene, int f) {
        LocalPlayer player = minecraft.player;
        Entity seen = curatorId >= 0 ? minecraft.level.getEntity(curatorId) : null;
        double dist = seen != null ? player.distanceTo(seen) : 99.0;
        boolean fwd = false, back = false, left = false, right = false, jump = false, sprint = false, hit = false;
        int cue = -1;
        boolean wake = false;
        switch (beat) {
            case "wake" -> {
                fwd = f < 16;
                back = f >= 40 && f < 52;
                wake = f == 24;
            }
            case "charge" -> {
                cue = f == 6 ? Curator.ACT_CROUCH : -1;
                boolean dodge = f >= 27 && f < 40;
                boolean toRight = strafeRight(player, across);   // aside toward the middle of the floor
                right = dodge && toRight;
                left = dodge && !toRight;
                sprint = dodge;
                jump = f == 28 || f == 29;
            }
            case "strike" -> {
                fwd = dist > 3.0 && f < 100;
                sprint = fwd;
                jump = f == 12 || f == 13 || f == 44 || f == 45;
                hit = f == 18 || f == 34 || f == 52 || f == 68 || f == 88;
                cue = f == 70 ? Curator.ACT_PUNCH_LEFT : -1;
            }
            case "beam" -> {
                cue = f == 4 ? Curator.ACT_AIM : -1;
                boolean dodge = f >= 30 && f < 41;
                boolean toRight = strafeRight(player, across);
                right = dodge && toRight;
                left = dodge && !toRight;
                sprint = dodge;
                jump = f == 31 || f == 32;
            }
            case "flip" -> {
                jump = f == 2 || f == 3;
                hit = f == 8 || f == 26;
            }
            case "ceiling" -> {
                hit = f == 16 || f == 34 || f == 54 || f == 74 || f == 92;
                cue = f == 26 ? Curator.ACT_PUNCH_LEFT : f == 62 ? Curator.ACT_PUNCH_RIGHT : -1;
            }
            case "finish" -> hit = f == 10;
            default -> {
            }
        }
        Options keys = minecraft.options;
        keys.keyUp.setDown(fwd);
        keys.keyDown.setDown(back);
        keys.keyLeft.setDown(left);
        keys.keyRight.setDown(right);
        keys.keyJump.setDown(jump);
        keys.keySprint.setDown(sprint);
        if (seen != null && seen.isAlive()) {
            turnToward(player, seen.position().add(0.0, 2.4, 0.0));
        }
        if (hit) {
            attack(minecraft);
        }
        clearToasts(minecraft);   // no "Cover Me in Debris!" over the fight
        int action = cue;
        boolean wakeNow = wake;
        server.execute(() -> {
            if (curator == null) {
                return;
            }
            if (wakeNow) {
                curator.cueWake((ServerLevel) curator.level());
            }
            if (action >= 0) {
                curator.cue(action);
            }
            keepGoing(server);
        });
    }

    // A strike, as the attack button makes one: at whatever is under the crosshair, with the swing.
    private static java.lang.reflect.Method startAttack;

    private static void attack(Minecraft minecraft) {
        try {
            if (startAttack == null) {
                startAttack = Minecraft.class.getDeclaredMethod("startAttack");
                startAttack.setAccessible(true);
            }
            startAttack.invoke(minecraft);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.error(TAG + "couldn't strike", e);
        }
    }

    // The toasts in the corner, cleared (found by type: 26.3 doesn't hand them out).
    private static Object toasts;
    private static java.lang.reflect.Method clear;

    private static void clearToasts(Minecraft minecraft) {
        try {
            if (toasts == null) {
                for (Object owner : new Object[] {minecraft, minecraft.gui}) {
                    for (java.lang.reflect.Field field : owner.getClass().getDeclaredFields()) {
                        if (toasts == null && field.getType().getSimpleName().contains("Toast")) {
                            field.setAccessible(true);
                            toasts = field.get(owner);
                        }
                    }
                }
                if (toasts == null) {
                    return;
                }
                clear = toasts.getClass().getMethod("clear");
            }
            clear.invoke(toasts);
        } catch (ReflectiveOperationException | RuntimeException e) {
            toasts = null;
        }
    }

    // Which strafe key moves the player most nearly along `toward` (flat): right, or left.
    private static boolean strafeRight(LocalPlayer player, Vec3 toward) {
        double yaw = Math.toRadians(player.getYRot());
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);   // facing south (+z), your right hand is west (-x)
        return rx * toward.x + rz * toward.z > 0.0;
    }

    // The player stays on their feet: hurt, but never below half health; fed; and back on the floor if a
    // throw carried them through a gap.
    private static void keepGoing(IntegratedServer server) {
        ServerPlayer player = server.getPlayerList().getPlayers().get(0);
        if (player.getHealth() < 10.0F) {
            player.setHealth(10.0F);
        }
        player.getFoodData().setFoodLevel(20);
        if (home != null && player.getY() < home.getY() - 8) {
            Vec3 p = at(0.0, 4.0, 0.0);
            player.teleportTo((ServerLevel) player.level(), p.x, p.y, p.z, java.util.Set.of(), player.getYRot(), player.getXRot(), true);
            player.setDeltaMovement(Vec3.ZERO);
            LOGGER.warn(TAG + "fight {}: the player fell through the floor; put back", beat);
        }
    }

    // Turn smoothly toward a point, as a player would with the mouse.
    private static void turnToward(LocalPlayer player, Vec3 to) {
        Vec3 d = to.subtract(player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        float dy = Mth.clamp(Mth.wrapDegrees(yaw - player.getYRot()) * 0.35F, -20.0F, 20.0F);
        float dp = Mth.clamp((pitch - player.getXRot()) * 0.35F, -12.0F, 12.0F);
        player.setYRot(player.getYRot() + dy);
        player.setXRot(Mth.clamp(player.getXRot() + dp, -90.0F, 90.0F));
        player.setYHeadRot(player.getYRot());
        UpsideDown.acceptTurn(player);
    }

    // What the third-person camera follows: between the two of them (the player alone, once it's gone).
    static Vec3 focus(Minecraft minecraft) {
        Player player = minecraft.player;
        Entity seen = curatorId >= 0 ? minecraft.level.getEntity(curatorId) : null;
        Vec3 p = player.position().add(0.0, 1.0, 0.0);
        if (seen == null || !seen.isAlive()) {
            return p;
        }
        return p.lerp(seen.position().add(0.0, 2.2, 0.0), 0.45);
    }

    // Across the line between them: the side the third-person camera starts from.
    static Vec3 across(Minecraft minecraft) {
        Player player = minecraft.player;
        Entity seen = curatorId >= 0 ? minecraft.level.getEntity(curatorId) : null;
        if (seen == null) {
            return across;
        }
        Vec3 d = seen.position().subtract(player.position());
        return new Vec3(-d.z, 0.0, d.x);
    }

    static void release(Minecraft minecraft) {
        Options keys = minecraft.options;
        keys.keyUp.setDown(false);
        keys.keyDown.setDown(false);
        keys.keyLeft.setDown(false);
        keys.keyRight.setDown(false);
        keys.keyJump.setDown(false);
        keys.keySprint.setDown(false);
    }

    private TrailerFight() {}
}
