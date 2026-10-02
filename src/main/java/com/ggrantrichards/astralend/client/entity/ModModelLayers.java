package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.client.model.MobMeshes;
import com.ggrantrichards.astralend.entity.ModEntities;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.EntityRenderersEvent;

// Registers the mobs' models (their geometry, from MobMeshes) and renderers with the game.
public final class ModModelLayers {
    public static final ModelLayerLocation POLLINATOR = layer("pollinator");
    public static final ModelLayerLocation VOID_WANDERER = layer("void_wanderer");
    public static final ModelLayerLocation UMBRAL_STALKER = layer("umbral_stalker");
    public static final ModelLayerLocation CINDER_MITE = layer("cinder_mite");
    public static final ModelLayerLocation VOID_RAY = layer("void_ray");
    public static final ModelLayerLocation HOLLOW = layer("hollow");
    public static final ModelLayerLocation DRIFT_WHALE = layer("drift_whale");
    public static final ModelLayerLocation WATCHER = layer("watcher");
    public static final ModelLayerLocation LIVE_COMET = layer("live_comet");
    public static final ModelLayerLocation SPIRE_WYRM = layer("spire_wyrm");

    public static void register() {
        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event -> {
            event.registerLayerDefinition(POLLINATOR, MobMeshes::pollinator);
            event.registerLayerDefinition(VOID_WANDERER, MobMeshes::voidWanderer);
            event.registerLayerDefinition(UMBRAL_STALKER, MobMeshes::umbralStalker);
            event.registerLayerDefinition(CINDER_MITE, MobMeshes::cinderMite);
            event.registerLayerDefinition(VOID_RAY, MobMeshes::voidRay);
            event.registerLayerDefinition(HOLLOW, MobMeshes::hollow);
            event.registerLayerDefinition(DRIFT_WHALE, MobMeshes::driftWhale);
            event.registerLayerDefinition(WATCHER, MobMeshes::watcher);
            event.registerLayerDefinition(LIVE_COMET, MobMeshes::liveComet);
            event.registerLayerDefinition(SPIRE_WYRM, MobMeshes::spireWyrm);
        });
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerEntityRenderer(ModEntities.POLLINATOR.get(), PollinatorRenderer::new);
            event.registerEntityRenderer(ModEntities.VOID_WANDERER.get(), VoidWandererRenderer::new);
            event.registerEntityRenderer(ModEntities.UMBRAL_STALKER.get(), UmbralStalkerRenderer::new);
            event.registerEntityRenderer(ModEntities.CINDER_MITE.get(), CinderMiteRenderer::new);
            event.registerEntityRenderer(ModEntities.VOID_RAY.get(), VoidRayRenderer::new);
            event.registerEntityRenderer(ModEntities.HOLLOW.get(), HollowRenderer::new);
            event.registerEntityRenderer(ModEntities.DRIFT_WHALE.get(), DriftWhaleRenderer::new);
            event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
            event.registerEntityRenderer(ModEntities.LIVE_COMET.get(), LiveCometRenderer::new);
            event.registerEntityRenderer(ModEntities.SPIRE_WYRM.get(), SpireWyrmRenderer::new);
        });
    }

    static Identifier texture(String mob) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, "textures/entity/" + mob + "/" + mob + ".png");
    }

    static Identifier glowTexture(String mob) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, "textures/entity/" + mob + "/" + mob + "_glow.png");
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(AstralEnd.MODID, name), "main");
    }

    private ModModelLayers() {}
}
