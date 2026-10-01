package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.StellarFlares;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// What a Collector Pedestal holds, and how long it has been charging. Once a second during a flare,
// if the pedestal is in the Corona with nothing above it, the charge grows; after 30 seconds of
// flare the Void Crystal or Flare Bottle is charged.
public class CollectorPedestalBlockEntity extends BlockEntity {
    public static final int CHARGE_SECONDS = 30;
    private ItemStack held = ItemStack.EMPTY;
    private int charge;

    public CollectorPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COLLECTOR_PEDESTAL.get(), pos, state);
    }

    public void setHeld(ItemStack stack) {
        this.held = stack;
        this.charge = 0;
        this.update();
    }

    public ItemStack takeHeld() {
        ItemStack taken = this.held;
        this.held = ItemStack.EMPTY;
        this.charge = 0;
        this.update();
        return taken;
    }

    private void update() {
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            CollectorPedestalBlock.Holding holding = CollectorPedestalBlock.Holding.of(this.held);
            if (state.getValue(CollectorPedestalBlock.HOLDING) != holding) {
                this.level.setBlock(this.worldPosition, state.setValue(CollectorPedestalBlock.HOLDING, holding), Block.UPDATE_ALL);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CollectorPedestalBlockEntity pedestal) {
        if (level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        CollectorPedestalBlock.Holding holding = CollectorPedestalBlock.Holding.of(pedestal.held);
        if (holding != CollectorPedestalBlock.Holding.VOID_CRYSTAL && holding != CollectorPedestalBlock.Holding.FLARE_BOTTLE) {
            return;
        }
        if (StellarFlares.phaseIn(level) != FlarePhase.FLARE || !StellarFlares.isCorona(level, pos)
            || !StellarFlares.underOpenSky(level, pos.above())) {
            return;
        }
        pedestal.charge++;
        pedestal.setChanged();
        server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 3, 0.2, 0.3, 0.2, 0.02);
        if (pedestal.charge >= CHARGE_SECONDS) {
            pedestal.held = new ItemStack(holding == CollectorPedestalBlock.Holding.VOID_CRYSTAL
                ? ModItems.CHARGED_VOID_CRYSTAL.get() : ModItems.BOTTLED_FLARE.get());
            pedestal.charge = 0;
            pedestal.update();
            server.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.4F);
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 24, 0.1, 0.1, 0.1, 0.12);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.held.isEmpty()) {
            Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, this.held);
            this.held = ItemStack.EMPTY;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("held", ItemStack.OPTIONAL_CODEC, this.held);
        output.putInt("charge", this.charge);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.held = input.read("held", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.charge = input.getIntOr("charge", 0);
    }
}
