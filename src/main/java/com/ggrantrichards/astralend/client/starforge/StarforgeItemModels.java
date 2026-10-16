package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.ModItems;
import com.ggrantrichards.astralend.starforge.Starforge;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jspecify.annotations.Nullable;

// (1.21.1 port) What 26.3 says in assets/minecraft/items/*.json and assets/astralend/items/star_compass.json:
//  - a Starforged tool, weapon or piece of armour wears its upgrade's accent over its own model (in 26.3
//    a "select" on the upgrade component, each case a composite of the vanilla model and the accent);
//    here every such item's model is wrapped, and when the stack carries an upgrade the accent's quads
//    are drawn after whatever model the item would have shown (pulled bow, trimmed armour and all);
//  - the Star Compass turns like a compass toward its lodestone target ("angle" for its model overrides).
public final class StarforgeItemModels {
    private static final Map<Item, String> KIND = new LinkedHashMap<>();

    static {
        for (String tier : new String[] {"diamond", "netherite"}) {
            for (String kind : new String[] {"axe", "pickaxe", "shovel", "hoe", "sword", "helmet", "chestplate", "leggings", "boots"}) {
                KIND.put(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(tier + "_" + kind)), kind);
            }
        }
        KIND.put(Items.BOW, "bow");
        KIND.put(Items.CROSSBOW, "crossbow");
        KIND.put(Items.MACE, "mace");
        KIND.put(Items.TRIDENT, "trident");
    }

    private static ModelResourceLocation accent(String kind, StarforgeUpgrade upgrade) {
        return ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "item/starforge/" + kind + "_" + upgrade.getSerializedName()));
    }

    public static void register() {
        com.ggrantrichards.astralend.Events.on(ModelEvent.RegisterAdditional.class, event -> {
            for (String kind : new HashSet<>(KIND.values())) {
                for (StarforgeUpgrade upgrade : StarforgeUpgrade.values()) {
                    event.register(accent(kind, upgrade));
                }
            }
        });
        com.ggrantrichards.astralend.Events.on(ModelEvent.ModifyBakingResult.class, event -> {
            Map<ModelResourceLocation, BakedModel> models = event.getModels();
            KIND.forEach((item, kind) -> {
                ModelResourceLocation key = ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item));
                BakedModel base = models.get(key);
                Map<StarforgeUpgrade, BakedModel> accents = new EnumMap<>(StarforgeUpgrade.class);
                for (StarforgeUpgrade upgrade : StarforgeUpgrade.values()) {
                    BakedModel model = models.get(accent(kind, upgrade));
                    if (model != null) {
                        accents.put(upgrade, model);
                    }
                }
                if (base != null && !accents.isEmpty()) {
                    models.put(key, new Upgradable(base, accents));
                }
            });
        });
        com.ggrantrichards.astralend.Events.on(FMLClientSetupEvent.class, event -> event.enqueueWork(() ->
            ItemProperties.register(ModItems.STAR_COMPASS.get(), ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    var tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    return tracker == null ? null : tracker.target().orElse(null);
                }))));
    }

    // An item's own model, whose overrides add the upgrade's accent to whatever the item resolves to.
    private static final class Upgradable extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;

        Upgradable(BakedModel base, Map<StarforgeUpgrade, BakedModel> accents) {
            super(base);
            ItemOverrides inner = base.getOverrides();
            Map<BakedModel, Map<StarforgeUpgrade, BakedModel>> layered = new IdentityHashMap<>();
            this.overrides = new ItemOverrides() {
                @Override
                public @Nullable BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                    BakedModel resolved = inner.resolve(base, stack, level, entity, seed);
                    BakedModel shown = resolved == null ? base : resolved;
                    StarforgeUpgrade upgrade = Starforge.get(stack);
                    BakedModel accent = upgrade == null ? null : accents.get(upgrade);
                    if (accent == null || shown.isCustomRenderer()) {
                        return shown;   // (a trident in hand is drawn by its own renderer, as in 26.3)
                    }
                    synchronized (layered) {
                        return layered.computeIfAbsent(shown, k -> new EnumMap<>(StarforgeUpgrade.class))
                            .computeIfAbsent(upgrade, k -> new Layered(shown, accent));
                    }
                }
            };
        }

        @Override
        public ItemOverrides getOverrides() {
            return this.overrides;
        }
    }

    // A model and an accent drawn over it.
    private static final class Layered extends BakedModelWrapper<BakedModel> {
        private final BakedModel accent;

        Layered(BakedModel base, BakedModel accent) {
            super(base);
            this.accent = accent;
        }

        private static List<BakedQuad> both(List<BakedQuad> a, List<BakedQuad> b) {
            List<BakedQuad> out = new ArrayList<>(a.size() + b.size());
            out.addAll(a);
            out.addAll(b);
            return out;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return both(this.originalModel.getQuads(state, side, random), this.accent.getQuads(state, side, random));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                        @Nullable RenderType renderType) {
            return both(this.originalModel.getQuads(state, side, random, data, renderType),
                this.accent.getQuads(state, side, random, ModelData.EMPTY, renderType));
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            this.originalModel.applyTransform(context, pose, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }
    }

    private StarforgeItemModels() {}
}
