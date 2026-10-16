package com.ggrantrichards.astralend.client.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;

// (1.21.1 port) 26.3's EntityModel<S>: a model posed from a render state. Each frame it's reset to its
// rest pose, then setupAnim(state) moves it. The renderer (StateMobRenderer) hands it the state.
public abstract class StateModel<S extends LivingEntityRenderState> extends EntityModel<Entity> {
    protected final ModelPart root;
    S state;
    int tint = -1;

    protected StateModel(ModelPart root) {
        this(root, RenderType::entityCutoutNoCull);
    }

    protected StateModel(ModelPart root, Function<ResourceLocation, RenderType> renderType) {
        super(renderType);
        this.root = root;
    }

    public ModelPart root() {
        return this.root;
    }

    // Override to pose the model; call super first (it resets the pose).
    public void setupAnim(S state) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
    }

    @Override
    public final void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (this.state == null) {
            return;
        }
        this.state.walkAnimationPos = limbSwing;
        this.state.walkAnimationSpeed = limbSwingAmount;
        this.state.ageInTicks = ageInTicks;
        this.state.yRot = netHeadYaw;
        this.state.xRot = headPitch;
        this.setupAnim(this.state);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color) {
        this.root.render(poseStack, buffer, light, overlay, this.tint == -1 ? color : FastColor.ARGB32.multiply(color, this.tint));
    }
}
