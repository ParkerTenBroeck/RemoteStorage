package com.parkertenbroeck.remotestorage.packets.c2s.edit;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.Group;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record UpdateGroupRemoteStorageC2S(Group group) implements CustomPayload {
    public static final Id<UpdateGroupRemoteStorageC2S> ID = Utils.createId(UpdateGroupRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, UpdateGroupRemoteStorageC2S> CODEC = PacketCodec.tuple(Group.PACKET_CODEC, UpdateGroupRemoteStorageC2S::group, UpdateGroupRemoteStorageC2S::new);
    @Override
    public Id<UpdateGroupRemoteStorageC2S> getId() {
        return ID;
    }
}
