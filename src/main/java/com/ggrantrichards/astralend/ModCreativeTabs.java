package com.ggrantrichards.astralend;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// "The Astral End" tab in the creative inventory, holding everything the mod adds.
//
// It isn't a hand-written list: it takes every block (in the order ModBlocks declares them, so
// each stone's stairs, slab and wall sit next to it), then every other item (ingots, food, and
// spawn eggs once mobs arrive). Anything new shows up here automatically.
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralEnd.MODID);

    public static final RegistryObject<CreativeModeTab> ASTRAL_END = TABS.register("astral_end", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.astralend"))
        .icon(() -> new ItemStack(ModItems.VOID_CRYSTAL.get()))
        .displayItems((parameters, output) -> allItems().forEach(output::accept))
        .build());

    // Every item of the mod, blocks first, without duplicates.
    public static Set<Item> allItems() {
        Set<Item> items = new LinkedHashSet<>();
        for (var block : ModBlocks.BLOCKS.getEntries()) {
            Item item = block.get().asItem();
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        for (var item : ModItems.ITEMS.getEntries()) {
            items.add(item.get());
        }
        return items;
    }

    private ModCreativeTabs() {}
}
