package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// Keeps track of the loaded Lodestars, so a comet can ask whether one is near where it would land.
// (Comets only land near players, where the chunks are loaded.)
public class LodestarBlockEntity extends BlockEntity {
    public static final double RANGE = 48.0;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> LOADED = new HashMap<>();

    public LodestarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LODESTAR.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            synchronized (LOADED) {
                LOADED.computeIfAbsent(this.level.dimension(), k -> new HashSet<>()).add(this.worldPosition.immutable());
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            synchronized (LOADED) {
                Set<BlockPos> set = LOADED.get(this.level.dimension());
                if (set != null) {
                    set.remove(this.worldPosition);
                }
            }
        }
    }

    // True if a Lodestar stands within RANGE blocks (horizontally) of `at`.
    public static boolean guards(Level level, Vec3 at) {
        synchronized (LOADED) {
            Set<BlockPos> set = LOADED.get(level.dimension());
            if (set == null) {
                return false;
            }
            for (BlockPos p : set) {
                double dx = p.getX() + 0.5 - at.x, dz = p.getZ() + 0.5 - at.z;
                if (dx * dx + dz * dz <= RANGE * RANGE && level.getBlockState(p).getBlock() instanceof LodestarBlock) {
                    return true;
                }
            }
            return false;
        }
    }
}
