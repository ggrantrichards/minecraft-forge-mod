package com.ggrantrichards.astralend.entity;

import com.ggrantrichards.astralend.world.ModBiomes;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Mirror Wretch (the Choir): a long grey worm that prowls the Ashen Barrens (since the golden-meadow
// rework; it was the Seam's). It has no face, only a round mouth ringed with teeth, and down its throat
// a vocal sac for every voice it has stolen. It slithers low, its back half crawling on a row of small legs; when it
// hunts it rears up over two blocks high and strikes down from there. It never comes straight at
// you. Now and then it throws a sound you know - a chest opening, a door, footsteps, a sheep - from
// somewhere near the island's edge, to draw you over; when you stand at an edge with it close, it
// lunges, and its blow throws you outward. It craves Memory Moss (hold some and it follows you, and
// won't strike). The Ashen Barrens' endermen stand at the edges listening to it
// (EclipseLife); it leaves them be. It doesn't burn, and doesn't despawn in daylight - there is none.
public class MirrorWretch extends Monster {
    private static final List<SoundEvent> LURES = List.of(SoundEvents.CHEST_OPEN, SoundEvents.WOODEN_DOOR_OPEN,
        SoundEvents.SHEEP_AMBIENT, SoundEvents.ITEM_PICKUP, SoundEvents.STONE_STEP, SoundEvents.WOODEN_DOOR_CLOSE,
        SoundEvents.EXPERIENCE_ORB_PICKUP, SoundEvents.BELL_BLOCK);
    private int lureCooldown = 200;
    private float rear, rearO;          // (client) how far it has reared: 0 low, 1 up - eased
    private boolean reared;

    public MirrorWretch(EntityType<? extends MirrorWretch> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.ATTACK_KNOCKBACK, 1.6)
            .add(Attributes.FOLLOW_RANGE, 28.0)
            .add(Attributes.TEMPT_RANGE, 16.0);     // Memory Moss draws it from this far
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // It craves Memory Moss: hold some, and it follows you instead of striking.
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.TemptGoal(this, 1.0, MirrorWretch::isMoss, false));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false) {
            @Override
            public boolean canUse() {
                // Players only when they stand at an edge (endermen always).
                return super.canUse() && (!(MirrorWretch.this.getTarget() instanceof Player p) || (atEdge(MirrorWretch.this.level(), p) && !holdingMoss(p)));
            }

            @Override
            public boolean canContinueToUse() {
                return super.canContinueToUse()
                    && (!(MirrorWretch.this.getTarget() instanceof Player p) || (atEdge(MirrorWretch.this.level(), p) && !holdingMoss(p)));
            }
        });
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    static boolean isMoss(net.minecraft.world.item.ItemStack stack) {
        return stack.is(com.ggrantrichards.astralend.ModBlocks.MEMORY_MOSS.get().asItem());
    }

    static boolean holdingMoss(Player player) {
        return isMoss(player.getMainHandItem()) || isMoss(player.getOffhandItem());
    }

    public static boolean atEdgeForTest(Level level, Entity entity) {
        return atEdge(level, entity);
    }

    // Within two blocks of a drop into the void.
    static boolean atEdge(Level level, Entity entity) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        BlockPos at = entity.blockPosition();
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= 2; i++) {
                BlockPos side = at.relative(d, i);
                if (server.getBlockState(side).isAir() && server.getBlockState(side.below()).isAir() && EndMobs.voidBelow(server, side)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Hunting (its melee goal marks it aggressive), it rears up: its hitbox is the full 2.2 blocks
    // then, half that while it slithers.
    @Override
    public void tick() {
        super.tick();
        boolean up = this.isAggressive();
        if (up != this.reared) {
            this.reared = up;
            this.refreshDimensions();
        }
        if (this.level().isClientSide()) {
            this.rearO = this.rear;
            this.rear = net.minecraft.util.Mth.approach(this.rear, up ? 1.0F : 0.0F, 0.12F);
        }
    }

    public float rear(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, this.rearO, this.rear);
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose) {
        var full = super.getDefaultDimensions(pose);
        return this.reared ? full : full.scale(1.0F, 0.5F);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel level)) {
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.5), this.getY() + 2.2, this.getRandomZ(0.5), 0, 0.02, 0);
            }
            return;
        }
        if (--this.lureCooldown <= 0) {
            this.lureCooldown = 160 + this.random.nextInt(240);
            Player player = level.getNearestPlayer(this, 24.0);
            if (player != null && !player.isCreative() && !player.isSpectator()) {
                this.lure(level, player);
            }
        }
    }

    // A familiar sound from somewhere near an edge, beyond the player as seen from here.
    private void lure(ServerLevel level, Player player) {
        BlockPos edge = findEdge(level, player.blockPosition(), this.random);
        if (edge == null) {
            return;
        }
        SoundEvent sound = LURES.get(this.random.nextInt(LURES.size()));
        level.playSound(null, edge, sound, SoundSource.AMBIENT, 1.0F, 0.95F + this.random.nextFloat() * 0.1F);
    }

    private static @Nullable BlockPos findEdge(ServerLevel level, BlockPos from, RandomSource random) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            for (int r = 4; r <= 16; r++) {
                BlockPos p = from.offset((int) Math.round(Math.cos(angle) * r), 0, (int) Math.round(Math.sin(angle) * r));
                if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isAir() && EndMobs.voidBelow(level, p)) {
                    return p;   // just over the edge: the sound comes from the void's lip
                }
            }
        }
        return null;
    }

    // Its blow throws a player outward, away from it - toward the void, if that's where they stand.
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof Player player) {
            this.throwOutward(player);
        }
        return hit;
    }

    public void throwOutward(Player player) {
        Vec3 away = player.position().subtract(this.position()).multiply(1, 0, 1).normalize();
        player.push(away.x * 0.6, 0.25, away.z * 0.6);
        player.syncVelocity = true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 64.0 * 64.0;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;   // it only ever makes other things' sounds
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    // Spawns only in the Ashen Barrens (since beta.4), where the Endermen stand at the islands' edges facing
    // the void: they're listening to it.
    public static boolean checkWretchSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level,
                                                EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, reason, pos, random) && level.getBiome(pos).is(ModBiomes.ASHEN_BARRENS);
    }
}
