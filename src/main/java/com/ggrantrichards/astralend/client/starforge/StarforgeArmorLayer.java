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

// Starforged armour you're wearing glows faintly in its upgrade's colour: the armour's own texture
// drawn again at full brightness, tinted and dimmed, and slowly breathing, so you can read
// someone's build at a glance (and see it in the dark). (1.21.1 port: drawn on armour models a hair
// larger than the armour's own, so it sits over it.)
public class StarforgeArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final HumanoidModel<AbstractClientPlayer> inner;
    private final HumanoidModel<AbstractClientPlayer> outer;

    public StarforgeArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                               HumanoidModel<AbstractClientPlayer> inner, HumanoidModel<AbstractClientPlayer> outer) {
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
        HumanoidModel<AbstractClientPlayer> model = legs ? this.inner : this.outer;
        this.getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> { model.head.visible = true; model.hat.visible = true; }
            case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
            case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
            case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
            default -> { }
        }
        // Added on top of the armour (the render type is additive), so the tint is kept low.
        float strength = 0.30F + 0.12F * Mth.sin(ageInTicks * 0.06F);
        int c = upgrade.colour();
        int tint = 0xFF000000 | channel(c >> 16, strength) << 16 | channel(c >> 8, strength) << 8 | channel(c, strength);
        for (ArmorMaterial.Layer layer : armor.getMaterial().value().layers()) {
            ResourceLocation texture = layer.texture(legs);
            model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(texture)), 0xF000F0, OverlayTexture.NO_OVERLAY, tint);
        }
    }

    private static int channel(int value, float strength) {
        return Math.round((value & 0xFF) * strength);
    }
}
