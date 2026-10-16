package com.ggrantrichards.astralend.client.starforge;

import net.minecraft.client.renderer.RenderType;
import com.ggrantrichards.astralend.AstralEnd;
import com.ggrantrichards.astralend.starforge.Starforge;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

// The colour accent of a Starforge upgrade on armour you're wearing: a few pixels drawn over the
// armour like a trim (a gem on the brow, a clasp at the collar, a buckle, bands at the knees and
// ankles), in the upgrade's colour, so you can read someone's build at a glance.
public class StarforgeArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final ArmorModelSet<HumanoidModel<S>> models;

    public StarforgeArmorLayer(RenderLayerParent<S, M> parent, ArmorModelSet<HumanoidModel<S>> models) {
        super(parent);
        this.models = models;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        this.piece(poseStack, collector, light, state, state.headEquipment, EquipmentSlot.HEAD);
        this.piece(poseStack, collector, light, state, state.chestEquipment, EquipmentSlot.CHEST);
        this.piece(poseStack, collector, light, state, state.legsEquipment, EquipmentSlot.LEGS);
        this.piece(poseStack, collector, light, state, state.feetEquipment, EquipmentSlot.FEET);
    }

    private void piece(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, ItemStack stack, EquipmentSlot slot) {
        StarforgeUpgrade upgrade = Starforge.get(stack);
        if (upgrade == null || !stack.is(Starforge.ARMOUR) || Starforge.slotOf(stack) != slot) {
            return;
        }
        HumanoidModel<S> model = this.models.get(slot);
        String layer = slot == EquipmentSlot.LEGS ? "humanoid_leggings" : "humanoid";
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(AstralEnd.MODID,
            "textures/entity/equipment/starforge/" + layer + "_" + upgrade.getSerializedName() + ".png");
        // Drawn after the armour itself (and its trim), like a trim, or the armour would cover it.
        collector.order(4).submitModel(model, state, poseStack, RenderType.armorTrim(texture, false), light, OverlayTexture.NO_OVERLAY, -1, null,
            state.outlineColor);
    }
}
