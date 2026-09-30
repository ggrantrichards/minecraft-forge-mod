package com.ggrantrichards.astralend;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// This is the entry point of the mod. Forge finds this class because of the @Mod annotation.
// The value MUST match modId in src/main/resources/META-INF/mods.toml
@Mod(AstralEnd.MODID)
public final class AstralEnd {
    public static final String MODID = "astralend";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Forge calls this constructor once, while the game is loading.
    public AstralEnd(FMLJavaModLoadingContext context) {
        var modBusGroup = context.getModBusGroup();

        // 1. Register our blocks and items. The order doesn't matter; Forge handles it.
        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);

        // 2. Run some code once setup is finished.
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        // 3. Put our items into existing creative-mode tabs.
        BuildCreativeModeTabContentsEvent.BUS.addListener(AstralEnd::addToCreativeTabs);

        // 4. React to something happening in the game: greet players when they join a world.
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(AstralEnd::onPlayerJoin);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Look for this line in the console/log to confirm the mod loaded.
        LOGGER.info("The Astral End loaded.");
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
            event.accept(ModItems.RUBY);
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(ModItems.RUBY_BLOCK_ITEM);
    }

    private static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().sendSystemMessage(Component.literal("Welcome! Go find some rubies."));
    }
}
