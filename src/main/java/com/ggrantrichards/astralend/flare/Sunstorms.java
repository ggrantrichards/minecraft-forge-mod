package com.ggrantrichards.astralend.flare;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModEffects;
import com.ggrantrichards.astralend.block.FlarebloomBlock;
import com.ggrantrichards.astralend.block.SunscaleLichenBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import com.ggrantrichards.astralend.compat.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

// Sunstorms: the Eclipse's star - the white star with six long spikes over it - flares every 15-25
// minutes. Only the Eclipse feels it (after beta.4: the golden meadow; it was the Corona, the hell half
// that's gone): the one living, lit place in the End, and its light can turn on you. Everywhere else a
// flare is just a glow in the sky. The giant Lumineshrooms' caps are the meadow's shelter.
//
//   WARNING   ~1 min    the star brightens and a hum rises; there's always time to get under cover
//   FLARE     1-2 min   open sky in the Eclipse burns (Sunburn); Flareblooms open; Void Rays take off;
//                       Void Crystal and Flare Bottles on Collector Pedestals charge
//   AFTERGLOW ~2 min    Sunscale Lichen glows bright, Void Ray wings refill
//
// One timer for the whole End, saved with the world. Clients hear about it through FlareNetwork.
public class Sunstorms extends SavedData {
    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return TYPE.save(this, tag);
    }

    public static final int WARNING_TICKS = 1200;
    public static final int FLARE_MIN_TICKS = 1200;
    public static final int FLARE_MAX_TICKS = 2400;
    public static final int AFTERGLOW_TICKS = 2400;
    public static final int CALM_MIN_TICKS = 18000;
    public static final int CALM_MAX_TICKS = 30000;
    public static final ResourceKey<DamageType> SUNBURN = ResourceKey.create(Registries.DAMAGE_TYPE,
        ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "sunburn"));

    public static final Codec<Sunstorms> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("phase").forGetter(f -> f.phase.ordinal()),
        Codec.INT.fieldOf("ticks_left").forGetter(f -> f.ticksLeft),
        Codec.INT.fieldOf("phase_length").forGetter(f -> f.phaseLength),
        Codec.FLOAT.fieldOf("wind_yaw").forGetter(f -> f.windYaw)
    ).apply(instance, Sunstorms::new));
    public static final SavedDataType<Sunstorms> TYPE = new SavedDataType<>(
        ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "sunstorms"), Sunstorms::new, CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);   // nothing to upgrade yet; any saved-data type will do

    private FlarePhase phase;
    private int ticksLeft;
    private int phaseLength;
    private float windYaw;

    // Per player, not saved: how long they've been exposed this flare.
    private final Map<UUID, Integer> exposure = new HashMap<>();

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
        com.ggrantrichards.astralend.Events.post(TickEvent.LevelTickEvent.class, Sunstorms::onLevelTick);
    }

    public static Sunstorms get(ServerLevel end) {
        return TYPE.get(end);
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

    // Where Sunstorms are felt: biomes tagged #astralend:sunstorms (the Eclipse). An empty tag
    // in a data pack turns flares off.
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> FLARE_BIOMES = net.minecraft.tags.TagKey.create(
        Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "sunstorms"));

    public static boolean inSunstorms(Level level, BlockPos pos) {
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

    private static void onLevelTick(TickEvent.LevelTickEvent event) {
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
            this.wakeTheMeadow(level);
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

    // Sounds only (no words), for players in the Eclipse; everyone else just sees the sky.
    private void announce(ServerLevel level, FlarePhase next) {
        for (ServerPlayer player : level.players()) {
            if (!inSunstorms(level, player.blockPosition())) {
                continue;
            }
            switch (next) {
                case WARNING -> {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.AMBIENT, 1.0F, 0.5F);
                }
                case FLARE -> {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 0.8F, 0.5F);
                }
                case AFTERGLOW -> {
                    com.ggrantrichards.astralend.world.ModAdvancements.award(player, "weather_the_storm");
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.0F, 0.6F);
                }
                default -> {
                }
            }
        }
    }

    private void syncAll(ServerLevel level, boolean force) {
        for (ServerPlayer player : level.players()) {
            FlareNetwork.send(player, new FlareState(this.phase, this.ticksLeft, this.phaseLength, this.windYaw));
        }
    }

    // ------------------------------------------------------------------ Sunburn

    // The Solar Array's old mirrors still focus the flares onto its field (Phase 6).
    public static final ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> SOLAR_ARRAY = ResourceKey.create(
        Registries.STRUCTURE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "solar_array"));

    // Once a second during a flare: everyone in the Eclipse under open sky burns - half a heart every
    // 3 seconds at first, a full heart after 30 seconds of exposure, and twice that on a Solar Array's
    // field. Cover, a Sunshade or Sun Ward stops it.
    private void burn(ServerLevel level) {
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SUNBURN));
        for (ServerPlayer player : level.players()) {
            UUID id = player.getUUID();
            if (player.isSpectator() || player.isCreative() || !inSunstorms(level, player.blockPosition())
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

    // ------------------------------------------------------------------ the meadow responds

    // Opens Flareblooms and charges Sunscale Lichen around players in the Eclipse: a few dozen random
    // spots per tick, so a whole area wakes up within seconds. (They close and fade again on their own
    // random ticks once the flare is over.)
    private void wakeTheMeadow(ServerLevel level) {
        RandomSource random = level.getRandom();
        for (ServerPlayer player : level.players()) {
            if (!inSunstorms(level, player.blockPosition())) {
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
}
