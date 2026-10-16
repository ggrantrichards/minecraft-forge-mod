package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.AstralEnd;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

// The colour accent of a Starforge upgrade on armour you're wearing: a few pixels drawn over the
// armour like a trim (a gem on the brow, a clasp at the collar, a buckle, bands at the knees and
// ankles), in the upgrade's colour, so you can read someone's build at a glance. (1.21.1 port: drawn
// on armour models a hair larger than the armour's own, so it sits over it.)
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
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.piece(poseStack, buffers, light, player.getItemBySlot(slot), slot);
        }
    }

    private void piece(PoseStack poseStack, MultiBufferSource buffers, int light, ItemStack stack, EquipmentSlot slot) {
        StarforgeUpgrade upgrade = Starforge.get(stack);
        if (upgrade == null || !stack.is(Starforge.ARMOUR) || Starforge.slotOf(stack) != slot) {
            return;
        }
        HumanoidModel<AbstractClientPlayer> model = slot == EquipmentSlot.LEGS ? this.inner : this.outer;
        this.getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> { model.head.visible = true; model.hat.visible = true; }
            case CHEST -> { model.body.visible = true; model.rightArm.visible = true; model.leftArm.visible = true; }
            case LEGS -> { model.body.visible = true; model.rightLeg.visible = true; model.leftLeg.visible = true; }
            case FEET -> { model.rightLeg.visible = true; model.leftLeg.visible = true; }
            default -> { }
        }
        String layer = slot == EquipmentSlot.LEGS ? "humanoid_leggings" : "humanoid";
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID,
            "textures/entity/equipment/starforge/" + layer + "_" + upgrade.getSerializedName() + ".png");
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(texture)), light, OverlayTexture.NO_OVERLAY, -1);
    }
}
