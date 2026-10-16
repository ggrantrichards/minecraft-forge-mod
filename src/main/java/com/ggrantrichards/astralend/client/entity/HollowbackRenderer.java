package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Hollowback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// The Hollowback: the Hushback's own model (it was one), its head split into a hinged jaw full of fangs
// that opens only when its prey is close; the violet glow down its throat shows only then.
public class HollowbackRenderer extends com.ggrantrichards.astralend.client.compat.StateMobRenderer<Hollowback, HushbackRenderer.State, HushbackRenderer.Model> {
    private static final ResourceLocation TEXTURE = ModModelLayers.texture("hollowback");

    public HollowbackRenderer(EntityRendererProvider.Context context) {
        super(context, new HushbackRenderer.Model(context.bakeLayer(ModModelLayers.HOLLOWBACK)), 0.6F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("hollowback")));
    }

    @Override
    public ResourceLocation getTextureLocation(HushbackRenderer.State state) {
        return TEXTURE;
    }

    @Override
    public HushbackRenderer.State createRenderState() {
        return new HushbackRenderer.State();
    }

    @Override
    public void extractRenderState(Hollowback entity, HushbackRenderer.State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.snarl = entity.jawsOpen();
        state.bite = entity.getAttackAnim(partialTicks);
    }
}
