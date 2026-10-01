package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

// The Astral Forge's menu: like a smithing table's, with two more slots. In a row: the Starforge
// Template, the item, the Starforge Alloy, and the upgrade's material (one or two items); the result
// on the right.
public class AstralForgeMenu extends ItemCombinerMenu {
    public static final int TEMPLATE = 0;
    public static final int BASE = 1;
    public static final int ALLOY = 2;
    public static final int MATERIAL = 3;
    public static final int MATERIAL_2 = 4;
    public static final int RESULT = 5;
    public static final int SLOT_Y = 48;
    private final DataSlot hasError = DataSlot.standalone();

    public AstralForgeMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public AstralForgeMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.ASTRAL_FORGE.get(), containerId, inventory, access, ItemCombinerMenuSlotDefinition.create()
            .withSlot(TEMPLATE, 8, SLOT_Y, stack -> stack.is(ModItems.STARFORGE_TEMPLATE.get()))
            .withSlot(BASE, 26, SLOT_Y, stack -> StarforgeUpgrade.Kind.of(stack).any())
            .withSlot(ALLOY, 44, SLOT_Y, stack -> stack.is(ModItems.STARFORGE_ALLOY.get()))
            .withSlot(MATERIAL, 62, SLOT_Y, StarforgeUpgrade::isMaterial)
            .withSlot(MATERIAL_2, 80, SLOT_Y, StarforgeUpgrade::isMaterial)
            .withResultSlot(RESULT, 134, SLOT_Y)
            .build());
        this.addDataSlot(this.hasError).set(0);
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.ASTRAL_FORGE.get());
    }

    // The upgrade the material slots spell out, if any.
    public StarforgeUpgrade upgrade() {
        return StarforgeUpgrade.byMaterials(this.inputSlots.getItem(MATERIAL), this.inputSlots.getItem(MATERIAL_2));
    }

    @Override
    public void createResult() {
        ItemStack template = this.inputSlots.getItem(TEMPLATE);
        ItemStack base = this.inputSlots.getItem(BASE);
        ItemStack alloy = this.inputSlots.getItem(ALLOY);
        StarforgeUpgrade upgrade = this.upgrade();
        ItemStack result = ItemStack.EMPTY;
        if (template.is(ModItems.STARFORGE_TEMPLATE.get()) && alloy.is(ModItems.STARFORGE_ALLOY.get()) && upgrade != null
            && upgrade.fits(base) && Starforge.get(base) != upgrade) {
            result = Starforge.apply(base, upgrade);
        }
        this.resultSlots.setItem(0, result);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        boolean full = this.getSlot(TEMPLATE).hasItem() && this.getSlot(BASE).hasItem() && this.getSlot(ALLOY).hasItem()
            && this.getSlot(MATERIAL).hasItem();
        this.hasError.set(full && !this.getSlot(RESULT).hasItem() ? 1 : 0);
    }

    public boolean hasError() {
        return this.hasError.get() > 0;
    }

    @Override
    protected void onTake(Player player, ItemStack carried) {
        carried.onCraftedBy(player, carried.getCount());
        for (int slot = TEMPLATE; slot <= MATERIAL_2; slot++) {
            ItemStack stack = this.inputSlots.getItem(slot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                this.inputSlots.setItem(slot, stack);
            }
        }
        this.access.execute((level, pos) -> {
            level.levelEvent(1044, pos, 0);   // the smithing table's ring
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel server && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                com.ggrantrichards.astralend.world.ModAdvancements.award(serverPlayer, "starforged");
            }
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    // Shift-clicking an item in sends it to the slot it belongs in.
    @Override
    protected boolean canMoveIntoInputSlots(ItemStack stack) {
        for (int slot = TEMPLATE; slot <= MATERIAL_2; slot++) {
            if (!this.getSlot(slot).hasItem() && this.getSlot(slot).mayPlace(stack)) {
                return true;
            }
        }
        return false;
    }
}
