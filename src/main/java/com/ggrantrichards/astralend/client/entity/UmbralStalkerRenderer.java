package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.UmbralStalker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class UmbralStalkerRenderer extends MobRenderer<UmbralStalker, UmbralStalkerRenderer.State, UmbralStalkerRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("umbral_stalker");
    public static final float SCALE = 1.35F;

    public UmbralStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.UMBRAL_STALKER)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("umbral_stalker"), state -> state.revealed));
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
    public void extractRenderState(UmbralStalker entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.revealed = entity.isRevealed();
        state.attackTime = entity.getAttackAnimation(partialTicks);
    }

    // Hidden, it is a faint shimmer (about 15% opaque); revealed, fully visible.
    @Override
    protected int getModelTint(State state) {
        return state.revealed ? 0xFFFFFFFF : 0x26FFFFFF;
    }

    public static class State extends LivingEntityRenderState {
        public boolean revealed;
        public float attackTime;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    // It prowls on its knuckles and backward-bent legs: diagonal pairs move together like a big
    // cat's, the tail sways, the jaw works. When it strikes it rears up and both arms lash forward.
    public static class Model extends EntityModel<State> {
        private final ModelPart torso;
        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightForearm;
        private final ModelPart leftForearm;
        private final ModelPart rightThigh;
        private final ModelPart leftThigh;
        private final ModelPart rightShin;
        private final ModelPart leftShin;
        private final ModelPart tail;
        private final ModelPart tailTip;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            ModelPart hips = root.getChild("hips");
            this.torso = hips.getChild("torso");
            this.head = this.torso.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.rightArm = this.torso.getChild("right_arm");
            this.leftArm = this.torso.getChild("left_arm");
            this.rightForearm = this.rightArm.getChild("right_forearm");
            this.leftForearm = this.leftArm.getChild("left_forearm");
            this.rightThigh = hips.getChild("right_thigh");
            this.leftThigh = hips.getChild("left_thigh");
            this.rightShin = this.rightThigh.getChild("right_shin");
            this.leftShin = this.leftThigh.getChild("left_shin");
            this.tail = hips.getChild("tail");
            this.tailTip = this.tail.getChild("tail_tip");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);  // back to the rest pose; everything below is added to it
            float pos = state.walkAnimationPos * 0.6F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            float t = state.ageInTicks;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.8F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F + Mth.sin(t * 0.08F) * 0.04F;
            this.jaw.xRot += 0.08F + Mth.sin(t * 0.13F) * 0.06F;
            // diagonal gait: right arm with left leg, left arm with right leg
            float a = Mth.cos(pos) * 0.55F * speed;
            this.rightArm.xRot += a;
            this.leftArm.xRot -= a;
            this.rightForearm.xRot += Math.max(0.0F, -a) * 0.6F;
            this.leftForearm.xRot += Math.max(0.0F, a) * 0.6F;
            this.leftThigh.xRot += a;
            this.rightThigh.xRot -= a;
            this.leftShin.xRot += Math.max(0.0F, -a) * 0.8F;
            this.rightShin.xRot += Math.max(0.0F, a) * 0.8F;
            this.torso.xRot += Mth.sin(pos * 2.0F) * 0.04F * speed;
            this.tail.yRot += Mth.sin(t * 0.07F) * 0.3F;
            this.tailTip.yRot += Mth.sin(t * 0.07F - 0.8F) * 0.35F;
            if (state.attackTime > 0.0F) {
                float strike = Mth.sin(state.attackTime * Mth.PI);
                this.torso.xRot -= strike * 0.55F;
                this.head.xRot += strike * 0.45F;
                this.jaw.xRot += strike * 0.5F;
                this.rightArm.xRot -= strike * 1.3F;
                this.leftArm.xRot -= strike * 1.3F;
            }
        }
    }
}
