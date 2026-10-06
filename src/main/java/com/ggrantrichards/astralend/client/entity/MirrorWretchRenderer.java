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
        state.rear = entity.rear(partialTicks);
    }

    public static class State extends LivingEntityRenderState {
        public float attackTime;
        public float rear;
    }

    // The Choir: it slithers, low, its crawling half rippling along the ground and its head carried a
    // little way up; when it hunts it rears - the chest swings up from the segments, the front legs
    // paw the air, the neck and head rise over two blocks - and it strikes down from there. Its back
    // legs never leave the ground. The vocal sacs down its throat swell one after another.
    public static class Model extends EntityModel<State> {
        private final ModelPart chest;
        private final ModelPart neck;
        private final ModelPart head;
        private final ModelPart finL;
        private final ModelPart finR;
        private final ModelPart[] frontFeet = new ModelPart[2];
        private final ModelPart[] segments = new ModelPart[4];
        private final ModelPart[][] feet = new ModelPart[4][2];
        private final ModelPart[] sacs = new ModelPart[5];

        public Model(ModelPart root) {
            super(root);
            this.chest = root.getChild("chest");
            this.neck = this.chest.getChild("neck");
            this.head = this.neck.getChild("head");
            this.finL = this.neck.getChild("fin_l");
            this.finR = this.neck.getChild("fin_r");
            this.frontFeet[0] = this.chest.getChild("chest_foot_l");
            this.frontFeet[1] = this.chest.getChild("chest_foot_r");
            for (int i = 0; i < 4; i++) {
                this.segments[i] = root.getChild("seg" + i);
                this.feet[i][0] = this.segments[i].getChild("seg" + i + "_foot_l");
                this.feet[i][1] = this.segments[i].getChild("seg" + i + "_foot_r");
            }
            for (int k = 0; k < 5; k++) {
                this.sacs[k] = this.neck.getChild("sac" + k);
            }
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float pos = state.walkAnimationPos * 0.8F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            float rear = state.rear * state.rear * (3.0F - 2.0F * state.rear);
            // The crawling half: a ripple runs down it, and its little legs step in turn.
            for (int i = 0; i < 4; i++) {
                float wave = Mth.sin(pos - i * 1.1F) * speed;
                this.segments[i].y -= Math.max(0.0F, wave) * 0.8F;
                this.segments[i].x += Mth.sin(t * 0.08F - i * 0.9F) * 0.5F + Mth.sin(pos - i * 0.9F) * speed * 1.2F;
                this.feet[i][0].xRot += Mth.cos(pos - i * 1.1F) * 0.9F * speed;
                this.feet[i][1].xRot += Mth.cos(pos - i * 1.1F + Mth.PI) * 0.9F * speed;
            }
            // Low, it slithers: the chest and neck sway side to side as it goes.
            float sway = Mth.sin(pos + 1.1F) * speed * (1.0F - rear);
            this.chest.yRot += sway * 0.2F;
            this.neck.yRot -= sway * 0.25F;
            this.frontFeet[0].xRot += Mth.cos(pos + 1.1F) * 0.9F * speed * (1.0F - rear);
            this.frontFeet[1].xRot += Mth.cos(pos + 1.1F + Mth.PI) * 0.9F * speed * (1.0F - rear);
            // Reared: the chest stands up from the segments, the neck straightens over it, the head
            // levels out to face what it hunts, and the front legs paw the air.
            this.chest.xRot -= rear * 1.25F;
            this.neck.xRot += rear * 0.25F;
            this.head.xRot += rear * 1.0F;
            this.chest.zRot += Mth.sin(t * 0.06F) * 0.06F * rear;
            this.neck.xRot += Mth.sin(t * 0.045F) * 0.05F;
            this.frontFeet[0].xRot += rear * (-0.6F + Mth.sin(t * 0.25F) * 0.4F);
            this.frontFeet[1].xRot += rear * (-0.6F + Mth.sin(t * 0.25F + Mth.PI) * 0.4F);
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
                this.chest.xRot += strike * 0.7F;              // it strikes down from its height
                this.neck.xRot += strike * 0.3F;
                this.head.xRot -= strike * 0.5F;
            }
        }
    }
}
