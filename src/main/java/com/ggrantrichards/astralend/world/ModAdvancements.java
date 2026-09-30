package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

// Advancements that the game can't detect on its own (seeing a Drift Whale, catching the Watcher
// in a spyglass...). Their JSON uses the "impossible" trigger; the mob's code grants them here.
public final class ModAdvancements {
    public static void award(ServerPlayer player, String name) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder advancement = server.getAdvancements().get(Identifier.fromNamespaceAndPath(AstralEnd.MODID, "end/" + name));
        if (advancement != null && !player.getAdvancements().getOrStartProgress(advancement).isDone()) {
            player.getAdvancements().award(advancement, "done");
        }
    }

    private ModAdvancements() {}
}
