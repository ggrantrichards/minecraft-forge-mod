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
    public static final float SCALE = 1.1F;

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

    // A stiff, deliberate walk like the Warden's, arms hanging long; the shadow cape streams back as it
    // moves. When it strikes, both clawed arms come up and down.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;
        private final ModelPart cape;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.rightArm = this.body.getChild("right_arm");
            this.leftArm = this.body.getChild("left_arm");
            this.cape = this.body.getChild("cape");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);  // back to the rest pose; everything below is added to it
            float pos = state.walkAnimationPos * 0.6662F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
            this.rightLeg.xRot += Mth.cos(pos) * 1.2F * speed;
            this.leftLeg.xRot += Mth.cos(pos + Mth.PI) * 1.2F * speed;
            this.rightArm.xRot += Mth.cos(pos + Mth.PI) * 0.8F * speed;
            this.leftArm.xRot += Mth.cos(pos) * 0.8F * speed;
            this.rightArm.zRot += Mth.sin(state.ageInTicks * 0.06F) * 0.04F + 0.05F;
            this.leftArm.zRot -= Mth.sin(state.ageInTicks * 0.06F) * 0.04F + 0.05F;
            this.cape.xRot += speed * 0.6F + Mth.sin(state.ageInTicks * 0.1F) * 0.05F;
            this.body.yRot += Mth.cos(pos) * 0.05F * speed;
            if (state.attackTime > 0.0F) {
                float strike = Mth.sin(state.attackTime * Mth.PI);
                this.rightArm.xRot -= strike * 2.0F;
                this.leftArm.xRot -= strike * 2.0F;
                this.body.xRot += strike * 0.2F;
            }
        }
    }
}
