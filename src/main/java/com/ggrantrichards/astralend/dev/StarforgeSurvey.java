package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.starforge.AstralForgeMenu;
import com.ggrantrichards.astralend.starforge.Starforge;
import com.ggrantrichards.astralend.starforge.StarforgeEffects;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

// The survey's Starforging tests (Phase 8): the Astral Forge's menu forges an upgrade and uses up its
// inputs; which items an upgrade fits; and each upgrade's effect, tried on a stand-in player or a zombie.
final class StarforgeSurvey {
    // Returns the screenshot lines for Phase 8.
    static List<String> check(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        // A stand-in player, never added to the world (Forge 66 has no fake players).
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(end.getServer(), end,
            new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("astralend-survey".getBytes()), "AstralSurvey"),
            net.minecraft.server.level.ClientInformation.createDefault());
        player.snapTo(standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5, 0.0F, 0.0F);

        // ---- the Astral Forge: Radiant onto a diamond chestplate
        AstralForgeMenu menu = new AstralForgeMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(AstralForgeMenu.TEMPLATE).set(new ItemStack(ModItems.STARFORGE_TEMPLATE.get(), 2));
        menu.getSlot(AstralForgeMenu.BASE).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
        menu.getSlot(AstralForgeMenu.ALLOY).set(new ItemStack(ModItems.STARFORGE_ALLOY.get()));
        menu.getSlot(AstralForgeMenu.MATERIAL).set(new ItemStack(ModItems.CHARGED_VOID_CRYSTAL.get()));
        menu.getSlot(AstralForgeMenu.MATERIAL_2).set(new ItemStack(ModItems.SOLARITE_INGOT.get()));
        ItemStack result = menu.getSlot(AstralForgeMenu.RESULT).getItem();
        boolean forged = Starforge.get(result) == StarforgeUpgrade.RADIANT && result.is(Items.DIAMOND_CHESTPLATE);
        if (forged) {
            menu.getSlot(AstralForgeMenu.RESULT).onTake(player, result.copy());
        }
        boolean used = menu.getSlot(AstralForgeMenu.TEMPLATE).getItem().getCount() == 1 && !menu.getSlot(AstralForgeMenu.BASE).hasItem()
            && !menu.getSlot(AstralForgeMenu.ALLOY).hasItem() && !menu.getSlot(AstralForgeMenu.MATERIAL_2).hasItem();
        // The wrong material for the item: Voidwalker doesn't go on a sword.
        menu.getSlot(AstralForgeMenu.BASE).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(AstralForgeMenu.ALLOY).set(new ItemStack(ModItems.STARFORGE_ALLOY.get()));
        menu.getSlot(AstralForgeMenu.MATERIAL).set(new ItemStack(ModItems.VOID_CRYSTAL.get()));
        menu.getSlot(AstralForgeMenu.MATERIAL_2).set(new ItemStack(ModItems.UMBRAL_MEMBRANE.get()));
        boolean refused = menu.getSlot(AstralForgeMenu.RESULT).getItem().isEmpty() && menu.hasError();
        log.accept(String.format("starforge: the Astral Forge made %s (Radiant: %s), used its inputs: %s; refused Voidwalker on a sword: %s",
            result.getHoverName().getString(), forged, used, refused));
        if (!forged || !used || !refused) {
            failures.add("starforge: the Astral Forge should forge Radiant onto a chestplate, use up its inputs, and refuse Voidwalker on a sword");
        }
        player.getInventory().clearContent();

        // ---- what fits where
        boolean fitsRight = StarforgeUpgrade.VOIDWALKER.fits(new ItemStack(Items.NETHERITE_BOOTS))
            && !StarforgeUpgrade.VOIDWALKER.fits(new ItemStack(Items.NETHERITE_HELMET))
            && StarforgeUpgrade.METEORIC.fits(new ItemStack(Items.DIAMOND_PICKAXE)) && !StarforgeUpgrade.METEORIC.fits(new ItemStack(Items.BOW))
            && StarforgeUpgrade.STARDUST.fits(new ItemStack(Items.CROSSBOW)) && StarforgeUpgrade.SINGULARITY.fits(new ItemStack(Items.DIAMOND_SWORD))
            && !StarforgeUpgrade.TEMPORAL.fits(new ItemStack(Items.DIAMOND_SHOVEL)) && !StarforgeUpgrade.RADIANT.fits(new ItemStack(Items.IRON_SWORD));
        log.accept("starforge: upgrades fit the right items: " + fitsRight);
        if (!fitsRight) {
            failures.add("starforge: an upgrade fits an item it shouldn't (or not one it should)");
        }

        // ---- armour on a zombie: Stardust gravity and step, Radiant's room for a golden heart,
        // Meteoric's Heat Shield, Temporal's Time Stretch
        BlockPos at = standOn.offset(3, 0, 0);
        var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(end, EntitySpawnReason.COMMAND);
        zombie.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        zombie.setNoAi(true);
        end.addFreshEntity(zombie);
        double gravity = zombie.getAttributeValue(Attributes.GRAVITY);
        zombie.setItemSlot(EquipmentSlot.FEET, Starforge.apply(new ItemStack(Items.DIAMOND_BOOTS), StarforgeUpgrade.STARDUST));
        zombie.setItemSlot(EquipmentSlot.LEGS, Starforge.apply(new ItemStack(Items.DIAMOND_LEGGINGS), StarforgeUpgrade.STARDUST));
        zombie.setItemSlot(EquipmentSlot.HEAD, Starforge.apply(new ItemStack(Items.DIAMOND_HELMET), StarforgeUpgrade.RADIANT));
        zombie.setItemSlot(EquipmentSlot.CHEST, Starforge.apply(new ItemStack(Items.DIAMOND_CHESTPLATE), StarforgeUpgrade.RADIANT));
        zombie.tick();   // equipment changes apply their attribute modifiers on a tick
        double lighter = zombie.getAttributeValue(Attributes.GRAVITY);
        double step = zombie.getAttributeValue(Attributes.STEP_HEIGHT);
        float roomForHearts = zombie.getMaxAbsorption();
        log.accept(String.format("starforge: Stardust x2 - gravity %.4f -> %.4f, step height %.2f; Radiant x2 - room for %.0f absorption",
            gravity, lighter, step, roomForHearts));
        if (lighter >= gravity * 0.95 || step < 1.0 || roomForHearts < 2.0F) {
            failures.add("starforge: Stardust armour should lower gravity and raise step height, Radiant should make room for golden hearts");
        }
        zombie.setItemSlot(EquipmentSlot.FEET, Starforge.apply(new ItemStack(Items.DIAMOND_BOOTS), StarforgeUpgrade.METEORIC));
        zombie.setItemSlot(EquipmentSlot.LEGS, Starforge.apply(new ItemStack(Items.DIAMOND_LEGGINGS), StarforgeUpgrade.METEORIC));
        zombie.setItemSlot(EquipmentSlot.HEAD, Starforge.apply(new ItemStack(Items.DIAMOND_HELMET), StarforgeUpgrade.TEMPORAL));
        zombie.setItemSlot(EquipmentSlot.CHEST, Starforge.apply(new ItemStack(Items.DIAMOND_CHESTPLATE), StarforgeUpgrade.TEMPORAL));
        float before = zombie.getHealth();
        zombie.hurtServer(end, end.damageSources().inFire(), 3.0F);
        boolean shielded = zombie.getHealth() == before;
        zombie.addEffect(new MobEffectInstance(MobEffects.SPEED, 200));
        zombie.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200));
        StarforgeEffects.flush(true);
        MobEffectInstance speed = zombie.getEffect(MobEffects.SPEED);
        MobEffectInstance slowness = zombie.getEffect(MobEffects.SLOWNESS);
        int speedTicks = speed == null ? 0 : speed.getDuration();
        int poisonTicks = slowness == null ? 0 : slowness.getDuration();
        log.accept(String.format("starforge: Meteoric x2 - fire did %s; Temporal x2 - Speed 200 -> %d ticks, Slowness 200 -> %d ticks",
            shielded ? "nothing" : "damage", speedTicks, poisonTicks));
        if (!shielded || speedTicks != 240 || poisonTicks != 160) {
            failures.add("starforge: Heat Shield should stop fire damage, and Time Stretch make Speed last 240 ticks and Slowness 160");
        }
        zombie.removeAllEffects();
        zombie.setHealth(zombie.getMaxHealth());

        // ---- weapons, held by the fake player: Echo Strike and Glow
        player.setItemSlot(EquipmentSlot.MAINHAND, Starforge.apply(new ItemStack(Items.DIAMOND_SWORD), StarforgeUpgrade.TEMPORAL));
        float full = zombie.getHealth();
        zombie.hurtServer(end, end.damageSources().playerAttack(player), 4.0F);
        float afterHit = zombie.getHealth();
        settle(zombie);   // the echo comes a second later, after the hurt immunity has worn off
        StarforgeEffects.flush(true);
        float afterEcho = zombie.getHealth();
        player.setItemSlot(EquipmentSlot.MAINHAND, Starforge.apply(new ItemStack(Items.NETHERITE_SWORD), StarforgeUpgrade.RADIANT));
        settle(zombie);
        zombie.hurtServer(end, end.damageSources().playerAttack(player), 1.0F);
        boolean glowing = zombie.hasEffect(MobEffects.GLOWING);
        log.accept(String.format("starforge: Echo Strike - health %.1f, after the hit %.1f, after the echo %.1f; Glow - glowing %s",
            full, afterHit, afterEcho, glowing));
        if (!(afterHit < full) || !(afterEcho < afterHit) || !glowing) {
            failures.add("starforge: Echo Strike should land a second, smaller hit, and a Radiant weapon should make its target glow");
        }
        zombie.discard();

        // ---- tools, used by the fake player: Smelting and Pull
        BlockPos ore = standOn.offset(-3, 2, 0);
        end.setBlock(ore, Blocks.IRON_ORE.defaultBlockState(), 3);
        ItemStack meteoricPick = Starforge.apply(new ItemStack(Items.DIAMOND_PICKAXE), StarforgeUpgrade.METEORIC);
        player.setItemSlot(EquipmentSlot.MAINHAND, meteoricPick);
        List<ItemStack> smelted = Block.getDrops(end.getBlockState(ore), end, ore, null, player, meteoricPick);
        end.setBlock(ore, ModBlocks.METEORIC_IRON_ORE.get().defaultBlockState(), 3);
        List<ItemStack> smeltedMeteoric = Block.getDrops(end.getBlockState(ore), end, ore, null, player, meteoricPick);
        ItemStack singularityPick = Starforge.apply(new ItemStack(Items.DIAMOND_PICKAXE), StarforgeUpgrade.SINGULARITY);
        player.setItemSlot(EquipmentSlot.MAINHAND, singularityPick);
        end.setBlock(ore, Blocks.STONE.defaultBlockState(), 3);
        List<ItemStack> pulled = Block.getDrops(end.getBlockState(ore), end, ore, null, player, singularityPick);
        boolean inPocket = player.getInventory().countItem(Items.COBBLESTONE) > 0;
        end.setBlock(ore, Blocks.AIR.defaultBlockState(), 3);
        log.accept(String.format("starforge: Smelting - iron ore gave %s, meteoric iron ore gave %s; Pull - drops on the ground %d, cobblestone in the inventory %s",
            smelted, smeltedMeteoric, pulled.size(), inPocket));
        if (smelted.isEmpty() || !smelted.get(0).is(Items.IRON_INGOT) || smeltedMeteoric.isEmpty()
            || !smeltedMeteoric.get(0).is(ModItems.METEORIC_IRON_INGOT.get()) || !pulled.isEmpty() || !inPocket) {
            failures.add("starforge: a Meteoric pickaxe should smelt what it mines, and a Singularity pickaxe pull it into the inventory");
        }
        player.getInventory().clearContent();

        // ---- Rift Walker: walking off into the air lays tiles of rift ahead
        BlockPos air = standOn.above(24);
        player.snapTo(air.getX() + 0.5, air.getY(), air.getZ() + 0.5, 0.0F, 0.0F);
        StarforgeEffects.riftWalk(end, player, new Vec3(0.0, 0.0, 0.2));
        int tiles = 0;
        for (BlockPos p : BlockPos.betweenClosed(air.offset(-1, -2, -1), air.offset(1, 0, 3))) {
            if (end.getBlockState(p).is(ModBlocks.RIFT_TILE.get())) {
                tiles++;
                end.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        log.accept("starforge: Rift Walker laid " + tiles + " tile(s) over the drop");
        if (tiles < 2) {
            failures.add("starforge: Rift Walker boots should lay rift tiles under and ahead of you over a drop");
        }

        // ---- for the screenshots: an Astral Forge on the ground (the client opens this same world)
        int fx = standOn.getX() + 4, fz = standOn.getZ() + 4;
        BlockPos forge = new BlockPos(fx, end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fx, fz), fz);
        end.setBlock(forge, ModBlocks.ASTRAL_FORGE.get().defaultBlockState(), 3);
        String cam = String.format(java.util.Locale.ROOT, "%.1f %.1f %.1f", forge.getX() + 0.5 - 2.0, forge.getY() + 0.4, forge.getZ() + 0.5 - 2.0);
        return List.of(
            "starforge_astral_forge " + cam + " -45 32.0",
            "starforge_forge_screen " + cam + " -45 32.0 forge",
            String.format(java.util.Locale.ROOT, "starforge_armor %.1f %d %.1f 135 8.0 armor", standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5),
            String.format(java.util.Locale.ROOT, "starforge_rift_walker %.1f %d %.1f 135 18.0 armor,rift", standOn.getX() + 0.5, standOn.getY() + 30,
                standOn.getZ() + 0.5));
    }

    private static void settle(net.minecraft.world.entity.LivingEntity entity) {
        for (int i = 0; i < 20; i++) {
            entity.tick();
        }
    }

    private StarforgeSurvey() {}
}
