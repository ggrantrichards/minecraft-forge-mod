package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.starforge.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// Client side of Starforging: the Astral Forge's screen, and the upgrade accents on worn armour.
public final class StarforgeClient {
    // The accents' armour models: a hair larger than the armour's (0.5 and 1.0), so they sit over it.
    private static final ModelLayerLocation INNER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "starforge_accent"), "inner");
    private static final ModelLayerLocation OUTER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "starforge_accent"), "outer");

    public static void register(IEventBus modBusGroup) {
        com.ggrantrichards.astralend.Events.on(FMLClientSetupEvent.class, event ->
            event.enqueueWork(() -> MenuScreens.register(ModMenus.ASTRAL_FORGE.get(), AstralForgeScreen::new)));
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
            event.registerLayerDefinition(INNER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.53F), 0.0F), 64, 32));
            event.registerLayerDefinition(OUTER, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.03F), 0.0F), 64, 32));
        });
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.AddLayers.class, event -> {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getSkin(skin.id()) instanceof LivingEntityRenderer<?, ?> living && living.getModel() instanceof PlayerModel<?>) {
                    @SuppressWarnings("unchecked")
                    LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer =
                        (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) living;
                    renderer.addLayer(new StarforgeArmorLayer(renderer,
                        new HumanoidModel<>(event.getEntityModels().bakeLayer(INNER)), new HumanoidModel<>(event.getEntityModels().bakeLayer(OUTER))));
                }
            }
        });
    }

    private StarforgeClient() {}
}
