package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.List;
import java.util.Optional;

public final class StorageMember {
    public static final Codec<StorageMember> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Position.CODEC.fieldOf("pos").forGetter(v -> v.pos),
                    Codec.optionalField("linked", Position.CODEC, true).forGetter(v -> Optional.ofNullable(v.linked)),
                    Codec.INT.fieldOf("group").forGetter(v -> v.group)
            ).apply(instance, StorageMember::new)
    );

    public static final PacketCodec<RegistryByteBuf, StorageMember> PACKET_CODEC = PacketCodec.tuple(
            Position.PACKET_CODEC, s -> s.pos,
            PacketCodecs.optional(Position.PACKET_CODEC), s -> Optional.ofNullable(s.linked),
            PacketCodecs.INTEGER, s -> s.group,
            StorageMember::new
    );

    public final Position pos;
    Position linked;
    int group;

    private StorageMember(Position pos, Optional<Position> linked, int group) {
        this.pos = pos;
        this.linked = linked.orElse(null);
        this.group = group;
    }

    public StorageMember(Position pos) {
        this.pos = pos;
    }

    public Position pos(){
        return this.pos;
    }

    public Position linked(){
        return this.linked;
    }

    public int group(){
        return this.group;
    }

    void setGroup(int group) {
        this.linked = null;
        this.group = group;
    }

    void linkTo(Position pos) {
        this.group = 0;
        this.linked = pos;
    }
}
