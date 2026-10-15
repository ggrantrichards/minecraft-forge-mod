package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.flare.FlarePhase;
import com.ggrantrichards.astralend.flare.Sunstorms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

// What the five Starforge upgrades do (docs/DISTILLATION.md, "Starforging"). Each is a vanilla-sized
// effect that works in every dimension, and a sidegrade, not a new tier:
//
//   Void Recall   chestplate: load a Rift Pearl into it; fall into the void and the pearl is spent to
//                 put you back on your last footing.
//   Rift Walker   boots: tiles of rift under your feet over a drop, gone in a second.
//   Lensing       helmet, leggings: 10% per piece that a projectile curves around you.
//   Echo Strike   melee weapons: a tenth of every hit lands again a second later.
//   Radiant       armour: Soak Light - in bright light, half a golden heart every 30 s, up to half a
//                 heart per piece; 2+ pieces and Sunburn can't touch you. Weapons: targets glow for 5 s.
//
// Rift Pearls drop from every boss: the Ender Dragon, the Wither, the Warden, Elder Guardians - and
// a Leviathan's Maw gives one up when it's first woken.
public final class StarforgeEffects {
    public static void register() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(StarforgeEffects::onPlayerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(StarforgeEffects::onServerTick);
        LivingAttackEvent.BUS.addListener((Predicate<LivingAttackEvent>) StarforgeEffects::onAttacked);
        LivingDamageEvent.BUS.addListener((Predicate<LivingDamageEvent>) StarforgeEffects::onDamage);
        ProjectileImpactEvent.BUS.addListener(StarforgeEffects::onProjectileImpact);
        LivingDropsEvent.BUS.addListener((Predicate<LivingDropsEvent>) StarforgeEffects::onDrops);
        LivingDeathEvent.BUS.addListener((Predicate<LivingDeathEvent>) StarforgeEffects::onDeath);
    }

    // ------------------------------------------------------------------ per-player state

    private static final class PlayerState {
        Vec3 lastPos;
        GlobalPos footing;
        int soak;
    }

    private static final Map<UUID, PlayerState> PLAYERS = new HashMap<>();

    private static PlayerState state(Player player) {
        return PLAYERS.computeIfAbsent(player.getUUID(), id -> new PlayerState());
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level) || player.isSpectator()) {
            return;
        }
        PlayerState s = state(player);
        Vec3 pos = player.position();
        Vec3 moved = s.lastPos == null ? Vec3.ZERO : pos.subtract(s.lastPos);
        s.lastPos = pos;
        // The last solid ground stood on (not a rift tile), for Void Recall.
        BlockPos under = player.getOnPos();
        if (player.onGround() && !level.getBlockState(under).is(ModBlocks.RIFT_TILE.get()) && level.getBlockState(under).isSolid()) {
            s.footing = GlobalPos.of(level.dimension(), under.above());
        }
        if (Starforge.wearing(player, EquipmentSlot.FEET, StarforgeUpgrade.RIFT_WALKER)) {
            riftWalk(level, player, moved);
        }
        if (player.tickCount % 20 == 0) {
            soakLight(level, player, s);
            int forged = 0;
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (Starforge.get(player.getItemBySlot(slot)) != null) {
                    forged++;
                }
            }
            if (forged == 4) {
                com.ggrantrichards.astralend.world.ModAdvancements.award(player, "head_to_toe");
            }
        }
    }

    // ------------------------------------------------------------------ Voidwalker

    // Walking or sprinting over a drop: a tile under you, and one a step ahead.
    public static void riftWalk(ServerLevel level, ServerPlayer player, Vec3 moved) {
        double speed = Math.sqrt(moved.x * moved.x + moved.z * moved.z);
        if (speed < 0.05 || player.isFallFlying() || player.getAbilities().flying || player.isInWater() || player.isPassenger()
            || player.isShiftKeyDown() || moved.y > 0.05 || player.fallDistance > 1.5) {
            return;
        }
        Vec3 ahead = new Vec3(moved.x / speed, 0, moved.z / speed);
        int y = Mth.floor(player.getY() - 0.2);
        for (double step : new double[] {0.0, 0.7, 1.3}) {
            BlockPos at = BlockPos.containing(player.getX() + ahead.x * step, y, player.getZ() + ahead.z * step);
            placeTile(level, at);
        }
    }

    private static void placeTile(ServerLevel level, BlockPos at) {
        BlockState there = level.getBlockState(at);
        boolean lava = there.is(net.minecraft.world.level.block.Blocks.LAVA) && there.getFluidState().isSource();
        // Only over a real drop: stepping down a block onto the ground below works as usual.
        if ((!there.isAir() && !lava) || level.getBlockState(at.below()).isSolid() || level.getBlockState(at.below(2)).isSolid()) {
            return;
        }
        level.setBlock(at, ModBlocks.RIFT_TILE.get().defaultBlockState().setValue(RiftTileBlock.OVER_LAVA, lava), 3);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 3, 0.3, 0.05, 0.3, 0.01);
    }

    // Void Recall: falling into the void with a pearl in a Voidwalker chestplate brings you back to
    // your last footing instead.
    private static boolean voidRecall(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!Starforge.has(chest, StarforgeUpgrade.VOID_RECALL) || !chest.has(Starforge.RIFT_PEARL.get())) {
            return false;
        }
        PlayerState s = state(player);
        MinecraftServer server = player.level().getServer();
        ServerLevel target = s.footing != null && server != null ? server.getLevel(s.footing.dimension()) : null;
        if (target == null) {
            return false;
        }
        chest.remove(Starforge.RIFT_PEARL.get());
        BlockPos to = s.footing.pos();
        ServerLevel from = (ServerLevel) player.level();
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.4, 1.0, 0.4, 0.1);
        player.teleport(new net.minecraft.world.level.portal.TeleportTransition(target, Vec3.atBottomCenterOf(to), Vec3.ZERO, player.getYRot(),
            player.getXRot(), net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        target.sendParticles(ParticleTypes.REVERSE_PORTAL, to.getX() + 0.5, to.getY() + 1, to.getZ() + 0.5, 60, 0.4, 1.0, 0.4, 0.1);
        target.playSound(null, to, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.2F);
        com.ggrantrichards.astralend.world.ModAdvancements.award(player, "void_recall");
        return true;
    }

    // ------------------------------------------------------------------ Radiant: Soak Light

    private static void soakLight(ServerLevel level, ServerPlayer player, PlayerState s) {
        int pieces = Starforge.pieces(player, StarforgeUpgrade.RADIANT);
        if (pieces == 0) {
            s.soak = 0;
            return;
        }
        boolean flare = Sunstorms.phaseIn(level) == FlarePhase.FLARE && Sunstorms.inSunstorms(level, player.blockPosition());
        boolean bright = flare || level.getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())) >= 13;
        if (!bright) {
            return;
        }
        s.soak += flare ? 5 : 1;              // seconds of light soaked up
        float cap = Math.min(1.0F * pieces, player.getMaxAbsorption());
        if (s.soak >= 30 && player.getAbsorptionAmount() < cap) {
            s.soak = 0;
            player.setAbsorptionAmount(Math.min(cap, player.getAbsorptionAmount() + 1.0F));
            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.8F);
        }
    }

    // Radiant armour, two pieces or more: Sunburn can't touch you (Sunstorms asks).
    public static boolean flareProof(Player player) {
        return Starforge.pieces(player, StarforgeUpgrade.RADIANT) >= 2;
    }

    // ------------------------------------------------------------------ damage

    // Before damage: Void Recall can cancel it outright.
    private static boolean onAttacked(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return false;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && entity instanceof ServerPlayer player && voidRecall(player)) {
            return true;
        }
        return false;
    }

    private static final List<Echo> ECHOES = new ArrayList<>();
    private static boolean echoing;

    private record Echo(ServerLevel level, LivingEntity target, Player attacker, float amount, long at) {}

    // After armour: Echo Strike and Glow.
    private static boolean onDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        if (!(target.level() instanceof ServerLevel level) || !(source.getEntity() instanceof Player player) || echoing) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        ItemStack weapon = direct == player ? player.getMainHandItem()
            : direct instanceof AbstractArrow arrow && arrow.getWeaponItem() != null ? arrow.getWeaponItem() : ItemStack.EMPTY;
        StarforgeUpgrade upgrade = Starforge.get(weapon);
        if (upgrade == StarforgeUpgrade.ECHO_STRIKE && direct == player && weapon.is(Starforge.MELEE) && event.getAmount() > 0.0F) {
            ECHOES.add(new Echo(level, target, player, event.getAmount() * 0.1F, level.getGameTime() + 20));
        }
        if (upgrade == StarforgeUpgrade.RADIANT && (weapon.is(Starforge.MELEE) || weapon.is(Starforge.RANGED))) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100), player);
        }
        return false;
    }

    private static void echo(Echo e) {
        if (!e.target().isAlive() || e.target().level() != e.level()) {
            return;
        }
        echoing = true;
        try {
            e.target().hurtServer(e.level(), e.level().damageSources().indirectMagic(e.attacker(), e.attacker()), e.amount());
        } finally {
            echoing = false;
        }
        Vec3 c = e.target().position().add(0, e.target().getBbHeight() * 0.6, 0);
        e.level().sendParticles(ParticleTypes.ENCHANTED_HIT, c.x, c.y, c.z, 12, 0.3, 0.4, 0.3, 0.2);
        e.level().sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 4, 0.1, 0.1, 0.1, 0.05);
        e.level().playSound(null, e.target().blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    // ------------------------------------------------------------------ Lensing

    // Lensing: an incoming projectile, now and then, bends around you.
    private static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)
            || !(projectile.level() instanceof ServerLevel level) || projectile.getOwner() == target) {
            return;
        }
        int pieces = Starforge.pieces(target, StarforgeUpgrade.LENSING);
        if (pieces == 0 || level.getRandom().nextFloat() >= 0.1F * pieces) {
            return;
        }
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        Vec3 v = projectile.getDeltaMovement();
        double angle = (level.getRandom().nextBoolean() ? 1 : -1) * (0.45 + level.getRandom().nextDouble() * 0.25);
        projectile.setDeltaMovement(v.yRot((float) angle));
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.6, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, 30, 0.5, 0.6, 0.5, 0.05);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 1.8F);
    }

    // ------------------------------------------------------------------ projectiles and drops

    // Rift Pearls from the bosses that leave a body behind (the Dragon is handled in onDeath).
    private static boolean onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if ((entity instanceof WitherBoss || entity instanceof Warden || entity instanceof ElderGuardian) && event.isRecentlyHit()) {
            event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.5, entity.getZ(),
                new ItemStack(ModItems.RIFT_PEARL.get())));
        }
        return false;
    }

    // The Ender Dragon leaves no body: its Rift Pearl lands on the exit portal, where the egg goes.
    private static boolean onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && dragon.level() instanceof ServerLevel level && level.dimension() == Level.END) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            ItemEntity pearl = new ItemEntity(level, 0.5, Math.max(y, 64) + 2.0, 0.5, new ItemStack(ModItems.RIFT_PEARL.get()));
            pearl.setUnlimitedLifetime();
            pearl.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(pearl);
        }
        return false;
    }

    // ------------------------------------------------------------------ the end of each tick

    // Echoes that are due (or all of them, for the survey).
    public static void flush(boolean allEchoesNow) {
        for (Iterator<Echo> it = new ArrayList<>(ECHOES).iterator(); it.hasNext(); ) {
            Echo e = it.next();
            if (allEchoesNow || e.level().getGameTime() >= e.at()) {
                ECHOES.remove(e);
                echo(e);
            }
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        flush(false);
        if (event.server().getTickCount() % 1200 == 0) {
            PLAYERS.keySet().removeIf(id -> event.server().getPlayerList().getPlayer(id) == null);
        }
    }

    private StarforgeEffects() {}
}
