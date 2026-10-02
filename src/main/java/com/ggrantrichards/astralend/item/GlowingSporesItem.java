package com.ggrantrichards.astralend.item;

import com.ggrantrichards.astralend.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

// Glowing Spores, left behind by End Pollinators. Used on End Stone, they grow it over with
// Chorus Moss (so you can spread the Nebula Fields' ground by hand). They also make Spore Lanterns
// and Glow Ink Sacs.
public class GlowingSporesItem extends Item {
    public GlowingSporesItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.END_STONE)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, ModBlocks.CHORUS_MOSS.get().defaultBlockState(), 3);
            server.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 8, 0.3, 0.05, 0.3, 0.0);
            server.playSound(null, pos, SoundEvents.MOSS_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (context.getPlayer() == null || !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
