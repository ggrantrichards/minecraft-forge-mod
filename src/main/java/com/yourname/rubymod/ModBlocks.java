package com.yourname.rubymod;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Every block in the game lives in a "registry". A DeferredRegister lets us add ours
// at the right moment during loading. Everything here gets the ID "rubymod:<name>".
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RubyMod.MODID);

    // /give @s rubymod:ruby_block
    public static final RegistryObject<Block> RUBY_BLOCK = BLOCKS.register("ruby_block",
        () -> new Block(BlockBehaviour.Properties.of()
            .setId(BLOCKS.key("ruby_block"))
            .mapColor(MapColor.COLOR_RED)
            .strength(5.0f, 6.0f)          // hardness (mining time), blast resistance - same as an iron block
            .requiresCorrectToolForDrops() // drops nothing unless mined with the right tool (see the tags in data/)
            .sound(SoundType.METAL)
        )
    );

    private ModBlocks() {}
}
