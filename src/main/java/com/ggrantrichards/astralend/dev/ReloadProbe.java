package com.ggrantrichards.astralend.dev;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

// (1.21.1 port, temporary) Why the client's own server has no loot modifier manager: does Forge's
// reload event fire there, and is the manager set once the server has started?
public final class ReloadProbe {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        com.ggrantrichards.astralend.Events.on(AddReloadListenerEvent.class, event ->
            LOGGER.info("[AstralEnd probe] AddReloadListenerEvent fired ({} listeners so far)", event.getListeners().size()));
        com.ggrantrichards.astralend.Events.on(ServerStartedEvent.class, event -> {
            Object manager = null;
            try {
                var field = net.minecraftforge.common.ForgeInternalHandler.class.getDeclaredField("INSTANCE");
                field.setAccessible(true);
                manager = field.get(null);
            } catch (ReflectiveOperationException e) {
                LOGGER.info("[AstralEnd probe] no INSTANCE field: {}", e.toString());
            }
            LOGGER.info("[AstralEnd probe] server started ({}); loot modifier manager {}",
                event.getServer().isDedicatedServer() ? "dedicated" : "integrated", manager);
        });
    }

    private ReloadProbe() {}
}
