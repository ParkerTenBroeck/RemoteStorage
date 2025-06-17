package com.parkertenbroeck.remotestorage.packets.c2s.edit;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.Position;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record RemoveFromRemoteStorageC2S(Position pos) implements CustomPayload {
    public static final Id<RemoveFromRemoteStorageC2S> ID = Utils.createId(RemoveFromRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, RemoveFromRemoteStorageC2S> CODEC = PacketCodec.tuple(Position.PACKET_CODEC, RemoveFromRemoteStorageC2S::pos, RemoveFromRemoteStorageC2S::new);
    @Override
    public Id<RemoveFromRemoteStorageC2S> getId() {
        return ID;
    }
}
