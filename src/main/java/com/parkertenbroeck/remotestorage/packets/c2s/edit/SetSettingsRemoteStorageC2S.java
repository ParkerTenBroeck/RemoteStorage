package com.parkertenbroeck.remotestorage.packets.c2s.edit;

import com.parkertenbroeck.remotestorage.Utils;
import com.parkertenbroeck.remotestorage.system.MemberSettings;
import com.parkertenbroeck.remotestorage.system.Position;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SetSettingsRemoteStorageC2S(Position memberPos, MemberSettings settings) implements CustomPayload {
    public static final Id<SetSettingsRemoteStorageC2S> ID = Utils.createId(SetSettingsRemoteStorageC2S.class);
    public static final PacketCodec<RegistryByteBuf, SetSettingsRemoteStorageC2S> CODEC = PacketCodec.tuple(
            Position.PACKET_CODEC, SetSettingsRemoteStorageC2S::memberPos,
            MemberSettings.PACKET_CODEC, SetSettingsRemoteStorageC2S::settings,
            SetSettingsRemoteStorageC2S::new
    );
    @Override
    public Id<SetSettingsRemoteStorageC2S> getId() {
        return ID;
    }
}
