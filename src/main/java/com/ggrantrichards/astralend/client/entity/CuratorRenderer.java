package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Curator;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.ggrantrichards.astralend.client.compat.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// The Curator, drawn at 1.6x (a hulking war-construct: see tools/mobs/make_mobs.py). Dormant, it stands
// bowed, arms hanging, visor dark... awake, it hovers on its thrusters, bobbing, and every attack shows
// before it lands: the cannon arm raised and aimed (then kicked back by the shot), the crouch before
// the charge and the lunge of it, a fist drawn back for each punch, both arms up for the slam. In its
// second phase the display case in its chest is gone, the core shows, and it hangs upside down.
public class CuratorRenderer extends com.ggrantrichards.astralend.client.compat.StateMobRenderer<Curator, CuratorRenderer.State, CuratorRenderer.Model> {
    private static final ResourceLocation TEXTURE = ModModelLayers.texture("curator");
    private static final float SCALE = 1.6F;

    public CuratorRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.CURATOR)), 1.3F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("curator")));
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
    public void extractRenderState(Curator entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.dormant = entity.isDormant();
        state.slam = entity.slamProgress();
        state.exposed = entity.isExposed();
        state.action = entity.action();
        state.actionTicks = Math.max(0.0F, entity.actionTicks() - partialTicks);
        // Its second phase is fought upside down: it turns over with the reliquary (vanilla's own flip, as
        // for a mob named Dinnerbone).
        state.isUpsideDown = state.exposed && !state.dormant;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    public static class State extends LivingEntityRenderState {
        public boolean dormant;
        public boolean exposed;
        public float slam;
        public int action;
        public float actionTicks;
    }

    public static class Model extends com.ggrantrichards.astralend.client.compat.StateModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart chestCase;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.chestCase = this.body.getChild("case");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        // 0 at the start of an action, 1 at its end.
        private static float progress(State state, int length) {
            return Mth.clamp(1.0F - state.actionTicks / length, 0.0F, 1.0F);
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            this.chestCase.visible = !state.exposed;
            if (state.dormant) {
                // Powered down: bowed, its arms hanging forward, a knee bent.
                this.body.xRot += 0.25F;
                this.head.xRot += 0.5F;
                this.rightArm.xRot -= 0.25F;
                this.leftArm.xRot -= 0.25F;
                this.rightArm.z -= 1.5F;
                this.leftArm.z -= 1.5F;
                this.rightLeg.xRot -= 0.35F;
                return;
            }
            float t = state.ageInTicks;
            this.head.yRot += Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD;
            // Hovering on its thrusters: a slow, heavy bob, the legs hanging; moving, its arms swing.
            float bob = Mth.sin(t * 0.09F) * 0.6F;
            this.body.y += bob;
            this.rightArm.y += bob;
            this.leftArm.y += bob;
            this.rightLeg.y += bob;
            this.leftLeg.y += bob;
            float swing = Mth.sin(state.walkAnimationPos * 0.5F) * Math.min(state.walkAnimationSpeed, 1.0F);
            this.rightArm.xRot += swing * 0.5F;
            this.leftArm.xRot -= swing * 0.5F;
            this.rightLeg.xRot += 0.1F - swing * 0.25F;
            this.leftLeg.xRot += 0.1F + swing * 0.25F;
            this.rightArm.zRot += 0.08F;
            this.leftArm.zRot -= 0.08F;
            switch (state.action) {
                case Curator.ACT_AIM -> {
                    // The cannon arm comes up and levels at you; the other arm braces; it leans back a little.
                    float p = Math.min(1.0F, progress(state, Curator.AIM_TICKS) * 3.0F);
                    this.rightArm.xRot = Mth.lerp(p, this.rightArm.xRot, -Mth.HALF_PI + state.xRot * Mth.DEG_TO_RAD);
                    this.rightArm.yRot = Mth.lerp(p, 0.0F, 0.15F);
                    this.leftArm.xRot = Mth.lerp(p, this.leftArm.xRot, -0.5F);
                    this.body.xRot -= 0.1F * p;
                }
                case Curator.ACT_RECOIL -> {
                    // The kick of the shot (or the end of a charge), easing off.
                    float p = progress(state, Curator.RECOIL_TICKS);
                    this.rightArm.xRot = -Mth.HALF_PI - 0.6F * (1.0F - p) + p * (Mth.HALF_PI + this.rightArm.xRot);
                    this.body.xRot -= 0.2F * (1.0F - p);
                }
                case Curator.ACT_CROUCH -> {
                    // Down into a crouch, shoulder forward, arms drawn back.
                    float p = progress(state, Curator.CROUCH_TICKS);
                    this.body.xRot += 0.45F * p;
                    this.rightArm.xRot += 0.7F * p;
                    this.leftArm.xRot += 0.4F * p;
                    this.rightLeg.xRot -= 0.6F * p;
                    this.leftLeg.xRot += 0.5F * p;
                }
                case Curator.ACT_CHARGE -> {
                    // The charge: leaning hard into it, the left shoulder leading, the legs trailing.
                    this.body.xRot += 0.6F;
                    this.leftArm.xRot = -1.1F;
                    this.leftArm.zRot -= 0.3F;
                    this.rightArm.xRot = 0.6F;
                    this.rightLeg.xRot = 0.7F;
                    this.leftLeg.xRot = 0.9F;
                }
                case Curator.ACT_PUNCH_LEFT, Curator.ACT_PUNCH_RIGHT -> {
                    float p = progress(state, Curator.PUNCH_TICKS);
                    float blow = Mth.sin(p * Mth.PI);
                    ModelPart arm = state.action == Curator.ACT_PUNCH_LEFT ? this.leftArm : this.rightArm;
                    arm.xRot -= 1.7F * blow;
                    this.body.yRot += (state.action == Curator.ACT_PUNCH_LEFT ? 0.25F : -0.25F) * blow;
                }
                default -> { }
            }
            if (state.slam > 0.0F) {
                // 1 -> 0 over the slam: both arms raised high, then brought down.
                float raise = state.slam > 0.5F ? (1.0F - state.slam) * 2.0F : state.slam * 2.0F;
                this.rightArm.xRot -= raise * 2.8F;
                this.leftArm.xRot -= raise * 2.8F;
            }
        }
    }
}
