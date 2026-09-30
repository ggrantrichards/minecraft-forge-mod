package com.ggrantrichards.astralend;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Items that aren't just a placeable block. (Block items are registered in ModBlocks.)
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, AstralEnd.MODID);

    public static final RegistryObject<Item> VOID_CRYSTAL = simple("void_crystal");
    public static final RegistryObject<Item> RAW_METEORIC_IRON = simple("raw_meteoric_iron");
    public static final RegistryObject<Item> METEORIC_IRON_INGOT = simple("meteoric_iron_ingot");
    public static final RegistryObject<Item> METEORIC_IRON_NUGGET = simple("meteoric_iron_nugget");

    // Comet Cress is both a food and the "seed" you plant under Stardust, like a carrot.
    public static final RegistryObject<Item> COMET_CRESS = ITEMS.register("comet_cress",
        () -> new BlockItem(ModBlocks.COMET_CRESS.get(), new Item.Properties()
            .setId(ITEMS.key("comet_cress"))
            .useItemDescriptionPrefix()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().setId(ITEMS.key(name))));
    }

    private ModItems() {}
}
