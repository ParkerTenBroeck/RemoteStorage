package com.parkertenbroeck.remotestorage.packets.c2s.edit;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.Position;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record AddToRemoteStorageC2S(Position pos) implements CustomPayload {
    public static final Id<AddToRemoteStorageC2S> ID = Utils.createId(AddToRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, AddToRemoteStorageC2S> CODEC = PacketCodec.tuple(Position.PACKET_CODEC, AddToRemoteStorageC2S::pos, AddToRemoteStorageC2S::new);
    @Override
    public Id<AddToRemoteStorageC2S> getId() {
        return ID;
    }
}
