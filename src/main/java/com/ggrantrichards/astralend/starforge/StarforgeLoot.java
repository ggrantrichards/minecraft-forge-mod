package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.mojang.serialization.MapCodec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// The mod's global loot modifier types. (The distillation cut the two upgrades that changed block
// drops, Meteoric's Smelting and Singularity's Pull; what's left is AddItemLoot, which puts Starforge
// Templates into End City chests.)
public final class StarforgeLoot {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, AstralEnd.MODID);
    public static final RegistryObject<MapCodec<AddItemLoot>> ADD_ITEM = SERIALIZERS.register("add_item", () -> AddItemLoot.CODEC);

    private StarforgeLoot() {}
}
