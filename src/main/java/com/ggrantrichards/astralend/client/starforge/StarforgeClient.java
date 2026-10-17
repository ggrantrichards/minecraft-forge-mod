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

// Client side of Starforging: the Astral Forge's screen, and the glow on worn armour.
public final class StarforgeClient {
    // The glow's armour models: a hair larger than the armour's (0.5 and 1.0), so they sit over it, and
    // one shell half a pixel further out for a thin halo round it.
    private static final float[] SPREAD = {0.03F, 0.5F};

    private static ModelLayerLocation layer(String part, int shell) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, shell == 0 ? "starforge_accent" : "starforge_glow_" + shell), part);
    }

    public static void register(IEventBus modBusGroup) {
        com.ggrantrichards.astralend.Events.on(FMLClientSetupEvent.class, event ->
            event.enqueueWork(() -> MenuScreens.register(ModMenus.ASTRAL_FORGE.get(), AstralForgeScreen::new)));
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
            for (int shell = 0; shell < SPREAD.length; shell++) {
                float spread = SPREAD[shell];
                event.registerLayerDefinition(layer("inner", shell), () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.5F + spread), 0.0F), 64, 32));
                event.registerLayerDefinition(layer("outer", shell), () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F + spread), 0.0F), 64, 32));
            }
        });
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.AddLayers.class, event -> {
            for (PlayerSkin.Model skin : event.getSkins()) {
                if (event.getPlayerSkin(skin) instanceof LivingEntityRenderer<?, ?> living && living.getModel() instanceof PlayerModel<?>) {
                    @SuppressWarnings("unchecked")
                    LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderer =
                        (LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>) living;
                    java.util.List<HumanoidModel<AbstractClientPlayer>> inner = new java.util.ArrayList<>(), outer = new java.util.ArrayList<>();
                    for (int shell = 0; shell < SPREAD.length; shell++) {
                        inner.add(new HumanoidModel<>(event.getEntityModels().bakeLayer(layer("inner", shell))));
                        outer.add(new HumanoidModel<>(event.getEntityModels().bakeLayer(layer("outer", shell))));
                    }
                    renderer.addLayer(new StarforgeArmorLayer(renderer, inner, outer));
                }
            }
        });
    }

    private StarforgeClient() {}
}
