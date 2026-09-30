package com.ggrantrichards.astralend.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Predicate;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

// Draws a mob's "_glow" texture at full brightness over its model, like a Spider's or an
// Enderman's eyes: the parts of the mob that shine in the dark. Optionally only while `when` holds.
public class GlowLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends EyesLayer<S, M> {
    private final RenderType type;
    private final Predicate<S> when;

    public GlowLayer(RenderLayerParent<S, M> parent, Identifier texture) {
        this(parent, texture, state -> true);
    }

    public GlowLayer(RenderLayerParent<S, M> parent, Identifier texture, Predicate<S> when) {
        super(parent);
        this.type = RenderTypes.eyes(texture);
        this.when = when;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, S state, float yRot, float xRot) {
        if (this.when.test(state) && !state.isInvisibleToPlayer) {
            super.submit(poseStack, collector, lightCoords, state, yRot, xRot);
        }
    }

    @Override
    public RenderType renderType() {
        return this.type;
    }
}
