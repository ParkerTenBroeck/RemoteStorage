package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;
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

    private static RemoteStorageSavedState getInstance(ServerPlayerEntity player){
        return player.getServer().getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE);
    }

    public static StorageSystem get(ServerPlayerEntity player) {
        var state = player.getServer().getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(TYPE);
        return state.getSystem(player);
    }

    public static boolean removeMember(ServerPlayerEntity player, Position pos) {
        var instance = getInstance(player);
        if(instance.getSystem(player).remove(pos)){
            instance.markDirty();
            return true;
        }
        return false;
    }

    public static boolean addMember(ServerPlayerEntity player, Position pos) {
        var instance = getInstance(player);
        if(instance.getSystem(player).addMember(pos)){
            instance.markDirty();
            return true;
        }
        return false;
    }

    public static void syncWithPlayer(ServerPlayerEntity player){
        ServerPlayNetworking.send(player, RemoteStorageSavedState.get(player).getPositions());
    }

    public static boolean unlinkMember(ServerPlayerEntity player, Position pos) {
        var instance = getInstance(player);
        if(instance.getSystem(player).unlink(pos)){
            instance.markDirty();
            return true;
        }
        return false;
    }

    public enum LinkResult{
        Success,
        CannotLinkToSelf,
        ChildIsNotMember,
        ParentIsNotMember,
    }

    public static LinkResult linkMember(ServerPlayerEntity player, Position child, Position parent) {
        if(child.equals(parent)) return LinkResult.CannotLinkToSelf;
        var instance = getInstance(player);
        var system = instance.getSystem(player);

        if(!system.members.containsKey(child)) return LinkResult.ChildIsNotMember;
        if(!system.members.containsKey(parent)) return LinkResult.ParentIsNotMember;

        instance.markDirty();
        system.members.get(child).linkTo(parent);

        return LinkResult.Success;
    }

    private StorageSystem getSystem(ServerPlayerEntity player){
        if(!playerMap.containsKey(player.getUuid())){
            playerMap.put(player.getUuid(), new StorageSystem());
            markDirty();
        }
        return playerMap.get(player.getUuid());
    }

}
