package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.UmbralStalker;
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
        state.attackTime = entity.swinging ? Math.min(1.0F, (entity.swingTime + partialTicks) / 6.0F) : 0.0F;
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

    // A long, loping walk with the arms swinging low; the arms lash forward when it strikes.
    public static class Model extends EntityModel<State> {
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            ModelPart body = root.getChild("body");
            this.head = body.getChild("head");
            this.rightArm = body.getChild("right_arm");
            this.leftArm = body.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float pos = state.walkAnimationPos;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot = state.xRot * Mth.DEG_TO_RAD - 0.3F;
            this.rightLeg.xRot = Mth.cos(pos * 0.5F) * 0.9F * speed;
            this.leftLeg.xRot = Mth.cos(pos * 0.5F + Mth.PI) * 0.9F * speed;
            // Arms hang straight down despite the hunched body, and swing against the legs.
            this.rightArm.xRot = -0.3F + Mth.cos(pos * 0.5F + Mth.PI) * 0.6F * speed;
            this.leftArm.xRot = -0.3F + Mth.cos(pos * 0.5F) * 0.6F * speed;
            this.rightArm.zRot = 0.08F + Mth.sin(state.ageInTicks * 0.05F) * 0.03F;
            this.leftArm.zRot = -0.08F - Mth.sin(state.ageInTicks * 0.05F) * 0.03F;
            if (state.attackTime > 0.0F) {
                float swing = Mth.sin(state.attackTime * Mth.PI);
                this.rightArm.xRot -= swing * 1.6F;
                this.leftArm.xRot -= swing * 1.6F;
            }
        }
    }
}
