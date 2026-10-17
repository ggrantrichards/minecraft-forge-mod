package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.block.CollectorPedestalBlockEntity;
import com.ggrantrichards.astralend.block.FlarebloomBlock;
import com.ggrantrichards.astralend.block.SunscaleLichenBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

// For the CI screenshots only: a little display of the Phase 5 blocks on a flattened patch of the
// Eclipse, laid out across the camera's view (the camera looks south-east, so "across" is +x/-z):
// open Flareblooms and a shut one, two Collector Pedestals (one with Charged Void Crystal, one with a
// glass bottle) under a Sunshade, and charged Sunscale Lichen on the ground.
final class FlareDisplay {
    static void build(ServerLevel level, BlockPos centre) {
        BlockState ground = ModBlocks.SUNSCORCHED_STONE.get().defaultBlockState();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                level.setBlock(centre.offset(dx, -1, dz), ground, 3);
                for (int dy = 0; dy <= 5; dy++) {
                    level.setBlock(centre.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        bloom(level, across(centre, -3), true);
        bloom(level, across(centre, -2).offset(1, 0, 1), false);
        bloom(level, across(centre, 3), true);
        pedestal(level, across(centre, -1), new ItemStack(ModItems.CHARGED_VOID_CRYSTAL.get()));
        pedestal(level, across(centre, 1), new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
        BlockState shade = ModBlocks.SUNSHADE.get().defaultBlockState();
        for (int k = -1; k <= 1; k++) {
            level.setBlock(across(centre, k).above(3), shade, 3);
            level.setBlock(across(centre, k).offset(1, 3, 1), shade, 3);
        }
        BlockState lichen = ModBlocks.SUNSCALE_LICHEN.get().defaultBlockState().setValue(SunscaleLichenBlock.CHARGED, true);
        for (int[] o : new int[][] {{2, 2}, {3, 1}, {1, 3}, {-1, 2}, {2, -1}, {0, 2}, {3, 3}, {-2, 1}}) {
            level.setBlock(centre.offset(o[0], 0, o[1]), lichen, 3);
        }
    }

    private static BlockPos across(BlockPos centre, int k) {
        return centre.offset(k, 0, -k);
    }

    private static void bloom(ServerLevel level, BlockPos pos, boolean open) {
        BlockState lower = ModBlocks.FLAREBLOOM.get().defaultBlockState().setValue(FlarebloomBlock.OPEN, open);
        level.setBlock(pos, lower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
        level.setBlock(pos.above(), lower.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
    }

    private static void pedestal(ServerLevel level, BlockPos pos, ItemStack held) {
        level.setBlock(pos, ModBlocks.COLLECTOR_PEDESTAL.get().defaultBlockState(), 3);
        if (level.getBlockEntity(pos) instanceof CollectorPedestalBlockEntity pedestal) {
            pedestal.setHeld(held);
        }
    }

    private FlareDisplay() {}
}
