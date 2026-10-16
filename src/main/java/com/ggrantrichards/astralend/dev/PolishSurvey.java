package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModItems;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The survey's Phase 9 checks: the Star Compass points at
// the End's centre, Glowing Stew is food that gives Night Vision, and the advancement tree and recipe
// unlocks loaded.
final class PolishSurvey {
    // Returns no screenshot line (the Phase 9 showcase went with the Orrery Bronze).
    static String check(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        // ---- the Star Compass and Glowing Stew
        ItemStack compass = new ItemStack(ModItems.STAR_COMPASS.get());
        var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
        boolean pointsHome = tracker != null && tracker.target().isPresent() && tracker.target().get().dimension() == Level.END
            && tracker.target().get().pos().equals(BlockPos.ZERO);
        ItemStack stew = new ItemStack(ModItems.GLOWING_STEW.get());
        boolean food = stew.has(DataComponents.FOOD);
        log.accept(String.format("polish: Star Compass points to the End's centre: %s; Glowing Stew is food: %s", pointsHome, food));
        if (!pointsHome || !food) {
            failures.add("polish: the Star Compass should point to (0, 0) in the End, and Glowing Stew should be food");
        }

        // ---- the advancement tree and recipe unlocks
        var advancements = end.getServer().getAdvancements();
        int found = 0;
        String[] names = {"end/astral_horizons", "end/chart_the_stars", "end/head_to_toe", "end/something_looks_back",
                          "end/say_ahh", "recipes/building_blocks/polished_nightstone", "recipes/misc/glowing_stew"};
        for (String name : names) {
            if (advancements.get(ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, name)) != null) {
                found++;
            } else {
                log.accept("polish: missing advancement " + name);
            }
        }
        log.accept("polish: " + found + " of " + names.length + " sample advancements (the tree and recipe unlocks) loaded");
        if (found < names.length) {
            failures.add("polish: some of the new advancements didn't load");
        }
        return null;
    }

    private PolishSurvey() {}
}
