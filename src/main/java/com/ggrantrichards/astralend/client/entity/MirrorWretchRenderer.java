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
        super(context, new Model(context.bakeLayer(ModModelLayers.MIRROR_WRETCH)), 0.4F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("mirror_wretch")));
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
        state.attackTime = entity.getAttackAnim(partialTicks);
    }

    public static class State extends LivingEntityRenderState {
        public float attackTime;
    }

    // A slow, loose-limbed walk, its long arms trailing; its head tilts as if listening.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float pos = state.walkAnimationPos * 0.5F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
            this.head.zRot += Mth.sin(state.ageInTicks * 0.03F) * 0.25F;
            this.rightLeg.xRot += Mth.cos(pos) * 0.8F * speed;
            this.leftLeg.xRot += Mth.cos(pos + Mth.PI) * 0.8F * speed;
            this.rightArm.xRot += Mth.cos(pos + Mth.PI) * 0.4F * speed;
            this.leftArm.xRot += Mth.cos(pos) * 0.4F * speed;
            this.rightArm.zRot += 0.06F + Mth.sin(state.ageInTicks * 0.05F) * 0.04F;
            this.leftArm.zRot -= 0.06F + Mth.sin(state.ageInTicks * 0.05F) * 0.04F;
            this.body.xRot += 0.12F;   // stooped
            if (state.attackTime > 0.0F) {
                float strike = Mth.sin(state.attackTime * Mth.PI);
                this.rightArm.xRot -= strike * 2.0F;
                this.leftArm.xRot -= strike * 2.0F;
            }
        }
    }
}
