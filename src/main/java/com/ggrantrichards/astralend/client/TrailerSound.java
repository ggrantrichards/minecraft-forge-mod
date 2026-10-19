package com.ggrantrichards.astralend.client;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import org.slf4j.Logger;

// The trailer's sound (dev tool, with ClientTrailer): the game films without a sound device, so instead
// every sound the world plays while a shot is filmed is written down - which, on what frame, where, how
// loud and at what pitch - with where the camera is on each frame. tools/trailer/soundtrack.py plays them
// back from the game's own sound files, placed and faded as you'd have heard them, under the music.
// Sounds are taken as the server plays them (every sound of the fight starts there), once each.
final class TrailerSound {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static PrintWriter out;
    private static volatile int scene = -1;
    private static volatile int frame = -1;

    static void register() {
        PlayLevelSoundEvent.AtPosition.BUS.addListener((Consumer<PlayLevelSoundEvent.AtPosition>) event ->
            heard(event.getLevel() instanceof ServerLevel, event.getSound(), event.getSource(), event.getNewVolume(), event.getNewPitch(),
                event.getPosition()));
        PlayLevelSoundEvent.AtEntity.BUS.addListener((Consumer<PlayLevelSoundEvent.AtEntity>) event ->
            heard(event.getLevel() instanceof ServerLevel, event.getSound(), event.getSource(), event.getNewVolume(), event.getNewPitch(),
                event.getEntity().position()));
    }

    static synchronized void open(File file) {
        try {
            out = new PrintWriter(new FileWriter(file));
        } catch (IOException e) {
            LOGGER.error("[AstralEnd trailer] couldn't write {}", file, e);
        }
    }

    static synchronized void close() {
        if (out != null) {
            out.close();
            out = null;
        }
    }

    // The frame being filmed (between shots: none, and nothing is written down).
    static void at(int sceneIndex, int frameIndex) {
        scene = sceneIndex;
        frame = frameIndex;
    }

    static synchronized void camera(int sceneIndex, int frameIndex, Vec3 at, float yaw) {
        if (out != null) {
            out.printf(Locale.ROOT, "cam %d %d %.2f %.2f %.2f %.1f%n", sceneIndex, frameIndex, at.x, at.y, at.z, yaw);
        }
    }

    private static synchronized void heard(boolean server, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, Vec3 at) {
        if (!server || out == null || frame < 0 || sound == null) {
            return;
        }
        out.printf(Locale.ROOT, "sound %d %d %s %s %.3f %.3f %.2f %.2f %.2f%n", scene, frame, sound.value().location(),
            source.getName(), volume, pitch, at.x, at.y, at.z);
    }

    private TrailerSound() {}
}
