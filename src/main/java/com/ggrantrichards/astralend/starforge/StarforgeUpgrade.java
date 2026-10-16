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

// The Starforge's five upgrades (the distillation; docs/DISTILLATION.md, "Starforging"). One per
// item, all sidegrades: none is simply better, none a new tier. Each answers to a constellation, which
// you must first sight through a spyglass from the Eclipse, and needs one rare material from
// somewhere else - so every upgrade is a journey. Each leaves a colour accent on the item icon (see
// tools/textures/starforge_textures.py, which uses the same colours), and worn armour glows in it.
public enum StarforgeUpgrade implements StringRepresentable {
    VOID_RECALL("void_recall", 0xA98CFF, "leviathan", () -> List.of(ModItems.CURATORS_CORE.get())),
    ECHO_STRIKE("echo_strike", 0xEEA650, "maw", () -> List.of(ModItems.RIFT_PEARL.get())),
    LENSING("lensing", 0xEE62D4, "watcher", () -> List.of(ModItems.CRACKED_LENS.get())),
    RADIANT("radiant", 0xFFF07A, "remnant", () -> List.of(ModItems.BOTTLED_FLARE.get())),
    RIFT_WALKER("rift_walker", 0x7AD8FF, "whale", () -> List.of(ModBlocks.WHALELIGHT_SCALE.get().asItem()));

    public static final Codec<StarforgeUpgrade> CODEC = StringRepresentable.fromEnum(StarforgeUpgrade::values);
    public static final StreamCodec<ByteBuf, StarforgeUpgrade> STREAM_CODEC = ByteBufCodecs.idMapper(
        ByIdMap.continuous(StarforgeUpgrade::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), StarforgeUpgrade::ordinal);

    private final String name;
    private final int colour;
    private final String constellation;
    private final Supplier<List<Item>> materials;

    StarforgeUpgrade(String name, int colour, String constellation, Supplier<List<Item>> materials) {
        this.name = name;
        this.colour = colour;
        this.constellation = constellation;
        this.materials = materials;
    }

    // The constellation that must have been sighted from the Eclipse (maze.Constellations).
    public String constellation() {
        return this.constellation;
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

    // The one thing this upgrade does on this item, or null if it doesn't fit. Used for the tooltip
    // line and to decide what fits.
    public String effect(ItemStack stack, Kind kind) {
        if (kind.armour) {
            EquipmentSlot slot = Starforge.slotOf(stack);
            return switch (this) {
                case VOID_RECALL -> slot == EquipmentSlot.CHEST ? "void_recall" : null;
                case RIFT_WALKER -> slot == EquipmentSlot.FEET ? "rift_walker" : null;
                case LENSING -> slot == EquipmentSlot.HEAD || slot == EquipmentSlot.LEGS ? "lensing" : null;
                case RADIANT -> "soak_light";
                case ECHO_STRIKE -> null;
            };
        }
        return switch (this) {
            case ECHO_STRIKE -> kind.melee ? "echo_strike" : null;
            case RADIANT -> kind.melee || kind.ranged ? "glow" : null;
            default -> null;
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
