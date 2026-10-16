package com.ggrantrichards.astralend.client;

import com.ggrantrichards.astralend.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

// The void hum: within a few blocks of an island's edge in the End, a low drone rises - atmosphere,
// and a warning. Loudest right at the edge, gone six blocks back from it; it eases in and out.
public final class VoidHum {
    private static final int REACH = 6;
    private static Hum playing;
    private static float target;

    public static void register() {
        com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, event -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.level.dimension() != Level.END || player.isSpectator()) {
            target = 0.0F;
        } else if (player.tickCount % 5 == 0) {
            target = edgeFactor(mc.level, player.blockPosition());
        }
        if (target > 0.02F && (playing == null || playing.isStopped())) {
            playing = new Hum();
            mc.getSoundManager().play(playing);
        }
    }

    // 1 at the very edge (the void one block away), falling to 0 six blocks back.
    private static float edgeFactor(Level level, BlockPos at) {
        int nearest = REACH + 1;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= REACH && i < nearest; i++) {
                BlockPos side = at.relative(d, i);
                if (level.getBlockState(side).isAir() && level.getBlockState(side.below()).isAir() && voidBelow(level, side)) {
                    nearest = i;
                    break;
                }
            }
        }
        return nearest > REACH ? 0.0F : 1.0F - (nearest - 1) / (float) REACH;
    }

    private static boolean voidBelow(Level level, BlockPos pos) {
        for (int dy = 2; dy <= 48; dy++) {
            BlockPos p = pos.below(dy);
            if (p.getY() < level.getMinBuildHeight()) {
                return true;
            }
            if (!level.getBlockState(p).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private float level;

        Hum() {
            super(ModSounds.VOID_HUM.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 0.01F;
            this.relative = true;
        }

        @Override
        public void tick() {
            this.level += (target - this.level) * 0.05F;
            this.volume = Mth.clamp(this.level * 0.8F, 0.0F, 1.0F);
            if (this.level < 0.01F && target <= 0.0F) {
                this.stop();
            }
        }
    }

    private VoidHum() {}
}
