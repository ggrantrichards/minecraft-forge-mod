package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Hushback;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// The Hushback, drawn see-through: its clear hide and fleece over opaque bones.
public class HushbackRenderer extends MobRenderer<Hushback, HushbackRenderer.State, HushbackRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("hushback");

    public HushbackRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.HUSHBACK)), 0.6F);
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
    public void extractRenderState(Hushback entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.sheared = entity.isSheared();
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    public static class State extends LivingEntityRenderState {
        public boolean sheared;
    }

    // Walks like a sheep, grazes with its head low, and breathes slowly.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart fleece;
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.fleece = this.body.getChild("fleece");
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
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5F + 0.35F;   // head carried low, grazing
            this.body.y += Mth.sin(state.ageInTicks * 0.06F) * 0.2F;
            this.fleece.visible = !state.sheared;
        }
    }
}
