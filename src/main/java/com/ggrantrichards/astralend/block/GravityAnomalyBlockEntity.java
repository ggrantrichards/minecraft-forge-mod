package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import com.ggrantrichards.astralend.world.GravityInversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Every few ticks, flips gravity for living things inside the pocket's sphere (see GravityInversion).
public class GravityAnomalyBlockEntity extends BlockEntity {
    public static final double RADIUS = 5.5;
    public static final double CENTRE_BELOW = 6.0;  // the sphere's centre is this far below the core
    private static final int INTERVAL = 2;

    public GravityAnomalyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVITY_ANOMALY.get(), pos, state);
    }

    public static Vec3 centre(BlockPos core) {
        return Vec3.atCenterOf(core).subtract(0, CENTRE_BELOW, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GravityAnomalyBlockEntity anomaly) {
        if (level.getGameTime() % INTERVAL == 0 && GravityInversion.enabledAt(level, pos)) {
            invertInside(level, pos);
        }
    }

    // Flips gravity for every living thing inside the pocket below the core at `pos`.
    public static void invertInside(Level level, BlockPos pos) {
        Vec3 centre = centre(pos);
        AABB box = AABB.ofSize(centre, RADIUS * 2, RADIUS * 2, RADIUS * 2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.getBoundingBox().getCenter().distanceToSqr(centre) <= RADIUS * RADIUS)) {
            GravityInversion.markInside(entity, level.getGameTime());
        }
    }
}
