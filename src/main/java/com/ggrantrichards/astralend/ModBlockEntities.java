package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.block.CollectorPedestalBlockEntity;
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
            () -> BlockEntityType.Builder.of(UpwellCoreBlockEntity::new, ModBlocks.UPWELL_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CollectorPedestalBlockEntity>> COLLECTOR_PEDESTAL =
        BLOCK_ENTITIES.register("collector_pedestal",
            () -> BlockEntityType.Builder.of(CollectorPedestalBlockEntity::new, ModBlocks.COLLECTOR_PEDESTAL.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.ggrantrichards.astralend.block.EndChimesBlockEntity>> END_CHIMES =
        BLOCK_ENTITIES.register("end_chimes",
            () -> BlockEntityType.Builder.of(com.ggrantrichards.astralend.block.EndChimesBlockEntity::new, ModBlocks.END_CHIMES.get()).build(null));

    private ModBlockEntities() {}
}
