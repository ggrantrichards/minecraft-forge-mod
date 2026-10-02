package com.ggrantrichards.astralend.starforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

// Adds one item, with a chance, to the loot of the tables its conditions pick (a forge:loot_table_id
// condition). Used to put Starforge Templates in End City treasure
// (data/astralend/loot_modifiers/end_city_templates.json) without replacing vanilla's table.
public class AddItemLoot extends LootModifier {
    public static final MapCodec<AddItemLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).and(inst.group(
        BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
        Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance)
    )).apply(inst, AddItemLoot::new));

    private final Item item;
    private final float chance;

    public AddItemLoot(LootItemCondition[] conditions, Item item, float chance) {
        super(conditions);
        this.item = item;
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(LootTable table, ObjectArrayList<ItemStack> loot, LootContext context) {
        if (context.getRandom().nextFloat() < this.chance) {
            loot.add(new ItemStack(this.item));
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
