package com.ggrantrichards.astralend.item;

import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

// A Star Chart, given by the Watcher. It marks another part of the End for you to find: the
// nearest region of a biome picked at random from the ones other than where you met the Watcher.
// Use it to see which way to go and how far. (In Phase 6 it will point at structures instead.)
public class StarChartItem extends Item {
    private static final List<ResourceKey<Biome>> DESTINATIONS = List.of(ModBiomes.NEBULA_FIELDS, ModBiomes.AURORA_SHALLOWS,
        ModBiomes.COMET_WAKE, ModBiomes.DARK_MATTER_BELT, ModBiomes.CORONA);

    public StarChartItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack create(ServerLevel level, BlockPos origin) {
        ItemStack chart = new ItemStack(ModItems.STAR_CHART.get());
        ResourceKey<Biome> wanted = DESTINATIONS.get(level.getRandom().nextInt(DESTINATIONS.size()));
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(wanted), origin, 3200, 64, 64);
        if (found == null) {
            return chart;
        }
        BlockPos pos = found.getFirst();
        CompoundTag tag = new CompoundTag();
        tag.putInt("TargetX", pos.getX());
        tag.putInt("TargetZ", pos.getZ());
        tag.putString("Biome", wanted.identifier().getPath());
        chart.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        chart.set(DataComponents.LORE, new ItemLore(List.of(
            Component.translatable("item.astralend.star_chart.marks", biomeName(wanted.identifier().getPath()))
                .withStyle(ChatFormatting.GRAY))));
        return chart;
    }

    private static Component biomeName(String path) {
        return Component.translatable("biome.astralend." + path);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack chart = player.getItemInHand(hand);
        CustomData data = chart.get(DataComponents.CUSTOM_DATA);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (data == null) {
            player.sendSystemMessage(Component.translatable("item.astralend.star_chart.blank"));
            return InteractionResult.SUCCESS;
        }
        CompoundTag tag = data.copyTag();
        int dx = tag.getIntOr("TargetX", 0) - player.getBlockX();
        int dz = tag.getIntOr("TargetZ", 0) - player.getBlockZ();
        int distance = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
        player.sendSystemMessage(Component.translatable("item.astralend.star_chart.direction",
            biomeName(tag.getStringOr("Biome", "nebula_fields")), distance, Component.translatable(compass(dx, dz))));
        return InteractionResult.SUCCESS;
    }

    // "north", "north-east"... for a horizontal offset (Minecraft's north is -z).
    private static String compass(int dx, int dz) {
        String[] names = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));  // 0 = south, 90 = west
        int index = (int) Math.round(((angle % 360) + 360) % 360 / 45.0) % 8;
        return "item.astralend.star_chart." + names[index];
    }
}
