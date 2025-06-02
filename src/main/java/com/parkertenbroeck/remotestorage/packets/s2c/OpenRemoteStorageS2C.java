package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.packets.NetworkingUtils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;


public record OpenRemoteStorageS2C(int syncId, Text title) implements CustomPayload {
    public static final Id<OpenRemoteStorageS2C> ID = NetworkingUtils.createId(OpenRemoteStorageS2C.class);
    public static final PacketCodec<RegistryByteBuf, OpenRemoteStorageS2C> CODEC = PacketCodec.tuple(
            PacketCodecs.SYNC_ID, OpenRemoteStorageS2C::syncId,
            TextCodecs.REGISTRY_PACKET_CODEC, OpenRemoteStorageS2C::title,
            OpenRemoteStorageS2C::new
    );
    @Override
    public Id<OpenRemoteStorageS2C> getId() {
        return ID;
    }
}
