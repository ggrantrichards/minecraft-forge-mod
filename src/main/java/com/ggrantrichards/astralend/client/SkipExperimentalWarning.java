package com.ggrantrichards.astralend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;

// (1.21.1 port) 1.21.1 warns before opening a world whose world generation comes from data ("Worlds
// using Experimental Settings are not supported"), which the mod's End is. The automated runs - the
// screenshots, playtests and trailer - can't click, so they answer "I know what I'm doing!" here.
public final class SkipExperimentalWarning {
    public static void register() {
        if (Boolean.getBoolean("astralend.screenshots") || Boolean.getBoolean("astralend.playtests")
                || Boolean.getBoolean("astralend.trailer")) {
            com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, event -> {
                if (Minecraft.getInstance().screen instanceof BackupConfirmScreen screen) {
                    String skip = Component.translatable("selectWorld.backupJoinSkipButton").getString();
                    for (var child : screen.children()) {
                        if (child instanceof Button button && button.getMessage().getString().equals(skip)) {
                            button.onPress();
                            return;
                        }
                    }
                }
            });
        }
    }

    private SkipExperimentalWarning() {}
}
