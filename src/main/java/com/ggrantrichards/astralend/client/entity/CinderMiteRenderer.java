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
        return Math.max(9, super.getBlockLightLevel(entity, pos));   // warm from inside
    }

    // Six legs scuttle in two alternating sets of three, the abdomen sways and the tail flicks.
    public static class Model extends EntityModel<LivingEntityRenderState> {
        private final ModelPart head;
        private final ModelPart abdomen;
        private final ModelPart tail;
        private final ModelPart[] legs = new ModelPart[6];

        public Model(ModelPart root) {
            super(root);
            this.head = root.getChild("head");
            this.abdomen = root.getChild("abdomen");
            this.tail = this.abdomen.getChild("tail");
            String[] names = {"right_leg_1", "left_leg_2", "right_leg_3", "left_leg_1", "right_leg_2", "left_leg_3"};
            for (int i = 0; i < 6; i++) {
                this.legs[i] = root.getChild(names[i]);
            }
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float pos = state.walkAnimationPos * 1.6F;
            float speed = Math.min(1.0F, state.walkAnimationSpeed * 2.0F);
            for (int i = 0; i < 6; i++) {
                float phase = i < 3 ? 0.0F : Mth.PI;         // two tripods, like a real insect
                this.legs[i].yRot += Mth.cos(pos + phase) * 0.45F * speed;
                this.legs[i].zRot += Math.max(0.0F, Mth.sin(pos + phase)) * 0.3F * speed * (this.legs[i].x < 0 ? -1 : 1);
            }
            this.head.yRot += Mth.sin(t * 0.2F) * 0.08F;
            this.abdomen.yRot += Mth.sin(pos * 0.5F) * 0.08F * speed + Mth.sin(t * 0.1F) * 0.03F;
            this.tail.xRot += -0.15F + Mth.sin(t * 0.25F) * 0.12F;
        }
    }
}
