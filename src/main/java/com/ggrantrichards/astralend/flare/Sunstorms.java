package com.ggrantrichards.astralend.flare;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModEffects;
import com.ggrantrichards.astralend.block.FlarebloomBlock;
import com.ggrantrichards.astralend.block.SunscaleLichenBlock;
import com.ggrantrichards.astralend.entity.CinderMite;
import com.ggrantrichards.astralend.entity.ModEntities;
import com.ggrantrichards.astralend.world.ModBiomes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

// Sunstorms: the Remnant (the dying ember of the collapsed star) flares every 15-25 minutes.
// Only the Corona feels it - everywhere else a flare is just a glow on the horizon.
//
//   WARNING   ~1 min    the Remnant brightens and a hum rises; there's always time to get under cover
//   FLARE     1-2 min   open sky in the Corona burns (Sunburn); Flareblooms open; Void Rays take off;
//                       Void Crystal and Flare Bottles on Collector Pedestals charge
//   AFTERGLOW ~2 min    Sunscale Lichen glows bright, Void Ray wings refill, Cinder Mites swarm out
//
// One timer for the whole End, saved with the world. Clients hear about it through FlareNetwork.
public class Sunstorms extends SavedData {
    public static final int WARNING_TICKS = 1200;
    public static final int FLARE_MIN_TICKS = 1200;
    public static final int FLARE_MAX_TICKS = 2400;
    public static final int AFTERGLOW_TICKS = 2400;
    public static final int CALM_MIN_TICKS = 18000;
    public static final int CALM_MAX_TICKS = 30000;
    public static final ResourceKey<DamageType> SUNBURN = ResourceKey.create(Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "sunburn"));

    public static final Codec<Sunstorms> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("phase").forGetter(f -> f.phase.ordinal()),
        Codec.INT.fieldOf("ticks_left").forGetter(f -> f.ticksLeft),
        Codec.INT.fieldOf("phase_length").forGetter(f -> f.phaseLength),
        Codec.FLOAT.fieldOf("wind_yaw").forGetter(f -> f.windYaw)
    ).apply(instance, Sunstorms::new));
    public static final SavedDataType<Sunstorms> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "sunstorms"), Sunstorms::new, CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);   // nothing to upgrade yet; any saved-data type will do

    private FlarePhase phase;
    private int ticksLeft;
    private int phaseLength;
    private float windYaw;

    // Per player, not saved: how long they've been exposed this flare, and where their Remnant hangs.
    private final Map<UUID, Integer> exposure = new HashMap<>();
    private final Map<UUID, BlockPos> remnants = new HashMap<>();
    private final Map<UUID, BlockPos> remnantSearchedFrom = new HashMap<>();

    public Sunstorms() {
        this(FlarePhase.CALM.ordinal(), 6000, 6000, 0.0F);   // a new world's first flare comes after 5 minutes
    }

    private Sunstorms(int phase, int ticksLeft, int phaseLength, float windYaw) {
        this.phase = FlarePhase.byId(phase);
        this.ticksLeft = ticksLeft;
        this.phaseLength = phaseLength;
        this.windYaw = windYaw;
    }

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(Sunstorms::onLevelTick);
    }

    public static Sunstorms get(ServerLevel end) {
        return end.getDataStorage().computeIfAbsent(TYPE);
    }

    // The current phase in this level: always CALM outside the End.
    public static FlarePhase phaseIn(Level level) {
        if (level.dimension() != Level.END) {
            return FlarePhase.CALM;
        }
        if (level instanceof ServerLevel server) {
            return get(server).phase;
        }
        return ClientFlares.phase();
    }

    // Where Sunstorms are felt: biomes tagged #astralend:sunstorms (the Corona). An empty tag
    // in a data pack turns flares off.
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> FLARE_BIOMES = net.minecraft.tags.TagKey.create(
        Registries.BIOME, net.minecraft.resources.Identifier.fromNamespaceAndPath(AstralEnd.MODID, "sunstorms"));

    public static boolean isCorona(Level level, BlockPos pos) {
        return level.dimension() == Level.END && level.getBiome(pos).is(FLARE_BIOMES);
    }

    // Is nothing solid between this spot and the sky? Glass and Sunshades count as cover; plants don't.
    public static boolean underOpenSky(Level level, BlockPos pos) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
        if (pos.getY() >= top) {
            return true;
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int y = pos.getY(); y < top; y++) {
            cursor.setY(y);
            BlockState state = level.getBlockState(cursor);
            if (state.is(ModBlocks.SUNSHADE.get()) || !state.getCollisionShape(level, cursor).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public FlarePhase phase() {
        return this.phase;
    }

    // ------------------------------------------------------------------ the cycle

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.side() != LogicalSide.SERVER || !(event.level() instanceof ServerLevel level) || level.dimension() != Level.END) {
            return;
        }
        get(level).tick(level);
    }

    private void tick(ServerLevel level) {
        if (--this.ticksLeft <= 0) {
            this.advance(level, level.getRandom());
        }
        if (this.ticksLeft % 20 == 0) {
            this.setDirty();
        }
        long time = level.getGameTime();
        if (time % 40 == 0) {
            this.syncAll(level, false);
        }
        if (this.phase == FlarePhase.FLARE && time % 20 == 0) {
            this.burn(level);
        }
        if (this.phase == FlarePhase.FLARE || this.phase == FlarePhase.AFTERGLOW) {
            this.wakeTheCorona(level);
        }
        if (this.phase == FlarePhase.AFTERGLOW && time % 300 == 0) {
            this.releaseMites(level);
        }
    }

    // Moves to the next phase (from a command too: /astralend flare).
    public void advance(ServerLevel level, RandomSource random) {
        FlarePhase next = switch (this.phase) {
            case CALM -> FlarePhase.WARNING;
            case WARNING -> FlarePhase.FLARE;
            case FLARE -> FlarePhase.AFTERGLOW;
            case AFTERGLOW -> FlarePhase.CALM;
        };
        this.setPhase(level, next, random);
    }

    public void setPhase(ServerLevel level, FlarePhase next, RandomSource random) {
        this.phase = next;
        this.phaseLength = switch (next) {
            case CALM -> CALM_MIN_TICKS + random.nextInt(CALM_MAX_TICKS - CALM_MIN_TICKS);
            case WARNING -> WARNING_TICKS;
            case FLARE -> FLARE_MIN_TICKS + random.nextInt(FLARE_MAX_TICKS - FLARE_MIN_TICKS);
            case AFTERGLOW -> AFTERGLOW_TICKS;
        };
        this.ticksLeft = this.phaseLength;
        if (next == FlarePhase.WARNING) {
            this.windYaw = random.nextFloat() * 360.0F;
        }
        if (next != FlarePhase.FLARE) {
            this.exposure.clear();
        }
        this.setDirty();
        this.announce(level, next);
        this.syncAll(level, true);
    }

    // Sounds and a line above the hotbar, only for players in the Corona; everyone else just sees the sky.
    private void announce(ServerLevel level, FlarePhase next) {
        for (ServerPlayer player : level.players()) {
            if (!isCorona(level, player.blockPosition())) {
                continue;
            }
            switch (next) {
                case WARNING -> {
                    actionBar(player, Component.translatable("flare.astralend.warning").withStyle(ChatFormatting.GOLD));
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 1.0F, 0.5F);
                }
                case FLARE -> {
                    actionBar(player, Component.translatable("flare.astralend.flare").withStyle(ChatFormatting.RED));
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 0.8F, 0.5F);
                }
                case AFTERGLOW -> {
                    actionBar(player, Component.translatable("flare.astralend.afterglow").withStyle(ChatFormatting.YELLOW));
                    com.ggrantrichards.astralend.world.ModAdvancements.award(player, "weather_the_storm");
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.0F, 0.6F);
                }
                default -> {
                }
            }
        }
    }

    private static void actionBar(ServerPlayer player, Component message) {
        player.connection.send(new ClientboundSetActionBarTextPacket(message));
    }

    private void syncAll(ServerLevel level, boolean force) {
        for (ServerPlayer player : level.players()) {
            BlockPos remnant = this.remnantFor(level, player, force);
            FlareNetwork.send(player, new FlareState(this.phase, this.ticksLeft, this.phaseLength, this.windYaw,
                remnant != null, remnant != null ? remnant.getX() : 0, remnant != null ? remnant.getY() : 0, remnant != null ? remnant.getZ() : 0));
        }
    }

    // ------------------------------------------------------------------ Sunburn

    // The Solar Array's old mirrors still focus the flares onto its field (Phase 6).
    public static final ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> SOLAR_ARRAY = ResourceKey.create(
        Registries.STRUCTURE, net.minecraft.resources.Identifier.fromNamespaceAndPath(AstralEnd.MODID, "solar_array"));

    // Once a second during a flare: everyone in the Corona under open sky burns - half a heart every
    // 3 seconds at first, a full heart after 30 seconds of exposure, and twice that on a Solar Array's
    // field. Cover, a Sunshade or Sun Ward stops it.
    private void burn(ServerLevel level) {
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SUNBURN));
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            if (player.isSpectator() || player.isCreative() || !isCorona(level, player.blockPosition())
                || player.hasEffect(ModEffects.SUN_WARD.getHolder().orElseThrow())
                || com.ggrantrichards.astralend.starforge.StarforgeEffects.flareProof(player)
                || !underOpenSky(level, BlockPos.containing(player.getEyePosition()))) {
                this.exposure.remove(id);
                continue;
            }
            int seconds = this.exposure.merge(id, 1, Integer::sum);
            if (seconds % 3 == 0) {
                float damage = seconds >= 30 ? 2.0F : 1.0F;
                if (level.structureManager().getStructureWithPieceAt(player.blockPosition(), holder -> holder.is(SOLAR_ARRAY)).isValid()) {
                    damage *= 2.0F;
                }
                player.hurtServer(level, source, damage);
            }
        }
    }

    // ------------------------------------------------------------------ the Corona responds

    // Opens Flareblooms and charges Sunscale Lichen around players in the Corona: a few dozen random
    // spots per tick, so a whole area wakes up within seconds. (They close and fade again on their own
    // random ticks once the flare is over.)
    private void wakeTheCorona(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (!isCorona(level, player.blockPosition())) {
                continue;
            }
            for (int i = 0; i < 48; i++) {
                int x = Mth.floor(player.getX()) + random.nextInt(81) - 40;
                int z = Mth.floor(player.getZ()) + random.nextInt(81) - 40;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int dy = 1; dy <= 2; dy++) {
                    BlockPos pos = new BlockPos(x, top - dy, z);
                    BlockState state = level.getBlockState(pos);
                    if (this.phase == FlarePhase.FLARE && state.getBlock() instanceof FlarebloomBlock) {
                        FlarebloomBlock.setOpen(level, pos, state, true);
                    } else if (state.getBlock() instanceof SunscaleLichenBlock && !state.getValue(SunscaleLichenBlock.CHARGED)) {
                        level.setBlock(pos, state.setValue(SunscaleLichenBlock.CHARGED, true), 2);
                    }
                }
            }
        }
    }

    // In the afterglow, Cinder Mites come up out of the stone near players in the Corona, in swarms.
    private void releaseMites(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative() || !isCorona(level, player.blockPosition())
                || level.getEntitiesOfClass(CinderMite.class, new AABB(player.blockPosition()).inflate(32.0)).size() >= 8) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 10.0 + random.nextDouble() * 10.0;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            if (y <= level.getMinY() + 1 || !isCorona(level, at)) {
                continue;
            }
            int count = 3 + random.nextInt(2);
            for (int i = 0; i < count; i++) {
                CinderMite mite = ModEntities.CINDER_MITE.get().create(level, EntitySpawnReason.EVENT);
                if (mite == null) {
                    continue;
                }
                mite.snapTo(x + 0.5 + random.nextDouble() - 0.5, y, z + 0.5 + random.nextDouble() - 0.5, random.nextFloat() * 360.0F, 0.0F);
                if (level.noCollision(mite)) {
                    mite.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
                    level.addFreshEntity(mite);
                }
            }
        }
    }

    // ------------------------------------------------------------------ where the Remnant hangs

    // The Remnant hangs over the nearest Corona: the nearest Corona is found from the biome layout
    // itself (no chunks are generated), searching outward in rings, then the Remnant structure nearest
    // to it - a real place, so the sky shows it where it truly is. Looked up again only when the player
    // has moved far.
    private BlockPos remnantFor(ServerLevel level, ServerPlayer player, boolean force) {
        UUID id = player.getUUID();
        BlockPos here = player.blockPosition();
        BlockPos from = this.remnantSearchedFrom.get(id);
        if (!force && from != null && from.distSqr(here) < 256.0 * 256.0) {
            return this.remnants.get(id);
        }
        BlockPos found = nearestRemnant(level, here);
        this.remnantSearchedFrom.put(id, here);
        if (found != null) {
            this.remnants.put(id, found);
        } else {
            this.remnants.remove(id);
        }
        return found;
    }

    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> REMNANT =
        net.minecraft.tags.TagKey.create(Registries.STRUCTURE, net.minecraft.resources.Identifier.fromNamespaceAndPath(AstralEnd.MODID, "remnant"));

    // The Remnant over the Corona nearest `from` (its middle), or the middle of that Corona's sky if
    // there's no Remnant built for it.
    public static BlockPos nearestRemnant(ServerLevel level, BlockPos from) {
        BlockPos corona = nearestCorona(level, from);
        if (corona == null) {
            return null;
        }
        int y = level.getMinY() + com.ggrantrichards.astralend.world.structure.RemnantStructure.HEIGHT + 5;
        BlockPos remnant = level.findNearestMapStructure(REMNANT, corona, 4, false);
        return remnant != null ? new BlockPos(remnant.getX(), y, remnant.getZ()) : new BlockPos(corona.getX(), y, corona.getZ());
    }

    // Also used by the worldgen survey, to aim its screenshot of the Remnant.
    public static BlockPos nearestCorona(ServerLevel level, BlockPos from) {
        BiomeResolver biomes = level.getChunkSource().getGenerator().getBiomeSource()
            .createUncachedResolver(level.getChunkSource().randomState());
        int step = 128;
        int qy = QuartPos.fromBlock(64);
        for (int ring = 0; ring <= 40; ring++) {
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    int x = from.getX() + dx * step;
                    int z = from.getZ() + dz * step;
                    if (biomes.getNoiseBiome(QuartPos.fromBlock(x), qy, QuartPos.fromBlock(z)).is(ModBiomes.CORONA)) {
                        double d = (double) dx * dx + (double) dz * dz;
                        if (d < bestDistance) {
                            bestDistance = d;
                            best = new BlockPos(x, 64, z);
                        }
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }
}
