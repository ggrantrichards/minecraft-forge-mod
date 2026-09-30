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
    public static final float SCALE = 1.1F;   // about 2.2 blocks tall, hunched

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
        state.slamTime = entity.getSlamAnimation(partialTicks);
    }

    // Hidden, it is a faint shimmer (about 15% opaque); revealed, fully visible.
    @Override
    protected int getModelTint(State state) {
        return state.revealed ? 0xFFFFFFFF : 0x26FFFFFF;
    }

    public static class State extends LivingEntityRenderState {
        public boolean revealed;
        public float attackTime;
        public float slamTime;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    // It walks on its knuckles like a gorilla: each arm swings with the opposite leg, the shoulders
    // roll, and the head (low in front) follows its target. A normal blow is one fist swung down; the
    // Ground Slam rears up with both fists over its head and brings them down together.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);  // back to the rest pose; everything below is added to it
            float pos = state.walkAnimationPos * 0.6662F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
            this.rightLeg.xRot += Mth.cos(pos) * 1.0F * speed;
            this.leftLeg.xRot += Mth.cos(pos + Mth.PI) * 1.0F * speed;
            this.rightArm.xRot += Mth.cos(pos + Mth.PI) * 0.7F * speed;
            this.leftArm.xRot += Mth.cos(pos) * 0.7F * speed;
            this.body.zRot += Mth.cos(pos) * 0.06F * speed;                         // shoulders roll
            this.body.xRot += Mth.sin(state.ageInTicks * 0.08F) * 0.025F;           // breathing
            if (state.attackTime > 0.0F) {
                float strike = Mth.sin(state.attackTime * Mth.PI);
                this.rightArm.xRot -= strike * 1.8F;
                this.body.yRot -= strike * 0.2F;
            }
            if (state.slamTime > 0.0F) {
                float t = state.slamTime;
                float lift;
                if (t < 0.6F) {
                    lift = Mth.sin(t / 0.6F * Mth.PI * 0.5F);                        // rearing up
                } else if (t < 0.68F) {
                    lift = 1.0F - (t - 0.6F) / 0.08F * 1.2F;                         // down, hard
                } else {
                    lift = -0.2F * (1.0F - (t - 0.68F) / 0.32F);                      // fists in the ground
                }
                this.rightArm.xRot -= lift * 2.8F;
                this.leftArm.xRot -= lift * 2.8F;
                this.rightArm.zRot -= lift * 0.25F;
                this.leftArm.zRot += lift * 0.25F;
                this.rightArm.y -= lift * 3.0F;
                this.leftArm.y -= lift * 3.0F;
                this.body.xRot -= lift * 0.3F;
                this.head.xRot -= lift * 0.3F;
            }
        }
    }
}
