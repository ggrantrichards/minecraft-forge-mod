package com.ggrantrichards.astralend.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

// (1.21.1 port) 26.3's MobRenderer<E, S, M>: each frame the mob is copied into a render state
// (createRenderState, extractRenderState), and the texture, scale, tint and model all read that.
public abstract class StateMobRenderer<E extends Mob, S extends LivingEntityRenderState, M extends StateModel<S>>
        extends MobRenderer<E, EntityModel<E>> {
    protected final M stateModel;
    protected S current;

    @SuppressWarnings("unchecked")
    protected StateMobRenderer(EntityRendererProvider.Context context, M model, float shadowRadius) {
        super(context, (EntityModel<E>) (EntityModel<?>) model, shadowRadius);
        this.stateModel = model;
    }

    public abstract S createRenderState();

    public abstract ResourceLocation getTextureLocation(S state);

    public void extractRenderState(E entity, S state, float partialTicks) {
        state.ageInTicks = entity.tickCount + partialTicks;
        state.bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.yRot = Mth.wrapDegrees(Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot) - state.bodyRot);
        state.xRot = entity.getViewXRot(partialTicks);
        state.walkAnimationPos = entity.walkAnimation.position(partialTicks);
        state.walkAnimationSpeed = entity.walkAnimation.speed(partialTicks);
        state.isBaby = entity.isBaby();
        var player = Minecraft.getInstance().player;
        state.isInvisibleToPlayer = player != null && entity.isInvisibleTo(player);
    }

    public S state() {
        return this.current;
    }

    @Override
    public void render(E entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        S state = this.createRenderState();
        this.extractRenderState(entity, state, partialTicks);
        this.current = state;
        this.stateModel.state = state;
        this.stateModel.tint = this.getModelTint(state);
        super.render(entity, entityYaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public final ResourceLocation getTextureLocation(E entity) {
        return this.getTextureLocation(this.current);
    }

    protected void scale(S state, PoseStack poseStack) {
    }

    @Override
    protected final void scale(E entity, PoseStack poseStack, float partialTicks) {
        if (this.current != null) {
            this.scale(this.current, poseStack);
        }
    }

    // 26.3's state.isUpsideDown: turned over as a Dinnerbone is (the Curator, in its flipped phase).
    @Override
    protected void setupRotations(E entity, PoseStack poseStack, float bob, float yBodyRot, float partialTicks, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTicks, scale);
        if (this.current != null && this.current.isUpsideDown && !isEntityUpsideDown(entity)) {
            poseStack.translate(0.0F, (entity.getBbHeight() + 0.1F) / scale, 0.0F);
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        }
    }

    protected int getModelTint(S state) {
        return -1;
    }

    protected float getFlipDegrees() {
        return 90.0F;
    }

    @Override
    protected float getFlipDegrees(E entity) {
        return this.getFlipDegrees();
    }
}
