package com.ggrantrichards.astralend;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Flare Ward (Phase 5): while it lasts, Stellar Flares can't burn you (StellarFlares checks for it).
// Brewed from an Awkward Potion and Skimmer Film; Flare Pollen makes it last longer (FlareBrewing).
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AstralEnd.MODID);
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, AstralEnd.MODID);

    public static final RegistryObject<MobEffect> FLARE_WARD = EFFECTS.register("flare_ward", () -> new WardEffect(0xE8913A));

    public static final RegistryObject<Potion> FLARE_WARD_POTION = POTIONS.register("flare_ward",
        () -> new Potion("flare_ward", new MobEffectInstance(ward(), 3600)));      // 3 minutes
    public static final RegistryObject<Potion> LONG_FLARE_WARD_POTION = POTIONS.register("long_flare_ward",
        () -> new Potion("flare_ward", new MobEffectInstance(ward(), 9600)));      // 8 minutes

    public static Holder<MobEffect> ward() {
        return FLARE_WARD.getHolder().orElseThrow();
    }

    private static final class WardEffect extends MobEffect {
        WardEffect(int colour) {
            super(MobEffectCategory.BENEFICIAL, colour);
        }
    }

    private ModEffects() {}
}
