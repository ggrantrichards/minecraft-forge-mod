package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Hushback;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.ggrantrichards.astralend.client.compat.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// The Hushback: its dark hide with the bones showing on it, and its fleece.
public class HushbackRenderer extends com.ggrantrichards.astralend.client.compat.StateMobRenderer<Hushback, HushbackRenderer.State, HushbackRenderer.Model> {
    private static final ResourceLocation TEXTURE = ModModelLayers.texture("hushback");

    public HushbackRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.HUSHBACK)), 0.6F);
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
    public void extractRenderState(Hushback entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.sheared = entity.isSheared();
        state.lookout = entity.isLookout();
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    public static class State extends LivingEntityRenderState {
        public boolean sheared;
        public boolean lookout;
        public boolean snarl;   // (the Hollowback) its prey is close: jaw open
        public float bite;      // (the Hollowback) its attack swing, 0 to 1
    }

    // Walks like a sheep, grazes with its head low (high, on watch), and breathes slowly.
    public static class Model extends com.ggrantrichards.astralend.client.compat.StateModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart fleece;
        private final ModelPart jaw;    // only the Hollowback has one
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.fleece = this.body.getChild("fleece");
            ModelPart jaw = null;
            try {
                jaw = this.head.getChild("jaw");
            } catch (java.util.NoSuchElementException e) {
                // a Hushback: no jaw to open
            }
            this.jaw = jaw;
            String[] names = {"leg_fr", "leg_fl", "leg_br", "leg_bl"};
            for (int i = 0; i < 4; i++) {
                this.legs[i] = root.getChild(names[i]);
            }
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float pos = state.walkAnimationPos * 0.6662F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.legs[0].xRot += Mth.cos(pos) * 1.2F * speed;
            this.legs[1].xRot += Mth.cos(pos + Mth.PI) * 1.2F * speed;
            this.legs[2].xRot += Mth.cos(pos + Mth.PI) * 1.2F * speed;
            this.legs[3].xRot += Mth.cos(pos) * 1.2F * speed;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.5F;
            // Head carried low, grazing - or held high on watch, turning slowly.
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5F + (state.lookout ? -0.45F : 0.35F);
            if (state.lookout) {
                this.head.yRot += Mth.sin(state.ageInTicks * 0.04F) * 0.35F;
            }
            this.body.y += Mth.sin(state.ageInTicks * 0.06F) * 0.2F;
            this.fleece.visible = !state.sheared;
            if (this.jaw != null) {
                // Shut, the jaw is just the bottom of a Hushback's head. With its prey close the head
                // splits open wide on its fangs, trembling, and each bite snaps it shut and open again.
                if (state.snarl) {
                    float snap = Mth.sin(Math.min(1.0F, state.bite) * Mth.PI) * 0.75F;
                    this.head.xRot -= 0.55F;     // the top of the head tips back, as a dog's does to bite
                    this.jaw.xRot = 1.15F + Mth.sin(state.ageInTicks * 1.3F) * 0.05F - snap;
                } else {
                    this.jaw.xRot = 0.0F;
                }
            }
        }
    }
}
