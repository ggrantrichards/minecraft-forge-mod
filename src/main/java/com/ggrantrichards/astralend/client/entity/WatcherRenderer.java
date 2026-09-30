package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Watcher;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class WatcherRenderer extends MobRenderer<Watcher, LivingEntityRenderState, WatcherRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("watcher");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.WATCHER)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("watcher")));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(1.45F, 1.45F, 1.45F);   // a head taller than an Enderman
    }

    // It doesn't move. Only its hood turns, slowly, to follow you, and the ring behind its head
    // turns on its own.
    public static class Model extends EntityModel<LivingEntityRenderState> {
        private final ModelPart hood;
        private final ModelPart halo;

        public Model(ModelPart root) {
            super(root);
            this.hood = root.getChild("hood");
            this.halo = this.hood.getChild("halo");
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            this.hood.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.hood.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5F;
            this.halo.zRot += state.ageInTicks * 0.01F;
        }
    }
}
