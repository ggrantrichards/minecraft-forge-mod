package com.ggrantrichards.astralend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;

// (1.21.1 port) The automated runs - the screenshots, playtests and trailer - open the survey's world
// themselves from the title screen, as a player would (--quickPlaySingleplayer loads it during start-up,
// before Forge's events run, and its loot modifiers are never set up). And 1.21.1 warns before opening a
// world whose world generation comes from data ("Worlds using Experimental Settings are not supported"),
// which the mod's End is; they can't click, so they answer "I know what I'm doing!" here.
public final class SkipExperimentalWarning {
    private static boolean opened;
    private static int titleTicks;

    public static void register() {
        if (Boolean.getBoolean("astralend.screenshots") || Boolean.getBoolean("astralend.playtests")
                || Boolean.getBoolean("astralend.trailer")) {
            com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, event -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (!opened && minecraft.screen instanceof TitleScreen && ++titleTicks > 40) {
                    opened = true;
                    minecraft.createWorldOpenFlows().openWorld("survey-world", () -> minecraft.setScreen(new TitleScreen()));
                    return;
                }
                if (minecraft.screen instanceof BackupConfirmScreen screen) {
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
