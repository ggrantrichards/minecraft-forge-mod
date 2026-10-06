package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Hydragrip;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class HydragripRenderer extends MobRenderer<Hydragrip, HydragripRenderer.State, HydragripRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("hydragrip");

    public HydragripRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.HYDRAGRIP)), 0.5F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("hydragrip")));
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Hydragrip entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.swimming = entity.isInWater();
        state.attackTime = entity.getSwingAnimation(partialTicks);
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.55F, 0.55F, 0.55F);
        }
    }

    public static class State extends LivingEntityRenderState {
        public boolean swimming;
        public float attackTime;
    }

    // Trots on land, high on its legs, its long neck bobbing and its tail swaying behind; in the water
    // its legs fold back and its body and finned tail undulate. Its frills ripple all the while, and
    // its jaw snaps open when it bites.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart neck;
        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart tail;
        private final ModelPart tailMid;
        private final ModelPart tailTip;
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart[] frills = new ModelPart[6];

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.neck = this.body.getChild("neck");
            this.head = this.neck.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.tail = this.body.getChild("tail");
            this.tailMid = this.tail.getChild("tail_mid");
            this.tailTip = this.tailMid.getChild("tail_tip");
            String[] names = {"leg_fr", "leg_fl", "leg_br", "leg_bl"};
            for (int i = 0; i < 4; i++) {
                this.legs[i] = root.getChild(names[i]);
            }
            for (int k = 0; k < 3; k++) {
                this.frills[k] = this.head.getChild("frill_l" + k);
                this.frills[k + 3] = this.head.getChild("frill_r" + k);
            }
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float pos = state.walkAnimationPos * 0.9F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.neck.yRot += state.yRot * Mth.DEG_TO_RAD * 0.4F;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.4F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
            this.neck.xRot += Mth.sin(t * 0.05F) * 0.04F + Mth.sin(pos * 2.0F) * 0.08F * speed;   // breathing; a bob as it trots
            for (int k = 0; k < 6; k++) {
                this.frills[k].yRot += Mth.sin(t * 0.15F + k) * 0.18F * (k < 3 ? 1 : -1);
            }
            this.jaw.xRot += 0.05F + Math.max(0.0F, Mth.sin(t * 0.04F)) * 0.08F;      // breathing
            if (state.attackTime > 0.0F) {
                this.jaw.xRot += Mth.sin(state.attackTime * Mth.PI) * 0.9F;
            }
            if (state.swimming) {
                for (ModelPart leg : this.legs) {
                    leg.xRot += 1.2F;
                }
                this.body.yRot += Mth.sin(t * 0.3F) * 0.12F;
                this.tail.yRot += Mth.sin(t * 0.3F - 0.8F) * 0.4F;
                this.tailMid.yRot += Mth.sin(t * 0.3F - 1.6F) * 0.5F;
                this.tailTip.yRot += Mth.sin(t * 0.3F - 2.4F) * 0.6F;
            } else {
                this.legs[0].xRot += Mth.cos(pos) * 0.9F * speed;
                this.legs[1].xRot += Mth.cos(pos + Mth.PI) * 0.9F * speed;
                this.legs[2].xRot += Mth.cos(pos + Mth.PI) * 0.9F * speed;
                this.legs[3].xRot += Mth.cos(pos) * 0.9F * speed;
                this.body.yRot += Mth.sin(pos) * 0.04F * speed;
                this.tail.yRot += Mth.sin(t * 0.1F) * 0.12F + Mth.cos(pos) * 0.2F * speed;
                this.tailMid.yRot += Mth.sin(t * 0.1F - 0.5F) * 0.15F + Mth.cos(pos - 0.6F) * 0.2F * speed;
                this.tailTip.yRot += Mth.sin(t * 0.1F - 1.0F) * 0.2F;
            }
        }
    }
}
