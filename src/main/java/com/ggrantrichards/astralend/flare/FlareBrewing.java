package com.ggrantrichards.astralend.flare;

import com.ggrantrichards.astralend.ModEffects;
import com.ggrantrichards.astralend.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;

// The brewing stand recipes for Flare Ward:
//   Awkward Potion + Skimmer Film -> Potion of Flare Ward (3:00)
//   Potion of Flare Ward + Flare Pollen -> Potion of Flare Ward (8:00)
// Works for splash and lingering potions too.
public final class FlareBrewing {
    public static void register() {
        BrewingRecipeRegistry.addRecipe(new Mix(Potions.AWKWARD, ModItems.SKIMMER_FILM.get(), false));
        BrewingRecipeRegistry.addRecipe(new Mix(null, ModItems.FLARE_POLLEN.get(), true));
    }

    private record Mix(Holder<Potion> from, Item ingredient, boolean lengthen) implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            PotionContents contents = input.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                return false;
            }
            Holder<Potion> wanted = this.from != null ? this.from : ModEffects.FLARE_WARD_POTION.getHolder().orElseThrow();
            return contents.is(wanted);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(this.ingredient);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!this.isInput(input) || !this.isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            Holder<Potion> result = (this.lengthen ? ModEffects.LONG_FLARE_WARD_POTION : ModEffects.FLARE_WARD_POTION).getHolder().orElseThrow();
            return PotionContents.createItemStack(input.getItem(), result);
        }
    }

    private FlareBrewing() {}
}
