package com.ggrantrichards.astralend.compat;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

// (1.21.1 port) 26.3's SavedDataType: saved data described by a codec. On 1.21.1 the codec's output
// is kept under "data" in the file, and get() looks it up the 1.21.1 way.
public final class SavedDataType<T extends SavedData> {
    private final String name;
    private final Supplier<T> constructor;
    private final Codec<T> codec;

    public SavedDataType(ResourceLocation id, Supplier<T> constructor, Codec<T> codec, DataFixTypes unused) {
        this.name = id.getNamespace() + "_" + id.getPath();
        this.constructor = constructor;
        this.codec = codec;
    }

    public T get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(this.constructor,
            (tag, registries) -> this.codec.parse(NbtOps.INSTANCE, tag.get("data")).result().orElseGet(this.constructor), null), this.name);
    }

    public CompoundTag save(T data, CompoundTag tag) {
        this.codec.encodeStart(NbtOps.INSTANCE, data).result().ifPresent(nbt -> tag.put("data", nbt));
        return tag;
    }
}
