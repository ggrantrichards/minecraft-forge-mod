package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.MirrorWretch;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class MirrorWretchRenderer extends MobRenderer<MirrorWretch, MirrorWretchRenderer.State, MirrorWretchRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("mirror_wretch");

    public MirrorWretchRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.MIRROR_WRETCH)), 0.7F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("mirror_wretch")));
    }

    // Drawn a quarter as big again: reared up, it stands over two blocks tall.
    @Override
    protected void scale(State state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        poseStack.scale(1.25F, 1.25F, 1.25F);
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
    public void extractRenderState(MirrorWretch entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.attackTime = entity.getSwingAnimation(partialTicks);
    }

    public static class State extends LivingEntityRenderState {
        public float attackTime;
    }

    // The Choir: its crawling half ripples along the ground, the reared half sways and turns its mouth
    // toward what it's listening to, and the vocal sacs down its throat swell one after another.
    public static class Model extends EntityModel<State> {
        private final ModelPart chest;
        private final ModelPart head;
        private final ModelPart finL;
        private final ModelPart finR;
        private final ModelPart[] segments = new ModelPart[4];
        private final ModelPart[][] feet = new ModelPart[4][2];
        private final ModelPart[] sacs = new ModelPart[5];

        public Model(ModelPart root) {
            super(root);
            this.chest = root.getChild("chest");
            this.head = this.chest.getChild("head");
            this.finL = this.chest.getChild("fin_l");
            this.finR = this.chest.getChild("fin_r");
            for (int i = 0; i < 4; i++) {
                this.segments[i] = root.getChild("seg" + i);
                this.feet[i][0] = this.segments[i].getChild("seg" + i + "_foot_l");
                this.feet[i][1] = this.segments[i].getChild("seg" + i + "_foot_r");
            }
            for (int k = 0; k < 5; k++) {
                this.sacs[k] = this.chest.getChild("sac" + k);
            }
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float pos = state.walkAnimationPos * 0.8F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            for (int i = 0; i < 4; i++) {
                float wave = Mth.sin(pos - i * 1.1F) * speed;
                this.segments[i].y -= Math.max(0.0F, wave) * 1.2F;
                this.segments[i].x += Mth.sin(t * 0.08F - i * 0.9F) * 0.5F;
                this.feet[i][0].xRot += Mth.cos(pos - i * 1.1F) * 0.9F * speed;
                this.feet[i][1].xRot += Mth.cos(pos - i * 1.1F + Mth.PI) * 0.9F * speed;
            }
            this.chest.zRot += Mth.sin(t * 0.06F) * 0.08F;
            this.chest.xRot += Mth.sin(t * 0.045F) * 0.05F;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.7F;
            this.head.zRot += Mth.sin(t * 0.03F) * 0.2F;     // listening
            this.finL.yRot += Mth.sin(t * 0.1F) * 0.15F;
            this.finR.yRot -= Mth.sin(t * 0.1F) * 0.15F;
            for (int k = 0; k < 5; k++) {
                float swell = 1.0F + 0.18F * Math.max(0.0F, Mth.sin(t * 0.12F - k * 0.9F));
                this.sacs[k].xScale = swell;
                this.sacs[k].yScale = swell;
                this.sacs[k].zScale = swell;
            }
            if (state.attackTime > 0.0F) {
                float strike = Mth.sin(state.attackTime * Mth.PI);
                this.chest.xRot -= strike * 0.6F;              // it lunges
                this.head.xRot += strike * 0.4F;
            }
        }
    }
}
