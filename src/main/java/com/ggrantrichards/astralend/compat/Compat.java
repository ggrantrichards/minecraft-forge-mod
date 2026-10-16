package com.ggrantrichards.astralend.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// (1.21.1 port) Small differences from 26.3 in one place.
public final class Compat {
    // 26.3's player.teleport(new TeleportTransition(level, pos, Vec3.ZERO, yaw, pitch, DO_NOTHING)).
    public static void teleport(Entity entity, ServerLevel level, Vec3 pos, float yaw, float pitch) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, pos.x, pos.y, pos.z, yaw, pitch);
        } else {
            entity.teleportTo(level, pos.x, pos.y, pos.z, java.util.Set.of(), yaw, pitch);
        }
    }

    private Compat() {}
}
