package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.ModEffects;
import com.ggrantrichards.astralend.entity.DriftWhale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.level.NoteBlockEvent;

// Singing to a Drift Whale: a note block played within 16 blocks of someone Remembering (they've eaten
// Memory Moss) carries to every whale within 48 blocks; each turns toward it and sings back.
public final class WhaleSong {
    public static void register() {
        com.ggrantrichards.astralend.Events.cancelling(NoteBlockEvent.Play.class, WhaleSong::onPlay);
    }

    private static boolean onPlay(NoteBlockEvent.Play event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos pos = event.getPos();
        Player singer = null;
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(16.0))) {
            if (player.hasEffect(ModEffects.remembering())) {
                singer = player;
                break;
            }
        }
        if (singer == null) {
            return false;
        }
        for (DriftWhale whale : level.getEntitiesOfClass(DriftWhale.class, new AABB(pos).inflate(48.0))) {
            whale.hearSong(level, singer, pos);
        }
        return false;
    }

    private WhaleSong() {}
}
