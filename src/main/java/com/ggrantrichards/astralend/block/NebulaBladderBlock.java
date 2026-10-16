package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// A Drift Bladder: a sac of glowing nebula gas, lighter than air, that floats over the Nebula Fields
// tethered to the moss by a hanging Nebula Tendril. Its rose membrane is see-through, a cold blue glow
// at its heart, and it leaks the gas it's made of in slow wisps. Mined, it's a glowing, glassy block.
public class NebulaBladderBlock extends TransparentBlock {
    private static final DustParticleOptions GAS = com.ggrantrichards.astralend.compat.Compat.dust(0xE07AA8, 1.6F);

    public NebulaBladderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(GAS, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + 1.05,
                pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
        }
    }
}
