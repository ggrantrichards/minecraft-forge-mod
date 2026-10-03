package com.ggrantrichards.astralend.client.entity;

import com.ggrantrichards.astralend.entity.UmbralStalker;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class UmbralStalkerRenderer extends MobRenderer<UmbralStalker, UmbralStalkerRenderer.State, UmbralStalkerRenderer.Model> {
    private static final Identifier TEXTURE = ModModelLayers.texture("umbral_stalker");
    public static final float SCALE = 1.1F;   // about 2.2 blocks tall, hunched

    public UmbralStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModModelLayers.UMBRAL_STALKER)), 0.0F);
        this.addLayer(new GlowLayer<>(this, ModModelLayers.glowTexture("umbral_stalker"), state -> state.revealed));
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
    public void extractRenderState(UmbralStalker entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.revealed = entity.isRevealed();
        state.hunting = entity.isHunting();
        state.attackTime = entity.getAttackAnimation(partialTicks);
        state.pounceTime = entity.getPounceAnimation(partialTicks);
    }

    // Dormant in the dark: not drawn at all. Hunting: a faint shimmer (about 12% opaque). Revealed:
    // fully visible. A player in creative or spectator mode always sees it half-shown, the way they
    // see barriers and light blocks - otherwise one from a spawn egg seems never to have appeared.
    @Override
    protected int getModelTint(State state) {
        if (state.revealed) {
            return 0xFFFFFFFF;
        }
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && (player.isCreative() || player.isSpectator())) {
            return 0x88FFFFFF;
        }
        return state.hunting ? 0x1FFFFFFF : 0x00FFFFFF;
    }

    public static class State extends LivingEntityRenderState {
        public boolean revealed;
        public boolean hunting;
        public float attackTime;
        public float pounceTime;
    }

    @Override
    protected void scale(State state, PoseStack poseStack) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    // It walks on its knuckles like a gorilla: each arm swings with the opposite leg, the shoulders
    // roll, and the head (low in front) follows its target. While hidden it creeps low, like a big cat
    // stalking; a normal blow is one fist swung down; in a pounce both arms reach out ahead of it.
    public static class Model extends EntityModel<State> {
        private final ModelPart body;
        private final ModelPart head;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public Model(ModelPart root) {
            super(root, RenderTypes::entityTranslucent);
            this.body = root.getChild("body");
            this.head = this.body.getChild("head");
            this.rightArm = root.getChild("right_arm");
            this.leftArm = root.getChild("left_arm");
            this.rightLeg = root.getChild("right_leg");
            this.leftLeg = root.getChild("left_leg");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);  // back to the rest pose; everything below is added to it
            float pos = state.walkAnimationPos * 0.6662F;
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            this.head.yRot += state.yRot * Mth.DEG_TO_RAD;
            this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
            this.rightLeg.xRot += Mth.cos(pos) * 1.0F * speed;
            this.leftLeg.xRot += Mth.cos(pos + Mth.PI) * 1.0F * speed;
            this.rightArm.xRot += Mth.cos(pos + Mth.PI) * 0.7F * speed;
            this.leftArm.xRot += Mth.cos(pos) * 0.7F * speed;
            this.body.zRot += Mth.cos(pos) * 0.06F * speed;                         // shoulders roll
            this.body.xRot += Mth.sin(state.ageInTicks * 0.08F) * 0.025F;           // breathing
            if (state.attackTime > 0.0F) {
                float strike = Mth.sin(state.attackTime * Mth.PI);
                this.rightArm.xRot -= strike * 1.8F;
                this.body.yRot -= strike * 0.2F;
            }
            if (!state.revealed) {
                this.body.y += 1.5F;                                                 // crouched low
                this.body.xRot += 0.1F;
                this.head.xRot -= 0.1F;
                this.rightArm.y += 1.5F;
                this.leftArm.y += 1.5F;
            }
            if (state.pounceTime > 0.0F) {
                float reach = Mth.sin(state.pounceTime * Mth.PI);
                this.rightArm.xRot -= reach * 1.7F;
                this.leftArm.xRot -= reach * 1.7F;
                this.rightArm.zRot -= reach * 0.2F;
                this.leftArm.zRot += reach * 0.2F;
                this.rightLeg.xRot += reach * 0.8F;                                  // legs thrown back
                this.leftLeg.xRot += reach * 0.8F;
                this.body.xRot -= reach * 0.2F;
            }
        }
    }
}
