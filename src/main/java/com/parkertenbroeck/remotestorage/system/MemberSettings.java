package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.Optional;

public final class MemberSettings {
    public static final Codec<MemberSettings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("name").forGetter(g -> g.name),
                    IOConfiguration.CODEC.fieldOf("input").forGetter(g -> g.input),
                    IOConfiguration.CODEC.fieldOf("output").forGetter(g -> g.output)
            ).apply(instance, MemberSettings::new)
    );

    public static final PacketCodec<RegistryByteBuf, MemberSettings> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.optional(PacketCodecs.STRING), g -> g.name,
            IOConfiguration.PACKET_CODEC, g -> g.input,
            IOConfiguration.PACKET_CODEC, g -> g.output,
            MemberSettings::new
    );

    Optional<String> name;
    IOConfiguration input;
    IOConfiguration output;

    public static MemberSettings defaultGroup() {
        return new MemberSettings();
    }

    public MemberSettings(){
        this.name = Optional.empty();
        this.input = new IOConfiguration();
        this.output = new IOConfiguration();
    }

    private MemberSettings(Optional<String> name, IOConfiguration input, IOConfiguration output) {
        this.name = name;
        this.input = input;
        this.output = output;
    }

    public String name(){
        return name.orElse("");
    }

    public void setName(String name){
        this.name = name==null||name.isBlank()?Optional.empty():Optional.of(name);
    }

    public IOConfiguration input(){
        return input;
    }

    public IOConfiguration output(){
        return output;
    }

}
