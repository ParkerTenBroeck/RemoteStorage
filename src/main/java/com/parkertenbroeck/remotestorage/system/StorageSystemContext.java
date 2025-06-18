package com.parkertenbroeck.remotestorage.system;

public interface StorageSystemContext {
    void unlink(Position pos);
    void add(Position pos);
    void clear();
    void remove(Position pos);
    void link(Position child, Position parent);
    void setSettings(Position member, MemberSettings settings);
}
