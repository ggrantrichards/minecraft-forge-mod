package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.OrrerySentinel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class OrrerySentinelRenderer extends MobRenderer<OrrerySentinel, OrrerySentinelRenderer.State, OrrerySentinelRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("orrery_sentinel");
    private static final float SCALE = 1.15F;

    public OrrerySentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.ORRERY_SENTINEL)), 1.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("orrery_sentinel")));
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
    public void extractRenderState(OrrerySentinel entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.awake = entity.isAwake();
        state.charge = entity.getCharge() / (float) OrrerySentinel.CHARGE_TICKS;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected int getBlockLightLevel(OrrerySentinel entity, BlockPos pos) {
        return Math.max(entity.isAwake() ? 8 : 3, super.getBlockLightLevel(entity, pos));
    }

    public static class State extends LivingEntityRenderState {
        public boolean awake;
        public float charge;
    }

    // Asleep, it stands slumped, head bowed and arms hanging. Awake, it walks heavily, fists
    // swinging; while it charges its beam it straightens, raises its head and braces its arms.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;
        private final ModelPart ring;

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.rightArm = this.body.getChild("right_arm");
            this.leftArm = this.body.getChild("left_arm");
            this.ring = this.body.getChild("ring");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            if (!state.awake) {
                this.head.xRot += 0.55F;
                this.body.xRot += 0.12F;
                this.rightArm.xRot -= 0.1F;
                this.leftArm.xRot -= 0.1F;
                return;
            }
            float walk = state.walkAnimationPos;
            float speed = Math.min(1.0F, state.walkAnimationSpeed);
            this.rightLeg.xRot += Mth.cos(walk * 0.5F) * 0.9F * speed;
            this.leftLeg.xRot += Mth.cos(walk * 0.5F + Mth.PI) * 0.9F * speed;
            this.rightArm.xRot += Mth.cos(walk * 0.5F + Mth.PI) * 0.7F * speed;
            this.leftArm.xRot += Mth.cos(walk * 0.5F) * 0.7F * speed;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
            this.ring.zRot += t * (state.charge > 0 ? 0.4F : 0.04F);
            if (state.charge > 0) {
                float c = state.charge;
                this.rightArm.xRot -= 0.9F * c;
                this.leftArm.xRot -= 0.9F * c;
                this.rightArm.zRot += 0.35F * c;
                this.leftArm.zRot -= 0.35F * c;
                this.head.xRot -= 0.15F * c;
            }
        }
    }
}
