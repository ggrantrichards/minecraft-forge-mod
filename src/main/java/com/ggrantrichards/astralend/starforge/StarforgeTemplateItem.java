package com.ggrantrichards.astralend.starforge;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

// The Starforge Template: found in station vaults, Observatory vaults and maze rewards, and
// copied like a smithing template. Its tooltip reads like a smithing template's.
public class StarforgeTemplateItem extends Item {
    public StarforgeTemplateItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("item.astralend.starforge_template.kind").withStyle(ChatFormatting.GRAY));
        builder.accept(CommonComponents.EMPTY);
        builder.accept(Component.translatable("item.minecraft.smithing_template.applies_to").withStyle(ChatFormatting.GRAY));
        builder.accept(CommonComponents.space().append(Component.translatable("item.astralend.starforge_template.applies_to"))
            .withStyle(ChatFormatting.BLUE));
        builder.accept(Component.translatable("item.minecraft.smithing_template.ingredients").withStyle(ChatFormatting.GRAY));
        builder.accept(CommonComponents.space().append(Component.translatable("item.astralend.starforge_template.ingredients"))
            .withStyle(ChatFormatting.BLUE));
    }
}
