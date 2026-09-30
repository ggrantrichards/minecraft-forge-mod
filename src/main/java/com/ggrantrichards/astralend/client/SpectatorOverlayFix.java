package com.ggrantrichards.astralend.client;

import java.util.function.Predicate;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;

// Restores vanilla spectator mode: no block texture over your screen when you fly into a block.
//
// Vanilla 26.3 only draws that "inside a block" overlay when the player has physics
// (LevelExtractor.getViewBlockingState returns nothing if player.noPhysics, which spectators have).
// Forge 66.0.8's patch to LevelExtractor.extractPlayerState calls getOverlayBlock directly instead,
// skipping that check, so every spectator saw the block they were inside. This isn't caused by
// the mod, but the mod fixes it: for players without physics we cancel Forge's BlockOverlayEvent
// and hand it a renderer that draws nothing. (Cancelling alone isn't enough: with no custom
// renderer, Forge falls back to drawing the block.)
public final class SpectatorOverlayFix {
    public static void register() {
        // A Predicate listener: returning true cancels the event.
        RenderBlockScreenEffectEvent.BlockOverlayEvent.BUS.addListener(
            (Predicate<RenderBlockScreenEffectEvent.BlockOverlayEvent>) SpectatorOverlayFix::onBlockOverlay);
    }

    private static boolean onBlockOverlay(RenderBlockScreenEffectEvent.BlockOverlayEvent event) {
        if (!event.getPlayer().noPhysics) {
            return false;  // survival/creative: suffocating in a wall still shows the block, as in vanilla
        }
        event.setCustomRenderer((pose, collector) -> {});
        return true;
    }

    private SpectatorOverlayFix() {}
}
