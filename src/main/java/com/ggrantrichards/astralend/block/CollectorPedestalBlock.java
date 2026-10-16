package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import com.ggrantrichards.astralend.ModItems;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

// Collector Pedestal: holds one Void Crystal or Flare Bottle. Under open sky in the Eclipse during a
// Sunstorm it charges what it holds (CollectorPedestalBlockEntity). Use it with the item to
// set it down, with an empty hand to take it back. The state shows what it holds (a model each).
public class CollectorPedestalBlock extends Block implements EntityBlock {
    public enum Holding implements StringRepresentable {
        NONE, VOID_CRYSTAL, CHARGED_VOID_CRYSTAL, FLARE_BOTTLE, BOTTLED_FLARE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public boolean charged() {
            return this == CHARGED_VOID_CRYSTAL || this == BOTTLED_FLARE;
        }

        public static Holding of(ItemStack stack) {
            Item item = stack.getItem();
            if (item == ModItems.VOID_CRYSTAL.get()) return VOID_CRYSTAL;
            if (item == ModItems.CHARGED_VOID_CRYSTAL.get()) return CHARGED_VOID_CRYSTAL;
            if (item == ModItems.FLARE_BOTTLE.get()) return FLARE_BOTTLE;
            if (item == ModItems.BOTTLED_FLARE.get()) return BOTTLED_FLARE;
            return NONE;
        }
    }

    public static final EnumProperty<Holding> HOLDING = EnumProperty.create("holding", Holding.class);
    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(1.0, 0.0, 1.0, 15.0, 2.0, 15.0),
        Block.box(3.0, 2.0, 3.0, 13.0, 10.0, 13.0),
        Block.box(1.0, 10.0, 1.0, 15.0, 13.0, 15.0));

    public CollectorPedestalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HOLDING, Holding.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOLDING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CollectorPedestalBlockEntity pedestal) {
            pedestal.dropHeld(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CollectorPedestalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.COLLECTOR_PEDESTAL.get()) {
            return null;
        }
        return (l, pos, s, entity) -> CollectorPedestalBlockEntity.serverTick(l, pos, s, (CollectorPedestalBlockEntity) entity);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        Holding holding = Holding.of(stack);
        if (holding == Holding.NONE || state.getValue(HOLDING) != Holding.NONE) {
            return net.minecraft.world.Itemnet.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CollectorPedestalBlockEntity pedestal) {
            pedestal.setHeld(stack.consumeAndReturn(1, player));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(HOLDING) == Holding.NONE) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CollectorPedestalBlockEntity pedestal) {
            ItemStack taken = pedestal.takeHeld();
            if (!player.getInventory().add(taken)) {
                Block.popResource(level, pos.above(), taken);
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }
}
