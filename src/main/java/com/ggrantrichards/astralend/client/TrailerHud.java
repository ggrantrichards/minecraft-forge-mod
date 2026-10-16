package com.ggrantrichards.astralend.client;

import net.minecraft.client.Minecraft;

// Hides the HUD for filming, as F1 does.
final class TrailerHud {
    static void hide(Minecraft minecraft) {
        minecraft.options.hideGui = true;
    }

    private TrailerHud() {}
}
