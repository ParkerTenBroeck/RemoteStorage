package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.Objects;
import java.util.Optional;

public final class StorageMember {
    public static final Codec<StorageMember> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Position.CODEC.fieldOf("memberPos").forGetter(v -> v.pos),
                    Position.CODEC.lenientOptionalFieldOf("linked").forGetter(StorageMember::linked),
                    MemberSettings.CODEC.lenientOptionalFieldOf("group").forGetter(StorageMember::settings)
            ).apply(instance, StorageMember::new)
    );

    public static final PacketCodec<RegistryByteBuf, StorageMember> PACKET_CODEC = PacketCodec.tuple(
            Position.PACKET_CODEC, s -> s.pos,
            PacketCodecs.optional(Position.PACKET_CODEC), StorageMember::linked,
            PacketCodecs.optional(MemberSettings.PACKET_CODEC), StorageMember::settings,
            StorageMember::new
    );

    private final Position pos;
    private Optional<Position> linked = Optional.empty();
    private Optional<MemberSettings> settings = Optional.of(new MemberSettings());

    private StorageMember(Position pos, Optional<Position> linked, Optional<MemberSettings> settings) {
        this.pos = pos;
        this.linked = linked;
        this.settings = settings;
    }

    public StorageMember(Position pos) {
        this.pos = pos;
    }

    public Position pos(){
        return this.pos;
    }

    public Optional<Position> linked(){
        return this.linked;
    }

    public Optional<MemberSettings> settings(){
        return this.settings;
    }

    void setSettings(MemberSettings settings){
        this.settings = Optional.of(settings);
        this.linked = Optional.empty();
    }

    void unlink(){
        this.settings = Optional.of(new MemberSettings());
        this.linked = Optional.empty();
    }

    void linkTo(Position pos) {
        this.settings = Optional.empty();
        this.linked = Optional.of(pos);
    }
}
