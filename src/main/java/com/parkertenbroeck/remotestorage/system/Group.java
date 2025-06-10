package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.StringIdentifiable;

import java.util.ArrayList;
import java.util.List;

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
        return new Group(0, "default", new Configuration(), new Configuration());
    }

    private Group(int id, String name, Configuration input, Configuration output) {
        this.id = id;
        this.name = name;
        this.input = input;
        this.output = output;
    }

    public static class Configuration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.fieldOf("priority").forGetter(g -> g.priority),
                        StringIdentifiable.createCodec(StorageSystem.ListKind::values).fieldOf("kind").forGetter(g -> g.kind),
                        Codec.list(Filter.CODEC).fieldOf("filters").forGetter(g -> g.filters)
                ).apply(instance, Configuration::new)
        );

        public static final PacketCodec<RegistryByteBuf, Configuration> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER, g -> g.priority,
                null, g -> g.kind,
                Filter.PACKET_CODEC.collect(PacketCodecs.toList()), g -> g.filters,
                Configuration::new
        );

        int priority = 0;
        StorageSystem.ListKind kind = StorageSystem.ListKind.Blacklist;
        final ArrayList<Filter> filters = new ArrayList<>();

        Configuration(int priority, StorageSystem.ListKind kind, List<Filter> filters){
            this.priority = priority;
            this.kind = kind;
            this.filters.addAll(filters);
        }

        public Configuration() {
        }

        public boolean check(ItemData item) {
            for (var filter : filters) {
                var match = filter.matches(item);
                if (match) return kind != StorageSystem.ListKind.Blacklist;
            }
            return kind == StorageSystem.ListKind.Blacklist;
        }
    }

}
