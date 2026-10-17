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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

        // ---- the Astral Forge: Radiant onto a diamond chestplate - refused until its constellation, the
        // Remnant, has been sighted from the Eclipse; then forged
        AstralForgeMenu menu = new AstralForgeMenu(1, player.getInventory(), ContainerLevelAccess.NULL);
        menu.getSlot(AstralForgeMenu.TEMPLATE).set(new ItemStack(ModItems.STARFORGE_TEMPLATE.get(), 2));
        menu.getSlot(AstralForgeMenu.BASE).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
        menu.getSlot(AstralForgeMenu.ALLOY).set(new ItemStack(ModItems.STARFORGE_ALLOY.get()));
        menu.getSlot(AstralForgeMenu.MATERIAL).set(new ItemStack(ModItems.BOTTLED_FLARE.get()));
        boolean unsighted = menu.getSlot(AstralForgeMenu.RESULT).getItem().isEmpty();
        com.ggrantrichards.astralend.world.ModAdvancements.award(player, "sighted_remnant");
        menu.slotsChanged(menu.getSlot(AstralForgeMenu.BASE).container);
        ItemStack result = menu.getSlot(AstralForgeMenu.RESULT).getItem();
        boolean forged = Starforge.get(result) == StarforgeUpgrade.RADIANT && result.is(Items.DIAMOND_CHESTPLATE);
        if (forged) {
            menu.getSlot(AstralForgeMenu.RESULT).onTake(player, result.copy());
        }
        boolean used = menu.getSlot(AstralForgeMenu.TEMPLATE).getItem().getCount() == 1 && !menu.getSlot(AstralForgeMenu.BASE).hasItem()
            && !menu.getSlot(AstralForgeMenu.ALLOY).hasItem() && !menu.getSlot(AstralForgeMenu.MATERIAL).hasItem();
        // The wrong material for the item: Echo Strike (a Rift Pearl) doesn't go on a chestplate.
        com.ggrantrichards.astralend.world.ModAdvancements.award(player, "sighted_maw");
        menu.getSlot(AstralForgeMenu.BASE).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
        menu.getSlot(AstralForgeMenu.ALLOY).set(new ItemStack(ModItems.STARFORGE_ALLOY.get()));
        menu.getSlot(AstralForgeMenu.MATERIAL).set(new ItemStack(ModItems.RIFT_PEARL.get()));
        boolean refused = menu.getSlot(AstralForgeMenu.RESULT).getItem().isEmpty() && menu.hasError();
        log.accept(String.format("starforge: unsighted, the Forge made nothing: %s; sighted, it made %s (Radiant: %s), used its inputs: %s; "
            + "refused Echo Strike on a chestplate: %s", unsighted, result.getHoverName().getString(), forged, used, refused));
        if (!unsighted || !forged || !used || !refused) {
            failures.add("starforge: the Astral Forge should need the constellation sighted, then forge Radiant onto a chestplate, "
                + "use up its inputs, and refuse Echo Strike on a chestplate");
        }
        player.getInventory().clearContent();

        // ---- what fits where: one upgrade per item, each on its own kind of item
        boolean fitsRight = StarforgeUpgrade.RIFT_WALKER.fits(new ItemStack(Items.NETHERITE_BOOTS))
            && !StarforgeUpgrade.RIFT_WALKER.fits(new ItemStack(Items.NETHERITE_HELMET))
            && StarforgeUpgrade.VOID_RECALL.fits(new ItemStack(Items.DIAMOND_CHESTPLATE)) && !StarforgeUpgrade.VOID_RECALL.fits(new ItemStack(Items.DIAMOND_SWORD))
            && StarforgeUpgrade.LENSING.fits(new ItemStack(Items.DIAMOND_HELMET)) && !StarforgeUpgrade.LENSING.fits(new ItemStack(Items.DIAMOND_BOOTS))
            && StarforgeUpgrade.ECHO_STRIKE.fits(new ItemStack(Items.DIAMOND_SWORD)) && !StarforgeUpgrade.ECHO_STRIKE.fits(new ItemStack(Items.DIAMOND_PICKAXE))
            && StarforgeUpgrade.RADIANT.fits(new ItemStack(Items.BOW)) && !StarforgeUpgrade.RADIANT.fits(new ItemStack(Items.IRON_SWORD));
        log.accept("starforge: upgrades fit the right items: " + fitsRight);
        if (!fitsRight) {
            failures.add("starforge: an upgrade fits an item it shouldn't (or not one it should)");
        }

        // ---- armour on a zombie: Radiant's room for a golden heart
        BlockPos at = standOn.offset(3, 0, 0);
        var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(end, EntitySpawnReason.COMMAND);
        zombie.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        zombie.setNoAi(true);
        end.addFreshEntity(zombie);
        zombie.setItemSlot(EquipmentSlot.HEAD, Starforge.apply(new ItemStack(Items.DIAMOND_HELMET), StarforgeUpgrade.RADIANT));
        zombie.setItemSlot(EquipmentSlot.CHEST, Starforge.apply(new ItemStack(Items.DIAMOND_CHESTPLATE), StarforgeUpgrade.RADIANT));
        zombie.tick();   // equipment changes apply their attribute modifiers on a tick
        float roomForHearts = zombie.getMaxAbsorption();
        log.accept(String.format("starforge: Radiant x2 - room for %.0f absorption", roomForHearts));
        if (roomForHearts < 2.0F) {
            failures.add("starforge: Radiant armour should make room for golden hearts");
        }
        zombie.removeAllEffects();
        zombie.setHealth(zombie.getMaxHealth());

        // ---- weapons, held by the fake player: Echo Strike and Glow
        player.setItemSlot(EquipmentSlot.MAINHAND, Starforge.apply(new ItemStack(Items.DIAMOND_SWORD), StarforgeUpgrade.ECHO_STRIKE));
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
        List<String> shots = new java.util.ArrayList<>();
        // The glows side by side: the same view of a full netherite set, plain and with each upgrade.
        for (String glow : new String[] {"none", "void_recall", "echo_strike", "lensing", "radiant", "rift_walker"}) {
            shots.add(String.format(java.util.Locale.ROOT, "starforge_glow_%s %.1f %d %.1f 135 8.0 armor,glow=%s", glow,
                standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5, glow));
        }
        for (int v = 1; v <= 5; v++) {   // (temporary) other ways of drawing the glow
            shots.add(String.format(java.util.Locale.ROOT, "starforge_glow_variant%d %.1f %d %.1f 135 8.0 armor,glow=void_recall,variant=%d", v,
                standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5, v));
        }
        shots.addAll(List.of(
            "starforge_astral_forge " + cam + " -45 32.0",
            "starforge_forge_screen " + cam + " -45 32.0 forge",
            String.format(java.util.Locale.ROOT, "starforge_armor %.1f %d %.1f 135 8.0 armor", standOn.getX() + 0.5, standOn.getY(), standOn.getZ() + 0.5),
            String.format(java.util.Locale.ROOT, "starforge_rift_walker %.1f %d %.1f 135 18.0 armor,rift", standOn.getX() + 0.5, standOn.getY() + 30,
                standOn.getZ() + 0.5)));
        return shots;
    }

    private static void settle(net.minecraft.world.entity.LivingEntity entity) {
        for (int i = 0; i < 20; i++) {
            entity.tick();
        }
    }

    private StarforgeSurvey() {}
}
