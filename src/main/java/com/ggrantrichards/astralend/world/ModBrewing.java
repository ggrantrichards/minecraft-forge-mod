package com.ggrantrichards.astralend.world;

import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModEffects;
import com.ggrantrichards.astralend.ModItems;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent;

// (1.21.1 port) The brewing recipes, which 26.3 reads from data/astralend/recipe/brewing/:
//   Awkward + Sunscale Lichen -> Sun Ward, + Flare Pollen -> long;
//   Awkward + Upwell Mote -> Upwelling, + Redstone -> long;
//   Awkward + Umbral Membrane -> Invisibility.
// Splash and lingering versions come from the vanilla gunpowder and dragon's breath steps.
public final class ModBrewing {
    public static void register() {
        com.ggrantrichards.astralend.Events.on(BrewingRecipeRegisterEvent.class, event -> {
            var builder = event.getBuilder();
            var sunWard = ModEffects.SUN_WARD_POTION.getHolder().orElseThrow();
            var upwelling = ModEffects.UPWELLING_POTION.getHolder().orElseThrow();
            builder.addMix(Potions.AWKWARD, ModBlocks.SUNSCALE_LICHEN.get().asItem(), sunWard);
            builder.addMix(sunWard, ModItems.FLARE_POLLEN.get(), ModEffects.LONG_SUN_WARD_POTION.getHolder().orElseThrow());
            builder.addMix(Potions.AWKWARD, ModItems.UPWELL_MOTE.get(), upwelling);
            builder.addMix(upwelling, net.minecraft.world.item.Items.REDSTONE, ModEffects.LONG_UPWELLING_POTION.getHolder().orElseThrow());
            builder.addMix(Potions.AWKWARD, ModItems.UMBRAL_MEMBRANE.get(), Potions.INVISIBILITY);
        });
    }

    private ModBrewing() {}
}
