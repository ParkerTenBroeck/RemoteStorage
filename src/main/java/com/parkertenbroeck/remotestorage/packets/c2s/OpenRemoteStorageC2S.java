package com.parkertenbroeck.remotestorage.packets.c2s;

import com.parkertenbroeck.remotestorage.Utils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record OpenRemoteStorageC2S() implements CustomPayload {
    public static final CustomPayload.Id<OpenRemoteStorageC2S> ID = Utils.createId(OpenRemoteStorageC2S.class);
    public static final OpenRemoteStorageC2S INSTANCE = new OpenRemoteStorageC2S();
    public static final PacketCodec<RegistryByteBuf, OpenRemoteStorageC2S> CODEC = PacketCodec.unit(INSTANCE);
    @Override
    public Id<OpenRemoteStorageC2S> getId() {
        return ID;
    }
}
