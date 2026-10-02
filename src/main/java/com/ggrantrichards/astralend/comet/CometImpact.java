package com.ggrantrichards.astralend.comet;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

// A comet's landfall: a flash, a boom, and a small crater - a bowl a few blocks across, ringed by a
// thrown-up rim, with the Comet Core (Meteoric Iron, with Stardust packed under it) in the middle. Never more
// than MAX_BLOCKS blocks change. It leaves the world alone if mob griefing is off, and it never touches
// anything that looks built (a block entity, planks, glass, wool, a door, a bed...) - the comet still
// lands, but only its loot is left.
public final class CometImpact {
    public static final int MAX_BLOCKS = 40;

    // Returns how many blocks changed.
    public static int strike(ServerLevel level, BlockPos hit, RandomSource random, @Nullable Entity comet) {
        BlockPos top = surface(level, hit);
        double x = top.getX() + 0.5, y = top.getY() + 1.0, z = top.getZ() + 0.5;
        level.playSound(null, top, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0F, 0.7F);
        level.playSound(null, top, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 3.0F, 0.5F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 120, 1.5, 1.0, 1.5, 0.3);
        level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 80, 2.0, 1.0, 2.0, 0.2);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 40, 1.6, 0.6, 1.6, 0.04);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(top).inflate(3.0), e -> e != comet)) {
            e.hurtServer(level, level.damageSources().explosion(comet, comet), 6.0F);
        }
        int changed = 0;
        if (griefing(level) && !looksBuilt(level, top)) {
            changed = carve(level, top, random);
        }
        // What the comet leaves behind, whether or not it could dig a crater.
        List<ItemStack> loot = new ArrayList<>();
        loot.add(new ItemStack(ModBlocks.STARDUST_BLOCK.get(), 2 + random.nextInt(3)));
        loot.add(new ItemStack(Blocks.BLUE_ICE, 1 + random.nextInt(2)));
        loot.add(new ItemStack(ModItems.RAW_METEORIC_IRON.get(), 1 + random.nextInt(2)));
        if (random.nextInt(5) == 0) {
            loot.add(new ItemStack(ModItems.TEMPORAL_CRYSTAL.get()));
        }
        for (ItemStack stack : loot) {
            ItemEntity item = new ItemEntity(level, x, y + 0.5, z, stack);
            item.setDeltaMovement((random.nextDouble() - 0.5) * 0.3, 0.25, (random.nextDouble() - 0.5) * 0.3);
            item.setDefaultPickUpDelay();
            level.addFreshEntity(item);
        }
        return changed;
    }

    private static boolean griefing(ServerLevel level) {
        return level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    // The highest solid block at or below `hit` (the comet aims at the surface).
    private static BlockPos surface(ServerLevel level, BlockPos hit) {
        BlockPos.MutableBlockPos p = hit.mutable().move(0, 3, 0);
        for (int i = 0; i < 12 && p.getY() > level.getMinY(); i++, p.move(0, -1, 0)) {
            if (level.getBlockState(p).isSolid()) {
                return p.immutable();
            }
        }
        return hit;
    }

    // Anything a player would have made: block entities, planks, glass, wool, doors, beds, rails...
    private static boolean looksBuilt(ServerLevel level, BlockPos top) {
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-6, -4, -6), top.offset(6, 6, 6))) {
            BlockState s = level.getBlockState(p);
            if (s.hasBlockEntity() || s.is(BlockTags.PLANKS) || s.is(BlockTags.WOOL) || s.is(BlockTags.DOORS) || s.is(BlockTags.BEDS)
                || s.is(BlockTags.RAILS) || s.is(Blocks.GLASS) || s.is(BlockTags.IMPERMEABLE) || s.is(Blocks.TORCH)) {
                return true;
            }
        }
        return false;
    }

    // A small copy of the Comet Craters the world generates with: a bowl five blocks across, a rim of the
    // ground it threw up around it (some of it charred, with lumps of the comet's ice), the floor glazed
    // with blue ice, and the Comet Core (Meteoric Iron, with Stardust packed under it where it can't fall
    // up) sitting in the middle.
    // The bowl and the Core come first; the rim and the glaze only use what's left of MAX_BLOCKS.
    private static int carve(ServerLevel level, BlockPos top, RandomSource random) {
        int[] changed = {0};
        BlockState ground = level.getBlockState(top);
        if (!ground.isSolid() || ground.getDestroySpeed(level, top) < 0) {
            ground = ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        }
        // The bowl: the top layer, all but the middle, where the Core sits.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dz * dz > 5 || (dx == 0 && dz == 0)) {
                    continue;
                }
                BlockPos p = top.offset(dx, 0, dz);
                BlockState s = level.getBlockState(p);
                if (!s.isAir() && s.getDestroySpeed(level, p) >= 0) {
                    set(level, p, Blocks.AIR.defaultBlockState(), changed);
                }
            }
        }
        set(level, top, ModBlocks.METEORIC_IRON_ORE.get().defaultBlockState(), changed);
        set(level, top.below(), ModBlocks.STARDUST_BLOCK.get().defaultBlockState(), changed);
        // The rim: what the impact threw out, piled around the edge, with lumps of the comet's ice in it.
        BlockState charred = ModBlocks.CHARRED_END_STONE.get().defaultBlockState();
        BlockState ice = Blocks.BLUE_ICE.defaultBlockState();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + (random.nextDouble() - 0.5) * 0.6;
                if (d < 2.6 || d > 3.7 || random.nextInt(5) < 2 || changed[0] >= MAX_BLOCKS - 7) {
                    continue;
                }
                BlockPos p = top.offset(dx, 1, dz);
                if (level.getBlockState(p).canBeReplaced() && level.getBlockState(p.below()).isSolid()) {
                    int roll = random.nextInt(3);
                    set(level, p, roll == 0 ? ice : roll == 1 ? charred : ground, changed);
                }
            }
        }
        // The floor: glazed with the comet's ice here and there, scorched elsewhere.
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-2, -1, -2), top.offset(2, -1, 2))) {
            int dx = p.getX() - top.getX(), dz = p.getZ() - top.getZ();
            if (changed[0] >= MAX_BLOCKS || dx * dx + dz * dz > 5 || (dx == 0 && dz == 0) || !level.getBlockState(p.above()).isAir()) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (!s.isSolid() || s.getDestroySpeed(level, p) < 0) {
                continue;
            }
            int roll = random.nextInt(8);
            if (roll < 4) {
                set(level, p, ice, changed);
            } else if (roll < 6 && !s.is(ModBlocks.CHARRED_END_STONE.get())) {
                set(level, p, charred, changed);
            }
        }
        return changed[0];
    }

    private static void set(ServerLevel level, BlockPos p, BlockState state, int[] changed) {
        if (changed[0] < MAX_BLOCKS && level.setBlock(p, state, Block.UPDATE_ALL)) {
            changed[0]++;
        }
    }

    private CometImpact() {}
}
