package com.parkertenbroeck.remotestorage.packets.c2s;

import com.parkertenbroeck.remotestorage.Utils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record RemoveFromRemoteStorageC2S(BlockPos blockPos) implements CustomPayload {
    public static final Id<RemoveFromRemoteStorageC2S> ID = Utils.createId(RemoveFromRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, RemoveFromRemoteStorageC2S> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, RemoveFromRemoteStorageC2S::blockPos, RemoveFromRemoteStorageC2S::new);
    @Override
    public Id<RemoveFromRemoteStorageC2S> getId() {
        return ID;
    }
}
