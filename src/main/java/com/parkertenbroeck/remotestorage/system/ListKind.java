package com.parkertenbroeck.remotestorage.system;

import net.minecraft.util.StringIdentifiable;

public enum ListKind implements StringIdentifiable {
    Blacklist,
    Whitelist;

    @Override
    public String asString() {
        return this.name().toLowerCase();
    }
}
