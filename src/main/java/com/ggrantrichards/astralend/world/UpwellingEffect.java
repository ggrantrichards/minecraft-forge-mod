package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModEffects;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.MobEffectEvent;

// Upwelling (the Potion of Upwelling, brewed from Upwell Motes): gravity points up, exactly as hard
// as it normally points down, and you're turned upside down (client.UpsideDown): you fall up onto
// the underside of whatever is over you and walk about on it, head down. You fall upward - at most MAX_RISE blocks above where it took hold,
// and if you hit a ceiling on the way you take fall damage as if you had hit the ground. When it ends
// (or you reach the top), Slow Falling lets you back down.
public final class UpwellingEffect extends MobEffect {
    public static final double MAX_RISE = 20.0;
    private static final int SLOW_FALLING_TICKS = 300;
    private static final Identifier ID = Identifier.fromNamespaceAndPath(AstralEnd.MODID, "upwelling");
    // entity -> {y where it took hold, y it last stood or hung at}. Server side; weak keys.
    private static final Map<LivingEntity, double[]> RISING = new WeakHashMap<>();

    public UpwellingEffect() {
        super(MobEffectCategory.NEUTRAL, 0x7FE0D8);
        this.addAttributeModifier(Attributes.GRAVITY, ID, -2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    public static void register() {
        MobEffectEvent.Expired.BUS.addListener((java.util.function.Consumer<MobEffectEvent.Expired>) event -> wearOff(event.getEntity(), event.getEffectInstance()));
        MobEffectEvent.Remove.BUS.addListener((java.util.function.Consumer<MobEffectEvent.Remove>) event -> wearOff(event.getEntity(), event.getEffectInstance()));
        // Standing on a ceiling, a jump pushes you off it: down.
        net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent.BUS.addListener(event -> {
            LivingEntity entity = event.getEntity();
            if (entity.getAttributeValue(Attributes.GRAVITY) < 0.0 && entity.getDeltaMovement().y > 0.0) {
                net.minecraft.world.phys.Vec3 v = entity.getDeltaMovement();
                entity.setDeltaMovement(v.x, -v.y, v.z);
            }
        });
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        double y = mob.getY();
        double[] s = RISING.computeIfAbsent(mob, m -> new double[] {y, y});
        mob.resetFallDistance();        // falling up never counts as falling down
        if (mob.verticalCollision && !mob.onGround()) {
            // Hit a ceiling: the fall upward ends here, as hard as a fall that far would.
            double fell = y - s[1];
            if (fell > 3.0) {
                mob.hurtServer(level, level.damageSources().fall(), (float) Math.ceil(fell - 3.0));
            }
            s[1] = y;
        } else if (mob.onGround()) {
            s[1] = y;
        }
        if (y - s[0] >= MAX_RISE) {
            mob.removeEffect(ModEffects.upwelling());   // as high as it goes: Slow Falling takes over
        }
        return true;
    }

    // The next fall upward does no harm (the Curator turning its reliquary over shouldn't cost a dozen hearts).
    public static void softLanding(LivingEntity entity) {
        RISING.put(entity, new double[] {entity.getY(), Double.POSITIVE_INFINITY});
    }

    private static void wearOff(LivingEntity entity, MobEffectInstance instance) {
        if (entity == null || instance == null || !instance.is(ModEffects.upwelling()) || entity.level().isClientSide()) {
            return;
        }
        RISING.remove(entity);
        entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS, 0, false, true));
    }
}
