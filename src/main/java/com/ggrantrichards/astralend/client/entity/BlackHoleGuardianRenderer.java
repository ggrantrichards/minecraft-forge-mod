package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.BlackHoleGuardian;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BlackHoleGuardianRenderer extends MobRenderer<BlackHoleGuardian, BlackHoleGuardianRenderer.State, BlackHoleGuardianRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("black_hole_guardian");

    public BlackHoleGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.BLACK_HOLE_GUARDIAN)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("black_hole_guardian")));
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
    public void extractRenderState(BlackHoleGuardian entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.exposed = entity.isExposed();
    }

    public static class State extends LivingEntityRenderState {
        public boolean exposed;
    }

    // The core wobbles; the accretion ring spins, and flickers when the core is exposed.
    public static class Model extends EntityModel<State> {
        private final ModelPart core;
        private final ModelPart ringTilt;
        private final ModelPart ring;

        public Model(ModelPart root) {
            super(root);
            this.core = root.getChild("core");
            this.ringTilt = root.getChild("ring_tilt");
            this.ring = this.ringTilt.getChild("ring");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float bob = Mth.sin(t * 0.08F) * 0.8F;
            this.core.y += bob;
            this.ringTilt.y += bob;
            float wobble = 1.0F + Mth.sin(t * 0.5F) * 0.04F;
            this.core.xScale = wobble;
            this.core.yScale = 2.0F - wobble;
            this.core.zScale = wobble;
            this.core.yRot = t * 0.03F;
            this.ring.yRot = t * 0.25F;
            this.ringTilt.visible = !state.exposed || ((int) t / 2) % 2 == 0;
        }
    }
}
