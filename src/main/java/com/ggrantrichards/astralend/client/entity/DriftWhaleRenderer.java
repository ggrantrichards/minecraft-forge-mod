package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.DriftWhale;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DriftWhaleRenderer extends MobRenderer<DriftWhale, DriftWhaleRenderer.State, DriftWhaleRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("drift_whale");
    public static final float SCALE = 3.0F;

    public DriftWhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.DRIFT_WHALE)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("drift_whale")));
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
    public void extractRenderState(DriftWhale entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.humming = entity.isHumming();
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
    }

    // A slow, heavy swim: the tail and fluke beat up and down, the fins row, the body rolls a little.
    // While it hums, the head lifts.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart tail;
        private final ModelPart fluke;
        private final ModelPart leftFin;
        private final ModelPart rightFin;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.tail = this.body.getChild("tail");
            this.fluke = this.tail.getChild("fluke");
            this.leftFin = this.body.getChild("left_fin");
            this.rightFin = this.body.getChild("right_fin");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks * 0.08F;
            this.body.xRot = Mth.sin(t) * 0.04F;
            this.body.zRot = Mth.sin(t * 0.5F) * 0.03F;
            this.tail.xRot = Mth.sin(t - 0.8F) * 0.25F;
            this.fluke.xRot = Mth.sin(t - 1.6F) * 0.35F;
            this.leftFin.zRot = 0.3F + Mth.sin(t) * 0.25F;
            this.rightFin.zRot = -0.3F - Mth.sin(t) * 0.25F;
            if (state.humming) {
                this.head.xRot = -0.12F + Mth.sin(state.ageInTicks * 0.3F) * 0.02F;
            }
        }
    }
}
