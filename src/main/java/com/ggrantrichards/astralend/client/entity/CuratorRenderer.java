package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Curator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

// The Curator, drawn half again as big as its model: obsidian plates, purpur inlay, a slit of violet light.
// Dormant, it stands with its head bowed and its arms hanging; awake, it hovers, swaying; when it
// slams, its arms come up over its head and down.
public class CuratorRenderer extends MobRenderer<Curator, CuratorRenderer.State, CuratorRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("curator");
    private static final float SCALE = 1.5F;

    public CuratorRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.CURATOR)), 0.9F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("curator")));
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
    public void extractRenderState(Curator entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.dormant = entity.isDormant();
        state.slam = entity.slamProgress();
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    public static class State extends LivingEntityRenderState {
        public boolean dormant;
        public float slam;
    }

    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            if (state.dormant) {
                this.head.xRot += 0.6F;
                this.body.xRot += 0.1F;
                return;
            }
            float t = state.ageInTicks;
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
            // Hovering: the legs trail, the arms drift.
            this.rightLeg.xRot += 0.15F + Mth.sin(t * 0.08F) * 0.08F;
            this.leftLeg.xRot += 0.15F + Mth.sin(t * 0.08F + 1.3F) * 0.08F;
            this.rightArm.zRot += 0.1F + Mth.sin(t * 0.06F) * 0.05F;
            this.leftArm.zRot -= 0.1F + Mth.sin(t * 0.06F) * 0.05F;
            if (state.slam > 0.0F) {
                // 1 -> 0 over the slam: arms raised high, then brought down.
                float raise = state.slam > 0.5F ? (1.0F - state.slam) * 2.0F : state.slam * 2.0F;
                this.rightArm.xRot -= raise * 2.8F;
                this.leftArm.xRot -= raise * 2.8F;
            }
        }
    }
}
