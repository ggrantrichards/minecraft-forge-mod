package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The two upgrades that change what a block drops, as one global loot modifier
// (data/astralend/loot_modifiers/starforge_tools.json):
//   - Meteoric tools smelt what they mine: ores drop ingots, sand glass, logs charcoal. Fortune
//     still works (it already multiplied the raw drops); Silk Touch wins, and nothing is smelted.
//   - Singularity tools pull what they mine straight into your inventory (what doesn't fit lands at
//     your feet). The experience follows you too (StarforgeEffects).
public class StarforgeLoot extends LootModifier {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AstralEnd.MODID);
    public static final MapCodec<StarforgeLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, StarforgeLoot::new));
    public static final RegistryObject<MapCodec<StarforgeLoot>> STARFORGE_TOOLS = SERIALIZERS.register("starforge_tools", () -> CODEC);

    public StarforgeLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(LootTable table, ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!context.hasParameter(LootContextParams.BLOCK_STATE)
            || !(context.getOptional(LootContextParams.THIS_ENTITY) instanceof Player player)) {
            return loot;
        }
        ItemStack tool = player.getMainHandItem();
        StarforgeUpgrade upgrade = Starforge.get(tool);
        if (upgrade == null || !tool.is(Starforge.TOOLS)) {
            return loot;
        }
        ServerLevel level = context.getLevel();
        if (upgrade == StarforgeUpgrade.METEORIC) {
            var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if (EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0) {
                return loot;
            }
            ObjectArrayList<ItemStack> smelted = new ObjectArrayList<>();
            for (ItemStack stack : loot) {
                var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
                if (recipe.isPresent()) {
                    ItemStack out = recipe.get().value().assemble(new SingleRecipeInput(stack));
                    out.setCount(Math.min(out.getMaxStackSize(), out.getCount() * stack.getCount()));
                    smelted.add(out);
                } else {
                    smelted.add(stack);
                }
            }
            return smelted;
        }
        if (upgrade == StarforgeUpgrade.SINGULARITY) {
            for (ItemStack stack : loot) {
                if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                    Block.popResource(level, player.blockPosition(), stack);
                }
            }
            return new ObjectArrayList<>();
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
