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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

// The Astral Forge's screen: the smithing table's, widened by two slots. Empty slots show what goes
// in them (cycling, like the smithing table's), and hovering one says so.
public class AstralForgeScreen extends ItemCombinerScreen<AstralForgeMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "textures/gui/container/astral_forge.png");
    private static final ResourceLocation ERROR = ResourceLocation.withDefaultNamespace("container/smithing/error");
    private static final List<ResourceLocation> TEMPLATE_ICONS = List.of(sprite("starforge_template"));
    private static final List<ResourceLocation> BASE_ICONS = List.of(vanilla("helmet"), vanilla("sword"), vanilla("chestplate"), vanilla("pickaxe"),
        vanilla("leggings"), vanilla("axe"), vanilla("boots"), sprite("bow"), vanilla("shovel"), sprite("trident"), vanilla("hoe"));
    private static final List<ResourceLocation> ALLOY_ICONS = List.of(vanilla("ingot"));
    private static final List<ResourceLocation> MATERIAL_ICONS = List.of(sprite("curators_core"), sprite("rift_pearl"), sprite("cracked_lens"),
        sprite("bottled_flare"), sprite("whalelight_scale"));
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

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID, "container/slot/" + name);
    }

    private static ResourceLocation vanilla(String name) {
        return ResourceLocation.withDefaultNamespace("container/slot/" + name);
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

    // (Every upgrade takes a single material now; the second slot only takes it instead of the first.)
    private List<ResourceLocation> partnerIcons() {
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
        this.extractConstellation(graphics);
    }

    // The forge draws the upgrade's constellation while you forge: its stars, joined, in the upgrade's
    // colour, in the space over the result slot.
    private void extractConstellation(GuiGraphicsExtractor graphics) {
        StarforgeUpgrade upgrade = this.menu.upgrade();
        if (upgrade == null) {
            return;
        }
        int index = com.ggrantrichards.astralend.maze.Constellations.byName(upgrade.constellation());
        if (index < 0) {
            return;
        }
        var c = com.ggrantrichards.astralend.maze.Constellations.get(index);
        int cx = this.leftPos + 143, cy = this.topPos + 26;
        int colour = 0xFF000000 | upgrade.colour();
        int dim = 0x88000000 | upgrade.colour();
        for (int[] line : c.lines()) {
            float[] p = c.stars()[line[0]], q = c.stars()[line[1]];
            for (int k = 1; k < 8; k++) {
                float t = k / 8.0F;
                int x = cx + Math.round((p[0] + (q[0] - p[0]) * t) * 1.1F);
                int y = cy - Math.round((p[1] + (q[1] - p[1]) * t) * 1.1F);
                graphics.fill(x, y, x + 1, y + 1, dim);
            }
        }
        for (float[] star : c.stars()) {
            int x = cx + Math.round(star[0] * 1.1F), y = cy - Math.round(star[1] * 1.1F);
            graphics.fill(x - 1, y, x + 2, y + 1, colour);
            graphics.fill(x, y - 1, x + 1, y + 2, colour);
        }
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
