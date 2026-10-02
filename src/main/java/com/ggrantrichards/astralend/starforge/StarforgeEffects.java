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
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;

// What the six Starforge upgrades do (docs/END_UPDATE.md, section 9.3). Each one is a vanilla-sized
// effect that works in every dimension:
//
//   Voidwalker   boots: Rift Walker - tiles of rift under your feet over a drop, gone in a second.
//                chestplate: Void Recall - a Rift Pearl brings you back from the void, once.
//   Meteoric     armour: Heat Shield - 2 s per piece of no fire damage; resets after 10 s out of fire.
//                tools: Smelting (StarforgeLoot).
//   Stardust     armour: lower gravity, boots step up (attribute modifiers, see Starforge.apply).
//                bows, crossbows, tridents: projectiles fall 20% slower, so they fly ~25% further.
//   Temporal     armour: Time Stretch - good effects last 10% longer per piece, bad ones 10% shorter.
//                melee weapons: Echo Strike - a quarter of every hit lands again a second later.
//   Singularity  armour: Lensing - 10% per piece that a projectile curves around you.
//                swords: a full sweep pulls nearby mobs toward your target. tools: Pull (StarforgeLoot).
//   Radiant      armour: Soak Light - in bright light, a golden heart every 20 s, up to one per
//                piece; 2+ pieces and Sunburn can't touch you. weapons: targets glow for 5 s.
//
// Rift Pearls drop from every boss: the Ender Dragon, the Wither, the Warden, Elder Guardians (and
// the Spire Wyrm, in its loot table).
public final class StarforgeEffects {
    public static void register() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(StarforgeEffects::onPlayerTick);
        TickEvent.ServerTickEvent.Post.BUS.addListener(StarforgeEffects::onServerTick);
        LivingAttackEvent.BUS.addListener((Predicate<LivingAttackEvent>) StarforgeEffects::onAttacked);
        LivingDamageEvent.BUS.addListener((Predicate<LivingDamageEvent>) StarforgeEffects::onDamage);
        MobEffectEvent.Added.BUS.addListener(StarforgeEffects::onEffectAdded);
        ProjectileImpactEvent.BUS.addListener(StarforgeEffects::onProjectileImpact);
        EntityJoinLevelEvent.BUS.addListener(StarforgeEffects::onEntityJoin);
        AttackEntityEvent.BUS.addListener((Predicate<AttackEntityEvent>) StarforgeEffects::onAttackEntity);
        BlockEvent.BreakEvent.BUS.addListener((Predicate<BlockEvent.BreakEvent>) StarforgeEffects::onBreak);
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
        if (Starforge.wearing(player, EquipmentSlot.FEET, StarforgeUpgrade.VOIDWALKER)) {
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
        if (!Starforge.has(chest, StarforgeUpgrade.VOIDWALKER) || !chest.has(Starforge.RIFT_PEARL.get())) {
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

    // ------------------------------------------------------------------ Meteoric: Heat Shield

    // entity -> {first fire damage of this exposure, last fire damage}
    private static final Map<UUID, long[]> HEAT = new HashMap<>();

    private static boolean heatShield(LivingEntity entity, DamageSource source) {
        int pieces = Starforge.pieces(entity, StarforgeUpgrade.METEORIC);
        if (pieces == 0 || !source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        long now = entity.level().getGameTime();
        long[] t = HEAT.computeIfAbsent(entity.getUUID(), id -> new long[] {now, now});
        if (now - t[1] > 200) {
            t[0] = now;       // 10 seconds out of the fire: the shield is whole again
        }
        t[1] = now;
        return now - t[0] < 40L * pieces;
    }

    // ------------------------------------------------------------------ Radiant: Soak Light

    private static void soakLight(ServerLevel level, ServerPlayer player, PlayerState s) {
        int pieces = Starforge.pieces(player, StarforgeUpgrade.RADIANT);
        if (pieces == 0) {
            s.soak = 0;
            return;
        }
        boolean flare = Sunstorms.phaseIn(level) == FlarePhase.FLARE && Sunstorms.isCorona(level, player.blockPosition());
        boolean bright = flare || level.getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())) >= 13;
        if (!bright) {
            return;
        }
        s.soak += flare ? 5 : 1;              // seconds of light soaked up
        float cap = Math.min(2.0F * pieces, player.getMaxAbsorption());
        if (s.soak >= 20 && player.getAbsorptionAmount() < cap) {
            s.soak = 0;
            player.setAbsorptionAmount(Math.min(cap, player.getAbsorptionAmount() + 2.0F));
            level.sendParticles(ParticleTypes.WAX_ON, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.8F);
        }
    }

    // Radiant armour, two pieces or more: Sunburn can't touch you (Sunstorms asks).
    public static boolean flareProof(Player player) {
        return Starforge.pieces(player, StarforgeUpgrade.RADIANT) >= 2;
    }

    // ------------------------------------------------------------------ damage

    // Before damage: Void Recall and Heat Shield can cancel it outright.
    private static boolean onAttacked(LivingAttackEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return false;
        }
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && entity instanceof ServerPlayer player && voidRecall(player)) {
            return true;
        }
        return heatShield(entity, source);
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
        if (upgrade == StarforgeUpgrade.TEMPORAL && direct == player && weapon.is(Starforge.MELEE) && event.getAmount() > 0.0F) {
            ECHOES.add(new Echo(level, target, player, event.getAmount() * 0.25F, level.getGameTime() + 20));
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

    // ------------------------------------------------------------------ Temporal: Time Stretch

    private record Stretch(LivingEntity entity, MobEffectInstance effect) {}

    private static final List<Stretch> STRETCHES = new ArrayList<>();
    private static boolean stretching;

    private static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();
        if (stretching || entity.level().isClientSide() || event.getOldEffectInstance() != null || effect.isAmbient()
            || effect.isInfiniteDuration() || effect.getDuration() < 20 || Starforge.pieces(entity, StarforgeUpgrade.TEMPORAL) == 0) {
            return;
        }
        STRETCHES.add(new Stretch(entity, effect));   // applied at the end of the tick, once the effect is in
    }

    private static void stretch(Stretch s) {
        LivingEntity entity = s.entity();
        Holder<MobEffect> type = s.effect().getEffect();
        MobEffectInstance now = entity.getEffect(type);
        int pieces = Starforge.pieces(entity, StarforgeUpgrade.TEMPORAL);
        if (now == null || !entity.isAlive() || pieces == 0) {
            return;
        }
        boolean good = type.value().isBeneficial();
        float scale = good ? 1.0F + 0.1F * pieces : 1.0F - 0.1F * pieces;
        stretching = true;
        try {
            entity.removeEffect(type);
            entity.addEffect(now.withScaledDuration(scale));
        } finally {
            stretching = false;
        }
    }

    // ------------------------------------------------------------------ Singularity

    // Lensing: an incoming projectile, now and then, bends around you.
    private static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)
            || !(projectile.level() instanceof ServerLevel level) || projectile.getOwner() == target) {
            return;
        }
        int pieces = Starforge.pieces(target, StarforgeUpgrade.SINGULARITY);
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

    // A full sweep with a Singularity sword pulls the mobs around your target in toward it.
    private static boolean onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (!(player.level() instanceof ServerLevel level) || !Starforge.holding(player, StarforgeUpgrade.SINGULARITY)
            || !player.getMainHandItem().is(net.minecraft.tags.ItemTags.SWORDS) || player.getAttackStrengthScale(0.5F) < 0.9F
            || !player.onGround() || player.isSprinting()) {
            return false;
        }
        AABB around = target.getBoundingBox().inflate(3.0, 0.5, 3.0);
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, around,
                e -> e != player && e != target && !(e instanceof ArmorStand) && !player.isAlliedTo(e) && e.isAlive())) {
            Vec3 pull = target.position().subtract(mob.position());
            double d = pull.horizontalDistance();
            if (d < 1.0) {
                continue;
            }
            Vec3 push = new Vec3(pull.x / d, 0, pull.z / d).scale(Math.min(0.6, 0.25 + d * 0.08));
            mob.push(push.x, 0.1, push.z);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 8, 0.2, 0.3, 0.2, 0.02);
        }
        return false;
    }

    // ------------------------------------------------------------------ projectiles and drops

    // Stardust bows, crossbows and tridents: their projectiles are tracked, and lifted a little each tick.
    private static final List<Projectile> FLATTER = new ArrayList<>();

    // Where a Singularity tool just broke a block, and who for: experience from it flies to them.
    private record Broken(ServerLevel level, BlockPos pos, Player player, long at) {}

    private static final List<Broken> BROKEN = new ArrayList<>();

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof AbstractArrow arrow && arrow.getWeaponItem() != null
                && Starforge.has(arrow.getWeaponItem(), StarforgeUpgrade.STARDUST)) {
            FLATTER.add(arrow);
        } else if (entity instanceof net.minecraft.world.entity.ExperienceOrb orb) {
            for (Broken b : BROKEN) {
                if (b.level() == level && b.pos().closerToCenterThan(orb.position(), 1.5) && b.player().isAlive()) {
                    orb.setPos(b.player().getX(), b.player().getY() + 0.5, b.player().getZ());
                    break;
                }
            }
        }
    }

    private static boolean onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && Starforge.holding(event.getPlayer(), StarforgeUpgrade.SINGULARITY)) {
            BROKEN.add(new Broken(level, event.getPos(), event.getPlayer(), level.getGameTime()));
        }
        return false;
    }

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

    // Echoes that are due (or all of them, for the survey), and effects waiting to be stretched.
    public static void flush(boolean allEchoesNow) {
        for (Iterator<Echo> it = new ArrayList<>(ECHOES).iterator(); it.hasNext(); ) {
            Echo e = it.next();
            if (allEchoesNow || e.level().getGameTime() >= e.at()) {
                ECHOES.remove(e);
                echo(e);
            }
        }
        if (!STRETCHES.isEmpty()) {
            List<Stretch> todo = new ArrayList<>(STRETCHES);
            STRETCHES.clear();
            todo.forEach(StarforgeEffects::stretch);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        flush(false);
        for (Iterator<Projectile> it = FLATTER.iterator(); it.hasNext(); ) {
            Projectile p = it.next();
            if (p.isRemoved()) {
                it.remove();
                continue;
            }
            Vec3 v = p.getDeltaMovement();
            if (v.lengthSqr() > 1.0E-4 && !p.onGround()) {
                p.setDeltaMovement(v.add(0, p.getGravity() * 0.2, 0));
            }
        }
        BROKEN.removeIf(b -> b.level().getGameTime() - b.at() > 2);
        if (event.server().getTickCount() % 1200 == 0) {
            PLAYERS.keySet().removeIf(id -> event.server().getPlayerList().getPlayer(id) == null);
            HEAT.entrySet().removeIf(e -> event.server().overworld().getGameTime() - e.getValue()[1] > 1200);
        }
    }

    private StarforgeEffects() {}
}
