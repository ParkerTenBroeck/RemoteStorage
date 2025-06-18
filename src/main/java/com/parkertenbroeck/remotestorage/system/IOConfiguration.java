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

public class IOConfiguration {
    public static final Codec<IOConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("priority").forGetter(IOConfiguration::priority),
                    StringIdentifiable.createCodec(ListKind::values).fieldOf("kind").forGetter(IOConfiguration::kind),
                    Codec.list(Filter.CODEC).fieldOf("filters").forGetter(IOConfiguration::filters)
            ).apply(instance, IOConfiguration::new)
    );

    public static final PacketCodec<RegistryByteBuf, IOConfiguration> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER, IOConfiguration::priority,
            PacketCodecs.indexed(i -> ListKind.values()[i], ListKind::ordinal), IOConfiguration::kind,
            Filter.PACKET_CODEC.collect(PacketCodecs.toList()), IOConfiguration::filters,
            IOConfiguration::new
    );

    int priority = 0;
    ListKind kind = ListKind.Blacklist;
    final ArrayList<Filter> filters = new ArrayList<>();

    IOConfiguration(int priority, ListKind kind, List<Filter> filters) {
        this.setPriority(priority);
        this.setKind(kind);
        this.filters().addAll(filters);
    }

    public IOConfiguration() {
    }

    public boolean matches(ItemData item) {
        for (var filter : filters()) {
            var match = filter.matches(item);
            if (match) return kind() != ListKind.Blacklist;
        }
        return kind() == ListKind.Blacklist;
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ListKind kind() {
        return kind;
    }

    public void setKind(ListKind kind) {
        this.kind = kind;
    }

    public ArrayList<Filter> filters() {
        return filters;
    }
}
