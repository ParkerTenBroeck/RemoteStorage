package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemResyncS2C;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.*;

public class StorageSystem {
    public static final int MAX_LINKED_LENGTH = 5;

    public String name = "Default";
    final HashMap<Position, StorageMember> members = new HashMap<>();
    final Int2ObjectOpenHashMap<Group> groups = new Int2ObjectOpenHashMap<>();

    StorageSystemContext context;

    public static final Codec<StorageSystem> CODEC = RecordCodecBuilder.create(
        instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(i -> i.name),
                    Codec.list(StorageMember.CODEC).fieldOf("members").forGetter(i -> i.members.values().stream().toList()),
                    Codec.list(Group.CODEC).fieldOf("groups").forGetter(i -> i.groups.values().stream().toList())
            ).apply(instance, StorageSystem::new)
    );
    public static final PacketCodec<RegistryByteBuf, StorageSystem> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, s -> s.name,
            StorageMember.PACKET_CODEC.collect(PacketCodecs.toList()), s -> s.members.values().stream().toList(),
            Group.PACKET_CODEC.collect(PacketCodecs.toList()), s -> s.groups.values().stream().toList(),
            StorageSystem::new
    );

    public StorageSystem(){
        clear();
    }

    private StorageSystem(String name, List<StorageMember> members, List<Group> groups){
        this.name = name;
        members.forEach(m -> this.members.put(m.pos, m));
        groups.forEach(g -> this.groups.put(g.id, g));
    }

    public void setContext(StorageSystemContext context) {
        if(this.context==context)return;
        this.context = context;
    }

    public void resync(ServerPlayerEntity player){
        ServerPlayNetworking.send(player, new StorageSystemResyncS2C(this));
    }

    public boolean remove(Position pos) {
        var removed = members.remove(pos) != null;
        if(removed&&context!=null)context.remove(pos);
        return removed;
    }

    public boolean unlink(Position pos) {
        var member = members.get(pos);
        if(member==null)return false;
        if(member.linked==null)return false;
        member.linked = null;
        if(context!=null)context.unlink(pos);
        return true;
    }

    public StorageMember member(Position pos) {
        return members.get(pos);
    }

    public enum LinkResult{
        Success(true),
        CannotLinkToSelf(false),
        ChildIsNotMember(false),
        ParentIsNotMember(false),
        CannotCreateCircularLink(true),
        ParentChildInDifferentWorlds(false);

        public final boolean modified;

        LinkResult(boolean modified) {
            this.modified = modified;
        }
    }

    public LinkResult link(Position child, Position parent) {
        if(child.equals(parent)) return LinkResult.CannotLinkToSelf;
        if(!child.world().equals(parent.world())) return LinkResult.ParentChildInDifferentWorlds;

        if(!members.containsKey(child)) return LinkResult.ChildIsNotMember;
        if(!members.containsKey(parent)) return LinkResult.ParentIsNotMember;

        var start = members.get(child);
        var current = start;
        current.linkTo(parent);

        // TODO this doesn't really work properly...
        var encountered = new HashSet<Position>();
        int i = 0;
        encountered.add(current.pos);
        while(current.linked!=null){
            if(encountered.contains(current.linked)){
                current.linked = null;
                if(context!=null)context.link(child, parent);
                return LinkResult.CannotCreateCircularLink;
            }
            encountered.add(current.linked);
            current = members.get(current.linked);
            if(current==null)break;
            if(i>=MAX_LINKED_LENGTH){
                start.linked = null;
                break;
            }
            i++;
        }

        if(context!=null)context.link(child, parent);
        return LinkResult.Success;
    }

    public boolean add(Position pos, World world){
        if(world!=null&&world.getRegistryKey().getValue().equals(pos.world())&&!(world.getBlockEntity(pos.pos()) instanceof Inventory))return false;
        if(members.containsKey(pos))return false;
        members.put(pos, new StorageMember(pos));
        if(context!=null)context.add(pos);
        return true;
    }

    public void clear() {
        groups.clear();
        members.clear();
        groups.put(0, Group.defaultGroup());
        if(context!=null)context.clear();
    }

    private Iterable<StorageMember> inputPriorityOrdered(){
        return members.values().stream().sorted(Comparator.comparingInt(o -> this.group(o).input.priority()))::iterator;
    }

    private Iterable<StorageMember> outputPriorityOrdered(){
        return members.values().stream().sorted(Comparator.comparingInt(o -> this.group(o).output.priority()))::iterator;
    }

    public Collection<StorageMember> unorderedMembers(){
        return members.values();
    }

    public void setGroup(StorageMember member, Group group){
        if(members.get(member.pos) != member)throw new IllegalArgumentException();
        if(groups.get(group.id) != group)throw new IllegalArgumentException();
        setGroup(member.pos, group.id);
    }

    public void setGroup(Position pos, int groupId){
        members.get(pos).group = groupId;
        if(context!=null)context.setGroup(pos, groupId);
    }

    public Collection<Group> groups(){
        return groups.values();
    }

    public void updateGroup(Group group){
        groups.put(group.id, group);
        if(context!=null)context.updateGroup(group);
    }

    public Group newGroup(){
        int id = 0;
        while(groups.containsKey(id)) id++;
        return new Group(id);
    }

    public Group groupNoDefault(int id){
        return groups.get(id);
    }

    public Group group(int id){
        return groups.getOrDefault(id, new Group(id));
    }

    public Group group(StorageMember member){
        if(members.get(member.pos) != member)throw new IllegalArgumentException();
        var linked = member;
        for(int i = 0; linked.linked!=null&&i< MAX_LINKED_LENGTH; i ++){
            linked = members.get(linked.linked);
            if(linked==null) return Group.defaultGroup();
        }
        return group(linked.group);
    }

    public ItemStack getFromStorage(ServerPlayerEntity player, ItemData item){
        return getFromStorage(player, item, item.stackSize());
    }

    public ItemStack getFromStorage(ServerPlayerEntity player, ItemData item, int desired){
        int moved = 0;
        for (var member : outputPriorityOrdered()) {
            if(!this.group(member).input.matches(item))continue;
            if (member.pos.blockEntityAt(player.server) instanceof Inventory inv) {
                for(var stack : inv){
                    if(item.equals(stack)){
                        var remaining = desired-moved;
                        var toMove = Math.min(remaining, stack.getCount());
                        if(toMove>0){
                            moved += toMove;
                            stack.setCount(stack.getCount()-toMove);
                            inv.markDirty();
                        }
                    }
                }
            }
            if(moved==desired)break;
        }
        return item.withCount(moved);
    }

    public void insertIntoStorage(ServerPlayerEntity player, ItemStack stack){
        insertIntoStorage(player, stack, stack.getCount());
    }

    public void insertIntoStorage(ServerPlayerEntity player, ItemStack stack, int desiredAmount){
        desiredAmount = Math.min(stack.getCount(), desiredAmount);
        outer:
        for (var member : inputPriorityOrdered()) {
            if(desiredAmount==0)break;
            if(stack.isEmpty())break;
            if(!this.group(member).input.matches(new ItemData(stack)))continue;

            if (member.pos.blockEntityAt(player.server) instanceof Inventory inv) {
                for (int i = 0 ; i < inv.size(); i ++) {
                    if(desiredAmount==0)break outer;

                    var other_stack = inv.getStack(i);
                    if (other_stack.isEmpty()) {
                        var amount = Math.min(desiredAmount, stack.getMaxCount());
                        inv.setStack(i, stack.copyWithCount(amount));
                        stack.setCount(stack.getCount()-amount);
                        desiredAmount -= amount;
                        inv.markDirty();
                    } else if (ItemData.equals(other_stack, stack)) {
                        var remaining = other_stack.getMaxCount() - other_stack.getCount();
                        var toMove = Math.min(remaining, desiredAmount);
                        if (toMove > 0) {
                            stack.setCount(stack.getCount() - toMove);
                            desiredAmount -= toMove;
                            other_stack.setCount(other_stack.getCount() + toMove);
                            inv.markDirty();
                        }
                    }
                }
            }
        }
    }
}
