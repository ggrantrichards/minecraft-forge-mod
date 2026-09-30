package com.ggrantrichards.astralend;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// "The Astral End" tab in the creative inventory, holding everything the mod adds.
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AstralEnd.MODID);

    public static final RegistryObject<CreativeModeTab> ASTRAL_END = TABS.register("astral_end", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.astralend"))
        .icon(() -> new ItemStack(ModItems.VOID_CRYSTAL.get()))
        .displayItems((parameters, output) -> {
            output.accept(ModBlocks.UMBRAL_STONE.get());
            output.accept(ModBlocks.UMBRAL_STONE_STAIRS.get());
            output.accept(ModBlocks.UMBRAL_STONE_SLAB.get());
            output.accept(ModBlocks.UMBRAL_STONE_WALL.get());
            output.accept(ModBlocks.POLISHED_UMBRAL_STONE.get());
            output.accept(ModBlocks.POLISHED_UMBRAL_STONE_STAIRS.get());
            output.accept(ModBlocks.POLISHED_UMBRAL_STONE_SLAB.get());
            output.accept(ModBlocks.POLISHED_UMBRAL_STONE_WALL.get());
            output.accept(ModBlocks.UMBRAL_STONE_BRICKS.get());
            output.accept(ModBlocks.UMBRAL_STONE_BRICK_STAIRS.get());
            output.accept(ModBlocks.UMBRAL_STONE_BRICK_SLAB.get());
            output.accept(ModBlocks.UMBRAL_STONE_BRICK_WALL.get());
            output.accept(ModBlocks.CHISELED_UMBRAL_STONE.get());
            output.accept(ModBlocks.VOID_CRYSTAL_ORE.get());
            output.accept(ModBlocks.VOID_CRYSTAL_BLOCK.get());
            output.accept(ModBlocks.METEORIC_IRON_ORE.get());
            output.accept(ModBlocks.RAW_METEORIC_IRON_BLOCK.get());
            output.accept(ModBlocks.METEORIC_IRON_BLOCK.get());
            output.accept(ModBlocks.STARDUST_BLOCK.get());
            output.accept(ModBlocks.ASTRAL_GLASS.get());
            output.accept(ModBlocks.LEVIATHAN_BONE_BLOCK.get());
            output.accept(ModBlocks.NEBULA_MOSS.get());
            output.accept(ModBlocks.LUMINESHROOM.get());
            output.accept(ModBlocks.STARBLOOM.get());
            output.accept(ModBlocks.ASTRAL_VINE.get());
            output.accept(ModItems.COMET_CRESS.get());
            output.accept(ModItems.VOID_CRYSTAL.get());
            output.accept(ModItems.RAW_METEORIC_IRON.get());
            output.accept(ModItems.METEORIC_IRON_INGOT.get());
            output.accept(ModItems.METEORIC_IRON_NUGGET.get());
        })
        .build());

    private ModCreativeTabs() {}
}
