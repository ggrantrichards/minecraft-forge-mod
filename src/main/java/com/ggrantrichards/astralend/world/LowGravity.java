package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.LogicalSide;

// Low gravity in biomes tagged #astralend:low_gravity (the Nebula Fields).
//
// Living things there get 35% less gravity, which makes them jump about 1.5x higher and fall more
// slowly, plus a few extra blocks of fall before taking damage. Ender pearls thrown there fly
// about 20% further. It uses vanilla's own gravity and safe-fall-distance attributes, checked
// once a second per entity, so it costs almost nothing.
public final class LowGravity {
    public static final double GRAVITY_CHANGE = -0.35;     // multiplier on the total: 0.08 -> 0.052
    public static final double EXTRA_SAFE_FALL = 3.0;      // blocks
    public static final double PEARL_BOOST = 1.2;

    private static final Identifier GRAVITY_ID = Identifier.fromNamespaceAndPath(AstralEnd.MODID, "low_gravity");
    private static final Identifier SAFE_FALL_ID = Identifier.fromNamespaceAndPath(AstralEnd.MODID, "low_gravity_safe_fall");

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(LowGravity::onLevelTick);
        EntityJoinLevelEvent.BUS.addListener(LowGravity::onEntityJoin);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.side() != LogicalSide.SERVER || !(event.level() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) {
            return;
        }
        boolean isEnd = level.dimension() == Level.END;
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living) {
                // Outside the End, only clean up entities that carried the effect through a portal.
                if (isEnd || hasLowGravity(living)) {
                    update(living);
                }
            }
        }
    }

    // Adds or removes the low-gravity modifiers to match where the entity is now.
    public static void update(LivingEntity entity) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        AttributeInstance safeFall = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (gravity == null) {
            return;
        }
        boolean wanted = isLowGravityAt(entity.level(), entity);
        boolean has = gravity.hasModifier(GRAVITY_ID);
        if (wanted && !has) {
            gravity.addTransientModifier(new AttributeModifier(GRAVITY_ID, GRAVITY_CHANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            if (safeFall != null) {
                safeFall.addTransientModifier(new AttributeModifier(SAFE_FALL_ID, EXTRA_SAFE_FALL, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (!wanted && has) {
            gravity.removeModifier(GRAVITY_ID);
            if (safeFall != null) {
                safeFall.removeModifier(SAFE_FALL_ID);
            }
        }
    }

    public static boolean hasLowGravity(LivingEntity entity) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        return gravity != null && gravity.hasModifier(GRAVITY_ID);
    }

    private static boolean isLowGravityAt(Level level, Entity entity) {
        return level.dimension() == Level.END && level.getBiome(entity.blockPosition()).is(ModBiomes.LOW_GRAVITY);
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ThrownEnderpearl pearl && !event.loadedFromDisk() && !event.getLevel().isClientSide()
            && isLowGravityAt(event.getLevel(), pearl)) {
            pearl.setDeltaMovement(pearl.getDeltaMovement().scale(PEARL_BOOST));
        }
    }

    private LowGravity() {}
}
