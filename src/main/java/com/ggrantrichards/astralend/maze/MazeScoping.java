package com.ggrantrichards.astralend.maze;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;

// The Constellation Maze puzzle. Using a Maze Telescope starts a "sighting": for 30 seconds, while
// you stay beside the telescope, the game watches where you look. Hold your gaze on its
// constellation (within 9 degrees) for a second and a half, and every star-bridge of that
// constellation within 48 blocks lights up for 20 seconds.
//
// Mazes catch you, too: fall out of one and its starlight sets you back on the first platform
// (beside the telescope aimed at the first constellation), slow-falling, instead of letting you drop
// into the void.
public final class MazeScoping {
    private static final int SIGHTING_TICKS = 600;
    private static final int HOLD_TICKS = 30;
    private static final double MAX_ANGLE = Math.toRadians(9.0);
    private static final int REACH = 48;
    public static final ResourceKey<Structure> MAZE = ResourceKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "constellation_maze"));

    private record Sighting(BlockPos telescope, int constellation, long started, int[] held) {}

    private static final Map<UUID, Sighting> SIGHTINGS = new HashMap<>();

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(MazeScoping::onLevelTick);
    }

    static void start(ServerPlayer player, BlockPos telescope, int constellation) {
        SIGHTINGS.put(player.getUUID(), new Sighting(telescope.immutable(), constellation, player.level().getGameTime(), new int[1]));
        Constellations.Constellation c = Constellations.get(constellation);
        player.sendSystemMessage(Component.translatable("maze.astralend.aimed",
            Component.translatable("maze.astralend.constellation." + c.name()), Component.translatable(Constellations.compassKey(c))));
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.END) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<UUID, Sighting>> it = SIGHTINGS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Sighting> entry = it.next();
            Sighting s = entry.getValue();
            if (!(level.getPlayerByUUID(entry.getKey()) instanceof ServerPlayer player)) {
                continue;
            }
            if (now - s.started() > SIGHTING_TICKS || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(s.telescope())) > 25.0
                || !level.getBlockState(s.telescope()).is(ModBlocks.MAZE_TELESCOPE.get())) {
                it.remove();
                continue;
            }
            Constellations.Constellation c = Constellations.get(s.constellation());
            double angle = Math.acos(Math.min(1.0, player.getLookAngle().dot(c.direction())));
            s.held()[0] = angle < MAX_ANGLE ? s.held()[0] + 1 : Math.max(0, s.held()[0] - 2);
            if (s.held()[0] > 0 && now % 10 == 0) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("maze.astralend.focusing",
                    Component.translatable("maze.astralend.constellation." + c.name()))));
            }
            if (s.held()[0] >= HOLD_TICKS) {
                int lit = lightBridges(level, s.telescope(), s.constellation());
                level.playSound(null, s.telescope(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.7F);
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable(
                    lit > 0 ? "maze.astralend.lit" : "maze.astralend.nothing", Component.translatable("maze.astralend.constellation." + c.name()))));
                it.remove();
            }
        }
        if (now % 5 == 0) {
            catchFallers(level);
        }
    }

    // Lights every star-bridge of this constellation near the telescope. Returns how many.
    public static int lightBridges(ServerLevel level, BlockPos telescope, int constellation) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(telescope.offset(-REACH, -24, -REACH), telescope.offset(REACH, 24, REACH))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.STAR_BRIDGE.get()) && state.getValue(StarBridgeBlock.CONSTELLATION) == constellation) {
                StarBridgeBlock.light(level, pos.immutable(), state);
                count++;
            }
        }
        return count;
    }

    // Anyone falling through the lower part of a maze is caught and set back at its start.
    private static void catchFallers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.onGround() || player.fallDistance < 6.0 || player.getDeltaMovement().y > -0.3) {
                continue;
            }
            StructureStart maze = level.structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> holder.is(MAZE));
            if (!maze.isValid()) {
                continue;
            }
            BoundingBox box = maze.getBoundingBox();
            if (player.getY() > box.minY() + 14) {
                continue;
            }
            BlockPos start = firstTelescope(level, box);
            if (start == null) {
                continue;
            }
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            player.teleportTo(start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5);
            player.resetFallDistance();
            player.setDeltaMovement(0, 0, 0);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100));
            level.playSound(null, start, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.6F);
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.translatable("maze.astralend.caught")));
        }
    }

    // Beside the telescope aimed at the first constellation: a spot on that platform.
    private static BlockPos firstTelescope(ServerLevel level, BoundingBox box) {
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.MAZE_TELESCOPE.get()) && state.getValue(MazeTelescopeBlock.CONSTELLATION) == 0) {
                return pos.relative(state.getValue(MazeTelescopeBlock.FACING).getOpposite()).immutable();
            }
        }
        return null;
    }

    private MazeScoping() {}
}
