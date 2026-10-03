package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.entity.ModEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

// The Mimic Plant: a dark shrub of the Hollow Isles that makes no sound of its own. It copies the
// sounds of what's near it - your footsteps on the ground you walk on, an enderman's teleport, a Void
// Wanderer's hum - a little later, from where it stands. The forest feels watched.
public class MimicPlantBlock extends EndPlantBlock {
    public MimicPlantBlock(VoxelShape shape, TagKey<Block> soil, BlockBehaviour.Properties properties) {
        super(shape, soil, properties);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(20.0),
            e -> e.isAlive() && !e.isSpectator());
        if (near.isEmpty()) {
            return;
        }
        LivingEntity source = near.get(random.nextInt(near.size()));
        SoundEvent sound;
        if (source instanceof Player player) {
            sound = level.getBlockState(player.getOnPos()).getSoundType().getStepSound();
        } else if (source instanceof Enderman) {
            sound = random.nextBoolean() ? SoundEvents.ENDERMAN_TELEPORT : SoundEvents.ENDERMAN_AMBIENT;
        } else if (source.getType() == ModEntities.VOID_WANDERER.get()) {
            sound = SoundEvents.BEACON_AMBIENT;
        } else {
            sound = level.getBlockState(source.getOnPos()).getSoundType().getStepSound();
        }
        level.playSound(null, pos, sound, SoundSource.AMBIENT, 0.7F, 0.9F + random.nextFloat() * 0.1F);
    }
}
