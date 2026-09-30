package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.CinderMite;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CinderMiteRenderer extends MobRenderer<CinderMite, LivingEntityRenderState, CinderMiteRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("cinder_mite");

    public CinderMiteRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.CINDER_MITE)), 0.25F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("cinder_mite")));
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected float getFlipDegrees() {
        return 180.0F;
    }

    @Override
    protected int getBlockLightLevel(CinderMite entity, BlockPos pos) {
        return 15;
    }

    // Its segments wriggle side to side as it scuttles.
    public static class Model extends EntityModel<LivingEntityRenderState> {
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart tail;

        public Model(ModelPart root) {
            super(root);
            this.head = root.getChild("head");
            this.body = root.getChild("body");
            this.tail = root.getChild("tail");
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks * 0.9F;
            this.head.yRot = Mth.cos(t) * 0.12F;
            this.head.x = Mth.sin(t) * 0.4F;
            this.body.yRot = Mth.cos(t + 1.0F) * 0.08F;
            this.tail.yRot = Mth.cos(t + 2.0F) * 0.25F;
            this.tail.x = Mth.sin(t + 2.0F) * 0.6F;
        }
    }
}
