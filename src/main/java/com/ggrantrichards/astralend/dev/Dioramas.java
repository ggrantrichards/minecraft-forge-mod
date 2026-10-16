package com.ggrantrichards.astralend.dev;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;

// Cut-outs of the survey's world for the website's dioramas: a box of blocks written to dioramas/<name>.txt
// (in the server's folder), then drawn in isometric with the game's own textures by
// tools/dioramas/render.py in CI. Only the blocks you could see are written, each with the faces that
// show (a face shows where the next block isn't a solid cube, or is outside the box - the cut face).
//
// The file: "size dx dy dz", then "palette" and one line per kind of block - id[axis] kind light height,
// kind b(lock) p(artial) c(ross: plants) f(lat: carpets) w(ater) l(ava) - then "x y z palette faces",
// faces six bits: down up north south west east.
final class Dioramas {
    private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    // A biome: 64 blocks square round `centre`, from the highest block down 72.
    static void biome(ServerLevel level, String name, BlockPos centre) {
        int top = level.getMinBuildHeight();
        for (int dx = -32; dx < 32; dx += 4) {
            for (int dz = -32; dz < 32; dz += 4) {
                top = Math.max(top, level.getHeight(Heightmap.Types.WORLD_SURFACE, centre.getX() + dx, centre.getZ() + dz));
            }
        }
        dump(level, name, new BlockPos(centre.getX() - 32, Math.max(level.getMinBuildHeight(), top - 72), centre.getZ() - 32),
            new BlockPos(centre.getX() + 31, top + 1, centre.getZ() + 31));
    }

    // A cave, cut open just above `floor` (its tunnels are low, so a higher cut shows only rock) so you
    // look down into it.
    static void cave(ServerLevel level, String name, BlockPos floor) {
        dump(level, name, floor.offset(-24, -14, -24), floor.offset(23, 1, 23));
    }

    // A structure round a block it's known by.
    static void around(ServerLevel level, String name, BlockPos marker, int radius, int below, int above) {
        dump(level, name, marker.offset(-radius, -below, -radius), marker.offset(radius, above, radius));
    }

    static void dump(ServerLevel level, String name, BlockPos min, BlockPos max) {
        Map<String, Integer> palette = new LinkedHashMap<>();
        StringBuilder blocks = new StringBuilder();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        int count = 0;
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    BlockState state = level.getBlockState(p.set(x, y, z));
                    if (state.isAir()) {
                        continue;
                    }
                    String kind = kind(level, p, state);
                    int faces = 0;
                    for (int f = 0; f < 6; f++) {
                        q.setWithOffset(p, FACES[f]);
                        boolean outside = q.getX() < min.getX() || q.getX() > max.getX() || q.getY() < min.getY() || q.getY() > max.getY()
                            || q.getZ() < min.getZ() || q.getZ() > max.getZ();
                        BlockState next = level.getBlockState(q);
                        boolean hidden = !outside && (next.isSolidRender(level, q) || (kind.equals("w") && next.is(state.getBlock())));
                        if (!hidden) {
                            faces |= 1 << f;
                        }
                    }
                    if (faces == 0) {
                        continue;
                    }
                    String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                    if (state.hasProperty(BlockStateProperties.AXIS)) {
                        id += "[axis=" + state.getValue(BlockStateProperties.AXIS).getName() + "]";
                    }
                    double height = state.getShape(level, p).isEmpty() ? 1.0 : state.getShape(level, p).max(Direction.Axis.Y);
                    String key = id + " " + kind + " " + state.getLightEmission() + " " + String.format(java.util.Locale.ROOT, "%.2f", height);
                    int index = palette.computeIfAbsent(key, k -> palette.size());
                    blocks.append(x - min.getX()).append(' ').append(y - min.getY()).append(' ').append(z - min.getZ()).append(' ')
                        .append(index).append(' ').append(faces).append('\n');
                    count++;
                }
            }
        }
        StringBuilder out = new StringBuilder();
        out.append("size ").append(max.getX() - min.getX() + 1).append(' ').append(max.getY() - min.getY() + 1).append(' ')
            .append(max.getZ() - min.getZ() + 1).append('\n');
        out.append("palette ").append(palette.size()).append('\n');
        palette.keySet().forEach(k -> out.append(k).append('\n'));
        out.append(blocks);
        try {
            Path dir = Path.of("dioramas");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(name + ".txt"), out);
            WorldgenSurvey.logLine("diorama " + name + ": " + count + " visible blocks from " + min.toShortString() + " to " + max.toShortString());
        } catch (IOException e) {
            WorldgenSurvey.logLine("diorama " + name + ": couldn't write it: " + e);
        }
    }

    private static String kind(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.WATER)) {
            return "w";
        }
        if (state.is(Blocks.LAVA)) {
            return "l";
        }
        if (state.isCollisionShapeFullBlock(level, pos)) {
            return "b";
        }
        if (state.getCollisionShape(level, pos).isEmpty()) {
            double h = state.getShape(level, pos).isEmpty() ? 0.0 : state.getShape(level, pos).max(Direction.Axis.Y);
            return h < 0.2 ? "f" : "c";
        }
        return "p";
    }

    private Dioramas() {}
}
