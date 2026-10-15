package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Leviathan Heart: the heart of a dead leviathan, which isn't quite dead. Its violet light swells
// and fades in a slow double beat (the texture's animation), and close by you can hear it - a low
// heartbeat, now and then, and a breath of violet in the air round it.
public class LeviathanHeartBlock extends Block {
    public LeviathanHeartBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(60) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS,
                0.9F, 0.55F, false);
        }
        if (random.nextInt(8) == 0) {
            level.addParticle(new DustParticleOptions(0x9B4CFF, 1.2F), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                pos.getZ() + random.nextDouble(), 0.0, 0.01, 0.0);
        }
    }
}
