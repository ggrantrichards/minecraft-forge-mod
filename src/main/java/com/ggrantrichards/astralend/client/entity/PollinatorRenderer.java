package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.EndPollinator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.ggrantrichards.astralend.client.compat.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PollinatorRenderer extends com.ggrantrichards.astralend.client.compat.StateMobRenderer<EndPollinator, LivingEntityRenderState, PollinatorRenderer.Model> {
    private static final ResourceLocation TEXTURE = ModModelLayers.texture("pollinator");

    public PollinatorRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.POLLINATOR)), 0.2F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("pollinator")));
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    protected int getBlockLightLevel(EndPollinator entity, BlockPos pos) {
        return Math.max(9, super.getBlockLightLevel(entity, pos));
    }

    // The bell pulses and bobs; the tendrils trail and sway.
    public static class Model extends com.ggrantrichards.astralend.client.compat.StateModel<LivingEntityRenderState> {
        private final ModelPart bell;
        private final ModelPart[] tendrils = new ModelPart[5];

        public Model(ModelPart root) {
            super(root);
            this.bell = root.getChild("bell");
            for (int i = 0; i < 5; i++) {
                this.tendrils[i] = this.bell.getChild("tendril_" + (i + 1));
            }
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            this.bell.y += Mth.sin(t * 0.12F) * 1.2F;
            float pulse = 1.0F + Mth.sin(t * 0.25F) * 0.06F;
            this.bell.xScale = pulse;
            this.bell.zScale = pulse;
            this.bell.yScale = 2.0F - pulse;
            for (int i = 0; i < 5; i++) {
                this.tendrils[i].xRot = Mth.sin(t * 0.15F + i * 1.3F) * 0.25F;
                this.tendrils[i].zRot = Mth.cos(t * 0.13F + i * 0.9F) * 0.2F;
            }
        }
    }
}
