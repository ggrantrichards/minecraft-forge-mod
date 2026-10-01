package com.ggrantrichards.astralend.item;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

// A Star Chart, given by the Watcher or found in ruins. It marks a place in the End for you to
// find: the nearest of a structure picked at random (a crater, a relay, a whalefall...), or, if none
// is in reach, the nearest region of a far-off biome. Charts found in chests start out unread and
// fill in the first time they're used in the End. Use one to see which way to go and how far.
public class StarChartItem extends Item {
    // Each destination is a structure tag holding just that structure (data/astralend/tags/worldgen/structure).
    private static final List<String> STRUCTURES = List.of("comet_crater", "wanderer_relay", "whalefall",
        "observatory_ruins", "solar_array", "wormhole_maw", "constellation_maze", "abandoned_orrery");
    private static final List<ResourceKey<Biome>> DESTINATIONS = List.of(ModBiomes.NEBULA_FIELDS, ModBiomes.AURORA_SHALLOWS,
        ModBiomes.COMET_WAKE, ModBiomes.DARK_MATTER_BELT, ModBiomes.CORONA);

    public StarChartItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack create(ServerLevel level, BlockPos origin) {
        ItemStack chart = new ItemStack(ModItems.STAR_CHART.get());
        mark(level, origin, chart);
        return chart;
    }

    // Fills in a chart: a structure if one is in reach, otherwise a biome region. A chart that names a
    // structure already (the Constellation Maze's, which always marks a Wormhole Maw) marks that one.
    private static void mark(ServerLevel level, BlockPos origin, ItemStack chart) {
        CustomData preset = chart.get(DataComponents.CUSTOM_DATA);
        String named = preset == null ? "" : preset.copyTag().getStringOr("Structure", "");
        String structure = STRUCTURES.contains(named) ? named : STRUCTURES.get(level.getRandom().nextInt(STRUCTURES.size()));
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, Identifier.fromNamespaceAndPath(AstralEnd.MODID, structure));
        BlockPos site = level.findNearestMapStructure(tag, origin, 64, false);
        if (site != null) {
            write(chart, site, "Structure", structure,
                Component.translatable("item.astralend.star_chart.marks_structure", structureName(structure)));
            return;
        }
        ResourceKey<Biome> wanted = DESTINATIONS.get(level.getRandom().nextInt(DESTINATIONS.size()));
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(wanted), origin, 3200, 64, 64);
        if (found != null) {
            String biome = wanted.identifier().getPath();
            write(chart, found.getFirst(), "Biome", biome, Component.translatable("item.astralend.star_chart.marks", biomeName(biome)));
        }
    }

    private static void write(ItemStack chart, BlockPos pos, String key, String value, Component lore) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TargetX", pos.getX());
        tag.putInt("TargetZ", pos.getZ());
        tag.putString(key, value);
        chart.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        chart.set(DataComponents.LORE, new ItemLore(List.of(lore.copy().withStyle(ChatFormatting.GRAY))));
    }

    private static Component structureName(String path) {
        return Component.translatable("structure.astralend." + path);
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
        if (data == null || !data.copyTag().contains("TargetX")) {
            // An unread chart from a chest: it only makes sense under the End's sky.
            if (level.dimension() != Level.END || !(level instanceof ServerLevel server)) {
                player.sendSystemMessage(Component.translatable("item.astralend.star_chart.blank"));
                return InteractionResult.SUCCESS;
            }
            mark(server, player.blockPosition(), chart);
            data = chart.get(DataComponents.CUSTOM_DATA);
            if (data == null || !data.copyTag().contains("TargetX")) {
                player.sendSystemMessage(Component.translatable("item.astralend.star_chart.blank"));
                return InteractionResult.SUCCESS;
            }
        }
        CompoundTag tag = data.copyTag();
        Component target = tag.contains("Structure")
            ? structureName(tag.getStringOr("Structure", "comet_crater"))
            : biomeName(tag.getStringOr("Biome", "nebula_fields"));
        int dx = tag.getIntOr("TargetX", 0) - player.getBlockX();
        int dz = tag.getIntOr("TargetZ", 0) - player.getBlockZ();
        int distance = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
        player.sendSystemMessage(Component.translatable("item.astralend.star_chart.direction",
            target, distance, Component.translatable(compass(dx, dz))));
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
