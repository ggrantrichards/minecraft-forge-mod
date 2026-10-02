package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// A Lodestar: a lodestone banded with Meteoric Iron. Its pull turns comets aside, so none comes down
// within RANGE blocks of it (they still fly past overhead). See LodestarBlockEntity.
public class LodestarBlock extends BaseEntityBlock {
    public LodestarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LodestarBlockEntity(pos, state);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            // Motes drawn in toward the star on its top.
            double a = random.nextDouble() * Math.PI * 2;
            double x = pos.getX() + 0.5 + Math.cos(a) * 1.2, z = pos.getZ() + 0.5 + Math.sin(a) * 1.2;
            level.addParticle(ParticleTypes.PORTAL, x, pos.getY() + 1.2, z,
                (pos.getX() + 0.5 - x) * 0.6, -0.2, (pos.getZ() + 0.5 - z) * 0.6);
        }
    }
}
