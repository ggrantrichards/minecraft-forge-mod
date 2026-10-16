package com.ggrantrichards.astralend.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jspecify.annotations.Nullable;

// One segment of a Spire Wyrm's body: a hitbox that follows the head along its path (like the Ender
// Dragon's parts). Hits on it hurt the Wyrm.
public class SpireWyrmPart extends net.minecraftforge.entity.PartEntity<SpireWyrm> {
    public final int index;
    private final EntityDimensions size;

    public SpireWyrmPart(SpireWyrm parent, int index, float size) {
        super(parent);
        this.index = index;
        this.size = EntityDimensions.scalable(size, size);
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag input) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag output) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return !this.isInvulnerableToBase(source) && this.getParent().hurtFromPart(level, this, source, damage);
    }

    @Override
    public boolean is(Entity other) {
        return this == other || this.getParent() == other;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
