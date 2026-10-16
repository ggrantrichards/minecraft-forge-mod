package com.ggrantrichards.astralend.item;

import com.ggrantrichards.astralend.entity.DriftWhale;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

// A Chorus Flower on a Stick: the Drift Whale's carrot on a stick. Use it on a whale to climb on;
// held while riding, the whale swims where you look; used while riding, it makes the whale surge
// ahead for a while (the whale nibbles the flower: 5 of its 30 uses).
public class ChorusFlowerOnAStickItem extends Item {
    public static final int BOOST_COST = 5;

    public ChorusFlowerOnAStickItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        Entity vehicle = player.getControlledVehicle();
        if (vehicle instanceof DriftWhale whale && whale.boost()) {
            ItemStack result = stack.hurtAndConvertOnBreak(BOOST_COST, Items.FISHING_ROD, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            return net.minecraft.world.InteractionResultHolder.success(result);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    public static boolean holding(Player player) {
        return player.getMainHandItem().getItem() instanceof ChorusFlowerOnAStickItem
            || player.getOffhandItem().getItem() instanceof ChorusFlowerOnAStickItem;
    }
}
