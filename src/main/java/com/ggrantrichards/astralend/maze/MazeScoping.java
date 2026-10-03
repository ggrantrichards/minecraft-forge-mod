package com.ggrantrichards.astralend.maze;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

// The Constellation Maze puzzle, played with a vanilla spyglass. Inside a maze, look through a
// spyglass and the game watches where you look: hold a constellation in the middle of the view
// (within 6 degrees) for a second and a half and every star-bridge of that constellation within 48
// blocks lights up for 20 seconds. The bridge leading on from each platform answers to one
// constellation; raise the spyglass there and a thread of starlight rises toward it. There are no
// words: chimes climb while you hold steady, and a dull note means nothing answers here.
//
// Mazes catch you, too: fall out of one and its starlight sets you back on the first platform,
// slow-falling, instead of letting you drop into the void.
public final class MazeScoping {
    private static final int HOLD_TICKS = 30;
    private static final double MAX_ANGLE = Math.toRadians(6.0);
    private static final int REACH = 48;
    public static final ResourceKey<Structure> MAZE = ResourceKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "constellation_maze"));

    // Per player, while they look through a spyglass in a maze: the constellation in view and for how long.
    private record Sighting(int[] constellation, int[] held, boolean[] hinted) {}

    private static final Map<UUID, Sighting> SIGHTINGS = new HashMap<>();

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(MazeScoping::onLevelTick);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.END) {
            return;
        }
        long now = level.getGameTime();
        for (ServerPlayer player : level.players()) {
            if (!player.isScoping() || player.isSpectator()) {
                SIGHTINGS.remove(player.getUUID());
                continue;
            }
            Sighting s = SIGHTINGS.get(player.getUUID());
            if (s == null) {
                if (!level.structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> holder.is(MAZE)).isValid()) {
                    continue;
                }
                s = new Sighting(new int[] {-1}, new int[1], new boolean[1]);
                SIGHTINGS.put(player.getUUID(), s);
            }
            if (!s.hinted()[0]) {
                s.hinted()[0] = true;
                hint(level, player);
            }
            int seen = -1;
            for (int id = 0; id < Constellations.ALL.length; id++) {
                double angle = Math.acos(Math.min(1.0, player.getLookAngle().dot(Constellations.get(id).direction())));
                if (angle < MAX_ANGLE) {
                    seen = id;
                }
            }
            if (seen != s.constellation()[0]) {
                s.constellation()[0] = seen;
                s.held()[0] = 0;
            }
            if (seen < 0) {
                continue;
            }
            Constellations.Constellation c = Constellations.get(seen);
            // Holding steady: a chime that climbs in pitch as the stars sharpen.
            if (++s.held()[0] < HOLD_TICKS) {
                if (s.held()[0] % 10 == 0) {
                    level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F,
                        0.6F + 0.4F * s.held()[0] / HOLD_TICKS);
                }
            } else if (s.held()[0] == HOLD_TICKS) {
                int lit = lightBridges(level, player.blockPosition(), seen);
                if (lit > 0) {
                    level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.7F);
                } else {
                    // Nothing answers here: a dull note.
                    level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 1.0F, 0.5F);
                }
            }
        }
        if (now % 5 == 0) {
            catchFallers(level);
        }
    }

    // The constellations the bridges starting near the player answer to (those still dark), and where
    // each lies in the sky.
    private static void hint(ServerLevel level, ServerPlayer player) {
        boolean[] near = new boolean[Constellations.ALL.length];
        BlockPos at = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-7, -3, -7), at.offset(7, 3, 7))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.STAR_BRIDGE.get()) && !state.getValue(StarBridgeBlock.LIT)) {
                near[state.getValue(StarBridgeBlock.CONSTELLATION)] = true;
            }
        }
        // No words: a thread of starlight rises from the player toward that constellation in the sky.
        for (int id = 0; id < near.length; id++) {
            if (near[id]) {
                Vec3 dir = Constellations.get(id).direction();
                Vec3 eye = player.getEyePosition();
                for (int k = 2; k <= 24; k++) {
                    Vec3 p = eye.add(dir.scale(k * 0.75));
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
                return;
            }
        }
    }

    // Lights every star-bridge of this constellation near where it was sighted from. Returns how many.
    public static int lightBridges(ServerLevel level, BlockPos from, int constellation) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(from.offset(-REACH, -24, -REACH), from.offset(REACH, 24, REACH))) {
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
            BlockPos start = firstPlatform(level, box);
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
        }
    }

    // The first platform: the lowest of the stars set into the platforms' floors (the maze climbs from
    // its first platform to the dais).
    private static BlockPos firstPlatform(ServerLevel level, BoundingBox box) {
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if ((best == null || pos.getY() < best.getY()) && level.getBlockState(pos).is(ModBlocks.STAR_INLAY_OBSERVATORY_QUARTZ.get())) {
                best = pos.immutable();
            }
        }
        return best;
    }

    private MazeScoping() {}
}
