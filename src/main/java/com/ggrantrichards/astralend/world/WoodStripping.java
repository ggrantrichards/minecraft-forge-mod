package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.ModBlocks;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

// Right-clicking the mod's logs, stems, wood and hyphae with an axe strips them, like vanilla's.
public final class WoodStripping {
    private static Map<Block, Block> stripped;

    public static void register() {
        PlayerInteractEvent.RightClickBlock.BUS.addListener((Predicate<PlayerInteractEvent.RightClickBlock>) WoodStripping::onRightClick);
    }

    private static Map<Block, Block> table() {
        if (stripped == null) {
            stripped = Map.of(
                ModBlocks.LUMINE.log().get(), ModBlocks.LUMINE.strippedLog().get(),
                ModBlocks.LUMINE.wood().get(), ModBlocks.LUMINE.strippedWood().get(),
                ModBlocks.NULLWOOD.log().get(), ModBlocks.NULLWOOD.strippedLog().get(),
                ModBlocks.NULLWOOD.wood().get(), ModBlocks.NULLWOOD.strippedWood().get());
        }
        return stripped;
    }

    private static boolean onRightClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack tool = event.getItemStack();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block result = table().get(state.getBlock());
        if (result == null || !tool.is(ItemTags.AXES)) {
            return false;
        }
        Player player = event.getEntity();
        level.playSound(player, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            level.setBlock(pos, result.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)), 11);
            tool.hurtAndBreak(1, player, event.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return true;  // handled: don't also place or use anything
    }

    private WoodStripping() {}
}
