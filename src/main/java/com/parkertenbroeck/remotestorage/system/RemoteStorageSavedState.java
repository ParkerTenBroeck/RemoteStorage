package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.RemoteStorage;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemResyncS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RemoteStorageSavedState extends PersistentState {

    public final HashMap<UUID, StorageSystem> playerMap = new HashMap<>();
    private final StorageSystemContext context = new StorageSystemContext() {
        @Override
        public void unlink(Position pos) {markDirty();}

        @Override
        public void add(Position pos) {markDirty();}

        @Override
        public void clear() {markDirty();}

        @Override
        public void remove(Position pos) {markDirty();}

        @Override
        public void link(Position child, Position parent) {markDirty();}

        @Override
        public void updateGroup(Group group) {markDirty();}

        @Override
        public void setGroup(Position member, int group) {markDirty();}
    };

    private RemoteStorageSavedState(Map<UUID, StorageSystem> map){
        this.playerMap.putAll(map);
    }

    private RemoteStorageSavedState(){
    }

    private static final Codec<RemoteStorageSavedState> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.unboundedMap(Uuids.CODEC, StorageSystem.CODEC).fieldOf("map").forGetter(s -> s.playerMap)
            ).apply(instance, RemoteStorageSavedState::new)
    );

    private static final PersistentStateType<RemoteStorageSavedState> TYPE = new PersistentStateType<>(
            RemoteStorage.MOD_ID+"_remote_storage",
            RemoteStorageSavedState::new,
            CODEC,
            null
    );

    private static RemoteStorageSavedState getInstance(ServerPlayerEntity player){
        return player.getServer().getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE);
    }

    public static StorageSystem get(ServerPlayerEntity player) {
        var state = getInstance(player);
        return state.getSystem(player);
    }

    public static void resyncWithPlayer(ServerPlayerEntity player){
        RemoteStorage.LOGGER.info("Resynced storage system with player " + player.toString());
        RemoteStorageSavedState.get(player).resync(player);
    }

    private StorageSystem getSystem(ServerPlayerEntity player){
        if(!playerMap.containsKey(player.getUuid())){
            playerMap.put(player.getUuid(), new StorageSystem());
            markDirty();
        }
        var sys = playerMap.get(player.getUuid());
        sys.setContext(context);
        return sys;
    }
}
