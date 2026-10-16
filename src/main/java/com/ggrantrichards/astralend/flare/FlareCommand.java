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
        com.ggrantrichards.astralend.Events.on(RegisterCommandsEvent.class, event -> event.getDispatcher().register(
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
                        .executes(context -> set(context.getSource(), StringArgumentType.getString(context, "phase")))))));
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

    private FlareCommand() {}
}
