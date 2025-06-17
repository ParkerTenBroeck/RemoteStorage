package com.parkertenbroeck.remotestorage.packets.c2s.edit;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.Position;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.Optional;

public record LinkRemoteStorageMemberC2S(Position child, Optional<Position> parent) implements CustomPayload {
    public static final Id<LinkRemoteStorageMemberC2S> ID = Utils.createId(LinkRemoteStorageMemberC2S.class);
    public static final PacketCodec<RegistryByteBuf, LinkRemoteStorageMemberC2S> CODEC = PacketCodec.tuple(
            Position.PACKET_CODEC, LinkRemoteStorageMemberC2S::child,
            Position.PACKET_CODEC.collect(PacketCodecs::optional), LinkRemoteStorageMemberC2S::parent,
            LinkRemoteStorageMemberC2S::new
    );
    @Override
    public Id<LinkRemoteStorageMemberC2S> getId() {
        return ID;
    }
}
