package com.ggrantrichards.astralend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.event.TickEvent;

// (1.21.1 port) The automated runs - the screenshots, playtests and trailer - open the survey's world
// themselves from the title screen, as a player would (--quickPlaySingleplayer loads it during start-up,
// before Forge's events run, and its loot modifiers are never set up). (The experimental-settings
// question is answered for everyone: client/ExperimentalWarning.)
public final class AutomatedWorldOpen {
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
            });
        }
    }

    private AutomatedWorldOpen() {}
}
