package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.packets.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record OpenRemoteStorageS2C() implements CustomPayload {
    public static final Id<OpenRemoteStorageS2C> ID = new Id<>(NetworkingConstants.OPEN_REMOTE_STORAGE_S2C_ID);
    public static final OpenRemoteStorageS2C INSTANCE = new OpenRemoteStorageS2C();
    public static final PacketCodec<RegistryByteBuf, OpenRemoteStorageS2C> CODEC = PacketCodec.unit(INSTANCE);
    @Override
    public Id<OpenRemoteStorageS2C> getId() {
        return ID;
    }
}
