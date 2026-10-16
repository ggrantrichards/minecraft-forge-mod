package com.ggrantrichards.astralend.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

// (1.21.1 port) 26.3's "minecraft:distance_to_point" density function, which 1.21.1 doesn't have:
// the straight-line distance from a block to a point. The End's outer rim is measured with it
// (data/astralend/worldgen/density_function/terrain/outer.json).
public record DistanceToPoint(Vec3i point) implements DensityFunction.SimpleFunction {
    public static final MapCodec<DistanceToPoint> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Vec3i.CODEC.fieldOf("point").forGetter(DistanceToPoint::point),
        Codec.STRING.optionalFieldOf("metric", "euclidean").forGetter(d -> "euclidean")
    ).apply(instance, (point, metric) -> new DistanceToPoint(point)));
    public static final KeyDispatchDataCodec<DistanceToPoint> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        double dx = context.blockX() - this.point.getX();
        double dy = context.blockY() - this.point.getY();
        double dz = context.blockZ() - this.point.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return 6.0E7;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
