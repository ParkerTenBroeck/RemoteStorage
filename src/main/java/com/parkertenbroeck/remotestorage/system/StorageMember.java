package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;

import java.util.Optional;

public final class StorageMember {
    public static final Codec<StorageMember> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Position.CODEC.fieldOf("pos").forGetter(v -> v.pos),
                    Codec.optionalField("linked", Position.CODEC, true).forGetter(v -> Optional.ofNullable(v.linked)),
                    Codec.INT.fieldOf("group").forGetter(v -> v.group)
            ).apply(instance, StorageMember::new)
    );

    public final Position pos;
    Position linked;
    int group;

    private StorageMember(Position pos, Optional<Position> linked, int group) {
        this.pos = pos;
        this.linked = linked.orElse(null);
        this.group = group;
    }

    public StorageMember(Position pos) {
        this.pos = pos;
    }

    void setGroup(int group) {
        this.linked = null;
        this.group = group;
    }

    void linkTo(Position pos) {
        this.group = 0;
        this.linked = pos;
    }

    public boolean canInsertItem(StorageSystem system, ItemData item) {
        var group = system.getGroup(this);
        for (var filter : group.filters) {
            var match = filter.matches(item);
            if (match) return group.kind != StorageSystem.ListKind.Blacklist;
        }
        return group.kind == StorageSystem.ListKind.Blacklist;
    }

    public boolean canRemoveItem(StorageSystem system, ItemData item) {
        return true;
    }
}
