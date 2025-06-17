package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public final class Group {
    public static final Codec<Group> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("id").forGetter(g -> g.id),
                    Codec.STRING.fieldOf("name").forGetter(g -> g.name),
                    Configuration.CODEC.fieldOf("input").forGetter(g -> g.input),
                    Configuration.CODEC.fieldOf("output").forGetter(g -> g.output)
            ).apply(instance, Group::new)
    );

    public static final PacketCodec<RegistryByteBuf, Group> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, g -> g.id,
            PacketCodecs.STRING, g -> g.name,
            Configuration.PACKET_CODEC, g -> g.input,
            Configuration.PACKET_CODEC, g -> g.output,
            Group::new
    );

    public final int id;
    String name;
    Configuration input;
    Configuration output;

    public static Group defaultGroup() {
        return new Group(0);
    }

    public Group(int id){
        this.id = id;
        this.name = "New Group " + id;
        this.input = new Configuration();
        this.output = new Configuration();
    }

    private Group(int id, String name, Configuration input, Configuration output) {
        this.id = id;
        this.name = name;
        this.input = input;
        this.output = output;
    }

    public String name(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public Configuration input(){
        return input;
    }

    public Configuration output(){
        return output;
    }

}
