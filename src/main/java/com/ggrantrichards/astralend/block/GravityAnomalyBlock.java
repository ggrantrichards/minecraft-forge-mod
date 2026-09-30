package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

// The core of a Gravity Inversion Pocket, set into the underside of a Dark Matter Belt island.
// Below it hangs a sphere where gravity points up (see GravityAnomalyBlockEntity). The sphere is
// marked by motes drifting slowly upward, so you can see a pocket before you walk into it.
public class GravityAnomalyBlock extends BaseEntityBlock {
    public GravityAnomalyBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GravityAnomalyBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, ModBlockEntities.GRAVITY_ANOMALY.get(), GravityAnomalyBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5 - GravityAnomalyBlockEntity.CENTRE_BELOW;
        double cz = pos.getZ() + 0.5;
        double r = GravityAnomalyBlockEntity.RADIUS;
        for (int i = 0; i < 3; i++) {
            // A random point inside the sphere, drifting upward.
            double dx, dy, dz;
            do {
                dx = random.nextDouble() * 2 - 1;
                dy = random.nextDouble() * 2 - 1;
                dz = random.nextDouble() * 2 - 1;
            } while (dx * dx + dy * dy + dz * dz > 1);
            level.addParticle(ParticleTypes.REVERSE_PORTAL, cx + dx * r, cy + dy * r, cz + dz * r, 0.0, 0.04, 0.0);
        }
    }
}
