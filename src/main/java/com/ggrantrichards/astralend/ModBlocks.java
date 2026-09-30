package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.block.AstralVineBlock;
import com.ggrantrichards.astralend.block.AstralVinePlantBlock;
import com.ggrantrichards.astralend.block.CometCressBlock;
import com.ggrantrichards.astralend.block.LumineshroomBlock;
import com.ggrantrichards.astralend.block.StarbloomBlock;
import com.ggrantrichards.astralend.block.StardustBlock;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Every block the mod adds. Each gets the ID "astralend:<name>", and most also get a
// BlockItem so they can sit in your inventory. Properties mirror the closest vanilla block.
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AstralEnd.MODID);

    // ---- Umbral Stone: the dark stone of the Dark Matter Belts (deepslate-like) ----
    public static final RegistryObject<Block> UMBRAL_STONE = register("umbral_stone", Block::new,
        () -> stone(SoundType.DEEPSLATE));
    public static final RegistryObject<Block> UMBRAL_STONE_STAIRS = stairs("umbral_stone_stairs", UMBRAL_STONE);
    public static final RegistryObject<Block> UMBRAL_STONE_SLAB = slab("umbral_stone_slab", UMBRAL_STONE);
    public static final RegistryObject<Block> UMBRAL_STONE_WALL = wall("umbral_stone_wall", UMBRAL_STONE);

    public static final RegistryObject<Block> POLISHED_UMBRAL_STONE = register("polished_umbral_stone", Block::new,
        () -> stone(SoundType.POLISHED_DEEPSLATE));
    public static final RegistryObject<Block> POLISHED_UMBRAL_STONE_STAIRS = stairs("polished_umbral_stone_stairs", POLISHED_UMBRAL_STONE);
    public static final RegistryObject<Block> POLISHED_UMBRAL_STONE_SLAB = slab("polished_umbral_stone_slab", POLISHED_UMBRAL_STONE);
    public static final RegistryObject<Block> POLISHED_UMBRAL_STONE_WALL = wall("polished_umbral_stone_wall", POLISHED_UMBRAL_STONE);

    public static final RegistryObject<Block> UMBRAL_STONE_BRICKS = register("umbral_stone_bricks", Block::new,
        () -> stone(SoundType.DEEPSLATE_BRICKS));
    public static final RegistryObject<Block> UMBRAL_STONE_BRICK_STAIRS = stairs("umbral_stone_brick_stairs", UMBRAL_STONE_BRICKS);
    public static final RegistryObject<Block> UMBRAL_STONE_BRICK_SLAB = slab("umbral_stone_brick_slab", UMBRAL_STONE_BRICKS);
    public static final RegistryObject<Block> UMBRAL_STONE_BRICK_WALL = wall("umbral_stone_brick_wall", UMBRAL_STONE_BRICKS);

    public static final RegistryObject<Block> CHISELED_UMBRAL_STONE = register("chiseled_umbral_stone", Block::new,
        () -> stone(SoundType.DEEPSLATE_BRICKS));

    // ---- Ores and material blocks ----
    // Void Crystal: like diamond ore (iron pickaxe, 3-7 XP, Fortune works through the loot table).
    public static final RegistryObject<Block> VOID_CRYSTAL_ORE = register("void_crystal_ore",
        p -> new DropExperienceBlock(UniformInt.of(3, 7), p),
        () -> stone(SoundType.DEEPSLATE).strength(4.5F, 3.0F));
    public static final RegistryObject<Block> VOID_CRYSTAL_BLOCK = register("void_crystal_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).requiresCorrectToolForDrops()
            .strength(5.0F, 6.0F).sound(SoundType.AMETHYST));

    // Meteoric Iron: embedded in comet cores. Needs a diamond pickaxe; XP comes from smelting, like iron.
    public static final RegistryObject<Block> METEORIC_IRON_ORE = register("meteoric_iron_ore",
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(5.0F, 9.0F).sound(SoundType.ANCIENT_DEBRIS));
    public static final RegistryObject<Block> METEORIC_IRON_BLOCK = register("meteoric_iron_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
            .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> RAW_METEORIC_IRON_BLOCK = register("raw_meteoric_iron_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.RAW_IRON).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(5.0F, 6.0F));

    // ---- Comet and cosmic blocks ----
    // Stardust drifts UP instead of falling down. See StardustBlock.
    public static final RegistryObject<Block> STARDUST_BLOCK = register("stardust_block", StardustBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.SNARE)
            .strength(0.5F).sound(SoundType.SAND));
    // Astral Glass: smelted from Stardust. Behaves like vanilla glass (needs Silk Touch to pick up).
    public static final RegistryObject<Block> ASTRAL_GLASS = register("astral_glass", TransparentBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.HAT)
            .strength(0.3F).sound(SoundType.GLASS).noOcclusion()
            .isValidSpawn((state, level, pos, entityType) -> false)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos, aabb) -> false));
    // Leviathan Bone: fossil bone of the ancient Star Leviathans. A pillar block, like vanilla's bone block.
    public static final RegistryObject<Block> LEVIATHAN_BONE_BLOCK = register("leviathan_bone_block", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.XYLOPHONE)
            .requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK));
    // Nebula Moss: the soft ground cover of the Nebula Fields.
    public static final RegistryObject<Block> NEBULA_MOSS = register("nebula_moss", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.1F).sound(SoundType.MOSS)
            .pushReaction(PushReaction.POPPED));

    // ---- Plants ----
    // Lumineshroom: a glowing mushroom whose texture slowly "breathes". Light level is fixed, for performance.
    public static final RegistryObject<Block> LUMINESHROOM = register("lumineshroom", LumineshroomBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().instabreak()
            .sound(SoundType.FUNGUS).lightLevel(state -> 10).pushReaction(PushReaction.POPPED));
    public static final RegistryObject<Block> STARBLOOM = register("starbloom", StarbloomBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak()
            .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED));
    // Astral Vine: grows upward like twisting vines. The "plant" block is the vine's body and has no item.
    public static final RegistryObject<Block> ASTRAL_VINE = register("astral_vine", AstralVineBlock::new,
        () -> vine());
    public static final RegistryObject<Block> ASTRAL_VINE_PLANT = registerNoItem("astral_vine_plant", AstralVinePlantBlock::new,
        () -> vine());
    // Comet Cress: a crop that hangs from the underside of Stardust. Its item is ModItems.COMET_CRESS.
    public static final RegistryObject<Block> COMET_CRESS = registerNoItem("comet_cress", CometCressBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noCollision().randomTicks().instabreak()
            .sound(SoundType.CROP).pushReaction(PushReaction.POPPED));

    // ---- helpers ----

    private static BlockBehaviour.Properties stone(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(sound);
    }

    private static BlockBehaviour.Properties vine() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).randomTicks().noCollision().instabreak()
            .sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.POPPED);
    }

    private static RegistryObject<Block> stairs(String name, RegistryObject<Block> base) {
        return register(name, p -> new StairBlock(base.get().defaultBlockState(), p),
            () -> BlockBehaviour.Properties.ofLegacyCopy(base.get()));
    }

    private static RegistryObject<Block> slab(String name, RegistryObject<Block> base) {
        return register(name, SlabBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(base.get()));
    }

    private static RegistryObject<Block> wall(String name, RegistryObject<Block> base) {
        return register(name, WallBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(base.get()).forceSolidOn());
    }

    // Registers a block and a matching BlockItem with the same name.
    private static RegistryObject<Block> register(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                  Supplier<BlockBehaviour.Properties> properties) {
        RegistryObject<Block> block = registerNoItem(name, factory, properties);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(),
            new Item.Properties().setId(ModItems.ITEMS.key(name)).useBlockDescriptionPrefix()));
        return block;
    }

    private static RegistryObject<Block> registerNoItem(String name, Function<BlockBehaviour.Properties, Block> factory,
                                                        Supplier<BlockBehaviour.Properties> properties) {
        return BLOCKS.register(name, () -> factory.apply(properties.get().setId(BLOCKS.key(name))));
    }

    private ModBlocks() {}
}
