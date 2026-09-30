package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.VoidWanderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class VoidWandererRenderer extends MobRenderer<VoidWanderer, VoidWandererRenderer.State, VoidWandererRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("void_wanderer");

    public VoidWandererRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.VOID_WANDERER)), 0.3F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("void_wanderer")));
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
    public void extractRenderState(VoidWanderer entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scanning = entity.getScanProgress() > 0;
        state.hostile = entity.isHostile();
    }

    @Override
    protected int getBlockLightLevel(VoidWanderer entity, BlockPos pos) {
        return Math.max(6, super.getBlockLightLevel(entity, pos));
    }

    public static class State extends LivingEntityRenderState {
        public boolean scanning;
        public boolean hostile;
    }

    // The drone bobs as it floats; its gimbal ring spins (fast while it scans you or is angry), and
    // its little fins trim its balance.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart ring;
        private final ModelPart leftFin;
        private final ModelPart rightFin;

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.ring = root.getChild("ring");
            this.leftFin = this.body.getChild("left_fin");
            this.rightFin = this.body.getChild("right_fin");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float bob = Mth.sin(t * 0.1F) * 1.0F;
            this.body.y += bob;
            this.ring.y += bob;
            this.body.xRot += state.xRot * Mth.DEG_TO_RAD * 0.5F;
            this.ring.yRot += t * (state.scanning || state.hostile ? 0.35F : 0.05F);
            this.ring.xRot += Mth.sin(t * 0.05F) * 0.12F;
            this.leftFin.zRot += Mth.sin(t * 0.3F) * 0.15F;
            this.rightFin.zRot -= Mth.sin(t * 0.3F) * 0.15F;
        }
    }
}
