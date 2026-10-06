package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Gloamgrub;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class GloamgrubRenderer extends MobRenderer<Gloamgrub, GloamgrubRenderer.State, GloamgrubRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("gloamgrub");

    public GloamgrubRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.GLOAMGRUB)), 0.35F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("gloamgrub")));
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURE;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    public static class State extends LivingEntityRenderState {
    }

    // It inches along: a ripple runs back down its rings, each lifting a little in turn; its
    // mandibles work.
    public static class Model extends EntityModel<State> {
        private final ModelPart head;
        private final ModelPart[] rings = new ModelPart[4];
        private final ModelPart mandibleL;
        private final ModelPart mandibleR;

        public Model(ModelPart root) {
            super(root);
            this.head = root.getChild("head");
            for (int i = 0; i < 4; i++) {
                this.rings[i] = root.getChild("seg" + (i + 1));
            }
            this.mandibleL = this.head.getChild("mandible_l");
            this.mandibleR = this.head.getChild("mandible_r");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float pos = state.walkAnimationPos * 1.2F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            float t = state.ageInTicks;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.5F;
            this.head.y -= Math.max(0.0F, Mth.sin(pos)) * 0.8F * speed;
            for (int i = 0; i < 4; i++) {
                this.rings[i].y -= Math.max(0.0F, Mth.sin(pos - (i + 1) * 0.9F)) * 0.9F * speed + Mth.sin(t * 0.08F - i) * 0.15F;
            }
            float chew = Mth.sin(t * 0.4F) * 0.25F;
            this.mandibleL.yRot -= chew;
            this.mandibleR.yRot += chew;
        }
    }
}
