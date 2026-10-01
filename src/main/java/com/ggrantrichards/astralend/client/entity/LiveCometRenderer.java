package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.comet.LiveComet;
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

public class LiveCometRenderer extends MobRenderer<LiveComet, LiveCometRenderer.State, LiveCometRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("live_comet");
    private static final float SCALE = 3.0F;

    public LiveCometRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.LIVE_COMET)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("live_comet")));
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
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected int getBlockLightLevel(LiveComet entity, BlockPos pos) {
        return 15;   // it shines by its own light
    }

    public static class State extends LivingEntityRenderState {
    }

    // The rock tumbles slowly as it flies, pitched along its path, inside a coma of two turning shells of
    // light. (Its tail is particles: see LiveComet.)
    public static class Model extends EntityModel<State> {
        private final ModelPart comet;
        private final ModelPart coma;
        private final ModelPart comaOuter;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            this.comet = root.getChild("comet");
            this.coma = this.comet.getChild("coma");
            this.comaOuter = this.comet.getChild("coma_outer");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            this.comet.xRot += state.xRot * Mth.DEG_TO_RAD;
            this.comet.zRot += t * 0.03F;                       // the slow tumble
            this.coma.xRot += t * 0.05F;                        // the coma's two shells turn against each other,
            this.coma.yRot += t * 0.07F;                        // like an End Crystal's glass
            this.comaOuter.xRot -= t * 0.04F + 0.6F;
            this.comaOuter.yRot -= t * 0.03F + 0.4F;
        }
    }
}
