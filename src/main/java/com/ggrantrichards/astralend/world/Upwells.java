package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;

// Upwells (the Deep Hollows).
//
// Inside a pocket, gravity points up at 60% of normal strength: you "fall" upward until you
// hit the island above. It uses vanilla's gravity attribute (0.08 * (1 - 1.6) = -0.048), so it
// works the same for players (the attribute is synced to their client) and mobs. The effect
// wears off a few ticks after you leave the pocket.
//
// Pockets only work in biomes tagged #astralend:upwells; a datapack can empty that tag
// to turn them off.
public final class Upwells {
    public static final double GRAVITY_CHANGE = -1.6;   // multiplier on the total: 0.08 -> -0.048
    private static final int LINGER_TICKS = 4;
    private static final Identifier ID = Identifier.fromNamespaceAndPath(AstralEnd.MODID, "upwells");

    // Server thread only. Weak keys, so unloaded or dead entities are forgotten.
    private static final Map<LivingEntity, Long> INSIDE = new WeakHashMap<>();

    public static void register() {
        TickEvent.LevelTickEvent.Post.BUS.addListener(Upwells::onLevelTick);
    }

    public static boolean enabledAt(Level level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomes.UPWELLS);
    }

    public static void markInside(LivingEntity entity, long gameTime) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity == null) {
            return;
        }
        if (!gravity.hasModifier(ID)) {
            gravity.addTransientModifier(new AttributeModifier(ID, GRAVITY_CHANGE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        INSIDE.put(entity, gameTime);
        entity.resetFallDistance();
    }

    public static boolean isInverted(LivingEntity entity) {
        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        return gravity != null && gravity.hasModifier(ID);
    }

    private static void onLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (event.side() != LogicalSide.SERVER || INSIDE.isEmpty()) {
            return;
        }
        long now = event.level().getGameTime();
        INSIDE.entrySet().removeIf(entry -> {
            LivingEntity entity = entry.getKey();
            if (entity.level() != event.level()) {
                return false;  // another dimension's tick handles it
            }
            if (entity.isRemoved() || now - entry.getValue() > LINGER_TICKS) {
                AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
                if (gravity != null) {
                    gravity.removeModifier(ID);
                }
                return true;
            }
            return false;
        });
    }

    private Upwells() {}
}
