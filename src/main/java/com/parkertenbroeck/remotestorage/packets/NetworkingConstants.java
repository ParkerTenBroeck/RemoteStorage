package com.parkertenbroeck.remotestorage.packets;


import com.parkertenbroeck.remotestorage.RemoteStorage;
import net.minecraft.util.Identifier;

public class NetworkingConstants {
    public static final Identifier OPEN_REMOTE_STORAGE_ID = Identifier.of(RemoteStorage.MOD_ID, "open_remote_storage");
    public static final Identifier REMOTE_STORAGE_CONTENTS_ID = Identifier.of(RemoteStorage.MOD_ID, "remote_storage_contents");
    public static final Identifier ADD_TO_REMOTE_STORAGE_ID = Identifier.of(RemoteStorage.MOD_ID, "add_to_remote_storage");
}
