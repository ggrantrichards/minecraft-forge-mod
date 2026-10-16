package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.client.model.MobMeshes;
import com.ggrantrichards.astralend.entity.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;

// Registers the mobs' models (their geometry, from MobMeshes) and renderers with the game.
public final class ModModelLayers {
    public static final ModelLayerLocation POLLINATOR = layer("pollinator");
    public static final ModelLayerLocation UMBRAL_STALKER = layer("umbral_stalker");
    public static final ModelLayerLocation DRIFT_WHALE = layer("drift_whale");
    public static final ModelLayerLocation WATCHER = layer("watcher");
    public static final ModelLayerLocation SPIRE_WYRM = layer("spire_wyrm");
    public static final ModelLayerLocation HUSHBACK = layer("hushback");
    public static final ModelLayerLocation HYDRAGRIP = layer("hydragrip");
    public static final ModelLayerLocation MIRROR_WRETCH = layer("mirror_wretch");
    public static final ModelLayerLocation HOLLOWBACK = layer("hollowback");
    public static final ModelLayerLocation CURATOR = layer("curator");
    public static final ModelLayerLocation SHARDBACK = layer("shardback");

    public static void register() {
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
            event.registerLayerDefinition(POLLINATOR, MobMeshes::pollinator);
            event.registerLayerDefinition(UMBRAL_STALKER, MobMeshes::umbralStalker);
            event.registerLayerDefinition(DRIFT_WHALE, MobMeshes::driftWhale);
            event.registerLayerDefinition(WATCHER, MobMeshes::watcher);
            event.registerLayerDefinition(SPIRE_WYRM, MobMeshes::spireWyrm);
            event.registerLayerDefinition(HUSHBACK, MobMeshes::hushback);
            event.registerLayerDefinition(HYDRAGRIP, MobMeshes::hydragrip);
            event.registerLayerDefinition(MIRROR_WRETCH, MobMeshes::mirrorWretch);
            event.registerLayerDefinition(HOLLOWBACK, MobMeshes::hollowback);
            event.registerLayerDefinition(CURATOR, MobMeshes::curator);
            event.registerLayerDefinition(SHARDBACK, MobMeshes::shardback);
        });
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerEntityRenderer(ModEntities.POLLINATOR.get(), PollinatorRenderer::new);
            event.registerEntityRenderer(ModEntities.UMBRAL_STALKER.get(), UmbralStalkerRenderer::new);
            event.registerEntityRenderer(ModEntities.DRIFT_WHALE.get(), DriftWhaleRenderer::new);
            event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
            event.registerEntityRenderer(ModEntities.SPIRE_WYRM.get(), SpireWyrmRenderer::new);
            event.registerEntityRenderer(ModEntities.HUSHBACK.get(), HushbackRenderer::new);
            event.registerEntityRenderer(ModEntities.HYDRAGRIP.get(), HydragripRenderer::new);
            event.registerEntityRenderer(ModEntities.MIRROR_WRETCH.get(), MirrorWretchRenderer::new);
            event.registerEntityRenderer(ModEntities.HOLLOWBACK.get(), HollowbackRenderer::new);
            event.registerEntityRenderer(ModEntities.CURATOR.get(), CuratorRenderer::new);
            event.registerEntityRenderer(ModEntities.SHARDBACK.get(), ShardbackRenderer::new);
        });
    }

    static ResourceLocation texture(String mob) {
        return ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "textures/entity/" + mob + "/" + mob + ".png");
    }

    static ResourceLocation glowTexture(String mob) {
        return ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "textures/entity/" + mob + "/" + mob + "_glow.png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, name), "main");
    }

    private ModModelLayers() {}
}
