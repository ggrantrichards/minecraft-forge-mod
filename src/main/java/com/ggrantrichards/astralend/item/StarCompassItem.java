package com.ggrantrichards.astralend.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// The Star Compass: a compass set among Starblooms, which always turn toward the End's centre. In the
// End it points to the central island (it carries a lodestone target there, so it needs no lodestone);
// anywhere else it spins, like a compass in the End.
public class StarCompassItem extends Item {
    public StarCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, java.util.function.Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("item.astralend.star_compass.points").withStyle(ChatFormatting.GRAY));
    }
}
