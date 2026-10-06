package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// Voidgel: jelly from the undersides of the Deep Hollows and the Glimmering Crags. Like a slime
// block it takes a fall and bounces you back up (a little less high); each bounce also lets you down
// gently, with a few seconds of Slow Falling.
public class VoidgelBlock extends SlimeBlock {
    public VoidgelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void bounceOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        super.bounceOn(level, state, pos, entity, fallDistance);
        if (!level.isClientSide() && entity instanceof LivingEntity living && fallDistance > 1.0) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true));
        }
    }
}
