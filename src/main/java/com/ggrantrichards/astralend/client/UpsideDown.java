package com.ggrantrichards.astralend.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// Upwelling turns you upside down: with gravity pointing up you fall onto the underside of whatever
// is above you, and stand there, head down, walking on the ceiling. (The server side - gravity, and
// the fall "up" hurting - is UpwellingEffect; this is everything you see and do.)
//
//   - the view rolls over (in half a second) and drops to where your eyes are when you hang from your
//     feet, 1.44 blocks below where they'd be standing;
//   - the mouse and the strafe keys turn the way the screen shows, not the way the world is;
//   - on a ceiling you have your footing (walking speed, and you can jump - down, away from it);
//   - you aim from your eyes, so what's under the crosshair is what you mine or use;
//   - every player, you included in third person, is drawn upside down while it lasts.
public final class UpsideDown {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DROP = 1.62F - 0.18F;    // standing eye height, mirrored about the middle of the body

    private static float flip;           // 0 = upright, 1 = upside down (eased)
    private static float flipOld;
    private static float lastYaw = Float.NaN, lastPitch = Float.NaN;
    private static Method setPosition;
    private static boolean warned;

    public static void register() {
        com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, UpsideDown::onClientTick);
        com.ggrantrichards.astralend.Events.on(ViewportEvent.ComputeCameraAngles.class, UpsideDown::onCamera);
        com.ggrantrichards.astralend.Events.on(MovementInputUpdateEvent.class, UpsideDown::onInput);
        com.ggrantrichards.astralend.Events.on(RenderPlayerEvent.Pre.class, UpsideDown::onRenderPlayer);
        com.ggrantrichards.astralend.Events.on(RenderPlayerEvent.Post.class, UpsideDown::afterRenderPlayer);
    }

    // Upside down: gravity reversed (the Upwelling effect, or anything else that turns it round).
    public static boolean isFlipped(Entity entity) {
        return entity instanceof LivingEntity living && living.getAttributes().hasAttribute(Attributes.GRAVITY)
            && living.getAttributeValue(Attributes.GRAVITY) < 0.0;
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        flipOld = flip;
        boolean flipped = player != null && isFlipped(player);
        flip = Mth.approach(flip, flipped ? 1.0F : 0.0F, 0.1F);
        if (player == null || !flipped) {
            return;
        }
        mirrorTurn(player);
        // Pressed against a ceiling: that's your floor now.
        if (player.verticalCollision && !player.verticalCollisionBelow && player.getDeltaMovement().y >= -0.01) {
            player.setOnGround(true);
        }
    }

    // How far over the view is, for this frame.
    private static float amount(float partialTick) {
        float f = Mth.lerp(partialTick, flipOld, flip);
        return f * f * (3.0F - 2.0F * f);
    }

    private static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        float f = amount((float) event.getPartialTick());
        if (player == null || f <= 0.0F) {
            lastYaw = Float.NaN;
            return;
        }
        if (event.getCamera().getEntity() != player) {
            return;
        }
        if (f > 0.5F) {
            mirrorTurn(player);
            event.setYaw(player.getViewYRot((float) event.getPartialTick()));
            event.setPitch(player.getViewXRot((float) event.getPartialTick()));
        }
        event.setRoll(event.getRoll() + 180.0F * f);
        if (!event.getCamera().isDetached()) {
            Camera camera = event.getCamera();
            Vec3 eye = camera.getPosition().add(0.0, -DROP * f, 0.0);
            moveCamera(camera, eye);
            if (f > 0.5F) {
                aimFrom(minecraft, player, eye, (float) event.getPartialTick());
            }
        }
    }

    // Turning: the mouse turned the view as if upright; mirror that, so it follows the screen. Called
    // every frame and every tick - it mirrors only what changed since it last looked, so however often
    // it's called, each turn is mirrored once. The mouse turns the previous tick's rotation (yRotO,
    // xRotO) along with the current one, and the view is drawn between the two, so both are mirrored.
    // A jump of more than 90 degrees isn't the mouse (a teleport, the server setting it): kept as is.
    private static void mirrorTurn(LocalPlayer player) {
        if (flip <= 0.5F) {
            lastYaw = Float.NaN;
            return;
        }
        if (!Float.isNaN(lastYaw)) {
            float dYaw = player.getYRot() - lastYaw, dPitch = player.getXRot() - lastPitch;
            if (Math.abs(dYaw) < 90.0F && (dYaw != 0.0F || dPitch != 0.0F)) {
                float yaw = lastYaw - dYaw;
                float pitch = Mth.clamp(lastPitch - dPitch, -90.0F, 90.0F);
                player.yRotO += yaw - player.getYRot();
                player.xRotO += pitch - player.getXRot();
                player.setYRot(yaw);
                player.setXRot(pitch);
            }
        }
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
    }

    // A turn made by something other than the mouse (the trailer's scripted aim): taken as it is, not mirrored.
    public static void acceptTurn(LocalPlayer player) {
        if (!Float.isNaN(lastYaw)) {
            lastYaw = player.getYRot();
            lastPitch = player.getXRot();
        }
    }

    private static void moveCamera(Camera camera, Vec3 to) {
        try {
            if (setPosition == null) {
                setPosition = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                setPosition.setAccessible(true);
            }
            setPosition.invoke(camera, to);
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Upwelling: couldn't move the camera to the upside-down eye", e);
            }
        }
    }

    // What's under the crosshair, looked for from the eyes you see through.
    private static void aimFrom(Minecraft minecraft, LocalPlayer player, Vec3 from, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        double blockReach = player.blockInteractionRange(), entityReach = player.entityInteractionRange();
        BlockHitResult block = minecraft.level.clip(new ClipContext(from, from.add(look.scale(blockReach)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 to = from.add(look.scale(entityReach));
        AABB sweep = player.getBoundingBox().move(from.subtract(player.getEyePosition(partialTick))).expandTowards(look.scale(entityReach)).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, from, to, sweep,
            e -> !e.isSpectator() && e.isPickable(), entityReach * entityReach);
        if (entity != null && from.distanceToSqr(entity.getLocation()) < from.distanceToSqr(block.getLocation())) {
            minecraft.hitResult = entity;
            minecraft.crosshairPickEntity = entity.getEntity();
        } else {
            minecraft.hitResult = block;
            minecraft.crosshairPickEntity = null;
        }
    }

    // Strafing follows the screen too.
    private static void onInput(MovementInputUpdateEvent event) {
        if (flip > 0.5F && event.getEntity() == Minecraft.getInstance().player) {
            event.getInput().leftImpulse = -event.getInput().leftImpulse;
        }
    }

    // Turned over as a Dinnerbone is: about the body's own forward axis, hanging from the feet.
    // (1.21.1 port: done round the player's renderer, which has no upside-down flag of its own.)
    private static final java.util.Set<Integer> TURNED = new java.util.HashSet<>();

    private static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        if (!isFlipped(player)) {
            return;
        }
        var pose = event.getPoseStack();
        float yaw = 180.0F - net.minecraft.util.Mth.rotLerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
        pose.pushPose();
        pose.translate(0.0F, player.getBbHeight() + 0.1F, 0.0F);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-yaw));
        TURNED.add(player.getId());
    }

    private static void afterRenderPlayer(RenderPlayerEvent.Post event) {
        if (TURNED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    private UpsideDown() {}
}
