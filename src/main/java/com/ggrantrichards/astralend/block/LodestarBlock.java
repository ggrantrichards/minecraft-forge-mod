package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.world.LodestarBindings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// A Lodestar: a lodestone banded with Meteoric Iron - the End's respawn point. Use one in the End and
// it holds you: die in the End, and you wake beside it instead of back in the Overworld. (A bed
// explodes here; the Lodestar doesn't.) It holds one player's place per Lodestar used, and lets go if
// it's broken. See LodestarBindings.
public class LodestarBlock extends Block {
    public LodestarBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.dimension() != Level.END) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            LodestarBindings.get(server).bind(serverPlayer, pos);
            server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 0.7F);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 0.4, 0.4, 0.4, 0.1);
            com.ggrantrichards.astralend.world.ModAdvancements.award(serverPlayer, "anchored");
        }
        return InteractionResult.SUCCESS;
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
