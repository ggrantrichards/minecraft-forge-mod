package com.ggrantrichards.astralend.starforge;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// A Rift Pearl: a boss's last breath of the void. Right-click it onto a Voidwalker chestplate in
// your inventory (the way you fill a bundle) and the chestplate holds it; fall into the void and it
// brings you back to your last footing (Void Recall, in StarforgeEffects).
public class RiftPearlItem extends Item {
    public RiftPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack self, Slot slot, ClickAction action, Player player) {
        ItemStack target = slot.getItem();
        if (action != ClickAction.SECONDARY || !Starforge.has(target, StarforgeUpgrade.VOID_RECALL)
            || Starforge.slotOf(target) != net.minecraft.world.entity.EquipmentSlot.CHEST || target.has(Starforge.RIFT_PEARL.get())
            || !slot.allowModification(player)) {
            return false;
        }
        target.set(Starforge.RIFT_PEARL.get(), Unit.INSTANCE);
        slot.setChanged();
        self.shrink(1);
        player.level().playSound(player, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }
}
