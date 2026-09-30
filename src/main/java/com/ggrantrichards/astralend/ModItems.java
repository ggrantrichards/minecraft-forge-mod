package com.ggrantrichards.astralend;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AstralEnd.MODID);

    // A plain item: /give @s astralend:ruby
    public static final RegistryObject<Item> RUBY = ITEMS.register("ruby",
        () -> new Item(new Item.Properties().setId(ITEMS.key("ruby")))
    );

    // Blocks need a matching BlockItem so they can exist in your inventory.
    public static final RegistryObject<Item> RUBY_BLOCK_ITEM = ITEMS.register("ruby_block",
        () -> new BlockItem(ModBlocks.RUBY_BLOCK.get(), new Item.Properties().setId(ITEMS.key("ruby_block")))
    );

    private ModItems() {}
}
