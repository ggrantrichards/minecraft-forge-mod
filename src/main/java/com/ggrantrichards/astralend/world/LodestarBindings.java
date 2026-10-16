package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import com.ggrantrichards.astralend.compat.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

// Which Lodestar holds each player (saved with the End). Die in the End with one, and on respawning
// you're sent back to stand beside it - if it's still there.
public class LodestarBindings extends SavedData {
    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return TYPE.save(this, tag);
    }

    public static final Codec<LodestarBindings> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC)
        .xmap(LodestarBindings::new, b -> b.bound);
    public static final SavedDataType<LodestarBindings> TYPE = new SavedDataType<>(
        ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "lodestars"), LodestarBindings::new, CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final Map<UUID, BlockPos> bound;
    private static final Set<UUID> DIED_IN_END = new HashSet<>();

    public LodestarBindings() {
        this(new HashMap<>());
    }

    private LodestarBindings(Map<UUID, BlockPos> bound) {
        this.bound = new HashMap<>(bound);
    }

    public static LodestarBindings get(ServerLevel end) {
        return TYPE.get(end);
    }

    public void bind(ServerPlayer player, BlockPos pos) {
        this.bound.put(player.getUUID(), pos.immutable());
        this.setDirty();
    }

    public BlockPos boundTo(ServerPlayer player) {
        return this.bound.get(player.getUUID());
    }

    public static void register() {
        com.ggrantrichards.astralend.Events.cancelling(LivingDeathEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && player.level().dimension() == Level.END) {
                DIED_IN_END.add(player.getUUID());
            }
            return false;
        });
        com.ggrantrichards.astralend.Events.on(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && !event.isEndConquered() && DIED_IN_END.remove(player.getUUID())) {
                sendBack(player);
            }
        });
    }

    // Sends a respawned player to their Lodestar, if they have one and it still stands.
    public static boolean sendBack(ServerPlayer player) {
        ServerLevel end = player.level().getServer().getLevel(Level.END);
        if (end == null) {
            return false;
        }
        LodestarBindings bindings = get(end);
        BlockPos star = bindings.boundTo(player);
        if (star == null) {
            return false;
        }
        end.getChunk(star);
        if (!end.getBlockState(star).is(ModBlocks.LODESTAR.get())) {
            bindings.bound.remove(player.getUUID());
            bindings.setDirty();
            return false;
        }
        BlockPos stand = star.above();
        for (BlockPos p : BlockPos.betweenClosed(star.offset(-1, 0, -1), star.offset(1, 1, 1))) {
            if (end.getBlockState(p).isAir() && end.getBlockState(p.above()).isAir() && !end.getBlockState(p.below()).isAir()) {
                stand = p.immutable();
                break;
            }
        }
        if (player.connection == null) {     // (the survey's stand-in player has no connection)
            player.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), player.getXRot());
            return true;
        }
        com.ggrantrichards.astralend.compat.Compat.teleport(player, end, Vec3.atBottomCenterOf(stand), player.getYRot(), player.getXRot());
        return true;
    }
}
