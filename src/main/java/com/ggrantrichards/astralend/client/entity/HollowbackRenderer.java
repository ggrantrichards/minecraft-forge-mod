package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Hollowback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

// The Hollowback: the Hushback's own model (it was one), plated in cracked horn, stitched, with purpur
// implants down its spine, and a dog's muzzle full of fangs that it bares when it hunts; the implants'
// lights, its one dull eye, its purpur lens and its throat glow.
public class HollowbackRenderer extends MobRenderer<Hollowback, HushbackRenderer.State, HushbackRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("hollowback");

    public HollowbackRenderer(EntityRendererProvider.Context context) {
        super(context, new HushbackRenderer.Model(context.bakeLayer(ModModelLayers.HOLLOWBACK)), 0.6F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("hollowback")));
    }

    @Override
    public Identifier getTextureLocation(HushbackRenderer.State state) {
        return TEXTURE;
    }

    @Override
    public HushbackRenderer.State createRenderState() {
        return new HushbackRenderer.State();
    }

    @Override
    public void extractRenderState(Hollowback entity, HushbackRenderer.State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.snarl = entity.isAggressive();
        state.bite = entity.getSwingAnimation(partialTicks);
    }
}
