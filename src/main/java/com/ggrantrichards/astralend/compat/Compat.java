package com.ggrantrichards.astralend.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// (1.21.1 port) Small differences from 26.3 in one place.
public final class Compat {
    // 26.3's player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, yaw, pitch, DO_NOTHING)).
    public static void teleport(Entity entity, ServerLevel level, Vec3 pos, float yaw, float pitch) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, pos.x, pos.y, pos.z, yaw, pitch);
        } else {
            entity.teleportTo(level, pos.x, pos.y, pos.z, java.util.Set.of(), yaw, pitch);
        }
    }

    // 26.3's Block.column(width, minY, maxY): a box centred in the block, in pixels.
    public static net.minecraft.world.phys.shapes.VoxelShape column(double width, double minY, double maxY) {
        double a = (16.0 - width) / 2.0;
        return net.minecraft.world.level.block.Block.box(a, minY, a, 16.0 - a, maxY, 16.0 - a);
    }

    // 26.3's Block.boxes(max, f): a shape per value 0..max.
    public static net.minecraft.world.phys.shapes.VoxelShape[] boxes(int max, java.util.function.IntFunction<net.minecraft.world.phys.shapes.VoxelShape> f) {
        net.minecraft.world.phys.shapes.VoxelShape[] shapes = new net.minecraft.world.phys.shapes.VoxelShape[max + 1];
        for (int i = 0; i <= max; i++) {
            shapes[i] = f.apply(i);
        }
        return shapes;
    }

    // 26.3's LivingEntity.travelFlying(input, inWater, inLava, inAir): flying movement with drag.
    public static void travelFlying(net.minecraft.world.entity.LivingEntity entity, Vec3 input, float inWater, float inLava, float inAir) {
        if (entity.isControlledByLocalInstance()) {
            boolean water = entity.isInWater();
            boolean lava = !water && entity.isInLava();
            entity.moveRelative(water ? inWater : lava ? inLava : inAir, input);
            entity.move(net.minecraft.world.entity.MoverType.SELF, entity.getDeltaMovement());
            entity.setDeltaMovement(entity.getDeltaMovement().scale(water ? 0.8 : lava ? 0.5 : 0.91));
        }
        entity.calculateEntityAnimation(false);
    }

    // 26.3's DensityFunction.sampleValue(x, y, z).
    public static double sample(net.minecraft.world.level.levelgen.DensityFunction function, int x, int y, int z) {
        return function.compute(new net.minecraft.world.level.levelgen.DensityFunction.SinglePointContext(x, y, z));
    }

    // 26.3's DustParticleOptions(rgb, scale).
    public static net.minecraft.core.particles.DustParticleOptions dust(int rgb, float scale) {
        return new net.minecraft.core.particles.DustParticleOptions(Vec3.fromRGB24(rgb).toVector3f(), scale);
    }

    // 26.3's BiomeSource.createResolver(sampler) / createUncachedResolver(randomState).
    public interface Resolver {
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> getNoiseBiome(int quartX, int quartY, int quartZ);
    }

    public static Resolver resolver(net.minecraft.world.level.biome.BiomeSource source, net.minecraft.world.level.biome.Climate.Sampler sampler) {
        return (x, y, z) -> source.getNoiseBiome(x, y, z, sampler);
    }

    private Compat() {}
}
