package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.block.AstralVineBlock;
import com.ggrantrichards.astralend.block.AstralVinePlantBlock;
import com.ggrantrichards.astralend.block.CollectorPedestalBlock;
import com.ggrantrichards.astralend.block.CometCressBlock;
import com.ggrantrichards.astralend.block.EndPlantBlock;
import com.ggrantrichards.astralend.block.EndTallPlantBlock;
import com.ggrantrichards.astralend.block.FlarebloomBlock;
import com.ggrantrichards.astralend.block.UpwellCoreBlock;
import com.ggrantrichards.astralend.block.LumineshroomBlock;
import com.ggrantrichards.astralend.block.NebulaTendrilBlock;
import com.ggrantrichards.astralend.block.NebulaTendrilPlantBlock;
import com.ggrantrichards.astralend.block.NullLanternBlock;
import com.ggrantrichards.astralend.block.NullwoodSaplingBlock;
import com.ggrantrichards.astralend.block.StarbloomBlock;
import com.ggrantrichards.astralend.block.StardustBlock;
import com.ggrantrichards.astralend.block.SunscaleLichenBlock;
import com.ggrantrichards.astralend.block.SunshadeBlock;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Every block the mod adds. Each gets the ID "astralend:<name>", and most also get a
// BlockItem so they can sit in your inventory. Properties mirror the closest vanilla block.
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, AstralEnd.MODID);

    // The End's natural ground blocks, which its plants grow on (a block tag, so datapacks can add to it).
    public static final TagKey<Block> ASTRAL_SOIL = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "astral_soil"));

    // ---- Nightstone: the dark stone of the Deep Hollows and the Crags (deepslate-like) ----
    public static final RegistryObject<Block> NIGHTSTONE = register("nightstone", Block::new,
        () -> stone(SoundType.DEEPSLATE));
    public static final RegistryObject<Block> NIGHTSTONE_STAIRS = stairs("nightstone_stairs", NIGHTSTONE);
    public static final RegistryObject<Block> NIGHTSTONE_SLAB = slab("nightstone_slab", NIGHTSTONE);
    public static final RegistryObject<Block> NIGHTSTONE_WALL = wall("nightstone_wall", NIGHTSTONE);

    public static final RegistryObject<Block> POLISHED_NIGHTSTONE = register("polished_nightstone", Block::new,
        () -> stone(SoundType.POLISHED_DEEPSLATE));
    public static final RegistryObject<Block> POLISHED_NIGHTSTONE_STAIRS = stairs("polished_nightstone_stairs", POLISHED_NIGHTSTONE);
    public static final RegistryObject<Block> POLISHED_NIGHTSTONE_SLAB = slab("polished_nightstone_slab", POLISHED_NIGHTSTONE);
    public static final RegistryObject<Block> POLISHED_NIGHTSTONE_WALL = wall("polished_nightstone_wall", POLISHED_NIGHTSTONE);

    public static final RegistryObject<Block> NIGHTSTONE_BRICKS = register("nightstone_bricks", Block::new,
        () -> stone(SoundType.DEEPSLATE_BRICKS));
    public static final RegistryObject<Block> NIGHTSTONE_BRICK_STAIRS = stairs("nightstone_brick_stairs", NIGHTSTONE_BRICKS);
    public static final RegistryObject<Block> NIGHTSTONE_BRICK_SLAB = slab("nightstone_brick_slab", NIGHTSTONE_BRICKS);
    public static final RegistryObject<Block> NIGHTSTONE_BRICK_WALL = wall("nightstone_brick_wall", NIGHTSTONE_BRICKS);

    public static final RegistryObject<Block> CHISELED_NIGHTSTONE = register("chiseled_nightstone", Block::new,
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
    // Its variants (after beta.4), so a skeleton isn't one block a thousand times over: weathered - grey,
    // cracked and pitted - and lichened, with the End's violet lichen grown over it.
    public static final RegistryObject<Block> WEATHERED_LEVIATHAN_BONE = register("weathered_leviathan_bone", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.XYLOPHONE)
            .requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK));
    public static final RegistryObject<Block> LICHENED_LEVIATHAN_BONE = register("lichened_leviathan_bone", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.XYLOPHONE)
            .requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK));
    // Chorus Moss: the soft ground cover of the Nebula Fields. It's springy:
    // you walk a little slower on it, and it softens a fall (ChorusMossBlock).
    public static final RegistryObject<Block> CHORUS_MOSS = register("chorus_moss", com.ggrantrichards.astralend.block.ChorusMossBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1F).sound(SoundType.MOSS)
            .speedFactor(0.9F).pushReaction(PushReaction.POPPED));

    // ---- Plants ----
    // Lumineshroom: a glowing mushroom whose texture slowly "breathes". Light level is fixed, for performance.
    public static final RegistryObject<Block> LUMINESHROOM = register("lumineshroom", LumineshroomBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollision().instabreak()
            .sound(SoundType.FUNGUS).lightLevel(state -> 10).pushReaction(PushReaction.POPPED));
    public static final RegistryObject<Block> STARBLOOM = register("starbloom", StarbloomBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak()
            .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED));
    // Astral Vine: grows upward like twisting vines. The "plant" block is the vine's body and has no item.
    // Its tip glows cold pale blue (after beta.4): the reed groves light the Nebula Fields' rose moss.
    public static final RegistryObject<Block> ASTRAL_VINE = register("astral_vine", AstralVineBlock::new,
        () -> vine().lightLevel(state -> 7));
    public static final RegistryObject<Block> ASTRAL_VINE_PLANT = registerNoItem("astral_vine_plant", AstralVinePlantBlock::new,
        () -> vine());
    // The distillation: Hollow Vines hang from the Deep Hollows' Nullwood to the ground, like weeping vines.
    public static final RegistryObject<Block> HOLLOW_VINES = register("hollow_vines", com.ggrantrichards.astralend.block.HollowVinesBlock::new,
        () -> vine().lightLevel(state -> 9));   // the glowing tips light the Deep Hollows
    public static final RegistryObject<Block> HOLLOW_VINES_PLANT = registerNoItem("hollow_vines_plant",
        com.ggrantrichards.astralend.block.HollowVinesPlantBlock::new, () -> vine());
    // Comet Cress: a crop that hangs from the underside of Stardust. Its item is ModItems.COMET_CRESS.
    public static final RegistryObject<Block> COMET_CRESS = registerNoItem("comet_cress", CometCressBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).noCollision().randomTicks().instabreak()
            .sound(SoundType.CROP).pushReaction(PushReaction.POPPED));

    // ================================================================ Phase 3

    // ---- Ashen Barrens: pale, wide shelf islands ----
    public static final RegistryObject<Block> PALE_END_STONE = register("pale_end_stone", Block::new,
        () -> endStone(MapColor.TERRACOTTA_WHITE));
    public static final RegistryObject<Block> PALE_END_STONE_STAIRS = stairs("pale_end_stone_stairs", PALE_END_STONE);
    public static final RegistryObject<Block> PALE_END_STONE_SLAB = slab("pale_end_stone_slab", PALE_END_STONE);
    public static final RegistryObject<Block> PALE_END_STONE_WALL = wall("pale_end_stone_wall", PALE_END_STONE);
    public static final RegistryObject<Block> PALE_END_STONE_BRICKS = register("pale_end_stone_bricks", Block::new,
        () -> endStone(MapColor.TERRACOTTA_WHITE));
    public static final RegistryObject<Block> PALE_END_STONE_BRICK_STAIRS = stairs("pale_end_stone_brick_stairs", PALE_END_STONE_BRICKS);
    public static final RegistryObject<Block> PALE_END_STONE_BRICK_SLAB = slab("pale_end_stone_brick_slab", PALE_END_STONE_BRICKS);
    public static final RegistryObject<Block> PALE_END_STONE_BRICK_WALL = wall("pale_end_stone_brick_wall", PALE_END_STONE_BRICKS);
    public static final RegistryObject<Block> PALE_END_STONE_TILES = register("pale_end_stone_tiles", Block::new,
        () -> endStone(MapColor.TERRACOTTA_WHITE));
    // Shimmer Grass: short, pale grass. Like vanilla grass, it drops only when sheared.
    public static final RegistryObject<Block> SHIMMER_GRASS = register("shimmer_grass",
        p -> new EndPlantBlock(Block.column(12.0, 0.0, 13.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_LIGHT_BLUE, SoundType.GRASS).replaceable().offsetType(BlockBehaviour.OffsetType.XYZ));

    // ---- Comet Wake: scorched islands, stardust and blue ice ----
    public static final RegistryObject<Block> CHARRED_END_STONE = register("charred_end_stone", Block::new,
        () -> endStone(MapColor.COLOR_BLACK));
    public static final RegistryObject<Block> CHARRED_END_STONE_STAIRS = stairs("charred_end_stone_stairs", CHARRED_END_STONE);
    public static final RegistryObject<Block> CHARRED_END_STONE_SLAB = slab("charred_end_stone_slab", CHARRED_END_STONE);
    public static final RegistryObject<Block> CHARRED_END_STONE_WALL = wall("charred_end_stone_wall", CHARRED_END_STONE);

    // ---- The Corona: bleached stone, Solarite, and the life that adapted (or didn't) ----
    public static final RegistryObject<Block> SUNSCORCHED_STONE = register("sunscorched_stone", Block::new,
        () -> endStone(MapColor.SAND));
    public static final RegistryObject<Block> SUNSCORCHED_STONE_STAIRS = stairs("sunscorched_stone_stairs", SUNSCORCHED_STONE);
    public static final RegistryObject<Block> SUNSCORCHED_STONE_SLAB = slab("sunscorched_stone_slab", SUNSCORCHED_STONE);
    public static final RegistryObject<Block> SUNSCORCHED_STONE_WALL = wall("sunscorched_stone_wall", SUNSCORCHED_STONE);
    public static final RegistryObject<Block> POLISHED_SUNSCORCHED_STONE = register("polished_sunscorched_stone", Block::new,
        () -> endStone(MapColor.SAND));
    public static final RegistryObject<Block> POLISHED_SUNSCORCHED_STONE_STAIRS = stairs("polished_sunscorched_stone_stairs", POLISHED_SUNSCORCHED_STONE);
    public static final RegistryObject<Block> POLISHED_SUNSCORCHED_STONE_SLAB = slab("polished_sunscorched_stone_slab", POLISHED_SUNSCORCHED_STONE);
    public static final RegistryObject<Block> POLISHED_SUNSCORCHED_STONE_WALL = wall("polished_sunscorched_stone_wall", POLISHED_SUNSCORCHED_STONE);
    public static final RegistryObject<Block> SUNSCORCHED_STONE_BRICKS = register("sunscorched_stone_bricks", Block::new,
        () -> endStone(MapColor.SAND));
    public static final RegistryObject<Block> SUNSCORCHED_STONE_BRICK_STAIRS = stairs("sunscorched_stone_brick_stairs", SUNSCORCHED_STONE_BRICKS);
    public static final RegistryObject<Block> SUNSCORCHED_STONE_BRICK_SLAB = slab("sunscorched_stone_brick_slab", SUNSCORCHED_STONE_BRICKS);
    public static final RegistryObject<Block> SUNSCORCHED_STONE_BRICK_WALL = wall("sunscorched_stone_brick_wall", SUNSCORCHED_STONE_BRICKS);
    // Solarite: only in the Ashen Barrens (it was the Corona's). Diamond pickaxe; XP comes from smelting, like Meteoric Iron.
    public static final RegistryObject<Block> SOLARITE_ORE = register("solarite_ore",
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        () -> endStone(MapColor.SAND).strength(4.5F, 9.0F).lightLevel(state -> 3));
    public static final RegistryObject<Block> RAW_SOLARITE_BLOCK = register("raw_solarite_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(5.0F, 6.0F).lightLevel(state -> 6));   // raw sun-metal, still warm
    public static final RegistryObject<Block> SOLARITE_BLOCK = register("solarite_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BELL)
            .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL).lightLevel(state -> 7));
    // Flarebloom: a two-block-tall flower, shut tight.
    public static final RegistryObject<Block> FLAREBLOOM = register("flarebloom", FlarebloomBlock::new,
        () -> plant(MapColor.COLOR_ORANGE, SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)
            .lightLevel(state -> state.getValue(FlarebloomBlock.OPEN) ? 9 : 0));
    // Sunscale Lichen: a scaly ground cover (a carpet) that holds a faint glow.
    // It's see-through between its patches, so it mustn't hide the top of the block it lies on
    // (noOcclusion), or the gaps would show the void.
    public static final RegistryObject<Block> SUNSCALE_LICHEN = register("sunscale_lichen", SunscaleLichenBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1F).sound(SoundType.MOSS_CARPET)
            .lightLevel(state -> state.getValue(SunscaleLichenBlock.CHARGED) ? 12 : 5).noOcclusion().pushReaction(PushReaction.POPPED));
    // Phase 5 (Sunstorms): cover you can hang anywhere, and the pedestal that charges crystals and cells.
    public static final RegistryObject<Block> SUNSHADE = register("sunshade", SunshadeBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.3F).sound(SoundType.WOOL).noOcclusion());
    public static final RegistryObject<Block> COLLECTOR_PEDESTAL = register("collector_pedestal", CollectorPedestalBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops().strength(2.0F, 6.0F)
            .sound(SoundType.STONE).noOcclusion()
            .lightLevel(state -> state.getValue(CollectorPedestalBlock.HOLDING).charged() ? 10 : 0));
    // The Maw, the Leviathan's skull (eye sockets, the dormant throat, and the woken wormhole)
    // and the Constellation Maze (star-bridges). Built into structures, so the parts
    // that make the puzzles work can't be broken, like an End Portal frame.
    public static final RegistryObject<Block> MAW_EYE = register("maw_eye", com.ggrantrichards.astralend.maw.MawEyeBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.XYLOPHONE)
            .strength(-1.0F, 3600000.0F).sound(SoundType.BONE_BLOCK).noLootTable()
            .lightLevel(state -> state.getValue(com.ggrantrichards.astralend.maw.MawEyeBlock.LIT) ? 10 : 0));
    public static final RegistryObject<Block> MAW_THROAT = register("maw_throat", com.ggrantrichards.astralend.maw.MawThroatBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .strength(-1.0F, 3600000.0F).sound(SoundType.DEEPSLATE).noLootTable());
    public static final RegistryObject<Block> WORMHOLE = registerNoItem("wormhole", com.ggrantrichards.astralend.maw.WormholeBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().strength(-1.0F, 3600000.0F)
            .sound(SoundType.GLASS).lightLevel(state -> 12).noLootTable());
    public static final RegistryObject<Block> STAR_BRIDGE = registerNoItem("star_bridge", com.ggrantrichards.astralend.maze.StarBridgeBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(-1.0F, 3600000.0F).sound(SoundType.AMETHYST)
            .noOcclusion().noLootTable().isValidSpawn((s, l, p, e) -> false)
            .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
            .lightLevel(state -> state.getValue(com.ggrantrichards.astralend.maze.StarBridgeBlock.LIT) ? 10 : 0));
    // Phase 9: Chorus Moss carpet, like moss carpet.
    public static final RegistryObject<Block> CHORUS_MOSS_CARPET = register("chorus_moss_carpet", net.minecraft.world.level.block.CarpetBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1F).sound(SoundType.MOSS_CARPET)
            .pushReaction(PushReaction.POPPED));
    // Phase 8 (Starforging): the Astral Forge, and the tiles Rift Walker boots lay over a drop.
    public static final RegistryObject<Block> ASTRAL_FORGE = register("astral_forge", com.ggrantrichards.astralend.starforge.AstralForgeBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(5.0F, 1200.0F)
            .sound(SoundType.METAL).lightLevel(state -> 7));
    public static final RegistryObject<Block> RIFT_TILE = registerNoItem("rift_tile", com.ggrantrichards.astralend.starforge.RiftTileBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.0F).sound(SoundType.AMETHYST)
            .noOcclusion().noLootTable().lightLevel(state -> 5));
    // Life that died out: grey husks of the Nebula Fields' mushrooms and vines, and Pollinator shells.
    public static final RegistryObject<Block> ASHEN_LUMINESHROOM = register("ashen_lumineshroom",
        p -> new EndPlantBlock(Block.column(6.0, 0.0, 10.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_GRAY, SoundType.FUNGUS));

    // ---- Shattered Observatory: the ruins' cut stone ----
    public static final RegistryObject<Block> OBSERVATORY_QUARTZ = register("observatory_quartz", Block::new,
        () -> quartz());
    public static final RegistryObject<Block> OBSERVATORY_QUARTZ_STAIRS = stairs("observatory_quartz_stairs", OBSERVATORY_QUARTZ);
    public static final RegistryObject<Block> OBSERVATORY_QUARTZ_SLAB = slab("observatory_quartz_slab", OBSERVATORY_QUARTZ);
    public static final RegistryObject<Block> OBSERVATORY_QUARTZ_WALL = wall("observatory_quartz_wall", OBSERVATORY_QUARTZ);
    public static final RegistryObject<Block> OBSERVATORY_QUARTZ_PILLAR = register("observatory_quartz_pillar", RotatedPillarBlock::new,
        () -> quartz());
    public static final RegistryObject<Block> STAR_INLAY_OBSERVATORY_QUARTZ = register("star_inlay_observatory_quartz", Block::new,
        () -> quartz());

    // ---- Deep Hollows: the core of a Upwell ----
    // Gravity flips inside a sphere below it (see UpwellCoreBlockEntity). Silk Touch to collect.
    public static final RegistryObject<Block> UPWELL_CORE = register("upwell_core", UpwellCoreBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(25.0F, 600.0F).sound(SoundType.SCULK_CATALYST).lightLevel(state -> 2));

    // ---- The distillation: the pedestal for the dragon egg in a Leviathan's reliquary. Unbreakable.
    public static final RegistryObject<Block> LEVIATHAN_PEDESTAL = register("leviathan_pedestal",
        com.ggrantrichards.astralend.block.LeviathanPedestalBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .strength(-1.0F, 3600000.0F).sound(SoundType.STONE).noLootTable().noOcclusion()
            .lightLevel(state -> state.getValue(com.ggrantrichards.astralend.block.LeviathanPedestalBlock.LIT) ? 12 : 3));

    // ---- The Lodestar: the End's respawn point (the distillation; LodestarBlock) ----
    public static final RegistryObject<Block> LODESTAR = register("lodestar", com.ggrantrichards.astralend.block.LodestarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(3.5F).sound(SoundType.LODESTONE).lightLevel(state -> 4));

    // ---- Phase 10: the Glimmering Crags. Smoky violet crystal grown through the Nightstone peaks. ----
    public static final RegistryObject<Block> GLIMMER_CRYSTAL = register("glimmer_crystal", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(1.5F).sound(SoundType.AMETHYST)
            .requiresCorrectToolForDrops().lightLevel(state -> 10));
    public static final RegistryObject<Block> GLIMMER_CLUSTER = register("glimmer_cluster",
        p -> new net.minecraft.world.level.block.AmethystClusterBlock(7.0F, 3.0F, p),
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.ICE).forceSolidOn().noOcclusion().strength(1.5F)
            .sound(SoundType.AMETHYST_CLUSTER).lightLevel(state -> 8).pushReaction(PushReaction.POPPED));

    // ---- Phase 10: Voidgel - jelly from the undersides of the islands. It bounces you like slime and
    // lets you down gently (Slow Falling). ----
    public static final RegistryObject<Block> VOIDGEL = register("voidgel", com.ggrantrichards.astralend.block.VoidgelBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).friction(0.8F).bounceRestitution(0.8F).sound(SoundType.SLIME_BLOCK)
            .noOcclusion().lightLevel(state -> 2));

    // ---- Phase 10: End Chimes - End Rods and amethyst on a chain. They hum, and when something
    // teleports near them they ring and still the air: no more teleporting near them for a while. ----
    public static final RegistryObject<Block> END_CHIMES = register("end_chimes", com.ggrantrichards.astralend.block.EndChimesBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.5F).sound(SoundType.AMETHYST_CLUSTER)
            .noOcclusion().noCollision().lightLevel(state -> 10).pushReaction(PushReaction.POPPED));

    // ================================================================ Phase 3.5: the biome worlds

    // ---- Giant Lumineshrooms (like the Nether's huge fungi) ----
    // ---- The distillation: purpur bricks (the Leviathan's builders laid them), and a purpur wall.
    public static final RegistryObject<Block> PURPUR_BRICKS = register("purpur_bricks", Block::new,
        () -> BlockBehaviour.Properties.ofLegacyCopy(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK));
    public static final RegistryObject<Block> PURPUR_BRICK_STAIRS = stairs("purpur_brick_stairs", PURPUR_BRICKS);
    public static final RegistryObject<Block> PURPUR_BRICK_SLAB = slab("purpur_brick_slab", PURPUR_BRICKS);
    public static final RegistryObject<Block> PURPUR_BRICK_WALL = wall("purpur_brick_wall", PURPUR_BRICKS);
    public static final RegistryObject<Block> PURPUR_WALL = register("purpur_wall", WallBlock::new,
        () -> BlockBehaviour.Properties.ofLegacyCopy(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK).forceSolidOn());

    // ---- The distillation: Memory Moss (the Ashen Barrens) and the Mimic Plant (the Deep Hollows).
    public static final RegistryObject<Block> MEMORY_MOSS = registerNoItem("memory_moss", com.ggrantrichards.astralend.block.MemoryMossBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1F).sound(SoundType.MOSS));
    public static final RegistryObject<Block> MIMIC_PLANT = register("mimic_plant",
        p -> new com.ggrantrichards.astralend.block.MimicPlantBlock(Block.column(12.0, 0.0, 13.0), ASTRAL_SOIL, p),
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).noCollision().instabreak().randomTicks()
            .sound(SoundType.AZALEA).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED));

    // ---- The distillation: the Eclipse's heaven half. Living Turf, its ground (the only green in the
    // End), and Breathcomb, whose light swells and fades over five minutes.
    public static final RegistryObject<Block> LIVING_TURF = register("living_turf", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.GRASS).strength(0.6F).sound(SoundType.GRASS));
    public static final RegistryObject<Block> BREATHCOMB = register("breathcomb", com.ggrantrichards.astralend.block.BreathcombBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.6F).sound(SoundType.CORAL_BLOCK)
            .lightLevel(com.ggrantrichards.astralend.block.BreathcombBlock::light));

    public static final RegistryObject<Block> LUMINE_CAP = register("lumine_cap", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASS).strength(1.0F)
            .sound(SoundType.SHROOMLIGHT).lightLevel(state -> 12));
    // Their stems, like vanilla's huge mushrooms: a stem and its all-bark hyphae, but no planks (the
    // distillation: Nullwood is the End's one wood).
    public static final RegistryObject<Block> LUMINE_STEM = register("lumine_stem", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0F)
            .sound(SoundType.STEM));
    public static final RegistryObject<Block> LUMINE_HYPHAE = register("lumine_hyphae", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).instrument(NoteBlockInstrument.BASS).strength(2.0F)
            .sound(SoundType.STEM));
    public static final RegistryObject<Block> NEBULA_TUFTS = register("nebula_tufts",
        p -> new EndPlantBlock(Block.column(12.0, 0.0, 10.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_PURPLE, SoundType.GRASS).replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
    // A Drift Bladder (after beta.4): glowing gas that floats over the Nebula Fields on a tendril.
    public static final RegistryObject<Block> NEBULA_BLADDER = register("nebula_bladder",
        com.ggrantrichards.astralend.block.NebulaBladderBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.3F).sound(SoundType.HONEY_BLOCK)
            .noOcclusion().lightLevel(state -> 12).isValidSpawn((state, level, pos, type) -> false)
            .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos, aabb) -> false));
    // Nebula Tendrils hang from island undersides (and Drift Bladders) and glow at the tip.
    public static final RegistryObject<Block> NEBULA_TENDRIL = register("nebula_tendril", NebulaTendrilBlock::new,
        () -> tendril().randomTicks().lightLevel(state -> 9));
    public static final RegistryObject<Block> NEBULA_TENDRIL_PLANT = registerNoItem("nebula_tendril_plant", NebulaTendrilPlantBlock::new,
        () -> tendril());

    // ---- Deep Hollows: twisted Nullwood trees ----
    public static final WoodSet NULLWOOD = woodSet("nullwood", "log", "wood", MapColor.COLOR_BLACK, MapColor.COLOR_PURPLE,
        SoundType.WOOD, BlockSetType.DARK_OAK, WoodType.DARK_OAK);
    public static final RegistryObject<Block> NULLWOOD_LEAVES = register("nullwood_leaves", p -> new LeavesBlock(0.01F, p),
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.2F).randomTicks().sound(SoundType.GRASS)
            .noOcclusion().isValidSpawn((state, level, pos, type) -> false).isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos, aabb) -> false).pushReaction(PushReaction.POPPED)
            .isRedstoneConductor((state, level, pos) -> false));
    public static final RegistryObject<Block> NULLWOOD_SAPLING = register("nullwood_sapling", NullwoodSaplingBlock::new,
        () -> plant(MapColor.COLOR_PURPLE, SoundType.GRASS).randomTicks());
    // Null Lanterns: glowing fruit hanging under Nullwood leaves.
    public static final RegistryObject<Block> NULL_LANTERN = register("null_lantern", NullLanternBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(0.3F).noOcclusion()
            .sound(SoundType.SHROOMLIGHT).lightLevel(state -> 13).pushReaction(PushReaction.POPPED));

    // ---- Ashen Barrens and the Corona ----
    public static final RegistryObject<Block> SHIMMER_REED = register("shimmer_reed", EndTallPlantBlock::new,
        () -> plant(MapColor.COLOR_LIGHT_BLUE, SoundType.GRASS).replaceable().offsetType(BlockBehaviour.OffsetType.XZ));

    // ---- Phase 4: from the mobs ----
    // Spore Lantern: Glowing Spores in a glass case. Its light also reveals Umbral Stalkers.
    public static final RegistryObject<Block> SPORE_LANTERN = register("spore_lantern", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.3F).sound(SoundType.GLASS)
            .lightLevel(state -> 15).isRedstoneConductor((state, level, pos) -> false));
    // The caves (after beta.3). The Hollow Roots: Hollow Glowcaps on the cave floors, Starsap - old
    // amber sap of the dead Nullwood - in the walls, and the Starsap block it makes.
    public static final RegistryObject<Block> HOLLOW_GLOWCAP = register("hollow_glowcap",
        p -> new EndPlantBlock(Block.column(8.0, 0.0, 9.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_PURPLE, SoundType.FUNGUS).lightLevel(state -> 9));
    public static final RegistryObject<Block> STARSAP_ORE = register("starsap_ore",
        p -> new DropExperienceBlock(UniformInt.of(1, 4), p),
        () -> stone(SoundType.DEEPSLATE).strength(3.5F, 3.0F).lightLevel(state -> 3));
    public static final RegistryObject<Block> STARSAP_BLOCK = register("starsap_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.HONEY_BLOCK)
            .lightLevel(state -> 12));
    // The Deep Hollows' carving: a pumpkin carved with a Hollow Thorn.
    public static final RegistryObject<Block> HOLLOW_LANTERN = register("hollow_lantern", com.ggrantrichards.astralend.block.HollowLanternBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F).sound(SoundType.WOOD)
            .lightLevel(state -> 15));

    // Leviathan Flesh: the heart's muscle and its great vessels - black-purple, its veins pulsing violet
    // (since beta.4; it was crimson).
    public static final RegistryObject<Block> LEVIATHAN_FLESH = register("leviathan_flesh", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.0F).sound(SoundType.WART_BLOCK)
            .lightLevel(state -> 3));
    // Leviathan Heart: in the Leviathan's ribcage, dormant, still beating violet.
    public static final RegistryObject<Block> LEVIATHAN_HEART = register("leviathan_heart", com.ggrantrichards.astralend.block.LeviathanHeartBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(1.5F).sound(SoundType.WART_BLOCK)
            .lightLevel(state -> 11));

    // Whalelight Scale: shed, very rarely, by a humming Drift Whale. Glows softly.
    public static final RegistryObject<Block> WHALELIGHT_SCALE = register("whalelight_scale", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(1.0F).sound(SoundType.AMETHYST)
            .lightLevel(state -> 10));

    // ---- helpers ----

    private static BlockBehaviour.Properties stone(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(3.5F, 6.0F).sound(sound);
    }


    // Every block of one wood type. "log"/"wood" are called "stem"/"hyphae" for mushroom wood.
    public record WoodSet(RegistryObject<Block> log, RegistryObject<Block> wood, RegistryObject<Block> strippedLog,
                          RegistryObject<Block> strippedWood, RegistryObject<Block> planks, RegistryObject<Block> stairs,
                          RegistryObject<Block> slab, RegistryObject<Block> fence, RegistryObject<Block> fenceGate,
                          RegistryObject<Block> door, RegistryObject<Block> trapdoor, RegistryObject<Block> button,
                          RegistryObject<Block> pressurePlate) {}

    // Registers a whole wood set, with the same properties vanilla gives its own woods.
    private static WoodSet woodSet(String name, String logWord, String woodWord, MapColor bark, MapColor planksColor,
                                   SoundType logSound, BlockSetType setType, WoodType woodType) {
        Supplier<BlockBehaviour.Properties> log = () -> BlockBehaviour.Properties.of().mapColor(bark)
            .instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(logSound);
        Supplier<BlockBehaviour.Properties> wood = () -> BlockBehaviour.Properties.of().mapColor(planksColor)
            .instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD);
        RegistryObject<Block> logBlock = register(name + "_" + logWord, RotatedPillarBlock::new, log);
        RegistryObject<Block> woodBlock = register(name + "_" + woodWord, RotatedPillarBlock::new, log);
        RegistryObject<Block> strippedLog = register("stripped_" + name + "_" + logWord, RotatedPillarBlock::new, log);
        RegistryObject<Block> strippedWood = register("stripped_" + name + "_" + woodWord, RotatedPillarBlock::new, log);
        RegistryObject<Block> planks = register(name + "_planks", Block::new, wood);
        return new WoodSet(logBlock, woodBlock, strippedLog, strippedWood, planks,
            stairs(name + "_stairs", planks),
            slab(name + "_slab", planks),
            register(name + "_fence", FenceBlock::new, wood),
            register(name + "_fence_gate", p -> new FenceGateBlock(woodType, p), () -> wood.get().forceSolidOn()),
            register(name + "_door", p -> new DoorBlock(setType, p),
                () -> wood.get().strength(3.0F).noOcclusion().pushReaction(PushReaction.POPPED)),
            register(name + "_trapdoor", p -> new TrapDoorBlock(setType, p),
                () -> wood.get().strength(3.0F).noOcclusion().isValidSpawn((state, level, pos, type) -> false)),
            register(name + "_button", p -> new ButtonBlock(setType, 30, p),
                () -> BlockBehaviour.Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED)),
            register(name + "_pressure_plate", p -> new PressurePlateBlock(setType, p),
                () -> wood.get().forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED)));
    }

    private static BlockBehaviour.Properties tendril() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().instabreak()
            .sound(SoundType.WEEPING_VINES).pushReaction(PushReaction.POPPED);
    }

    // Like vanilla End Stone (hardness 3, blast resistance 9).
    private static BlockBehaviour.Properties endStone(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(3.0F, 9.0F);
    }

    private static BlockBehaviour.Properties quartz() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(0.8F);
    }

    private static BlockBehaviour.Properties plant(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(color).noCollision().instabreak().sound(sound)
            .pushReaction(PushReaction.POPPED);
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
