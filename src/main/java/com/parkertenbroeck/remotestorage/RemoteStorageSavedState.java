package com.parkertenbroeck.remotestorage;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
            RemoteStorageSavedState.class.getName(),
            RemoteStorageSavedState::new,
            CODEC,
            null
    );

    public static StorageSystem get(ServerPlayerEntity player) {
        var state = player.getServer().getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE);
        return state.getSystem(player);
    }

    private StorageSystem getSystem(ServerPlayerEntity player){
        if(!playerMap.containsKey(player.getUuid())){
            playerMap.put(player.getUuid(), new StorageSystem());
            markDirty();
        }
        return playerMap.get(player.getUuid());
    }

}
