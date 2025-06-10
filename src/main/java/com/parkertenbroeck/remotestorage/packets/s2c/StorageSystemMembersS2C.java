package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.system.Position;
import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.StorageMember;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;
import java.util.Optional;

public record StorageSystemMembersS2C(String name, List<StorageMember> members) implements CustomPayload {
    public static final Id<StorageSystemMembersS2C> ID = Utils.createId(StorageSystemMembersS2C.class);
    public static final PacketCodec<RegistryByteBuf, StorageSystemMembersS2C> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, StorageSystemMembersS2C::name,
            StorageMember.PACKET_CODEC.collect(PacketCodecs.toList()), StorageSystemMembersS2C::members,
            StorageSystemMembersS2C::new
    );
    @Override
    public Id<StorageSystemMembersS2C> getId() {
        return ID;
    }
}
