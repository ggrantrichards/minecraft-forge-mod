package com.ggrantrichards.astralend.client.entity;

import net.minecraft.client.renderer.RenderType;
import com.ggrantrichards.astralend.entity.DriftWhale;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.ggrantrichards.astralend.client.compat.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DriftWhaleRenderer extends com.ggrantrichards.astralend.client.compat.StateMobRenderer<DriftWhale, DriftWhaleRenderer.State, DriftWhaleRenderer.Model> {
    private static final ResourceLocation TEXTURE = ModModelLayers.texture("drift_whale");
    public static final float SCALE = 2.5F;

    public DriftWhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.DRIFT_WHALE)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("drift_whale")));
    }

    @Override
    public ResourceLocation getTextureLocation(State state) {
        return TEXTURE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DriftWhale entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.humming = entity.isHumming();
        state.roll = entity.roll(partialTicks);
        state.tempo = entity.tempo();
        state.pitch = entity.getViewXRot(partialTicks);
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected int getBlockLightLevel(DriftWhale entity, BlockPos pos) {
        return Math.max(7, super.getBlockLightLevel(entity, pos));
    }

    public static class State extends LivingEntityRenderState {
        public boolean humming;
        public float roll;
        public float tempo = 1.0F;
        public float pitch;
    }

    // A slow, heavy swim: a wave runs down the body to the tail and fluke, the long fins row, and
    // the whole whale rolls a little, each at its own tempo. It pitches as it climbs and dives, and
    // rolls right over when it plays. While it hums, its head lifts and its jaw opens.
    public static class Model extends com.ggrantrichards.astralend.client.compat.StateModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart rear;
        private final ModelPart tailstock;
        private final ModelPart fluke;
        private final ModelPart leftFin;
        private final ModelPart rightFin;

        public Model(ModelPart root) {
            super(root, RenderType::entityTranslucent);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.rear = this.body.getChild("rear");
            this.tailstock = this.rear.getChild("tailstock");
            this.fluke = this.tailstock.getChild("fluke");
            this.leftFin = this.body.getChild("left_fin");
            this.rightFin = this.body.getChild("right_fin");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks * 0.07F * state.tempo;
            this.body.xRot += Mth.sin(t) * 0.03F + state.pitch * Mth.DEG_TO_RAD;
            this.body.zRot += Mth.sin(t * 0.5F) * 0.03F + state.roll * Mth.DEG_TO_RAD;
            this.rear.xRot += Mth.sin(t - 0.7F) * 0.08F;
            this.tailstock.xRot += Mth.sin(t - 1.4F) * 0.16F;
            this.fluke.xRot += Mth.sin(t - 2.1F) * 0.3F;
            this.leftFin.zRot += Mth.sin(t) * 0.22F;
            this.rightFin.zRot -= Mth.sin(t) * 0.22F;
            if (state.humming) {
                this.head.xRot -= 0.1F;
                this.jaw.xRot += 0.18F + Mth.sin(state.ageInTicks * 0.3F) * 0.03F;
            }
        }
    }
}
