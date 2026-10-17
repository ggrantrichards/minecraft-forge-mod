package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.starforge.Starforge;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

// Starforged armour you're wearing glows in its upgrade's colour: its edges shine (the armour drawn
// again at full brightness through a copy of its texture that keeps only its edges, fading inward,
// see GlowMasks) and the light spills just past them (the same, fainter, on a shell half a pixel
// outside the armour), slowly breathing, so you can read someone's build at a glance and see it
// in the dark. (1.21.1 port: drawn on armour models a hair larger than the armour's own.)
public class StarforgeArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    // How strong the glow is on the armour and on each shell of the halo round it.
    private static final float[] SHELLS = {1.0F, 0.25F};
    private final java.util.List<HumanoidModel<AbstractClientPlayer>> inner;
    private final java.util.List<HumanoidModel<AbstractClientPlayer>> outer;

    public StarforgeArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                               java.util.List<HumanoidModel<AbstractClientPlayer>> inner, java.util.List<HumanoidModel<AbstractClientPlayer>> outer) {
        super(parent);
        this.inner = inner;
        this.outer = outer;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, AbstractClientPlayer player, float limbSwing,
                       float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible()) {
            return;
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.piece(poseStack, buffers, player.getItemBySlot(slot), slot, ageInTicks);
        }
    }

    private void piece(PoseStack poseStack, MultiBufferSource buffers, ItemStack stack, EquipmentSlot slot, float ageInTicks) {
        StarforgeUpgrade upgrade = Starforge.get(stack);
        if (upgrade == null || !stack.is(Starforge.ARMOUR) || Starforge.slotOf(stack) != slot
            || !(stack.getItem() instanceof ArmorItem armor)) {
            return;
        }
        boolean legs = slot == EquipmentSlot.LEGS;
        float pulse = 0.52F + 0.08F * Mth.sin(ageInTicks * 0.06F);
        for (int shell = 0; shell < SHELLS.length; shell++) {
            this.shell((legs ? this.inner : this.outer).get(shell), poseStack, buffers, armor, upgrade, slot, pulse * SHELLS[shell]);
        }
    }

    private void shell(HumanoidModel<AbstractClientPlayer> model, PoseStack poseStack, MultiBufferSource buffers, ArmorItem armor,
                       StarforgeUpgrade upgrade, EquipmentSlot slot, float strength) {
        boolean legs = slot == EquipmentSlot.LEGS;
        this.getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> { model.head.visible = true; model.hat.visible = true; }
            case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
            case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
            case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
            default -> { }
        }
        // Added on top of the armour's edges (the render type is additive), in the upgrade's colour.
        int c = upgrade.colour();
        int tint = 0xFF000000 | channel(c >> 16, strength) << 16 | channel(c >> 8, strength) << 8 | channel(c, strength);
        for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
            ResourceLocation texture = layer.texture(legs);
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GlowMasks.of(texture))), 0xF000F0, OverlayTexture.NO_OVERLAY, tint);
        }
    }

    private static int channel(int value, float strength) {
        return Math.round((value & 0xFF) * strength);
    }
}
