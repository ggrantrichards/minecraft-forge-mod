package com.ggrantrichards.astralend.client.sky;

import com.ggrantrichards.astralend.flare.ClientFlares;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// The client side of Phase 5:
//   - draws the mod's sky (AstralSkyRenderer) over vanilla's End sky each frame: stars, the Eclipse's
//     star and clouds, flares, comets, distant shapes;
//   - keeps the flare countdown running between server updates (ClientFlares);
//   - in the Eclipse during a flare: charged particles stream past on the solar wind, and the wind
//     pushes you along while you glide with an elytra.
public final class AstralSky {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static AstralSkyRenderer renderer;
    private static boolean failed;

    public static void register() {
        com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, event -> onTick());
        com.ggrantrichards.astralend.Events.on(net.minecraftforge.client.event.RenderLevelStageEvent.class, AstralSky::onRenderStage);
    }

    private static void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.dimension() != Level.END) {
            ClientFlares.reset();
            return;
        }
        ClientFlares.tick();
        LocalPlayer player = minecraft.player;
        if (player != null && ClientFlares.phase() == FlarePhase.FLARE && com.ggrantrichards.astralend.flare.Sunstorms.inSunstorms(level, player.blockPosition())) {
            solarWind(level, player);
        }
    }

    private static void solarWind(ClientLevel level, LocalPlayer player) {
        float yaw = ClientFlares.state().windYaw() * Mth.DEG_TO_RAD;
        Vec3 wind = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        if (player.isFallFlying()) {
            player.addDeltaMovement(wind.scale(0.03));
        }
        RandomSource random = level.getRandom();
        for (int i = 0; i < 3; i++) {
            // Small sparks streaming past downwind, never right in front of the camera: they start
            // upwind and off to the side, 6-18 blocks away.
            double side = (random.nextBoolean() ? 1 : -1) * (6.0 + random.nextDouble() * 12.0);
            Vec3 start = player.position().add(wind.scale(-14.0)).add(-wind.z * side, random.nextDouble() * 8.0 - 1.0, wind.x * side);
            Vec3 velocity = wind.scale(0.7 + random.nextDouble() * 0.4);
            level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.ELECTRIC_SPARK,
                start.x, start.y, start.z, velocity.x, 0.01, velocity.z);
        }
    }

    // Drawn just after vanilla's End sky each frame (1.21.1 has no swappable sky renderer).
    private static void onRenderStage(net.minecraftforge.client.event.RenderLevelStageEvent event) {
        if (event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_SKY || failed) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.dimension() != Level.END) {
            return;
        }
        try {
            if (renderer == null) {
                renderer = new AstralSkyRenderer();
            }
            float partial = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
            renderer.extractRenderState(minecraft.level, partial, event.getCamera());
            renderer.render(new org.joml.Matrix4f(event.getPoseStack().last().pose()));
        } catch (RuntimeException e) {
            failed = true;   // the End keeps vanilla's sky; nothing else depends on this
            LOGGER.error("[AstralEnd] couldn't draw the End sky", e);
        }
    }

    private AstralSky() {}
}
