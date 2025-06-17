package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.StorageMember;
import com.parkertenbroeck.remotestorage.system.StorageSystem;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record StorageSystemResyncS2C(StorageSystem system) implements CustomPayload {
    public static final Id<StorageSystemResyncS2C> ID = Utils.createId(StorageSystemResyncS2C.class);
    public static final PacketCodec<RegistryByteBuf, StorageSystemResyncS2C> CODEC = PacketCodec.tuple(
            StorageSystem.PACKET_CODEC, StorageSystemResyncS2C::system,
            StorageSystemResyncS2C::new
    );
    @Override
    public Id<StorageSystemResyncS2C> getId() {
        return ID;
    }
}
