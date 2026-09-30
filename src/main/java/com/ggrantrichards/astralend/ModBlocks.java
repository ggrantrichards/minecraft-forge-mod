package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.block.AstralVineBlock;
import com.ggrantrichards.astralend.block.AstralVinePlantBlock;
import com.ggrantrichards.astralend.block.CometCressBlock;
import com.ggrantrichards.astralend.block.EndPlantBlock;
import com.ggrantrichards.astralend.block.EndTallPlantBlock;
import com.ggrantrichards.astralend.block.GravityAnomalyBlock;
import com.ggrantrichards.astralend.block.LumineshroomBlock;
import com.ggrantrichards.astralend.block.NebulaTendrilBlock;
import com.ggrantrichards.astralend.block.NebulaTendrilPlantBlock;
import com.ggrantrichards.astralend.block.NullLanternBlock;
import com.ggrantrichards.astralend.block.NullwoodSaplingBlock;
import com.ggrantrichards.astralend.block.StarbloomBlock;
import com.ggrantrichards.astralend.block.StardustBlock;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
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

    // The End's natural ground blocks, which its plants grow on (a block tag, so datapacks can add to it).
    public static final TagKey<Block> ASTRAL_SOIL = TagKey.create(Registries.BLOCK,
        Identifier.fromNamespaceAndPath(AstralEnd.MODID, "astral_soil"));

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

    // ================================================================ Phase 3

    // ---- Aurora Shallows: pale, wide shelf islands ----
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

    // ---- Comet Wake: scorched islands, stardust and comet ice ----
    public static final RegistryObject<Block> CHARRED_END_STONE = register("charred_end_stone", Block::new,
        () -> endStone(MapColor.COLOR_BLACK));
    public static final RegistryObject<Block> CHARRED_END_STONE_STAIRS = stairs("charred_end_stone_stairs", CHARRED_END_STONE);
    public static final RegistryObject<Block> CHARRED_END_STONE_SLAB = slab("charred_end_stone_slab", CHARRED_END_STONE);
    public static final RegistryObject<Block> CHARRED_END_STONE_WALL = wall("charred_end_stone_wall", CHARRED_END_STONE);
    // Comet Ice never melts and is as slippery as blue ice (same friction). Silk Touch to collect.
    public static final RegistryObject<Block> COMET_ICE = register("comet_ice", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(2.8F).friction(0.989F).sound(SoundType.GLASS));

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
    // Solarite: only in the Corona. Diamond pickaxe; XP comes from smelting, like Meteoric Iron.
    public static final RegistryObject<Block> SOLARITE_ORE = register("solarite_ore",
        p -> new DropExperienceBlock(ConstantInt.of(0), p),
        () -> endStone(MapColor.SAND).strength(4.5F, 9.0F).lightLevel(state -> 3));
    public static final RegistryObject<Block> RAW_SOLARITE_BLOCK = register("raw_solarite_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final RegistryObject<Block> SOLARITE_BLOCK = register("solarite_block", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).instrument(NoteBlockInstrument.BELL)
            .requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL).lightLevel(state -> 7));
    // Flarebloom: a two-block-tall flower, shut tight.
    public static final RegistryObject<Block> FLAREBLOOM = register("flarebloom", EndTallPlantBlock::new,
        () -> plant(MapColor.COLOR_ORANGE, SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ));
    // Sunscale Lichen: a scaly ground cover (a carpet) that holds a faint glow.
    // It's see-through between its patches, so it mustn't hide the top of the block it lies on
    // (noOcclusion), or the gaps would show the void.
    public static final RegistryObject<Block> SUNSCALE_LICHEN = register("sunscale_lichen", CarpetBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.1F).sound(SoundType.MOSS_CARPET)
            .lightLevel(state -> 5).noOcclusion().pushReaction(PushReaction.POPPED));
    // Life that died out: grey husks of the Nebula Fields' mushrooms and vines, and Pollinator shells.
    public static final RegistryObject<Block> ASHEN_LUMINESHROOM = register("ashen_lumineshroom",
        p -> new EndPlantBlock(Block.column(6.0, 0.0, 10.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_GRAY, SoundType.FUNGUS));
    public static final RegistryObject<Block> PETRIFIED_ASTRAL_VINE = register("petrified_astral_vine",
        p -> new EndPlantBlock(Block.column(8.0, 0.0, 15.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_GRAY, SoundType.DRIPSTONE_BLOCK));
    public static final RegistryObject<Block> POLLINATOR_HUSK = register("pollinator_husk", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).instrument(NoteBlockInstrument.XYLOPHONE)
            .strength(1.5F).sound(SoundType.BONE_BLOCK));

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

    // ---- Dark Matter Belt: the core of a gravity inversion pocket ----
    // Gravity flips inside a sphere below it (see GravityAnomalyBlockEntity). Silk Touch to collect.
    public static final RegistryObject<Block> GRAVITY_ANOMALY = register("gravity_anomaly", GravityAnomalyBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(25.0F, 600.0F).sound(SoundType.SCULK_CATALYST).lightLevel(state -> 2));

    // ================================================================ Phase 3.5: the biome worlds

    // ---- Nebula Fields: giant Lumineshrooms and their wood (like the Nether's huge fungi) ----
    public static final RegistryObject<Block> LUMINE_CAP = register("lumine_cap", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASS).strength(1.0F)
            .sound(SoundType.SHROOMLIGHT).lightLevel(state -> 12));
    public static final WoodSet LUMINE = woodSet("lumine", "stem", "hyphae", MapColor.COLOR_LIGHT_BLUE, MapColor.COLOR_PURPLE,
        SoundType.STEM, BlockSetType.CRIMSON, WoodType.CRIMSON);
    public static final RegistryObject<Block> NEBULA_TUFTS = register("nebula_tufts",
        p -> new EndPlantBlock(Block.column(12.0, 0.0, 10.0), ASTRAL_SOIL, p),
        () -> plant(MapColor.COLOR_PURPLE, SoundType.GRASS).replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
    // Nebula Tendrils hang from island undersides and glow at the tip.
    public static final RegistryObject<Block> NEBULA_TENDRIL = register("nebula_tendril", NebulaTendrilBlock::new,
        () -> tendril().randomTicks().lightLevel(state -> 9));
    public static final RegistryObject<Block> NEBULA_TENDRIL_PLANT = registerNoItem("nebula_tendril_plant", NebulaTendrilPlantBlock::new,
        () -> tendril());

    // ---- Dark Matter Belt: twisted Nullwood trees ----
    public static final WoodSet NULLWOOD = woodSet("nullwood", "log", "wood", MapColor.COLOR_BLACK, MapColor.COLOR_PURPLE,
        SoundType.WOOD, BlockSetType.DARK_OAK, WoodType.DARK_OAK);
    public static final RegistryObject<Block> NULLWOOD_LEAVES = register("nullwood_leaves", p -> new TintedParticleLeavesBlock(0.01F, p),
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

    // ---- Aurora Shallows and the Corona ----
    public static final RegistryObject<Block> SHIMMER_REED = register("shimmer_reed", EndTallPlantBlock::new,
        () -> plant(MapColor.COLOR_LIGHT_BLUE, SoundType.GRASS).replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
    // The stone-hard trunk of a giant Astral Vine that died in the Corona long ago.
    public static final RegistryObject<Block> PETRIFIED_STALK = register("petrified_stalk", RotatedPillarBlock::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(2.0F, 6.0F).sound(SoundType.DRIPSTONE_BLOCK));

    // ---- Phase 4: from the mobs ----
    // Spore Lantern: Glowing Spores in a glass case. Its light also reveals Umbral Stalkers.
    public static final RegistryObject<Block> SPORE_LANTERN = register("spore_lantern", Block::new,
        () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.3F).sound(SoundType.GLASS)
            .lightLevel(state -> 15).isRedstoneConductor((state, level, pos) -> false));
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
