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
    public static final float RESTITUTION = 0.8F;   // a little less high than a slime block (1.0)

    public VoidgelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public void updateEntityAfterFallOn(net.minecraft.world.level.BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        net.minecraft.world.phys.Vec3 v = entity.getDeltaMovement();
        if (v.y < 0.0) {
            entity.setDeltaMovement(v.x, -v.y * RESTITUTION * (entity instanceof LivingEntity ? 1.0 : 0.8), v.z);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        if (!level.isClientSide() && entity instanceof LivingEntity living && fallDistance > 1.0) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, true));
        }
    }
}
