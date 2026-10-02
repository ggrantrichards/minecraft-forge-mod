package com.ggrantrichards.astralend;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Sun Ward (Phase 5): while it lasts, Sunstorms can't burn you (Sunstorms checks for it).
// Brewed from an Awkward Potion and Ray Film; Flare Pollen makes it last longer (the brewing
// recipes are data: data/astralend/recipe/brewing/, written by tools/generate_resources.py).
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AstralEnd.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, AstralEnd.MODID);

    public static final RegistryObject<MobEffect> SUN_WARD = EFFECTS.register("sun_ward", () -> new WardEffect(0xE8913A));

    public static final RegistryObject<Potion> SUN_WARD_POTION = POTIONS.register("sun_ward",
        () -> new Potion("sun_ward", new MobEffectInstance(ward(), 3600)));      // 3 minutes
    public static final RegistryObject<Potion> LONG_SUN_WARD_POTION = POTIONS.register("long_sun_ward",
        () -> new Potion("sun_ward", new MobEffectInstance(ward(), 9600)));      // 8 minutes

    public static Holder<MobEffect> ward() {
        return SUN_WARD.getHolder().orElseThrow();
    }

    private static final class WardEffect extends MobEffect {
        WardEffect(int colour) {
            super(MobEffectCategory.BENEFICIAL, colour);
        }
    }

    private ModEffects() {}
}
