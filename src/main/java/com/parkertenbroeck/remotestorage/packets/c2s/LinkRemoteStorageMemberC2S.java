package com.parkertenbroeck.remotestorage.packets.c2s;

import com.parkertenbroeck.remotestorage.packets.NetworkingUtils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record LinkRemoteStorageMemberC2S(BlockPos child, BlockPos parent) implements CustomPayload {
    public static final Id<LinkRemoteStorageMemberC2S> ID = NetworkingUtils.createId(LinkRemoteStorageMemberC2S.class);
    public static final PacketCodec<RegistryByteBuf, LinkRemoteStorageMemberC2S> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, LinkRemoteStorageMemberC2S::child,
            BlockPos.PACKET_CODEC, LinkRemoteStorageMemberC2S::parent,
            LinkRemoteStorageMemberC2S::new
    );
    @Override
    public Id<LinkRemoteStorageMemberC2S> getId() {
        return ID;
    }
}
