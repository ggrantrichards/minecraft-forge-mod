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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
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
import net.minecraft.world.phys.Vec3;

// A Star Chart, found in ruins. It marks a place in the End for you to
// find: the nearest of a structure picked at random (a crater, a Leviathan, an observatory...), or, if none
// is in reach, the nearest region of a far-off biome. Charts found in chests start out unread and
// fill in the first time they're used in the End. Use one and a thread of starlight points the way;
// the chime is higher the nearer the place is.
public class StarChartItem extends Item {
    // Each destination is a structure tag holding just that structure (data/astralend/tags/worldgen/structure).
    private static final List<String> STRUCTURES = List.of("comet_crater", "leviathan",
        "shattered_observatory", "solar_array");
    private static final List<ResourceKey<Biome>> DESTINATIONS = List.of(ModBiomes.NEBULA_FIELDS, ModBiomes.ASHEN_BARRENS,
        ModBiomes.DEEP_HOLLOWS, ModBiomes.ECLIPSE, ModBiomes.GLIMMERING_CRAGS);

    public StarChartItem(Item.Properties properties) {
        super(properties);
    }

    public static ItemStack create(ServerLevel level, BlockPos origin) {
        ItemStack chart = new ItemStack(ModItems.STAR_CHART.get());
        mark(level, origin, chart);
        return chart;
    }

    // Fills in a chart: a structure if one is in reach, otherwise a biome region. A chart that names a
    // structure already (the Constellation Maze's, which always marks a Leviathan) marks that one.
    private static void mark(ServerLevel level, BlockPos origin, ItemStack chart) {
        CustomData preset = chart.get(DataComponents.CUSTOM_DATA);
        String named = preset == null ? "" : preset.copyTag().getString("Structure");
        String structure = STRUCTURES.contains(named) ? named : STRUCTURES.get(level.getRandom().nextInt(STRUCTURES.size()));
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, structure));
        BlockPos site = level.findNearestMapStructure(tag, origin, 64, false);
        if (site != null) {
            write(chart, site, "Structure", structure,
                Component.translatable("item.astralend.star_chart.marks_structure", structureName(structure)));
            return;
        }
        ResourceKey<Biome> wanted = DESTINATIONS.get(level.getRandom().nextInt(DESTINATIONS.size()));
        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome -> biome.is(wanted), origin, 3200, 64, 64);
        if (found != null) {
            String biome = wanted.location().getPath();
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
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack chart = player.getItemInHand(hand);
        CustomData data = chart.get(DataComponents.CUSTOM_DATA);
        if (level.isClientSide()) {
            return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
        }
        if (data == null || !data.copyTag().contains("TargetX")) {
            // An unread chart from a chest: it only makes sense under the End's sky.
            if (level.dimension() != Level.END || !(level instanceof ServerLevel server)) {
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
                return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
            }
            mark(server, player.blockPosition(), chart);
            data = chart.get(DataComponents.CUSTOM_DATA);
            if (data == null || !data.copyTag().contains("TargetX")) {
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 0.6F);
                return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        // No words: a thread of starlight runs from your eyes toward the place, rising as it goes, and
        // the nearer the place, the brighter the note.
        CompoundTag tag = data.copyTag();
        double dx = (tag.contains("TargetX") ? tag.getInt("TargetX") : 0) + 0.5 - player.getX();
        double dz = (tag.contains("TargetZ") ? tag.getInt("TargetZ") : 0) + 0.5 - player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (level instanceof ServerLevel server && distance > 1.0) {
            Vec3 dir = new Vec3(dx / distance, 0.12, dz / distance).normalize();
            Vec3 eye = player.getEyePosition();
            for (int k = 2; k <= 28; k++) {
                Vec3 p = eye.add(dir.scale(k * 0.6));
                server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
        float pitch = (float) Mth.clamp(1.6 - distance / 1500.0, 0.6, 1.6);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, pitch);
        return net.minecraft.world.InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
