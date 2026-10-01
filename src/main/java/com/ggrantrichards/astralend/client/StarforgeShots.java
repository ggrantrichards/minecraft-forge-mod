package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.starforge.AstralForgeMenu;
import com.ggrantrichards.astralend.starforge.RiftTileBlock;
import com.ggrantrichards.astralend.starforge.Starforge;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

// The Phase 8 screenshots (see ClientScreenshots): the player in upgraded gear, a trail of rift tiles
// under Rift Walker boots, and the Astral Forge's screen with an upgrade being forged.
final class StarforgeShots {
    static void setUp(MinecraftServer server, ServerLevel end, ServerPlayer player, double x, double y, double z, float yaw, String style) {
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
        if (style.contains("armor")) {
            // Every piece a different upgrade, so all their accents show.
            player.setItemSlot(EquipmentSlot.HEAD, up(Items.NETHERITE_HELMET, StarforgeUpgrade.RADIANT));
            player.setItemSlot(EquipmentSlot.CHEST, up(Items.DIAMOND_CHESTPLATE, StarforgeUpgrade.SINGULARITY));
            player.setItemSlot(EquipmentSlot.LEGS, up(Items.NETHERITE_LEGGINGS, StarforgeUpgrade.STARDUST));
            player.setItemSlot(EquipmentSlot.FEET, up(Items.DIAMOND_BOOTS, StarforgeUpgrade.VOIDWALKER));
            player.setItemSlot(EquipmentSlot.MAINHAND, up(Items.DIAMOND_SWORD, StarforgeUpgrade.TEMPORAL));
        }
        if (style.contains("rift")) {
            // A path of rift tiles behind the player, older (more cracked) further back; time stands
            // still for the picture so they don't crumble.
            Direction back = Direction.fromYRot(yaw).getOpposite();
            BlockPos under = BlockPos.containing(x, y - 1, z);
            for (int i = 0; i < 7; i++) {
                BlockPos p = under.relative(back, i);
                for (int side = -1; side <= 1; side += 2) {
                    if (i > 0 && (i + side) % 3 == 0) {
                        place(end, p.relative(back.getClockWise(), side), Math.min(3, i / 2));
                    }
                }
                place(end, p, Math.min(3, i / 2));
            }
            server.tickRateManager().setFrozen(true);
        }
        if (style.contains("forge")) {
            BlockPos forge = BlockPos.containing(x + 2.0, y - 0.4, z + 2.0);
            end.setBlock(forge, ModBlocks.ASTRAL_FORGE.get().defaultBlockState(), 3);
            // A hotbar and inventory of upgraded gear, every accent on show.
            int slot = 0;
            for (ItemStack stack : new ItemStack[] {
                    up(Items.DIAMOND_SWORD, StarforgeUpgrade.TEMPORAL), up(Items.NETHERITE_PICKAXE, StarforgeUpgrade.METEORIC),
                    up(Items.BOW, StarforgeUpgrade.STARDUST), up(Items.DIAMOND_AXE, StarforgeUpgrade.RADIANT),
                    up(Items.NETHERITE_SWORD, StarforgeUpgrade.SINGULARITY), up(Items.DIAMOND_BOOTS, StarforgeUpgrade.VOIDWALKER),
                    up(Items.TRIDENT, StarforgeUpgrade.TEMPORAL), up(Items.CROSSBOW, StarforgeUpgrade.RADIANT),
                    up(Items.DIAMOND_SHOVEL, StarforgeUpgrade.SINGULARITY),
                    up(Items.NETHERITE_HELMET, StarforgeUpgrade.RADIANT), pearl(up(Items.NETHERITE_CHESTPLATE, StarforgeUpgrade.VOIDWALKER)),
                    up(Items.DIAMOND_LEGGINGS, StarforgeUpgrade.STARDUST), up(Items.NETHERITE_BOOTS, StarforgeUpgrade.METEORIC),
                    up(Items.MACE, StarforgeUpgrade.RADIANT), up(Items.DIAMOND_HOE, StarforgeUpgrade.METEORIC),
                    new ItemStack(ModItems.STARFORGE_TEMPLATE.get(), 3), new ItemStack(ModItems.STARFORGE_ALLOY.get(), 5),
                    new ItemStack(ModItems.RIFT_PEARL.get()), new ItemStack(ModItems.TEMPORAL_CRYSTAL.get(), 2),
                    new ItemStack(ModItems.SINGULARITY_SHARD.get(), 4), new ItemStack(ModItems.VOID_CRYSTAL.get(), 12)}) {
                player.getInventory().setItem(slot++, stack);
            }
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new AstralForgeMenu(id, inventory, ContainerLevelAccess.create(end, forge)),
                Component.translatable("container.astralend.astral_forge")));
            if (player.containerMenu instanceof AstralForgeMenu menu) {
                menu.getSlot(AstralForgeMenu.TEMPLATE).set(new ItemStack(ModItems.STARFORGE_TEMPLATE.get()));
                menu.getSlot(AstralForgeMenu.BASE).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
                menu.getSlot(AstralForgeMenu.ALLOY).set(new ItemStack(ModItems.STARFORGE_ALLOY.get()));
                menu.getSlot(AstralForgeMenu.MATERIAL).set(new ItemStack(ModItems.SOLARITE_INGOT.get()));
                menu.getSlot(AstralForgeMenu.MATERIAL_2).set(new ItemStack(ModItems.CHARGED_VOID_CRYSTAL.get()));
                menu.broadcastChanges();
            }
        }
    }

    private static void place(ServerLevel end, BlockPos p, int age) {
        if (end.getBlockState(p).isAir()) {
            end.setBlock(p, ModBlocks.RIFT_TILE.get().defaultBlockState().setValue(RiftTileBlock.AGE, Mth.clamp(age, 0, 3)), 3);
        }
    }

    private static ItemStack up(Item item, StarforgeUpgrade upgrade) {
        return Starforge.apply(new ItemStack(item), upgrade);
    }

    private static ItemStack pearl(ItemStack stack) {
        stack.set(Starforge.RIFT_PEARL.get(), Unit.INSTANCE);
        return stack;
    }

    private StarforgeShots() {}
}
