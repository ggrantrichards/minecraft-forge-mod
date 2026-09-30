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

    // The crystal shell turns slowly, the ring spins (fast while scanning or angry), and the whole
    // probe bobs as it floats.
    public static class Model extends EntityModel<State> {
        private final ModelPart shell;
        private final ModelPart lens;
        private final ModelPart ring;

        public Model(ModelPart root) {
            super(root);
            this.shell = root.getChild("shell");
            this.lens = root.getChild("lens");
            this.ring = root.getChild("ring");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            float bob = Mth.sin(t * 0.1F) * 1.0F;
            this.shell.y += bob;
            this.lens.y += bob;
            this.ring.y += bob;
            this.shell.yRot += t * 0.02F;
            this.ring.yRot = t * (state.scanning || state.hostile ? 0.4F : 0.06F);
            this.lens.yRot = state.yRot * Mth.DEG_TO_RAD;
            this.lens.xRot = state.xRot * Mth.DEG_TO_RAD;
        }
    }
}
