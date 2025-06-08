package com.parkertenbroeck.remotestorage.packets.c2s;

import com.parkertenbroeck.remotestorage.Utils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record AddToRemoteStorageC2S(BlockPos blockPos) implements CustomPayload {
    public static final Id<AddToRemoteStorageC2S> ID = Utils.createId(AddToRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, AddToRemoteStorageC2S> CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, AddToRemoteStorageC2S::blockPos, AddToRemoteStorageC2S::new);
    @Override
    public Id<AddToRemoteStorageC2S> getId() {
        return ID;
    }
}
