package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.CoronaSkimmer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class CoronaSkimmerRenderer extends MobRenderer<CoronaSkimmer, CoronaSkimmerRenderer.State, CoronaSkimmerRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("corona_skimmer");

    public CoronaSkimmerRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.CORONA_SKIMMER)), 0.5F);
        // The wing membranes shimmer only while they're full (ready to shear).
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("corona_skimmer"), state -> state.full));
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
    public void extractRenderState(CoronaSkimmer entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flying = entity.isFlying();
        state.full = entity.isFull();
    }

    public static class State extends LivingEntityRenderState {
        public boolean flying;
        public boolean full;
    }

    // Resting, it lies flat as a stone with a slow breathing ripple along its wing edges. Flying,
    // the wings beat in long, rolling strokes and the tail streams behind.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart leftWing;
        private final ModelPart rightWing;
        private final ModelPart tail;

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.leftWing = this.body.getChild("left_wing");
            this.rightWing = this.body.getChild("right_wing");
            this.tail = this.body.getChild("tail");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            if (state.flying) {
                float flap = Mth.sin(t * 0.35F) * 0.55F;
                this.leftWing.zRot = -flap;
                this.rightWing.zRot = flap;
                this.body.xRot = Mth.sin(t * 0.35F + 1.0F) * 0.06F;
                this.tail.xRot = Mth.sin(t * 0.35F - 1.5F) * 0.25F;
            } else {
                float breathe = Mth.sin(t * 0.06F) * 0.04F;
                this.leftWing.zRot = -breathe;
                this.rightWing.zRot = breathe;
                this.tail.yRot = Mth.sin(t * 0.03F) * 0.15F;
            }
        }
    }
}
