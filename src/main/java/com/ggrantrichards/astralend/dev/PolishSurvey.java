package com.ggrantrichards.astralend.dev;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModBlocks;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.orrery.Bronze;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

// The survey's Phase 9 checks: Orrery Bronze waxes and scrapes like copper, the Star Compass points at
// the End's centre, Glowing Stew is food that gives Night Vision, and the advancement tree and recipe
// unlocks loaded.
final class PolishSurvey {
    static void check(ServerLevel end, BlockPos standOn, List<String> failures, Consumer<String> log) {
        ServerPlayer player = new ServerPlayer(end.getServer(), end,
            new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("astralend-survey-9".getBytes()), "AstralSurvey9"),
            net.minecraft.server.level.ClientInformation.createDefault());
        BlockPos at = standOn.offset(-4, 3, -4);

        // ---- bronze: wax it, scrape the wax off, scrape tarnish off
        end.setBlock(at, ModBlocks.ORRERY_BRONZE.get().defaultBlockState(), 3);
        String waxed = use(end, player, at, new ItemStack(Items.HONEYCOMB));
        String unwaxed = use(end, player, at, new ItemStack(Items.DIAMOND_AXE));
        end.setBlock(at, ModBlocks.WEATHERED_ORRERY_BRONZE.get().defaultBlockState(), 3);
        String scraped = use(end, player, at, new ItemStack(Items.DIAMOND_AXE));
        Block stairs = ModBlocks.BRONZE.get("tarnished_cut_orrery_bronze_stairs").get();
        end.setBlock(at, stairs.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING,
            net.minecraft.core.Direction.EAST), 3);
        String waxedStairs = use(end, player, at, new ItemStack(Items.HONEYCOMB));
        boolean facingKept = end.getBlockState(at).hasProperty(net.minecraft.world.level.block.StairBlock.FACING)
            && end.getBlockState(at).getValue(net.minecraft.world.level.block.StairBlock.FACING) == net.minecraft.core.Direction.EAST;
        end.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        log.accept(String.format("polish: bronze + honeycomb -> %s, + axe -> %s; weathered + axe -> %s; stairs + honeycomb -> %s (facing kept: %s)",
            waxed, unwaxed, scraped, waxedStairs, facingKept));
        if (!waxed.equals("waxed_orrery_bronze") || !unwaxed.equals("orrery_bronze") || !scraped.equals("tarnished_orrery_bronze")
            || !waxedStairs.equals("waxed_tarnished_cut_orrery_bronze_stairs") || !facingKept) {
            failures.add("polish: Orrery Bronze should wax with honeycomb and scrape back with an axe, like copper, keeping its shape");
        }

        // ---- the Star Compass and Glowing Stew
        ItemStack compass = new ItemStack(ModItems.STAR_COMPASS.get());
        var tracker = compass.get(DataComponents.LODESTONE_TRACKER);
        boolean pointsHome = tracker != null && tracker.target().isPresent() && tracker.target().get().dimension() == Level.END
            && tracker.target().get().pos().equals(BlockPos.ZERO);
        ItemStack stew = new ItemStack(ModItems.GLOWING_STEW.get());
        boolean food = stew.has(DataComponents.FOOD) && stew.has(DataComponents.CONSUMABLE);
        log.accept(String.format("polish: Star Compass points to the End's centre: %s; Glowing Stew is food: %s", pointsHome, food));
        if (!pointsHome || !food) {
            failures.add("polish: the Star Compass should point to (0, 0) in the End, and Glowing Stew should be food");
        }

        // ---- the advancement tree and recipe unlocks
        var advancements = end.getServer().getAdvancements();
        int found = 0;
        for (String name : new String[] {"end/astral_horizons", "end/chart_the_stars", "end/head_to_toe", "end/clockwork_undone",
                                         "end/event_horizon", "recipes/building_blocks/cut_orrery_bronze", "recipes/misc/glowing_stew"}) {
            if (advancements.get(Identifier.fromNamespaceAndPath(AstralEnd.MODID, name)) != null) {
                found++;
            }
        }
        log.accept("polish: " + found + " of 7 sample advancements (the tree and recipe unlocks) loaded");
        if (found < 7) {
            failures.add("polish: some of the new advancements didn't load");
        }
    }

    private static String use(ServerLevel end, ServerPlayer player, BlockPos at, ItemStack tool) {
        player.setItemSlot(EquipmentSlot.MAINHAND, tool);
        BlockState state = end.getBlockState(at);
        Bronze.Aging aging = state.getBlock() instanceof Bronze.Ages ages ? ages.aging() : null;
        if (aging != null) {
            Bronze.use(aging, tool, state, end, at, player, InteractionHand.MAIN_HAND);
        }
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(end.getBlockState(at).getBlock()).getPath();
    }

    private PolishSurvey() {}
}
