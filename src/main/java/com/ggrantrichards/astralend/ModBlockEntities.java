package com.ggrantrichards.astralend;

import com.ggrantrichards.astralend.block.GravityAnomalyBlockEntity;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Blocks that need to run code every tick get a block entity.
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AstralEnd.MODID);

    public static final RegistryObject<BlockEntityType<GravityAnomalyBlockEntity>> GRAVITY_ANOMALY =
        BLOCK_ENTITIES.register("gravity_anomaly",
            () -> new BlockEntityType<>(GravityAnomalyBlockEntity::new, Set.of(ModBlocks.GRAVITY_ANOMALY.get())));

    private ModBlockEntities() {}
}
