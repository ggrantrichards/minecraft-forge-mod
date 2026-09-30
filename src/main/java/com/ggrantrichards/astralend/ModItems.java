package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.item.GlowingSporesItem;
import com.ggrantrichards.astralend.item.StarChartItem;
import java.util.function.Function;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
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
    public static final RegistryObject<Item> RAW_SOLARITE = simple("raw_solarite");
    public static final RegistryObject<Item> SOLARITE_INGOT = simple("solarite_ingot");

    // Comet Cress is both a food and the "seed" you plant under Stardust, like a carrot.
    public static final RegistryObject<Item> COMET_CRESS = ITEMS.register("comet_cress",
        () -> new BlockItem(ModBlocks.COMET_CRESS.get(), new Item.Properties()
            .setId(ITEMS.key("comet_cress"))
            .useItemDescriptionPrefix()
            .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));

    // ---- Phase 4: what the mobs leave behind
    public static final RegistryObject<Item> GLOWING_SPORES = register("glowing_spores", GlowingSporesItem::new, new Item.Properties());
    public static final RegistryObject<Item> DATA_SHARD = simple("data_shard");
    public static final RegistryObject<Item> CRACKED_LENS = simple("cracked_lens");
    public static final RegistryObject<Item> SINGULARITY_SHARD = simple("singularity_shard");
    public static final RegistryObject<Item> UMBRAL_MEMBRANE = simple("umbral_membrane");
    public static final RegistryObject<Item> SKIMMER_FILM = simple("skimmer_film");
    public static final RegistryObject<Item> CINDER = simple("cinder");
    public static final RegistryObject<Item> STAR_CHART = register("star_chart", StarChartItem::new, new Item.Properties().stacksTo(1));

    // ---- spawn eggs
    public static final RegistryObject<Item> POLLINATOR_SPAWN_EGG = spawnEgg("pollinator", ModEntities.POLLINATOR);
    public static final RegistryObject<Item> VOID_WANDERER_SPAWN_EGG = spawnEgg("void_wanderer", ModEntities.VOID_WANDERER);
    public static final RegistryObject<Item> UMBRAL_STALKER_SPAWN_EGG = spawnEgg("umbral_stalker", ModEntities.UMBRAL_STALKER);
    public static final RegistryObject<Item> CINDER_MITE_SPAWN_EGG = spawnEgg("cinder_mite", ModEntities.CINDER_MITE);
    public static final RegistryObject<Item> CORONA_SKIMMER_SPAWN_EGG = spawnEgg("corona_skimmer", ModEntities.CORONA_SKIMMER);
    public static final RegistryObject<Item> BLACK_HOLE_GUARDIAN_SPAWN_EGG = spawnEgg("black_hole_guardian", ModEntities.BLACK_HOLE_GUARDIAN);
    public static final RegistryObject<Item> DRIFT_WHALE_SPAWN_EGG = spawnEgg("drift_whale", ModEntities.DRIFT_WHALE);
    public static final RegistryObject<Item> WATCHER_SPAWN_EGG = spawnEgg("watcher", ModEntities.WATCHER);

    private static RegistryObject<Item> register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        return ITEMS.register(name, () -> factory.apply(properties.setId(ITEMS.key(name))));
    }

    private static RegistryObject<Item> spawnEgg(String mob, RegistryObject<? extends EntityType<?>> type) {
        String name = mob + "_spawn_egg";
        return ITEMS.register(name, () -> new SpawnEggItem(new Item.Properties().setId(ITEMS.key(name)).spawnEgg(type.get())));
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().setId(ITEMS.key(name))));
    }

    private ModItems() {}
}
