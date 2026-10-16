package com.ggrantrichards.astralend.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.event.TickEvent;

// (1.21.1) Minecraft 1.21.1 calls any world whose world generation comes from data "experimental", and
// asks before creating one ("Experimental world settings") and before first opening it ("Worlds using
// Experimental Settings are not supported"). The mod's End is data, so every world with the mod would
// ask; this answers for the player - "Yes" / "I know what I'm doing!" - on those two screens only.
public final class ExperimentalWarning {
    public static void register() {
        com.ggrantrichards.astralend.Events.post(TickEvent.ClientTickEvent.class, event -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen instanceof BackupConfirmScreen && titled(screen, "selectWorld.backupQuestion.experimental")) {
                press(screen, Component.translatable("selectWorld.backupJoinSkipButton"));
            } else if (screen instanceof ConfirmScreen && titled(screen, "selectWorld.warning.experimental.title")) {
                press(screen, CommonComponents.GUI_YES);
            }
        });
    }

    private static boolean titled(Screen screen, String key) {
        return screen.getTitle().getContents() instanceof TranslatableContents contents && contents.getKey().equals(key);
    }

    private static void press(Screen screen, Component label) {
        String text = label.getString();
        for (var child : screen.children()) {
            if (child instanceof Button button && button.getMessage().getString().equals(text)) {
                button.onPress();
                return;
            }
        }
    }

    private ExperimentalWarning() {}
}
