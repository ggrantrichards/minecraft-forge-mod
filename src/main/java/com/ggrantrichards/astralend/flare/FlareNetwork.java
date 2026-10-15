package com.ggrantrichards.astralend.flare;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

// One small packet: the server tells each player in the End where the flare cycle is and which way its
// wind blows. Clients use it for the sky, solar wind and flare particles (ClientFlares).
public final class FlareNetwork {
    public static final SimpleChannel CHANNEL = ChannelBuilder.named(Identifier.fromNamespaceAndPath(AstralEnd.MODID, "flares"))
        .networkProtocolVersion(1)
        .simpleChannel();

    public static void register() {
        CHANNEL.messageBuilder(FlareState.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(FlareState::encode)
            .decoder(FlareState::decode)
            .consumerMainThread((state, context) -> ClientFlares.receive(state))
            .add();
        CHANNEL.build();
    }

    public static void send(ServerPlayer player, FlareState state) {
        CHANNEL.send(state, PacketDistributor.PLAYER.with(player));
    }

    private FlareNetwork() {}
}
