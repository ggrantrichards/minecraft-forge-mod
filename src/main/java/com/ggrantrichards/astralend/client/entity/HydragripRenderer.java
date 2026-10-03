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
        super(context, new Model(context.bakeLayer(ModModelLayers.HYDRAGRIP)), 0.35F);
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
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.55F, 0.55F, 0.55F);
        }
    }

    public static class State extends LivingEntityRenderState {
        public boolean swimming;
    }

    // Scurries on land; in the water its legs fold back and its body and tail undulate.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart tail;
        private final ModelPart tailTip;
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.tail = this.body.getChild("tail");
            this.tailTip = this.tail.getChild("tail_tip");
            String[] names = {"leg_fr", "leg_fl", "leg_br", "leg_bl"};
            for (int i = 0; i < 4; i++) {
                this.legs[i] = root.getChild(names[i]);
            }
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float pos = state.walkAnimationPos * 0.9F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.6F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
            if (state.swimming) {
                for (ModelPart leg : this.legs) {
                    leg.xRot += 1.2F;
                }
                this.body.yRot += Mth.sin(t * 0.3F) * 0.12F;
                this.tail.yRot += Mth.sin(t * 0.3F - 0.8F) * 0.5F;
                this.tailTip.yRot += Mth.sin(t * 0.3F - 1.6F) * 0.6F;
            } else {
                this.legs[0].xRot += Mth.cos(pos) * 1.1F * speed;
                this.legs[1].xRot += Mth.cos(pos + Mth.PI) * 1.1F * speed;
                this.legs[2].xRot += Mth.cos(pos + Mth.PI) * 1.1F * speed;
                this.legs[3].xRot += Mth.cos(pos) * 1.1F * speed;
                this.tail.yRot += Mth.sin(t * 0.1F) * 0.15F + Mth.cos(pos) * 0.3F * speed;
                this.tailTip.yRot += Mth.sin(t * 0.1F - 0.5F) * 0.2F;
            }
        }
    }
}
