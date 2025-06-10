package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;

import java.util.ArrayList;
import java.util.List;

public final class Group {
    public static final Codec<Group> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("id").forGetter(g -> g.id),
                    Codec.STRING.fieldOf("name").forGetter(g -> g.name),
                    StringIdentifiable.createCodec(StorageSystem.ListKind::values).fieldOf("kind").forGetter(g -> g.kind),
                    Codec.INT.fieldOf("inputPriority").forGetter(g -> g.inputPriority),
                    Codec.INT.fieldOf("outputPriority").forGetter(g -> g.outputPriority),
                    Codec.list(Filter.CODEC).fieldOf("filters").forGetter(g -> g.filters)
            ).apply(instance, Group::new)
    );

    public final int id;
    String name;
    StorageSystem.ListKind kind;
    int inputPriority;
    int outputPriority;
    final ArrayList<Filter> filters = new ArrayList<>();

    public static Group defaultGroup() {
        return new Group(0, "default", StorageSystem.ListKind.Blacklist, 0, 0, List.of());
    }

    private Group(int id, String name, StorageSystem.ListKind kind, int inputPriority, int outputPriority, List<Filter> filters) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.inputPriority = inputPriority;
        this.outputPriority = outputPriority;
        this.filters.addAll(filters);
    }

    public void setInputPriority(int inputPriority) {
        this.inputPriority = inputPriority;
    }

    public void setOutputPriority(int outputPriority) {
        this.outputPriority = outputPriority;
    }
}
