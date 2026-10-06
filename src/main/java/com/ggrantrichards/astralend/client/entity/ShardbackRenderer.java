package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.Shardback;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class ShardbackRenderer extends MobRenderer<Shardback, ShardbackRenderer.State, ShardbackRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("shardback");

    public ShardbackRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.SHARDBACK)), 0.6F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("shardback")));
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
    public void extractRenderState(Shardback entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.hidden = entity.isHidden();
        state.attackTime = entity.getSwingAnimation(partialTicks);
    }

    public static class State extends LivingEntityRenderState {
        public boolean hidden;
        public float attackTime;
    }

    // Hidden, it folds flat: legs splayed under it, head tucked down, nothing but shards showing.
    // Hunting, it stalks low and heavy, and lunges head first.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart[] legs = new ModelPart[4];

        public Model(ModelPart root) {
            super(root);
            this.body = root.getChild("body");
            this.head = root.getChild("head");
            String[] names = {"leg_fr", "leg_fl", "leg_br", "leg_bl"};
            for (int i = 0; i < 4; i++) {
                this.legs[i] = root.getChild(names[i]);
            }
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            if (state.hidden) {
                this.body.y += 4.0F;
                this.head.y += 5.0F;
                this.head.xRot += 0.5F;
                for (int i = 0; i < 4; i++) {
                    this.legs[i].y += 3.0F;
                    this.legs[i].zRot += (i % 2 == 0 ? 1 : -1) * 1.3F;
                }
                return;
            }
            float pos = state.walkAnimationPos * 0.7F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.6F;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
            this.legs[0].xRot += Mth.cos(pos) * 0.9F * speed;
            this.legs[1].xRot += Mth.cos(pos + Mth.PI) * 0.9F * speed;
            this.legs[2].xRot += Mth.cos(pos + Mth.PI) * 0.9F * speed;
            this.legs[3].xRot += Mth.cos(pos) * 0.9F * speed;
            this.body.zRot += Mth.sin(pos) * 0.04F * speed;
            if (state.attackTime > 0.0F) {
                float lunge = Mth.sin(state.attackTime * Mth.PI);
                this.head.z -= lunge * 3.0F;
                this.head.xRot -= lunge * 0.4F;
            }
        }
    }
}
