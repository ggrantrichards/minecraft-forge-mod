package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import com.ggrantrichards.astralend.world.Upwells;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Every few ticks, flips gravity for living things inside the pocket's sphere (see Upwells).
public class UpwellCoreBlockEntity extends BlockEntity {
    public static final double RADIUS = 5.5;
    public static final double CENTRE_BELOW = 6.0;  // the sphere's centre is this far below the core
    private static final int INTERVAL = 2;

    public UpwellCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPWELL_CORE.get(), pos, state);
    }

    public static Vec3 centre(BlockPos core) {
        return Vec3.atCenterOf(core).subtract(0, CENTRE_BELOW, 0);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, UpwellCoreBlockEntity anomaly) {
        if (level.getGameTime() % INTERVAL == 0 && Upwells.enabledAt(level, pos)) {
            invertInside(level, pos);
        }
        // Now and then an Upwell Mote condenses at the bottom of the pocket and drifts up it.
        if (level.getRandom().nextInt(900) == 0) {
            Vec3 bottom = centre(pos).subtract(0, RADIUS - 0.5, 0);
            AABB box = AABB.ofSize(centre(pos), RADIUS * 2, RADIUS * 2, RADIUS * 2);
            if (level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box,
                    e -> e.getItem().is(com.ggrantrichards.astralend.ModItems.UPWELL_MOTE.get())).size() < 2) {
                var mote = new net.minecraft.world.entity.item.ItemEntity(level, bottom.x, bottom.y, bottom.z,
                    new net.minecraft.world.item.ItemStack(com.ggrantrichards.astralend.ModItems.UPWELL_MOTE.get()));
                mote.setNoGravity(true);
                mote.setDeltaMovement(0.0, 0.02, 0.0);
                mote.setUnlimitedLifetime();
                level.addFreshEntity(mote);
            }
        }
    }

    // Flips gravity for every living thing inside the pocket below the core at `pos`.
    public static void invertInside(Level level, BlockPos pos) {
        Vec3 centre = centre(pos);
        AABB box = AABB.ofSize(centre, RADIUS * 2, RADIUS * 2, RADIUS * 2);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.getBoundingBox().getCenter().distanceToSqr(centre) <= RADIUS * RADIUS)) {
            Upwells.markInside(entity, level.getGameTime());
        }
    }
}
