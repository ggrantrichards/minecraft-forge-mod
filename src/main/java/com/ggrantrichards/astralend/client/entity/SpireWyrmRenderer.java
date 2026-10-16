package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.SpireWyrm;
import com.ggrantrichards.astralend.entity.SpireWyrmPart;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import com.ggrantrichards.astralend.client.compat.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Draws the Spire Wyrm: its head where the entity is, and each body segment where its SpireWyrmPart
// is - all as parts of one model, placed in the model's own space every frame.
public class SpireWyrmRenderer extends com.ggrantrichards.astralend.client.compat.StateMobRenderer<SpireWyrm, SpireWyrmRenderer.State, SpireWyrmRenderer.Model> {
    private static final ResourceLocation TEXTURE = ModModelLayers.texture("spire_wyrm");
    public static final float SCALE = 2.5F;

    public SpireWyrmRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.SPIRE_WYRM)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("spire_wyrm")));
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
    public void extractRenderState(SpireWyrm entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Vec3 origin = entity.getPosition(partialTicks);
        for (int i = 0; i < SpireWyrm.SEGMENTS; i++) {
            SpireWyrmPart part = entity.part(i);
            double x = Mth.lerp(partialTicks, part.xo, part.getX());
            double y = Mth.lerp(partialTicks, part.yo, part.getY()) + part.getBbHeight() / 2.0;
            double z = Mth.lerp(partialTicks, part.zo, part.getZ());
            state.offsets[i] = new Vec3(x - origin.x, y - origin.y, z - origin.z);
            state.yaws[i] = part.getYRot();
            state.pitches[i] = part.getXRot();
        }
        state.headHeight = entity.getBbHeight() / 2.0F;
        SpireWyrm.Phase phase = entity.phase();
        state.stunned = phase == SpireWyrm.Phase.STUNNED || phase == SpireWyrm.Phase.GROUNDED;
        state.breathing = phase == SpireWyrm.Phase.WIND_BREATH || phase == SpireWyrm.Phase.BREATH || phase == SpireWyrm.Phase.WIND_DIVE;
        state.asleep = phase == SpireWyrm.Phase.ASLEEP;
    }

    // Its body trails up to 20 blocks behind its head, so it's drawn whenever any of it could be in
    // view - not just when the head is (which made the body blink out as the head left the screen).

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    public static class State extends LivingEntityRenderState {
        public final Vec3[] offsets = new Vec3[SpireWyrm.SEGMENTS];
        public final float[] yaws = new float[SpireWyrm.SEGMENTS];
        public final float[] pitches = new float[SpireWyrm.SEGMENTS];
        public float headHeight;
        public boolean stunned;
        public boolean breathing;
        public boolean asleep;
    }

    public static class Model extends com.ggrantrichards.astralend.client.compat.StateModel<State> {
        private final ModelPart head;
        private final ModelPart jaw;
        private final ModelPart[] segments = new ModelPart[SpireWyrm.SEGMENTS];

        public Model(ModelPart root) {
            super(root);
            this.head = root.getChild("head");
            this.jaw = this.head.getChild("jaw");
            for (int i = 0; i < SpireWyrm.SEGMENTS; i++) {
                this.segments[i] = root.getChild("seg" + i);
            }
        }

        // LivingEntityRenderer turns the model by (180 - body yaw), flips it (scale -1, -1, 1), scales
        // it and lowers it 1.501 blocks. This undoes that for a point given as an offset from the
        // entity, giving where to put a part (in model pixels).
        private static void place(ModelPart part, State state, Vec3 offset) {
            double a = Math.toRadians(180.0F - state.bodyRot);
            double cos = Math.cos(-a), sin = Math.sin(-a);
            double x = offset.x * cos + offset.z * sin;
            double z = -offset.x * sin + offset.z * cos;
            part.x = (float) (-x / SCALE * 16.0);
            part.y = (float) (-offset.y / SCALE * 16.0 + 24.016);
            part.z = (float) (z / SCALE * 16.0);
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            place(this.head, state, new Vec3(0.0, state.headHeight, 0.0));
            this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
            float open = state.asleep ? 0.0F : state.breathing ? 0.5F + Mth.sin(state.ageInTicks * 0.4F) * 0.1F
                : state.stunned ? 0.25F : 0.08F + Mth.sin(state.ageInTicks * 0.1F) * 0.06F;
            this.jaw.xRot = open;
            for (int i = 0; i < SpireWyrm.SEGMENTS; i++) {
                ModelPart seg = this.segments[i];
                Vec3 offset = state.offsets[i];
                if (offset == null) {
                    continue;
                }
                place(seg, state, offset);
                seg.yRot = Mth.wrapDegrees(state.yaws[i] - state.bodyRot) * Mth.DEG_TO_RAD;
                seg.xRot = state.pitches[i] * Mth.DEG_TO_RAD;
                seg.zRot = Mth.sin(state.ageInTicks * 0.15F - i * 0.5F) * 0.08F;
            }
        }
    }
}
