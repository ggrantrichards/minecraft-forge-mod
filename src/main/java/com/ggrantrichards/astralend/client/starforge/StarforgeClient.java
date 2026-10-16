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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

// Client side of Starforging: the Astral Forge's screen, and the upgrade accents on worn armour.
public final class StarforgeClient {
    public static void register(IEventBus modBusGroup) {
        com.ggrantrichards.astralend.Events.on(FMLClientSetupEvent.class, event ->
            event.enqueueWork(() -> MenuScreens.register(ModMenus.ASTRAL_FORGE.get(), AstralForgeScreen::new)));
        com.ggrantrichards.astralend.Events.on(EntityRenderersEvent.AddLayers.class, event -> {
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
