package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.AstralEnd;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// Umbral Stalker: a hunched, almost invisible brute of the Glimmering Crags' caves that walks on its knuckles.
//
// While it has no prey it is completely invisible in the dark - not even a shimmer. Once it picks a
// target it activates: a faint shimmer shows where it moves, and it becomes fully visible when it
// attacks, is hurt, or stands in light (block light 7 or more). It knows where you are through any
// wall, creeps up from behind and backs off when you turn to face it - the reverse of an EnderMan.
//
// It hates light. While it hunts it snuffs out the lights around it for good - torches and lanterns
// go out and drop, candles and campfires are put out (the blocks in #astralend:snuffed_by_stalkers;
// only when mobGriefing is on). And it fears fire: set it alight, hit it with a burning weapon or a
// torch, or strike flint and steel at it, and it flees for a few seconds before it comes back.
//
// It hunts like a lion. Hidden, it creeps in low and slow, lumbering on its knuckles; once it is a
// few blocks behind you and unseen, it pounces - a sudden leap that reveals it and lands a heavy
// ambush blow. After that it fights in the open for a moment, then melts back into the dark.
//
// With no player about it hunts Endermen under the Deep Hollows' canopy - short fights in the dark,
// the Endermen blinking away. It keeps clear of light in any form: a drifting swarm of Pollinators, or
// a Shardback that has woken in its geode (two hunters, one dark). And a wolf knows it's there: it
// stares and growls at the spot, and the Stalker, given away, shows itself.
public class UmbralStalker extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_REVEALED = SynchedEntityData.defineId(UmbralStalker.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(UmbralStalker.class, EntityDataSerializers.BOOLEAN);
    public static final int REVEALING_LIGHT = 7;
    public static final byte POUNCE_EVENT = 70;
    public static final int POUNCE_TICKS = 14;    // the leap, arms reaching out
    public static final int FEAR_TICKS = 160;     // how long fire keeps it away
    public static final int SNUFF_RANGE = 5;
    public static final TagKey<Block> SNUFFED = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "snuffed_by_stalkers"));
    private static final TargetingConditions SENSES = TargetingConditions.forCombat().range(32.0).ignoreLineOfSight();
    private int revealTicks;
    private boolean displayHunting;
    private int pounceCooldown = 60;
    private int fearTicks;
    private int attackAnimationTicks;  // client side: counts down while the arms lash out
    private int pounceAnimationTicks;  // client side: counts down through a pounce

    public UmbralStalker(EntityType<? extends UmbralStalker> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.FOLLOW_RANGE, 32.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
            .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_REVEALED, false);
        builder.define(DATA_HUNTING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeFireGoal());
        this.goalSelector.addGoal(2, new ShunGoal());
        this.goalSelector.addGoal(2, new BackOffWhenWatchedGoal());
        this.goalSelector.addGoal(3, new PounceGoal());
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new SenseTargetGoal());
        this.targetSelector.addGoal(3, new HuntEndermenGoal());
    }

    public boolean isAfraid() {
        return this.fearTicks > 0;
    }

    // Fire drives it off for a while (it keeps its target, and comes back when the fear wears off).
    public void scare() {
        if (this.fearTicks <= 0) {
            this.playSound(SoundEvents.ENDERMAN_SCREAM, 0.8F, 0.6F);
        }
        this.fearTicks = FEAR_TICKS;
        this.revealTicks = Math.max(this.revealTicks, 40);
    }

    // Given away (a wolf growling at it): it shows itself for a moment.
    public void betray(int ticks) {
        this.revealTicks = Math.max(this.revealTicks, ticks);
    }

    // What it keeps clear of: a Pollinator (glowing) or a woken Shardback, within 7 blocks.
    public @Nullable LivingEntity shunned() {
        for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(7.0), LivingEntity::isAlive)) {
            if (e instanceof EndPollinator || (e instanceof Shardback shard && !shard.isHidden())) {
                return e;
            }
        }
        return null;
    }

    public boolean isRevealed() {
        return this.entityData.get(DATA_REVEALED);
    }

    // True once it has a target: the shimmer shows. Without one it is invisible in the dark.
    public boolean isHunting() {
        return this.entityData.get(DATA_HUNTING);
    }

    // For the screenshot tool, which summons it without AI: show it as if it were hunting.
    public void showAsHunting() {
        this.displayHunting = true;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackAnimationTicks = 10;
        } else if (id == POUNCE_EVENT) {
            this.pounceAnimationTicks = POUNCE_TICKS;
        } else {
            super.handleEntityEvent(id);
        }
    }

    // 0..1 through the strike, for the renderer.
    public float getAttackAnimation(float partialTicks) {
        return this.attackAnimationTicks > 0 ? 1.0F - (this.attackAnimationTicks - partialTicks) / 10.0F : 0.0F;
    }

    // 0..1 through a pounce, for the renderer (0 when not pouncing).
    public float getPounceAnimation(float partialTicks) {
        return this.pounceAnimationTicks > 0 ? 1.0F - (this.pounceAnimationTicks - partialTicks) / POUNCE_TICKS : 0.0F;
    }

    // True if the player is facing this stalker (within about 60 degrees) and can see it.
    public boolean isWatchedBy(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ()).normalize();
        return view.dot(toMe) > 0.5 && player.hasLineOfSight(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            if (this.attackAnimationTicks > 0) {
                this.attackAnimationTicks--;
            }
            if (this.pounceAnimationTicks > 0) {
                this.pounceAnimationTicks--;
            }
            // Distortion where it moves.
            if (this.isHunting() && !this.isRevealed() && this.walkAnimation.speed() > 0.1F && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.revealTicks > 0) {
            this.revealTicks--;
        }
        if (this.pounceCooldown > 0) {
            this.pounceCooldown--;
        }
        if (this.fearTicks > 0) {
            this.fearTicks--;
        }
        if (this.isOnFire()) {
            this.scare();
        }
        if (this.getTarget() != null && this.tickCount % 10 == 0 && this.level() instanceof ServerLevel level) {
            this.snuffLight(level);
        }
        boolean lit = this.level().getBrightness(LightLayer.BLOCK, this.blockPosition().above()) >= REVEALING_LIGHT;
        this.entityData.set(DATA_REVEALED, this.revealTicks > 0 || this.hurtTime > 0 || lit);
        this.entityData.set(DATA_HUNTING, this.displayHunting || this.getTarget() != null);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.revealTicks = 40;
        level.broadcastEntityEvent(this, (byte) 4);
        boolean hit = super.doHurtTarget(level, target);
        // An EnderMan, struck, often blinks away - and the hunt goes on in the dark.
        if (hit && target instanceof EnderMan enderman && enderman.isAlive() && this.random.nextInt(3) == 0) {
            var to = EndMobs.safeSpotNear(level, enderman.blockPosition(), 8, 16, this.random);
            if (to != null) {
                level.sendParticles(ParticleTypes.PORTAL, enderman.getX(), enderman.getY() + 1.5, enderman.getZ(), 30, 0.4, 1.0, 0.4, 0.3);
                level.playSound(null, enderman.getX(), enderman.getY(), enderman.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                enderman.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            }
        }
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt) {
            this.revealTicks = 60;
            if (source.is(DamageTypeTags.IS_FIRE) || burning(level, source.getEntity())) {
                this.scare();
            }
        }
        return hurt;
    }

    // A hit with fire in it: a Fire Aspect weapon, or a torch in the hand.
    private static boolean burning(ServerLevel level, @Nullable Entity attacker) {
        if (!(attacker instanceof LivingEntity living)) {
            return false;
        }
        ItemStack held = living.getMainHandItem();
        if (held.is(Items.TORCH) || held.is(Items.SOUL_TORCH) || held.is(Items.REDSTONE_TORCH) || held.is(Items.COPPER_TORCH)) {
            return true;
        }
        var fireAspect = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);
        return EnchantmentHelper.getItemEnchantmentLevel(fireAspect, held) > 0;
    }

    // Flint and steel struck at it: it catches, and flees.
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemTags.CREEPER_IGNITERS)) {
            this.playSound(stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE, 1.0F, 1.0F);
            if (!this.level().isClientSide()) {
                this.igniteForSeconds(3.0F);
                this.scare();
                if (stack.isDamageableItem()) {
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                } else {
                    stack.consume(1, player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // Puts out one light near it: torches and lanterns go out and drop, candles and campfires are
    // put out. Only with mobGriefing on, like an EnderMan's block-taking.
    private void snuffLight(ServerLevel level) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return;
        }
        BlockPos centre = this.blockPosition();
        BlockPos nearest = null;
        double best = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-SNUFF_RANGE, -2, -SNUFF_RANGE), centre.offset(SNUFF_RANGE, 3, SNUFF_RANGE))) {
            BlockState s = level.getBlockState(p);
            if (s.is(SNUFFED) && (!s.hasProperty(BlockStateProperties.LIT) || s.getValue(BlockStateProperties.LIT))) {
                double d = p.distSqr(centre);
                if (d < best) {
                    best = d;
                    nearest = p.immutable();
                }
            }
        }
        if (nearest == null) {
            return;
        }
        BlockState s = level.getBlockState(nearest);
        if (s.hasProperty(BlockStateProperties.LIT)) {
            level.setBlock(nearest, s.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
        } else {
            level.destroyBlock(nearest, true, this);
        }
        level.playSound(null, nearest, SoundEvents.CANDLE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, nearest.getX() + 0.5, nearest.getY() + 0.6,
            nearest.getZ() + 0.5, 6, 0.15, 0.15, 0.15, 0.01);
        level.gameEvent(this, GameEvent.BLOCK_CHANGE, nearest);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.random.nextInt(4) == 0 ? SoundEvents.WARDEN_NEARBY_CLOSER : null;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }

    // The ambush: from a few blocks behind an unsuspecting player, it leaps. If the leap carries it
    // into reach, the blow lands hard (12). Then it fights in the open until
    // it is watched again, and slinks off.
    private class PounceGoal extends Goal {
        private int ticks;
        private boolean struck;

        PounceGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            if (!(self.getTarget() instanceof Player player) || self.pounceCooldown > 0 || self.isRevealed() || !self.onGround()
                || self.isAfraid()) {
                return false;
            }
            double distance = self.distanceToSqr(player);
            return distance > 3.0 * 3.0 && distance < 7.0 * 7.0 && !self.isWatchedBy(player) && self.hasLineOfSight(player);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < POUNCE_TICKS && !this.struck;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            UmbralStalker self = UmbralStalker.this;
            LivingEntity target = self.getTarget();
            this.ticks = 0;
            this.struck = false;
            self.getNavigation().stop();
            self.revealTicks = 60;
            Vec3 leap = target.position().subtract(self.position()).multiply(1.0, 0.0, 1.0);
            double distance = leap.length();
            leap = leap.normalize().scale(Math.min(1.6, 0.25 + distance * 0.2));
            self.setDeltaMovement(leap.x, 0.45, leap.z);
            self.getLookControl().setLookAt(target, 60.0F, 30.0F);
            self.level().broadcastEntityEvent(self, POUNCE_EVENT);
            self.playSound(SoundEvents.RAVAGER_ROAR, 0.9F, 0.8F);
        }

        @Override
        public void tick() {
            UmbralStalker self = UmbralStalker.this;
            LivingEntity target = self.getTarget();
            this.ticks++;
            if (target == null || !(self.level() instanceof ServerLevel level)) {
                return;
            }
            self.getLookControl().setLookAt(target, 60.0F, 30.0F);
            if (self.distanceToSqr(target) < 2.6 * 2.6) {
                this.struck = true;
                level.broadcastEntityEvent(self, (byte) 4);
                target.hurtServer(level, self.damageSources().mobAttack(self), 12.0F);
            }
        }

        @Override
        public void stop() {
            UmbralStalker.this.pounceCooldown = 200;
        }
    }

    // When the target turns to face it, the stalker slips away and waits for another chance.
    private class BackOffWhenWatchedGoal extends Goal {
        private int ticks;

        BackOffWhenWatchedGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            return self.getTarget() instanceof Player player && self.revealTicks <= 0 && !self.isAfraid()
                && self.distanceToSqr(player) > 6.0 && self.isWatchedBy(player);
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks < 60 && !UmbralStalker.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            UmbralStalker self = UmbralStalker.this;
            this.ticks = 0;
            if (self.getTarget() != null) {
                Vec3 away = DefaultRandomPos.getPosAway(self, 12, 5, self.getTarget().position());
                if (away != null) {
                    self.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                }
            }
        }

        @Override
        public void tick() {
            this.ticks++;
        }
    }

    // It always knows where the nearest player is, walls or not (it doesn't need to see you).
    private class SenseTargetGoal extends Goal {
        SenseTargetGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            if ((self.getTarget() != null && !(self.getTarget() instanceof EnderMan)) || self.tickCount % 10 != 0
                    || !(self.level() instanceof ServerLevel level)) {
                return false;
            }
            Player player = level.getNearestPlayer(SENSES, self);
            if (player == null) {
                return false;
            }
            self.setTarget(player);
            return false;
        }
    }

    // The nearest EnderMan within 24 blocks, when no player is about (a player always comes first).
    private class HuntEndermenGoal extends Goal {
        HuntEndermenGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            if (self.getTarget() != null || self.random.nextInt(10) != 0 || !(self.level() instanceof ServerLevel level)
                    || level.getNearestPlayer(SENSES, self) != null) {
                return false;
            }
            EnderMan prey = self.nearestEnderman();
            if (prey != null) {
                self.setTarget(prey);
            }
            return false;
        }
    }

    public @Nullable EnderMan nearestEnderman() {
        EnderMan best = null;
        for (EnderMan e : this.level().getEntitiesOfClass(EnderMan.class, this.getBoundingBox().inflate(24.0), Entity::isAlive)) {
            if (best == null || e.distanceToSqr(this) < best.distanceToSqr(this)) {
                best = e;
            }
        }
        return best;
    }

    // Light it can't abide close by (a Pollinator, a woken Shardback): it backs off into the dark.
    private class ShunGoal extends Goal {
        ShunGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            UmbralStalker self = UmbralStalker.this;
            if (self.random.nextInt(3) != 0) {
                return false;
            }
            LivingEntity light = self.shunned();
            if (light == null) {
                return false;
            }
            Vec3 away = DefaultRandomPos.getPosAway(self, 12, 4, light.position());
            if (away == null) {
                return false;
            }
            self.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return !UmbralStalker.this.getNavigation().isDone();
        }
    }

    // Afraid of fire: runs from its target until the fear wears off.
    private class FleeFireGoal extends Goal {
        FleeFireGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return UmbralStalker.this.isAfraid();
        }

        @Override
        public void start() {
            this.run();
        }

        @Override
        public void tick() {
            if (UmbralStalker.this.getNavigation().isDone()) {
                this.run();
            }
        }

        private void run() {
            UmbralStalker self = UmbralStalker.this;
            Vec3 from = self.getTarget() != null ? self.getTarget().position() : self.position().add(self.getLookAngle());
            Vec3 away = DefaultRandomPos.getPosAway(self, 16, 6, from);
            if (away != null) {
                self.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            }
        }

        @Override
        public void stop() {
            UmbralStalker.this.getNavigation().stop();
        }
    }
}
