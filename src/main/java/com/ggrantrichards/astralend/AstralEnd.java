package com.ggrantrichards.astralend;

import com.mojang.logging.LogUtils;
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

        // Register everything the mod adds. The order doesn't matter; Forge handles it.
        ModBlocks.BLOCKS.register(modBusGroup);
        ModItems.ITEMS.register(modBusGroup);
        ModCreativeTabs.TABS.register(modBusGroup);

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Look for this line in the console/log to confirm the mod loaded.
        LOGGER.info("The Astral End loaded.");
    }
}
