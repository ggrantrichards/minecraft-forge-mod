package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.block.CollectorPedestalBlockEntity;
import com.ggrantrichards.astralend.block.LodestarBlockEntity;
import com.ggrantrichards.astralend.block.UpwellCoreBlockEntity;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Blocks that need to run code every tick get a block entity.
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AstralEnd.MODID);

    public static final RegistryObject<BlockEntityType<UpwellCoreBlockEntity>> UPWELL_CORE =
        BLOCK_ENTITIES.register("upwell_core",
            () -> new BlockEntityType<>(UpwellCoreBlockEntity::new, Set.of(ModBlocks.UPWELL_CORE.get())));

    public static final RegistryObject<BlockEntityType<CollectorPedestalBlockEntity>> COLLECTOR_PEDESTAL =
        BLOCK_ENTITIES.register("collector_pedestal",
            () -> new BlockEntityType<>(CollectorPedestalBlockEntity::new, Set.of(ModBlocks.COLLECTOR_PEDESTAL.get())));

    public static final RegistryObject<BlockEntityType<LodestarBlockEntity>> LODESTAR =
        BLOCK_ENTITIES.register("lodestar",
            () -> new BlockEntityType<>(LodestarBlockEntity::new, Set.of(ModBlocks.LODESTAR.get())));

    public static final RegistryObject<BlockEntityType<com.ggrantrichards.astralend.block.EndChimesBlockEntity>> END_CHIMES =
        BLOCK_ENTITIES.register("end_chimes",
            () -> new BlockEntityType<>(com.ggrantrichards.astralend.block.EndChimesBlockEntity::new, Set.of(ModBlocks.END_CHIMES.get())));

    private ModBlockEntities() {}
}
