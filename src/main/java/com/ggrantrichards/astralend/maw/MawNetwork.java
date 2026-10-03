package com.ggrantrichards.astralend.maw;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

// The star road: every Rift Maw that has been woken, and which of them each player has woken (or
// attuned to). Walking into a woken Maw's throat sends you on to the next Maw on your own list, for
// an ender pearl or four chorus fruit, and you're spat out of that Maw's mouth.
public class MawNetwork extends SavedData {
    // A woken Maw: the middle of its throat, where travellers come out, and which way its mouth faces.
    public record Maw(BlockPos throat, BlockPos exit, int facing) {
        static final Codec<Maw> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("throat").forGetter(Maw::throat),
            BlockPos.CODEC.fieldOf("exit").forGetter(Maw::exit),
            Codec.INT.fieldOf("facing").forGetter(Maw::facing)
        ).apply(i, Maw::new));

        Direction direction() {
            return Direction.from2DDataValue(this.facing);
        }
    }

    public static final Codec<MawNetwork> CODEC = RecordCodecBuilder.create(i -> i.group(
        Maw.CODEC.listOf().fieldOf("maws").forGetter(n -> n.maws),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, BlockPos.CODEC.listOf()).fieldOf("known").forGetter(n -> n.known)
    ).apply(i, MawNetwork::new));
    public static final SavedDataType<MawNetwork> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "maw_network"), MawNetwork::new, CODEC,
        DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private static final int MAX_THROAT = 1200;
    private static final int COOLDOWN = 80;

    private final List<Maw> maws;
    private final Map<UUID, List<BlockPos>> known;          // per player: the throats of the Maws they woke, in order
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public MawNetwork() {
        this(List.of(), Map.of());
    }

    private MawNetwork(List<Maw> maws, Map<UUID, List<BlockPos>> known) {
        this.maws = new ArrayList<>(maws);
        this.known = new HashMap<>();
        known.forEach((k, v) -> this.known.put(k, new ArrayList<>(v)));
    }

    public static MawNetwork get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(MawNetwork::onLevelTick);
    }

    public List<Maw> maws() {
        return List.copyOf(this.maws);
    }

    // ------------------------------------------------------------------ waking

    // Wakes the Maw whose throat includes `pos`, if all four of its eyes are lit. `player` may be null
    // (the survey wakes Maws to test them). Returns the woken Maw, or null.
    public static Maw wake(ServerLevel level, BlockPos pos, ServerPlayer player) {
        int lit = 0;
        int sockets = 0;
        long eyeX = 0, eyeZ = 0;
        List<BlockPos> dark = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-44, -24, -44), pos.offset(44, 32, 44))) {     // the eyes are high on the cranium
            BlockState state = level.getBlockState(p);
            if (state.is(ModBlocks.MAW_EYE.get())) {
                sockets++;
                eyeX += p.getX();
                eyeZ += p.getZ();
                if (state.getValue(MawEyeBlock.LIT)) {
                    lit++;
                } else {
                    dark.add(p.immutable());
                }
            }
        }
        if (lit < 4) {
            if (player != null) {
                // No words: the throat gives a hollow knock, and every dark socket breathes out smoke.
                level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.5F, 0.5F);
                for (BlockPos eye : dark) {
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, eye.getX() + 0.5, eye.getY() + 0.5, eye.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.02);
                }
            }
            return null;
        }
        // The throat: every connected block of it becomes the wormhole.
        List<BlockPos> throat = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos.immutable());
        seen.add(pos.immutable());
        while (!queue.isEmpty() && throat.size() < MAX_THROAT) {
            BlockPos p = queue.poll();
            if (!level.getBlockState(p).is(ModBlocks.MAW_THROAT.get())) {
                continue;
            }
            throat.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) {
                    queue.add(n);
                }
            }
        }
        long x = 0, y = 0, z = 0;
        for (BlockPos p : throat) {
            level.setBlock(p, ModBlocks.WORMHOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        BlockPos centre = new BlockPos((int) (x / throat.size()), (int) (y / throat.size()), (int) (z / throat.size()));
        // The mouth opens on the side of the throat away from the eyes (which sit in the cranium behind it).
        double dx = centre.getX() + 0.5 - (double) eyeX / sockets;
        double dz = centre.getZ() + 0.5 - (double) eyeZ / sockets;
        Direction mouth = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? Direction.EAST : Direction.WEST)
            : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos exit = standingSpot(level, centre.relative(mouth, 9));
        Maw maw = new Maw(centre, exit, mouth.get2DDataValue());
        MawNetwork network = get(level);
        network.maws.removeIf(m -> m.throat().closerThan(centre, 16));
        network.maws.add(maw);
        if (player != null) {
            network.learn(player, maw);
            com.ggrantrichards.astralend.world.ModAdvancements.award(player, "say_ahh");
        }
        network.setDirty();
        level.playSound(null, centre, SoundEvents.RAVAGER_ROAR, SoundSource.BLOCKS, 4.0F, 0.4F);
        level.playSound(null, centre, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 2.0F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5,
            200, 3.0, 3.0, 3.0, 0.2);
        // Woken, it gives up one Rift Pearl - Echo Strike's material - from its throat to the exit.
        net.minecraft.world.entity.item.ItemEntity pearl = new net.minecraft.world.entity.item.ItemEntity(level,
            exit.getX() + 0.5, exit.getY() + 1.0, exit.getZ() + 0.5, new net.minecraft.world.item.ItemStack(com.ggrantrichards.astralend.ModItems.RIFT_PEARL.get()));
        pearl.setNoPickUpDelay();
        level.addFreshEntity(pearl);
        return maw;
    }

    // Someone else's woken Maw: a Portal Lens on its wormhole adds it to your own road.
    static void attune(ServerLevel level, BlockPos pos, ServerPlayer player) {
        MawNetwork network = get(level);
        Maw maw = network.nearest(pos);
        if (maw == null) {
            return;
        }
        if (network.learn(player, maw)) {
            network.setDirty();
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.5F);
        } else {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0F, 1.4F);
        }
    }

    private boolean learn(ServerPlayer player, Maw maw) {
        List<BlockPos> list = this.known.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        if (list.contains(maw.throat())) {
            return false;
        }
        list.add(maw.throat());
        return true;
    }

    private Maw nearest(BlockPos pos) {
        Maw best = null;
        for (Maw m : this.maws) {
            if (m.throat().closerThan(pos, 24) && (best == null || m.throat().distSqr(pos) < best.throat().distSqr(pos))) {
                best = m;
            }
        }
        return best;
    }

    // Ground to stand on near `pos`: the first spot, searching down then up, with two blocks of air
    // over something solid.
    private static BlockPos standingSpot(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy <= 12; dy++) {
            for (int sign : new int[] {-1, 1}) {
                BlockPos p = pos.above(sign * dy);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                    && level.getBlockState(p.below()).isSolid()) {
                    return p;
                }
            }
        }
        return pos;
    }

    // ------------------------------------------------------------------ travel

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || level.dimension() != Level.END || level.getGameTime() % 2 != 0) {
            return;
        }
        MawNetwork network = null;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos feet = player.blockPosition();
            if (!level.getBlockState(feet).is(ModBlocks.WORMHOLE.get())
                && !level.getBlockState(BlockPos.containing(player.getEyePosition())).is(ModBlocks.WORMHOLE.get())) {
                continue;
            }
            if (network == null) {
                network = get(level);
            }
            network.travel(level, player);
        }
    }

    private void travel(ServerLevel level, ServerPlayer player) {
        long now = level.getGameTime();
        if (now < this.cooldowns.getOrDefault(player.getUUID(), 0L)) {
            return;
        }
        this.cooldowns.put(player.getUUID(), now + COOLDOWN);
        Maw here = this.nearest(player.blockPosition());
        if (here == null) {
            return;
        }
        List<BlockPos> road = this.known.getOrDefault(player.getUUID(), List.of());
        int index = road.indexOf(here.throat());
        Maw there = null;
        if (index >= 0 && road.size() > 1) {
            for (int step = 1; step < road.size() && there == null; step++) {
                BlockPos next = road.get((index + step) % road.size());
                there = this.maws.stream().filter(m -> m.throat().equals(next)).findFirst().orElse(null);
            }
        }
        if (there == null) {
            spit(player, here, index < 0 ? Refusal.STRANGER : Refusal.NOWHERE);
            return;
        }
        if (!player.isCreative() && !pay(player.getInventory())) {
            spit(player, here, Refusal.UNPAID);
            return;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1.0, 0.5, 0.1);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.5F);
        Direction out = there.direction();
        Vec3 to = Vec3.atBottomCenterOf(there.exit());
        player.teleport(new TeleportTransition(level, to, Vec3.ZERO, out.toYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        player.setDeltaMovement(out.getStepX() * 0.9, 0.45, out.getStepZ() * 0.9);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        player.resetFallDistance();
        this.cooldowns.put(player.getUUID(), now + COOLDOWN);
        level.playSound(null, there.exit(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0F, 0.6F);
        level.playSound(null, there.exit(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 80, 0.6, 1.0, 0.6, 0.2);
    }

    // Why a Maw turns you away. There are no words: each has its own sound, and you learn them.
    private enum Refusal { STRANGER, NOWHERE, UNPAID }

    // Turned away: pushed back out of the mouth. A stranger gets a growl and smoke; a Maw with no other
    // Maw to send you to, a hollow empty knock; one that wants its fare, a hungry snap of the jaw.
    private static void spit(ServerPlayer player, Maw maw, Refusal reason) {
        Direction out = maw.direction();
        player.setDeltaMovement(out.getStepX() * 0.8, 0.35, out.getStepZ() * 0.8);
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
        ServerLevel level = (ServerLevel) player.level();
        switch (reason) {
            case STRANGER -> {
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.BLOCKS, 0.6F, 0.5F);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.6, 0.5, 0.03);
            }
            case NOWHERE -> {
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, 0.6F);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
            }
            case UNPAID -> level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
    }

    // The fare: an ender pearl, or else four chorus fruit.
    private static boolean pay(Inventory inventory) {
        return take(inventory, Items.ENDER_PEARL, 1) || take(inventory, Items.CHORUS_FRUIT, 4);
    }

    private static boolean take(Inventory inventory, Item item, int count) {
        int have = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                have += stack.getCount();
            }
        }
        if (have < count) {
            return false;
        }
        int left = count;
        for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                int n = Math.min(left, stack.getCount());
                stack.shrink(n);
                left -= n;
            }
        }
        return true;
    }
}
