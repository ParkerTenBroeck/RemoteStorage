package com.parkertenbroeck.remotestorage.packets.c2s.edit;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.Position;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record SetGroupRemoteStorageC2S(Position memberPos, int groupId) implements CustomPayload {
    public static final Id<SetGroupRemoteStorageC2S> ID = Utils.createId(SetGroupRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, SetGroupRemoteStorageC2S> CODEC = PacketCodec.tuple(
            Position.PACKET_CODEC, SetGroupRemoteStorageC2S::memberPos,
            PacketCodecs.INTEGER, SetGroupRemoteStorageC2S::groupId,
            SetGroupRemoteStorageC2S::new
    );
    @Override
    public Id<SetGroupRemoteStorageC2S> getId() {
        return ID;
    }
}
