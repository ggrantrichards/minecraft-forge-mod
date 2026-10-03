package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jspecify.annotations.Nullable;

// Starforging (Phase 8): the data an upgraded item carries, which items can be upgraded, and the
// tooltip line. What each upgrade *does* is in StarforgeEffects; the Astral Forge applies them.
public final class Starforge {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AstralEnd.MODID);

    // The upgrade on an item. Saved with it and sent to clients (the accent and tooltip need it).
    public static final RegistryObject<DataComponentType<StarforgeUpgrade>> UPGRADE = COMPONENTS.register("starforge_upgrade",
        () -> DataComponentType.<StarforgeUpgrade>builder().persistent(StarforgeUpgrade.CODEC).networkSynchronized(StarforgeUpgrade.STREAM_CODEC).build());
    // A Void Recall chestplate holding a Rift Pearl, ready to bring you back from the void.
    public static final RegistryObject<DataComponentType<Unit>> RIFT_PEARL = COMPONENTS.register("rift_pearl",
        () -> DataComponentType.<Unit>builder().persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC).build());

    public static final TagKey<Item> ARMOUR = tag("starforgeable/armor");
    public static final TagKey<Item> MELEE = tag("starforgeable/melee");
    public static final TagKey<Item> RANGED = tag("starforgeable/ranged");
    public static final TagKey<Item> TOOLS = tag("starforgeable/tools");

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AstralEnd.MODID, path));
    }

    public static void register() {
        ItemTooltipEvent.BUS.addListener(Starforge::onTooltip);
    }

    public static @Nullable StarforgeUpgrade get(ItemStack stack) {
        return stack.isEmpty() ? null : stack.get(UPGRADE.get());
    }

    public static boolean has(ItemStack stack, StarforgeUpgrade upgrade) {
        return get(stack) == upgrade;
    }

    public static EquipmentSlot slotOf(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null ? equippable.slot() : EquipmentSlot.MAINHAND;
    }

    // How many pieces of armour this entity wears with the upgrade (0-4).
    public static int pieces(LivingEntity entity, StarforgeUpgrade upgrade) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = entity.getItemBySlot(slot);
            if (has(worn, upgrade) && worn.is(ARMOUR)) {
                n++;
            }
        }
        return n;
    }

    public static boolean wearing(LivingEntity entity, EquipmentSlot slot, StarforgeUpgrade upgrade) {
        return has(entity.getItemBySlot(slot), upgrade);
    }

    public static boolean holding(LivingEntity entity, StarforgeUpgrade upgrade) {
        return has(entity.getMainHandItem(), upgrade);
    }

    // The item with an upgrade forged onto it. Any upgrade it had is replaced, along with the
    // attribute modifiers that came with it. (A Rift Pearl in a chestplate stays put, so it's there
    // again if Void Recall goes back on.)
    public static ItemStack apply(ItemStack base, StarforgeUpgrade upgrade) {
        ItemStack result = base.copyWithCount(1);
        result.set(UPGRADE.get(), upgrade);
        ItemAttributeModifiers current = result.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        List<ItemAttributeModifiers.Entry> kept = new ArrayList<>();
        for (ItemAttributeModifiers.Entry entry : current.modifiers()) {
            if (!entry.modifier().id().getNamespace().equals(AstralEnd.MODID)) {
                kept.add(entry);
            }
        }
        ItemAttributeModifiers modifiers = new ItemAttributeModifiers(kept);
        if (result.is(ARMOUR)) {
            EquipmentSlot slot = slotOf(result);
            EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(slot);
            // One id per slot, so pieces on different slots add up (like vanilla's armour.<slot>).
            Identifier id = Identifier.fromNamespaceAndPath(AstralEnd.MODID, "starforge." + slot.getName());
            if (upgrade == StarforgeUpgrade.RADIANT) {
                // Room for the half heart Soak Light gives.
                modifiers = modifiers.withModifierAdded(Attributes.MAX_ABSORPTION,
                    new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_VALUE), group);
            }
        }
        result.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
        return result;
    }

    // "Starforged:" and the upgrade's one line, in its colour, just under the name (where an armour
    // trim's lines go).
    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        StarforgeUpgrade upgrade = get(stack);
        if (upgrade == null) {
            return;
        }
        String effect = upgrade.effect(stack, StarforgeUpgrade.Kind.of(stack));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("upgrade.astralend.header").withStyle(ChatFormatting.GRAY));
        Component name = Component.translatable("upgrade.astralend." + upgrade.getSerializedName());
        lines.add(CommonComponents.space().append(effect == null ? name
            : Component.translatable("upgrade.astralend.line", name, Component.translatable("upgrade.astralend.effect." + effect)))
            .withColor(upgrade.colour()));
        if (upgrade == StarforgeUpgrade.VOID_RECALL && "void_recall".equals(effect)) {
            lines.add(CommonComponents.space().append(Component.translatable(stack.has(RIFT_PEARL.get())
                ? "upgrade.astralend.pearl_loaded" : "upgrade.astralend.pearl_empty")).withStyle(ChatFormatting.DARK_GRAY));
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private Starforge() {}
}
