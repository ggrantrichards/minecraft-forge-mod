package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Chorus Moss: springy ground. You walk about 10% slower on it (its speed factor), and a fall onto it
// does half the damage, like landing on hay does less.
public class ChorusMossBlock extends Block {
    public ChorusMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.5F, level.damageSources().fall());
    }
}
