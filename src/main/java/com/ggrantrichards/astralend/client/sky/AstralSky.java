package com.ggrantrichards.astralend.client.sky;

import com.ggrantrichards.astralend.flare.ClientFlares;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;

// The client side of Phase 5:
//   - swaps the game's sky renderer for AstralSkyRenderer, which draws the vanilla End sky and then the
//     mod's sky on top (stars, auroras, the Remnant, flares, comets, distant shapes). LevelRenderer makes a
//     new SkyRenderer after resource reloads, so this checks every tick and swaps it again if needed;
//   - keeps the flare countdown running between server updates (ClientFlares);
//   - in the Corona during a flare: charged particles stream past on the solar wind, and the wind
//     pushes you along while you glide with an elytra.
public final class AstralSky {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Field skyField;
    private static boolean failed;

    public static void register() {
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> onTick());
    }

    private static void onTick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || level.dimension() != Level.END) {
            ClientFlares.reset();
            return;
        }
        ClientFlares.tick();
        installSkyRenderer(minecraft);
        LocalPlayer player = minecraft.player;
        if (player != null && ClientFlares.phase() == FlarePhase.FLARE && level.getBiome(player.blockPosition()).is(ModBiomes.CORONA)) {
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

    private static void installSkyRenderer(Minecraft minecraft) {
        if (failed) {
            return;
        }
        try {
            LevelRenderer levelRenderer = minecraft.levelRenderer;
            if (skyField == null) {
                skyField = LevelRenderer.class.getDeclaredField("skyRenderer");
                skyField.setAccessible(true);
            }
            Object current = skyField.get(levelRenderer);
            if (current instanceof AstralSkyRenderer) {
                return;
            }
            TextureManager textures = (TextureManager) field(levelRenderer, "textureManager");
            AtlasManager atlases = (AtlasManager) field(levelRenderer, "atlasManager");
            GameRenderer gameRenderer = (GameRenderer) field(levelRenderer, "gameRenderer");
            if (current instanceof SkyRenderer old) {
                old.close();
            }
            skyField.set(levelRenderer, new AstralSkyRenderer(textures, atlases, gameRenderer.mainRenderTarget()));
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;   // the End keeps vanilla's sky; nothing else depends on this
            LOGGER.error("[AstralEnd] couldn't install the End sky renderer", e);
        }
    }

    private static Object field(LevelRenderer levelRenderer, String name) throws ReflectiveOperationException {
        Field field = LevelRenderer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(levelRenderer);
    }

    private AstralSky() {}
}
