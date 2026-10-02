package com.ggrantrichards.astralend.block;

import com.ggrantrichards.astralend.ModBlockEntities;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;

// The End Chimes that are loaded, and how long each stays still after it last rang. Teleports are
// caught through Forge's teleport events (an Enderman's or Shulker's random teleport, an Ender Pearl,
// a Chorus Fruit): the first one within RANGE of a chime rings it; for the next 10 seconds any other
// is stopped.
public class EndChimesBlockEntity extends BlockEntity {
    public static final double RANGE = 16.0;
    public static final int STILL_TICKS = 200;
    private static final Map<ResourceKey<Level>, Set<EndChimesBlockEntity>> LOADED = new HashMap<>();
    private long stillUntil;

    public EndChimesBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.END_CHIMES.get(), pos, state);
    }

    public static void register() {
        EntityTeleportEvent.EntityRandom.BUS.addListener((java.util.function.Predicate<EntityTeleportEvent.EntityRandom>) EndChimesBlockEntity::onTeleport);
        EntityTeleportEvent.EnderPearl.BUS.addListener((java.util.function.Predicate<EntityTeleportEvent.EnderPearl>) EndChimesBlockEntity::onTeleport);
        EntityTeleportEvent.ChorusFruit.BUS.addListener((java.util.function.Predicate<EntityTeleportEvent.ChorusFruit>) EndChimesBlockEntity::onTeleport);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            synchronized (LOADED) {
                LOADED.computeIfAbsent(this.level.dimension(), k -> new HashSet<>()).add(this);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide()) {
            synchronized (LOADED) {
                Set<EndChimesBlockEntity> set = LOADED.get(this.level.dimension());
                if (set != null) {
                    set.remove(this);
                }
            }
        }
    }

    // Returns true to cancel the teleport.
    private static boolean onTeleport(EntityTeleportEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return false;
        }
        boolean stopped = stillsTeleport(level, event.getPrev(), event.getTarget());
        if (stopped && event.getEntity() instanceof net.minecraft.world.entity.monster.Enderman) {
            for (net.minecraft.server.level.ServerPlayer player : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,
                    event.getEntity().getBoundingBox().inflate(RANGE + 8.0))) {
                com.ggrantrichards.astralend.world.ModAdvancements.award(player, "still_air");
            }
        }
        return stopped;
    }

    // True if a chime near `from` or `to` is still from ringing (the teleport is stopped); otherwise
    // the nearest chime in range, if any, rings and goes still.
    public static boolean stillsTeleport(ServerLevel level, Vec3 from, Vec3 to) {
        long now = level.getGameTime();
        EndChimesBlockEntity ringing = null;
        synchronized (LOADED) {
            Set<EndChimesBlockEntity> set = LOADED.get(level.dimension());
            if (set == null) {
                return false;
            }
            for (EndChimesBlockEntity chime : set) {
                Vec3 c = Vec3.atCenterOf(chime.worldPosition);
                if (chime.isRemoved() || (c.distanceToSqr(from) > RANGE * RANGE && c.distanceToSqr(to) > RANGE * RANGE)) {
                    continue;
                }
                if (now < chime.stillUntil) {
                    level.playSound(null, chime.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.5F);
                    return true;
                }
                ringing = chime;
            }
        }
        if (ringing != null) {
            ringing.stillUntil = now + STILL_TICKS;
            level.playSound(null, ringing.worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0F, 0.7F);
            level.playSound(null, ringing.worldPosition, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        return false;
    }
}
