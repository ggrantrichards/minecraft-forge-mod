package com.ggrantrichards.astralend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

// Memory Moss: pale grey-violet moss of the Ashen Barrens. Brush it, or tread on it, and for a few
// seconds faint afterimages rise from it of what once stood there - a pillar, an arch, a doorway, a
// rib of something huge, a tall thin figure. The game never says what they were.
//
// Which shape a patch remembers is fixed by where it is. Its block state counts the echo down
// (ECHO 3..1, one step a second, each step drawing the shape again); 0 is quiet.
public class MemoryMossBlock extends Block {
    public static final IntegerProperty ECHO = IntegerProperty.create("echo", 0, 3);
    private static final DustParticleOptions AFTERIMAGE = new DustParticleOptions(0xC8C0DC, 1.0F);

    public MemoryMossBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ECHO, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ECHO);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity && state.getValue(ECHO) == 0
                && server.getRandom().nextInt(12) == 0) {
            remember(server, pos, state);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.BRUSH)) {
            if (level instanceof ServerLevel server && state.getValue(ECHO) == 0) {
                remember(server, pos, state);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    public static void remember(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(ECHO, 3), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, state.getBlock(), 1);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.4F, 0.5F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int echo = state.getValue(ECHO);
        if (echo == 0) {
            return;
        }
        drawAfterimage(level, pos, echo);
        level.setBlock(pos, state.setValue(ECHO, echo - 1), Block.UPDATE_CLIENTS);
        if (echo > 1) {
            level.scheduleTick(pos, this, 20);
        }
    }

    // The shape this patch remembers, drawn in faint dust above it, a little fainter each time.
    static void drawAfterimage(ServerLevel level, BlockPos pos, int strength) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        int shape = (int) Math.floorMod(h >>> 20, 5L);
        boolean alongX = ((h >>> 40) & 1) == 0;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 1.0, cz = pos.getZ() + 0.5;
        int count = 2 + strength;
        for (double[] p : points(shape)) {
            double x = alongX ? cx + p[0] : cx, z = alongX ? cz : cz + p[0];
            level.sendParticles(AFTERIMAGE, x, cy + p[1], z, count, 0.08, 0.08, 0.08, 0.0);
        }
    }

    // Points of each shape, (sideways, up) in blocks.
    private static java.util.List<double[]> points(int shape) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        switch (shape) {
            case 0 -> {   // a pillar
                for (double y = 0; y <= 4.0; y += 0.35) {
                    out.add(new double[] {-0.4, y});
                    out.add(new double[] {0.4, y});
                }
            }
            case 1 -> {   // an arch
                for (double a = 0; a <= Math.PI; a += 0.15) {
                    out.add(new double[] {Math.cos(a) * 2.0, 2.0 + Math.sin(a) * 2.0});
                }
                for (double y = 0; y < 2.0; y += 0.35) {
                    out.add(new double[] {-2.0, y});
                    out.add(new double[] {2.0, y});
                }
            }
            case 2 -> {   // a doorway
                for (double y = 0; y <= 2.6; y += 0.3) {
                    out.add(new double[] {-0.7, y});
                    out.add(new double[] {0.7, y});
                }
                for (double x = -0.7; x <= 0.7; x += 0.3) {
                    out.add(new double[] {x, 2.6});
                }
            }
            case 3 -> {   // a great rib, curving up and over
                for (double a = 0; a <= Math.PI * 0.8; a += 0.12) {
                    out.add(new double[] {Math.cos(a) * 3.0 - 1.0, Math.sin(a) * 4.5});
                }
            }
            default -> {  // a tall, thin figure
                for (double y = 0; y <= 2.0; y += 0.3) {
                    out.add(new double[] {-0.15, y});
                    out.add(new double[] {0.15, y});
                }
                for (double y = 2.0; y <= 2.9; y += 0.25) {
                    out.add(new double[] {0.0, y});
                }
                for (double y = 0.9; y <= 2.0; y += 0.3) {
                    out.add(new double[] {-0.5, y});
                    out.add(new double[] {0.5, y});
                }
            }
        }
        return out;
    }
}
