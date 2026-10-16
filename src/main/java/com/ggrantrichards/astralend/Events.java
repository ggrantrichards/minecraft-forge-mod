package com.ggrantrichards.astralend;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.IModBusEvent;

// (1.21.1 port) Listening to events the way the 26.3 build's EventBus 7 does: one call per listener,
// the bus chosen from the event (the mod's bus for loading events, Forge's for game events), a
// listener that returns true cancels its event, and "post" listeners for the end of a tick.
public final class Events {
    private static IEventBus modBus;

    static void setModBus(IEventBus bus) {
        modBus = bus;
    }

    public static IEventBus modBus() {
        return modBus;
    }

    private static IEventBus busFor(Class<?> event) {
        return IModBusEvent.class.isAssignableFrom(event) ? modBus : MinecraftForge.EVENT_BUS;
    }

    public static <T extends Event> void on(Class<T> event, Consumer<T> listener) {
        busFor(event).addListener(EventPriority.NORMAL, false, event, listener);
    }

    // A listener that cancels the event when it returns true.
    public static <T extends Event> void cancelling(Class<T> event, Predicate<T> listener) {
        busFor(event).addListener(EventPriority.NORMAL, false, event, e -> {
            if (listener.test(e)) {
                e.setCanceled(true);
            }
        });
    }

    // A tick listener run once per tick, at its end (26.3's TickEvent.*.Post).
    public static <T extends TickEvent> void post(Class<T> event, Consumer<T> listener) {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, event, e -> {
            if (e.phase == TickEvent.Phase.END) {
                listener.accept(e);
            }
        });
    }

    private Events() {}
}
