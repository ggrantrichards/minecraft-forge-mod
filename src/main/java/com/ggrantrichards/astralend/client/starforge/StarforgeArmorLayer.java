package com.ggrantrichards.astralend.client.starforge;

import com.ggrantrichards.astralend.starforge.Starforge;
import com.ggrantrichards.astralend.starforge.StarforgeUpgrade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.joml.Matrix4f;

// Starforged armour you're wearing glows in its upgrade's colour along its edges: the armour drawn
// again at full brightness through a copy of its texture that keeps only its edges (see GlowMasks),
// slowly breathing, so you can read someone's build at a glance (and see it in the dark).
public class StarforgeArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private final ArmorModelSet<HumanoidModel<S>> models;

    public StarforgeArmorLayer(RenderLayerParent<S, M> parent, ArmorModelSet<HumanoidModel<S>> models) {
        super(parent);
        this.models = models;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (state.isInvisibleToPlayer) {
            return;
        }
        this.piece(poseStack, collector, state, state.headEquipment, EquipmentSlot.HEAD);
        this.piece(poseStack, collector, state, state.chestEquipment, EquipmentSlot.CHEST);
        this.piece(poseStack, collector, state, state.legsEquipment, EquipmentSlot.LEGS);
        this.piece(poseStack, collector, state, state.feetEquipment, EquipmentSlot.FEET);
    }

    private void piece(PoseStack poseStack, SubmitNodeCollector collector, S state, ItemStack stack, EquipmentSlot slot) {
        StarforgeUpgrade upgrade = Starforge.get(stack);
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (upgrade == null || !stack.is(Starforge.ARMOUR) || equippable == null || equippable.slot() != slot
            || equippable.assetId().isEmpty()) {
            return;
        }
        // Diamond and netherite armour draw one texture named after their equipment asset.
        Identifier asset = equippable.assetId().get().identifier();
        String layer = slot == EquipmentSlot.LEGS ? "humanoid_leggings" : "humanoid";
        Identifier texture = asset.withPath(path -> "textures/entity/equipment/" + layer + "/" + path + ".png");
        // Light added over the armour (the render type is additive) in the upgrade's colour; the mask
        // says how strongly each pixel glows.
        float strength = 0.52F + 0.08F * Mth.sin(state.ageInTicks * 0.06F);
        int c = upgrade.colour();
        int tint = 0xFF000000 | channel(c >> 16, strength) << 16 | channel(c >> 8, strength) << 8 | channel(c, strength);
        // Armour is drawn pulled a hair toward the camera (its render type is offset that way, so it
        // doesn't flicker against the body), and would hide a glow drawn where the armour really is:
        // pull the glow a little further.
        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        pose.set(new Matrix4f().scaling(0.999F).mul(pose));
        collector.order(4).submitModel(this.models.get(slot), state, poseStack, RenderTypes.eyes(GlowMasks.of(texture)),
            0xF000F0, OverlayTexture.NO_OVERLAY, tint, null, state.outlineColor);
        poseStack.popPose();
    }

    private static int channel(int value, float strength) {
        return Math.round((value & 0xFF) * strength);
    }
}
