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
    public static final RegistryObject<Item> CHART_FRAGMENT = simple("chart_fragment");
    public static final RegistryObject<Item> CRACKED_LENS = simple("cracked_lens");
    public static final RegistryObject<Item> ABYSS_SHARD = simple("abyss_shard");
    public static final RegistryObject<Item> UMBRAL_MEMBRANE = simple("umbral_membrane");
    public static final RegistryObject<Item> RAY_FILM = simple("ray_film");
    // Phase 5: Sunstorms
    public static final RegistryObject<Item> FLARE_POLLEN = simple("flare_pollen");
    public static final RegistryObject<Item> CHARGED_VOID_CRYSTAL = register("charged_void_crystal", Item::new,
        new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    public static final RegistryObject<Item> FLARE_BOTTLE = register("flare_bottle", Item::new, new Item.Properties().stacksTo(16));
    // The Curator's Core: what's left of the Curator when it falls (the distillation; Void Recall's material).
    public static final RegistryObject<Item> CURATORS_CORE = register("curators_core", Item::new,
        new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant());
    public static final RegistryObject<Item> BOTTLED_FLARE = register("bottled_flare", Item::new,
        new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    // Phase 10: steers a Drift Whale.
    // Phase 10: caught drifting up through an Upwell; brews the Potion of Upwelling.
    public static final RegistryObject<Item> UPWELL_MOTE = simple("upwell_mote");
    // Dropped by the Spire Wyrm; opens the Resonance Vault at the top of its spire.
    public static final RegistryObject<Item> RESONANT_KEY = register("resonant_key", Item::new,
        new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE));
    // Phase 10: the Resonance Vault's treasures - two banner patterns and an armour trim.
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.entity.BannerPattern> WYRM_PATTERN = patternTag("wyrm");
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.entity.BannerPattern> SPIRE_PATTERN = patternTag("spire");
    public static final RegistryObject<Item> WYRM_BANNER_PATTERN = register("wyrm_banner_pattern", Item::new,
        new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
            .delayedComponent(net.minecraft.core.component.DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(WYRM_PATTERN)));
    public static final RegistryObject<Item> SPIRE_BANNER_PATTERN = register("spire_banner_pattern", Item::new,
        new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
            .delayedComponent(net.minecraft.core.component.DataComponents.PROVIDES_BANNER_PATTERNS, context -> context.getOrThrow(SPIRE_PATTERN)));
    public static final RegistryObject<Item> ECLIPSE_ARMOR_TRIM_SMITHING_TEMPLATE = register("eclipse_armor_trim_smithing_template",
        net.minecraft.world.item.SmithingTemplateItem::createArmorTrimTemplate, new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    // The Resonance Vault's music disc: "Observing the Star" by madeso (CC0).
    public static final RegistryObject<Item> MUSIC_DISC_OBSERVING_THE_STAR = register("music_disc_observing_the_star", Item::new,
        new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
            .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(AstralEnd.MODID, "observing_the_star"))));
    public static final RegistryObject<Item> CHORUS_FLOWER_ON_A_STICK = register("chorus_flower_on_a_stick",
        com.ggrantrichards.astralend.item.ChorusFlowerOnAStickItem::new, new Item.Properties().durability(30));
    public static final RegistryObject<Item> STAR_CHART = register("star_chart", StarChartItem::new, new Item.Properties().stacksTo(1));
    // Phase 6: structures. Temporal Crystals are loot only: they can't be mined or farmed.
    // The key to the Rift Maws: wakes a Maw (with its eyes lit), or attunes you to a woken one.
    public static final RegistryObject<Item> PORTAL_LENS = register("portal_lens", Item::new,
        new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    // Phase 7: caught from a live comet's tail.
    public static final RegistryObject<Item> TEMPORAL_CRYSTAL = register("temporal_crystal", Item::new,
        new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE));

    // Phase 8: Starforging.
    public static final RegistryObject<Item> STARFORGE_TEMPLATE = register("starforge_template",
        com.ggrantrichards.astralend.starforge.StarforgeTemplateItem::new, new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    public static final RegistryObject<Item> STARFORGE_ALLOY = register("starforge_alloy", Item::new,
        new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    public static final RegistryObject<Item> RIFT_PEARL = register("rift_pearl", com.ggrantrichards.astralend.starforge.RiftPearlItem::new,
        new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC));

    // Memory Moss: placed, it's moss; eaten, it brings on Remembering (a minute and a half).
    public static final RegistryObject<Item> MEMORY_MOSS = ITEMS.register("memory_moss",
        () -> new BlockItem(ModBlocks.MEMORY_MOSS.get(), new Item.Properties()
            .setId(ITEMS.key("memory_moss"))
            .useBlockDescriptionPrefix()
            .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).alwaysEdible().build(),
                net.minecraft.world.item.component.Consumables.defaultFood().onConsume(
                    new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(
                        new net.minecraft.world.effect.MobEffectInstance(ModEffects.remembering(), 1800))).build())));

    // The distillation: the Hushback's fleece, and its meat.
    public static final RegistryObject<Item> HUSHFLEECE = simple("hushfleece");
    public static final RegistryObject<Item> HUSHBACK_FLANK = register("hushback_flank", Item::new, new Item.Properties()
        .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
    public static final RegistryObject<Item> COOKED_HUSHBACK_FLANK = register("cooked_hushback_flank", Item::new, new Item.Properties()
        .food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build()));

    // Phase 9: a stew of Lumineshroom and Comet Cress that lets you see in the dark for a while, and a
    // compass that points the way home to the End's central island (like a lodestone compass).
    public static final RegistryObject<Item> GLOWING_STEW = register("glowing_stew", Item::new, new Item.Properties().stacksTo(1)
        .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build(),
            net.minecraft.world.item.component.Consumables.defaultFood().onConsume(
                new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(
                    new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 600))).build())
        .usingConvertsTo(net.minecraft.world.item.Items.BOWL));
    public static final RegistryObject<Item> STAR_COMPASS = register("star_compass", com.ggrantrichards.astralend.item.StarCompassItem::new,
        new Item.Properties().stacksTo(1).component(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER,
            new net.minecraft.world.item.component.LodestoneTracker(java.util.Optional.of(net.minecraft.core.GlobalPos.of(
                net.minecraft.world.level.Level.END, net.minecraft.core.BlockPos.ZERO)), false)));

    // ---- spawn eggs
    public static final RegistryObject<Item> POLLINATOR_SPAWN_EGG = spawnEgg("pollinator", ModEntities.POLLINATOR);
    public static final RegistryObject<Item> VOID_WANDERER_SPAWN_EGG = spawnEgg("void_wanderer", ModEntities.VOID_WANDERER);
    public static final RegistryObject<Item> UMBRAL_STALKER_SPAWN_EGG = spawnEgg("umbral_stalker", ModEntities.UMBRAL_STALKER);
    public static final RegistryObject<Item> VOID_RAY_SPAWN_EGG = spawnEgg("void_ray", ModEntities.VOID_RAY);
    public static final RegistryObject<Item> DRIFT_WHALE_SPAWN_EGG = spawnEgg("drift_whale", ModEntities.DRIFT_WHALE);
    public static final RegistryObject<Item> WATCHER_SPAWN_EGG = spawnEgg("watcher", ModEntities.WATCHER);
    public static final RegistryObject<Item> SPIRE_WYRM_SPAWN_EGG = spawnEgg("spire_wyrm", ModEntities.SPIRE_WYRM);
    public static final RegistryObject<Item> HUSHBACK_SPAWN_EGG = spawnEgg("hushback", ModEntities.HUSHBACK);
    public static final RegistryObject<Item> HYDRAGRIP_SPAWN_EGG = spawnEgg("hydragrip", ModEntities.HYDRAGRIP);
    public static final RegistryObject<Item> MIRROR_WRETCH_SPAWN_EGG = spawnEgg("mirror_wretch", ModEntities.MIRROR_WRETCH);

    private static net.minecraft.tags.TagKey<net.minecraft.world.level.block.entity.BannerPattern> patternTag(String name) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BANNER_PATTERN,
            net.minecraft.resources.Identifier.fromNamespaceAndPath(AstralEnd.MODID, "pattern_item/" + name));
    }

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
