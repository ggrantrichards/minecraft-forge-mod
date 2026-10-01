package com.ggrantrichards.astralend.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, AstralEnd.MODID);

    public static final RegistryObject<MenuType<AstralForgeMenu>> ASTRAL_FORGE = MENUS.register("astral_forge",
        () -> new MenuType<>(AstralForgeMenu::new, FeatureFlags.VANILLA_SET));

    private ModMenus() {}
}
