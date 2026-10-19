package com.ggrantrichards.astralend.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
import org.slf4j.Logger;

// Hides the HUD for filming, as F1 does (and shows it again, for the fight). In 26.3 the switch lives in the Hud that Gui holds (Hud.toggle());
// Gui has no getter for it, so it's found by type.
final class TrailerHud {
    private static final Logger LOGGER = LogUtils.getLogger();

    static void hide(Minecraft minecraft) {
        set(minecraft, true);
    }

    static void show(Minecraft minecraft) {
        set(minecraft, false);
    }

    private static void set(Minecraft minecraft, boolean hidden) {
        try {
            for (Field field : minecraft.gui.getClass().getDeclaredFields()) {
                if (Hud.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Hud hud = (Hud) field.get(minecraft.gui);
                    if (hud.isHidden() != hidden) {
                        hud.toggle();
                    }
                    return;
                }
            }
            LOGGER.warn("[AstralEnd trailer] couldn't find the HUD to hide it");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("[AstralEnd trailer] couldn't hide the HUD", e);
        }
    }

    private TrailerHud() {}
}
