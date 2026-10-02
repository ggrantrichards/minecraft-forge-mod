package com.ggrantrichards.astralend.flare;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;

// /astralend flare [warning|flare|afterglow|calm]   (operators only)
// With no argument, says where the cycle is; with one, jumps straight to that phase.
public final class FlareCommand {
    public static void register() {
        RegisterCommandsEvent.BUS.addListener(event -> event.getDispatcher().register(
            Commands.literal("astralend").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("flare")
                    .executes(context -> status(context.getSource()))
                    .then(Commands.argument("phase", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (FlarePhase phase : FlarePhase.values()) {
                                builder.suggest(phase.name().toLowerCase(Locale.ROOT));
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> set(context.getSource(), StringArgumentType.getString(context, "phase")))))
                // Phase 7: call a live comet past you (or down onto an island near you).
                .then(Commands.literal("comet")
                    .executes(context -> comet(context.getSource(), false))
                    .then(Commands.literal("landfall").executes(context -> comet(context.getSource(), true))))));
    }

    private static ServerLevel end(CommandSourceStack source) {
        return source.getServer().getLevel(Level.END);
    }

    private static int status(CommandSourceStack source) {
        ServerLevel end = end(source);
        if (end == null) {
            return 0;
        }
        Sunstorms flares = Sunstorms.get(end);
        source.sendSuccess(() -> Component.literal("Sunstorms: " + flares.phase().name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    private static int set(CommandSourceStack source, String name) {
        ServerLevel end = end(source);
        FlarePhase phase;
        try {
            phase = FlarePhase.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal("Unknown phase: " + name));
            return 0;
        }
        if (end == null) {
            return 0;
        }
        Sunstorms.get(end).setPhase(end, phase, end.getRandom());
        source.sendSuccess(() -> Component.literal("Sunstorms: now " + name.toLowerCase(Locale.ROOT)), true);
        return 1;
    }

    private static int comet(CommandSourceStack source, boolean landfall) {
        ServerLevel end = end(source);
        if (end == null || source.getLevel() != end) {
            source.sendFailure(Component.literal("Comets only fly in the End."));
            return 0;
        }
        var comet = com.ggrantrichards.astralend.comet.CometShowers.send(end, source.getPosition(), end.getRandom(), landfall);
        if (comet == null) {
            source.sendFailure(Component.literal("No comet could be sent."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(comet.isLandfall() ? "A comet is coming down nearby." : "A comet is passing by."), true);
        return 1;
    }

    private FlareCommand() {}
}
