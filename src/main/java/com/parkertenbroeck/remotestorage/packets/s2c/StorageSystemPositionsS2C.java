package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.system.Position;
import com.parkertenbroeck.remotestorage.Utils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;
import java.util.Optional;

public record StorageSystemPositionsS2C(String name, List<Member> members) implements CustomPayload {
    public static final Id<StorageSystemPositionsS2C> ID = Utils.createId(StorageSystemPositionsS2C.class);
    public static final PacketCodec<RegistryByteBuf, StorageSystemPositionsS2C> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, StorageSystemPositionsS2C::name,
            Member.PACKET_CODEC.collect(PacketCodecs.toList()), StorageSystemPositionsS2C::members,
            StorageSystemPositionsS2C::new
    );
    @Override
    public Id<StorageSystemPositionsS2C> getId() {
        return ID;
    }

    public record Member(Position pos, Position parent, int group){
        public static final PacketCodec<RegistryByteBuf, Member> PACKET_CODEC = PacketCodec.tuple(
                Position.PACKET_CODEC, Member::pos,
                Position.PACKET_CODEC.collect(PacketCodecs::optional), m -> Optional.ofNullable(m.parent),
                PacketCodecs.INTEGER, Member::group,
                (pos, parent, group) -> new Member(pos, parent.orElse(null), group)
        );
    }
}
