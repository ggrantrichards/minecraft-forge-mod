package com.ggrantrichards.astralend.entity;

import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// The Hollowback: a Hushback the Leviathan's builders kept and hollowed out - its hide gone dark and
// cracked, the light gone out of it, and the herd's flight turned to rage. It lives only in the dark
// lower ribs of a Leviathan (the structure places them). Hit it and it blinks a few blocks aside,
// as its kin do - then comes straight back at you.
//
// Something of the herd is left in it. Hold Breathcomb or Hushfleece and it falters as it comes at
// you - stops, head low, and chimes once, the way the herd does - before the rage takes it again.
public class Hollowback extends Monster {
    public static final int HESITATE_TICKS = 50;
    private int hesitateCooldown;
    private int hesitating;
    public Hollowback(EntityType<? extends Hollowback> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 26.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HesitateGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public boolean isHesitating() {
        return this.hesitating > 0;
    }

    // Something of the herd in a player's hands: Breathcomb (what the herd grazes) or Hushfleece.
    public static boolean remindsIt(Player player) {
        for (var stack : new net.minecraft.world.item.ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(com.ggrantrichards.astralend.ModBlocks.BREATHCOMB.get().asItem()) || stack.is(com.ggrantrichards.astralend.ModItems.HUSHFLEECE.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.hesitateCooldown > 0) {
            this.hesitateCooldown--;
        }
        if (this.hesitating > 0) {
            this.hesitating--;
        }
    }

    // It falters: stops, head low, one chime. Then it won't again for a while.
    public void hesitate() {
        this.hesitating = HESITATE_TICKS;
        this.hesitateCooldown = 300;
        this.getNavigation().stop();
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.4F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.2, this.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        }
    }

    private class HesitateGoal extends Goal {
        HesitateGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Hollowback self = Hollowback.this;
            if (self.hesitating > 0) {
                return true;
            }
            if (self.hesitateCooldown <= 0 && self.getTarget() instanceof Player player && self.distanceToSqr(player) < 8.0 * 8.0 && remindsIt(player)) {
                self.hesitate();
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return Hollowback.this.hesitating > 0;
        }

        @Override
        public void tick() {
            Hollowback self = Hollowback.this;
            self.getNavigation().stop();
            if (self.getTarget() != null) {
                self.getLookControl().setLookAt(self.getTarget(), 10.0F, 10.0F);
            }
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive() && this.random.nextInt(3) == 0) {
            var to = EndMobs.safeSpotNear(level, this.blockPosition(), 3, 6, this.random);
            if (to != null) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 0.6, this.getZ(), 16, 0.3, 0.5, 0.3, 0.05);
                this.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.6F, 0.8F);
            }
        }
        return hurt;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_HORSE_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;   // placed by the Leviathan; it never despawns
    }
}
