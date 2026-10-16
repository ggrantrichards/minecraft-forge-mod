package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.starforge.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// Client side of Starforging: the Astral Forge's screen, and the glow on worn armour.
public final class StarforgeClient {
    public static void register(BusGroup modBusGroup) {
        FMLClientSetupEvent.getBus(modBusGroup).addListener(event ->
            event.enqueueWork(() -> MenuScreens.register(ModMenus.ASTRAL_FORGE.get(), AstralForgeScreen::new)));
        EntityRenderersEvent.AddLayers.BUS.addListener(event -> {
            for (PlayerModelType type : event.getModelTypes()) {
                AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(type);
                if (renderer != null) {
                    addTo(renderer, type == PlayerModelType.SLIM, event.getContext());
                }
            }
        });
    }

    private static void addTo(AvatarRenderer<AbstractClientPlayer> renderer, boolean slim, net.minecraft.client.renderer.entity.EntityRendererProvider.Context context) {
        ArmorModelSet<HumanoidModel<AvatarRenderState>> models = ArmorModelSet.bake(slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR,
            context.getModelSet(), HumanoidModel::new);
        renderer.addLayer(new StarforgeArmorLayer<>(renderer, models));
    }

    private StarforgeClient() {}
}
