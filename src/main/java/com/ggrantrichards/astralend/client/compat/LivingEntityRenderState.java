package com.ggrantrichards.astralend.client.compat;

// (1.21.1 port) 26.3's LivingEntityRenderState: what a mob's model needs to pose it, copied from the
// mob once per frame. Renderers add their own fields in subclasses, as on 26.3.
public class LivingEntityRenderState {
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;          // the head's yaw relative to the body, degrees
    public float xRot;          // the head's pitch, degrees
    public float bodyRot;
    public boolean isBaby;
    public boolean isInvisibleToPlayer;
    public boolean isUpsideDown;
}
