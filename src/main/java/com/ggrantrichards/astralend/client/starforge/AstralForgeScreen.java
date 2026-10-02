package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.starforge.AstralForgeMenu;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

// The Astral Forge's screen: the smithing table's, widened by two slots. Empty slots show what goes
// in them (cycling, like the smithing table's), and hovering one says so.
public class AstralForgeScreen extends ItemCombinerScreen<AstralForgeMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(AstralEnd.MODID, "textures/gui/container/astral_forge.png");
    private static final Identifier ERROR = Identifier.withDefaultNamespace("container/smithing/error");
    private static final List<Identifier> TEMPLATE_ICONS = List.of(sprite("starforge_template"));
    private static final List<Identifier> BASE_ICONS = List.of(vanilla("helmet"), vanilla("sword"), vanilla("chestplate"), vanilla("pickaxe"),
        vanilla("leggings"), vanilla("axe"), vanilla("boots"), sprite("bow"), vanilla("shovel"), sprite("trident"), vanilla("hoe"));
    private static final List<Identifier> ALLOY_ICONS = List.of(vanilla("ingot"));
    private static final List<Identifier> MATERIAL_ICONS = List.of(sprite("void_crystal"), vanilla("ingot"), sprite("stardust"),
        sprite("temporal_crystal"), sprite("abyss_shard"), sprite("solarite"));
    private static final int ERROR_X = 100;
    private static final int ERROR_Y = 46;

    private final CyclingSlotBackground templateIcon = new CyclingSlotBackground(AstralForgeMenu.TEMPLATE);
    private final CyclingSlotBackground baseIcon = new CyclingSlotBackground(AstralForgeMenu.BASE);
    private final CyclingSlotBackground alloyIcon = new CyclingSlotBackground(AstralForgeMenu.ALLOY);
    private final CyclingSlotBackground materialIcon = new CyclingSlotBackground(AstralForgeMenu.MATERIAL);
    private final CyclingSlotBackground material2Icon = new CyclingSlotBackground(AstralForgeMenu.MATERIAL_2);

    public AstralForgeScreen(AstralForgeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
        this.titleLabelX = 44;
        this.titleLabelY = 15;
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(AstralEnd.MODID, "container/slot/" + name);
    }

    private static Identifier vanilla(String name) {
        return Identifier.withDefaultNamespace("container/slot/" + name);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.templateIcon.tick(TEMPLATE_ICONS);
        this.baseIcon.tick(BASE_ICONS);
        this.alloyIcon.tick(ALLOY_ICONS);
        this.materialIcon.tick(MATERIAL_ICONS);
        // The second material slot shows the partner of a two-material upgrade's first material.
        this.material2Icon.tick(this.partnerIcons());
    }

    private List<Identifier> partnerIcons() {
        ItemStack first = this.menu.getSlot(AstralForgeMenu.MATERIAL).getItem();
        for (StarforgeUpgrade upgrade : StarforgeUpgrade.values()) {
            if (upgrade.materials().size() == 2 && first.is(upgrade.materials().get(0))) {
                return List.of(upgrade == StarforgeUpgrade.VOIDWALKER ? sprite("umbral_membrane") : sprite("void_crystal"));
            }
            if (upgrade.materials().size() == 2 && first.is(upgrade.materials().get(1))) {
                return List.of(upgrade == StarforgeUpgrade.VOIDWALKER ? sprite("void_crystal") : sprite("solarite"));
            }
        }
        return List.of();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.extractHints(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        this.templateIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.baseIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.alloyIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.materialIcon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
        this.material2Icon.extractRenderState(this.menu, graphics, a, this.leftPos, this.topPos);
    }

    @Override
    protected void extractErrorIcon(GuiGraphicsExtractor graphics, int xo, int yo) {
        if (this.menu.hasError()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR, xo + ERROR_X, yo + ERROR_Y, 28, 21);
        }
    }

    // Hover hints, like the smithing table's: what each empty slot wants, and why nothing's coming out.
    private void extractHints(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Optional<Component> hint = Optional.empty();
        if (this.menu.hasError() && this.isHovering(ERROR_X, ERROR_Y, 28, 21, mouseX, mouseY)) {
            hint = Optional.of(Component.translatable("container.astralend.astral_forge.error"));
        }
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index <= AstralForgeMenu.MATERIAL_2) {
            hint = Optional.of(Component.translatable("container.astralend.astral_forge.slot." + this.hoveredSlot.index));
        }
        hint.ifPresent(text -> graphics.setTooltipForNextFrame(this.font, this.font.split(text, 115), mouseX, mouseY));
    }
}
