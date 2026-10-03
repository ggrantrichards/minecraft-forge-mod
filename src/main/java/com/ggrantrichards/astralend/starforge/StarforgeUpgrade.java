package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The six Starforge upgrades (docs/END_UPDATE.md, section 9). Each does one thing per kind of item,
// shown on its tooltip like an enchantment, and leaves a colour accent on the item (see
// tools/textures/starforge_textures.py, which uses the same colours).
public enum StarforgeUpgrade implements StringRepresentable {
    VOIDWALKER("voidwalker", 0xA98CFF, () -> List.of(ModItems.VOID_CRYSTAL.get(), ModItems.UMBRAL_MEMBRANE.get())),
    METEORIC("meteoric", 0xF07A3C, () -> List.of(ModItems.METEORIC_IRON_INGOT.get())),
    STARDUST("stardust", 0xA8D4FF, () -> List.of(ModBlocks.STARDUST_BLOCK.get().asItem())),
    TEMPORAL("temporal", 0xEEA650, () -> List.of(ModItems.TEMPORAL_CRYSTAL.get())),
    SINGULARITY("singularity", 0xEE62D4, () -> List.of(ModItems.ABYSS_SHARD.get())),
    RADIANT("radiant", 0xFFF07A, () -> List.of(ModItems.SOLARITE_INGOT.get(), ModItems.CHARGED_VOID_CRYSTAL.get()));

    public static final Codec<StarforgeUpgrade> CODEC = StringRepresentable.fromEnum(StarforgeUpgrade::values);
    public static final StreamCodec<ByteBuf, StarforgeUpgrade> STREAM_CODEC = ByteBufCodecs.idMapper(
        ByIdMap.continuous(StarforgeUpgrade::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), StarforgeUpgrade::ordinal);

    private final String name;
    private final int colour;
    private final Supplier<List<Item>> materials;

    StarforgeUpgrade(String name, int colour, Supplier<List<Item>> materials) {
        this.name = name;
        this.colour = colour;
        this.materials = materials;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int colour() {
        return this.colour;
    }

    // What goes in the Forge's material slots (one or two items, in either slot).
    public List<Item> materials() {
        return this.materials.get();
    }

    // Can this upgrade go on this item? (Not every upgrade fits every item, as with enchantments.)
    public boolean fits(ItemStack stack) {
        Kind kind = Kind.of(stack);
        return this.effect(stack, kind) != null;
    }

    // The one thing this upgrade does on this item ("rift_walker", "smelting"...), or null if it
    // doesn't fit. Used for the tooltip line and to decide what fits.
    public String effect(ItemStack stack, Kind kind) {
        if (kind.armour) {
            EquipmentSlot slot = Starforge.slotOf(stack);
            return switch (this) {
                case VOIDWALKER -> slot == EquipmentSlot.FEET ? "rift_walker" : slot == EquipmentSlot.CHEST ? "void_recall" : null;
                case METEORIC -> null;          // Heat Shield was cut (the distillation)
                case STARDUST -> slot == EquipmentSlot.FEET ? "light_step_boots" : "light_step";
                case TEMPORAL -> null;          // so was Time Stretch
                case SINGULARITY -> "lensing";
                case RADIANT -> "soak_light";
            };
        }
        return switch (this) {
            case VOIDWALKER -> null;
            case METEORIC -> kind.tool ? "smelting" : null;
            case STARDUST -> kind.ranged ? "flatter_shots" : null;
            case TEMPORAL -> kind.melee ? "echo_strike" : null;
            case SINGULARITY -> kind.tool ? "pull" : kind.sword ? "sweep_pull" : null;
            case RADIANT -> kind.melee || kind.ranged ? "glow" : null;
        };
    }

    public static StarforgeUpgrade byMaterials(ItemStack a, ItemStack b) {
        for (StarforgeUpgrade upgrade : values()) {
            List<Item> needed = upgrade.materials();
            if (needed.size() == 1) {
                if ((a.is(needed.get(0)) && b.isEmpty()) || (b.is(needed.get(0)) && a.isEmpty())) {
                    return upgrade;
                }
            } else if ((a.is(needed.get(0)) && b.is(needed.get(1))) || (a.is(needed.get(1)) && b.is(needed.get(0)))) {
                return upgrade;
            }
        }
        return null;
    }

    public static boolean isMaterial(ItemStack stack) {
        for (StarforgeUpgrade upgrade : values()) {
            for (Item item : upgrade.materials()) {
                if (stack.is(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    // What sort of thing an item is, as far as Starforging cares (from the astralend:starforgeable/*
    // item tags, so a data pack can add items). An axe is both a melee weapon and a tool.
    public record Kind(boolean armour, boolean melee, boolean ranged, boolean tool, boolean sword) {
        public static Kind of(ItemStack stack) {
            return new Kind(stack.is(Starforge.ARMOUR), stack.is(Starforge.MELEE), stack.is(Starforge.RANGED), stack.is(Starforge.TOOLS),
                stack.is(Starforge.MELEE) && stack.is(net.minecraft.tags.ItemTags.SWORDS));
        }

        public boolean any() {
            return this.armour || this.melee || this.ranged || this.tool;
        }
    }
}
