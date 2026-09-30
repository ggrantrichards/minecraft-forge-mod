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
        poseStack.scale(1.2F, 1.2F, 1.2F);   // nearly three blocks tall
    }

    // It hangs in the air, rising and falling a little. Its head turns to keep the eye on you, the
    // hands drift on their own, and the four shards circle it slowly.
    public static class Model extends EntityModel<LivingEntityRenderState> {
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart rightHand;
        private final ModelPart leftHand;
        private final ModelPart shards;

        public Model(ModelPart root) {
            super(root);
            this.head = root.getChild("head");
            this.body = root.getChild("body");
            this.rightHand = root.getChild("right_hand");
            this.leftHand = root.getChild("left_hand");
            this.shards = root.getChild("shards");
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float bob = Mth.sin(t * 0.07F) * 1.2F;
            this.head.y += bob - 0.4F + Mth.sin(t * 0.07F - 0.6F) * 0.4F;   // the head lags a little
            this.body.y += bob;
            this.rightHand.y += bob + Mth.sin(t * 0.09F + 1.0F) * 1.0F;
            this.leftHand.y += bob + Mth.sin(t * 0.09F + 2.6F) * 1.0F;
            this.rightHand.zRot += Mth.sin(t * 0.05F) * 0.06F;
            this.leftHand.zRot -= Mth.sin(t * 0.05F) * 0.06F;
            this.shards.y += bob;
            this.shards.yRot += t * 0.04F;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.7F;
        }
    }
}
