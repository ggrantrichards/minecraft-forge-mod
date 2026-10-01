package com.ggrantrichards.astralend.comet;

import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.world.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.jspecify.annotations.Nullable;

// When and where live comets appear. Every 10 seconds, for each player out among the outer islands
// with no comet already near them, there's a small chance one sets off on an arc past them: about
// one every 12 minutes in the Comet Wake, about one an hour anywhere else in the outer End. About a
// third of them end in landfall on an island nearby.
//
// A comet's whole path stays within ~110 blocks of the player it was sent to, so it's always in
// ticking chunks (a comet in a chunk that isn't ticking would just hang there).
public final class CometShowers {
    private static final int CHECK = 200;
    private static final double REACH = 110.0;
    // Where comets come: biomes tagged #astralend:live_comets (the outer End). An empty tag in a data
    // pack turns them off.
    public static final net.minecraft.tags.TagKey<Biome> COMET_BIOMES = net.minecraft.tags.TagKey.create(
        net.minecraft.core.registries.Registries.BIOME, net.minecraft.resources.Identifier.fromNamespaceAndPath(
            com.ggrantrichards.astralend.AstralEnd.MODID, "live_comets"));

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(CometShowers::onLevelTick);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.END || level.getGameTime() % CHECK != 0) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.blockPosition().getX() * (long) player.blockPosition().getX()
                + player.blockPosition().getZ() * (long) player.blockPosition().getZ() < 1000L * 1000L) {
                continue;
            }
            if (!level.getEntitiesOfClass(LiveComet.class, new AABB(player.blockPosition()).inflate(200.0)).isEmpty()) {
                continue;
            }
            Holder<Biome> biome = level.getBiome(player.blockPosition());
            if (!biome.is(COMET_BIOMES)) {
                continue;
            }
            int odds = biome.is(ModBiomes.COMET_WAKE) ? 72 : biome.is(ModBiomes.CORONA) ? 900 : 360;
            if (random.nextInt(odds) == 0) {
                send(level, player.position(), random, random.nextInt(3) == 0);
            }
        }
    }

    // Sends a comet past `near`; with landfall, it ends on the nearest suitable island ground.
    // Returns the comet, or null if it couldn't be placed.
    public static @Nullable LiveComet send(ServerLevel level, Vec3 near, RandomSource random, boolean landfall) {
        LiveComet comet = ModEntities.LIVE_COMET.get().create(level, EntitySpawnReason.EVENT);
        if (comet == null) {
            return null;
        }
        double angle = random.nextDouble() * Math.PI * 2;
        Vec3 from = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        double top = Math.min(level.getMinY() + 250.0, near.y + 70.0 + random.nextDouble() * 30.0);
        Vec3 start = near.add(from.scale(REACH)).with(net.minecraft.core.Direction.Axis.Y, top);
        Vec3 end = null;
        if (landfall) {
            end = landingSpot(level, near, from, random);
        }
        if (end == null) {
            landfall = false;
            Vec3 across = from.yRot((float) ((random.nextDouble() - 0.5) * 0.8)).scale(-REACH);
            end = near.add(across).with(net.minecraft.core.Direction.Axis.Y, Math.max(level.getMinY() + 10.0, near.y + 10.0 + random.nextDouble() * 30.0));
        }
        Vec3 side = new Vec3(-from.z, 0, from.x).scale((random.nextBoolean() ? 1 : -1) * (25.0 + random.nextDouble() * 20.0));
        Vec3 control = start.add(end).scale(0.5).add(side).add(0, 20.0, 0);
        int duration = (int) (start.distanceTo(end) / 0.17);      // about 3.4 blocks a second
        comet.launch(start, control, end, duration, landfall);
        level.addFreshEntity(comet);
        return comet;
    }

    // Island ground 25-80 blocks from `near`, toward the side the comet comes from (so it falls in front
    // of the player rather than behind them).
    private static @Nullable Vec3 landingSpot(ServerLevel level, Vec3 near, Vec3 from, RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double a = Math.atan2(from.z, from.x) + (random.nextDouble() - 0.5) * 1.6;
            double d = 25.0 + random.nextDouble() * 55.0;
            int x = Mth.floor(near.x + Math.cos(a) * d);
            int z = Mth.floor(near.z + Math.sin(a) * d);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y > level.getMinY() + 8 && level.getBlockState(new BlockPos(x, y - 1, z)).isSolid()) {
                return new Vec3(x + 0.5, y - 1, z + 0.5);
            }
        }
        return null;
    }

    private CometShowers() {}
}
