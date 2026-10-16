package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.client.compat.LivingEntityRenderState;
import com.ggrantrichards.astralend.client.compat.StateMobRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Predicate;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

// Draws a mob's "_glow" texture at full brightness over its model, like a Spider's or an
// Enderman's eyes: the parts of the mob that shine in the dark. Optionally only while `when` holds.
public class GlowLayer<E extends Mob, S extends LivingEntityRenderState> extends RenderLayer<E, EntityModel<E>> {
    private final StateMobRenderer<E, S, ?> renderer;
    private final RenderType type;
    private final Predicate<S> when;

    public GlowLayer(StateMobRenderer<E, S, ?> parent, ResourceLocation texture) {
        this(parent, texture, state -> true);
    }

    public GlowLayer(StateMobRenderer<E, S, ?> parent, ResourceLocation texture, Predicate<S> when) {
        super(parent);
        this.renderer = parent;
        this.type = RenderType.eyes(texture);
        this.when = when;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, E entity, float limbSwing, float limbSwingAmount,
                       float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        S state = this.renderer.state();
        if (state != null && this.when.test(state) && !state.isInvisibleToPlayer) {
            this.getParentModel().renderToBuffer(poseStack, buffers.getBuffer(this.type), 0xF00000, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
