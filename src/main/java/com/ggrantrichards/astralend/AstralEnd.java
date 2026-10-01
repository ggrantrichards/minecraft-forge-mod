package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.client.ClientScreenshots;
import com.ggrantrichards.astralend.client.SpectatorOverlayFix;
import com.ggrantrichards.astralend.client.entity.ModModelLayers;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.client.sky.AstralSky;
import com.ggrantrichards.astralend.dev.WorldgenSurvey;
import com.ggrantrichards.astralend.flare.FlareCommand;
import com.ggrantrichards.astralend.flare.FlareNetwork;
import com.ggrantrichards.astralend.flare.StellarFlares;
import com.ggrantrichards.astralend.world.GravityInversion;
import com.ggrantrichards.astralend.world.LowGravity;
import com.ggrantrichards.astralend.world.MobSpawning;
import com.ggrantrichards.astralend.world.ModWorldgen;
import com.ggrantrichards.astralend.world.WoodStripping;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        ModBlockEntities.BLOCK_ENTITIES.register(modBusGroup);
        ModEntities.ENTITY_TYPES.register(modBusGroup);
        ModEffects.EFFECTS.register(modBusGroup);
        ModEffects.POTIONS.register(modBusGroup);
        ModWorldgen.BIOME_SOURCES.register(modBusGroup);
        ModWorldgen.FEATURE_TYPES.register(modBusGroup);
        ModWorldgen.STRUCTURE_TYPES.register(modBusGroup);
        com.ggrantrichards.astralend.starforge.Starforge.COMPONENTS.register(modBusGroup);   // Phase 8: Starforging
        com.ggrantrichards.astralend.starforge.ModMenus.MENUS.register(modBusGroup);
        com.ggrantrichards.astralend.starforge.StarforgeLoot.SERIALIZERS.register(modBusGroup);

        LowGravity.register();
        GravityInversion.register();
        WoodStripping.register();
        ModEntities.register();   // mob attributes and where they may spawn
        MobSpawning.register();   // Drift Whales, the Watcher and Black Hole Guardians
        FlareNetwork.register();  // Phase 5: Stellar Flares - the timer, its effects, and syncing it to clients
        StellarFlares.register();
        com.ggrantrichards.astralend.maw.MawNetwork.register();
        com.ggrantrichards.astralend.maze.MazeScoping.register();
        com.ggrantrichards.astralend.comet.CometShowers.register();
        com.ggrantrichards.astralend.starforge.Starforge.register();          // upgrade tooltips
        com.ggrantrichards.astralend.starforge.StarforgeEffects.register();   // what the upgrades do
        FlareCommand.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModModelLayers.register();       // mob models and renderers
            SpectatorOverlayFix.register();  // client-only code; the server never loads it
            ClientScreenshots.register();    // does nothing unless started with -Dastralend.screenshots=true
            AstralSky.register();            // the End's sky: stars, auroras, the Remnant, flares
            com.ggrantrichards.astralend.client.starforge.StarforgeClient.register(modBusGroup);  // the Forge's screen, armour accents
        }
        WorldgenSurvey.register(); // does nothing unless started with -Dastralend.survey=true

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Look for this line in the console/log to confirm the mod loaded.
        LOGGER.info("The Astral End loaded.");
    }
}
