package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemMembersS2C;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.StringIdentifiable;

import java.util.*;
import java.util.stream.Stream;

public class StorageSystem {
    public String name = "Default";
    final HashMap<Position, StorageMember> members = new HashMap<>();
    final Int2ObjectOpenHashMap<Group> groups = new Int2ObjectOpenHashMap<>();
    private int next_id;

    public static final Codec<StorageSystem> CODEC = RecordCodecBuilder.create(
        instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(i -> i.name),
                    Codec.list(StorageMember.CODEC).fieldOf("members").forGetter(i -> i.members.values().stream().toList()),
                    Codec.list(Group.CODEC).fieldOf("groups").forGetter(i -> i.groups.values().stream().toList()),
                    Codec.INT.fieldOf("next_id").forGetter(i -> i.next_id)
            ).apply(instance, StorageSystem::new)
    );

    public StorageSystem(){
        next_id = 1;
        clear();
    }

    private StorageSystem(String name, List<StorageMember> members, List<Group> groups, int next_id){
        this.name = name;
        members.forEach(m -> this.members.put(m.pos, m));
        groups.forEach(g -> this.groups.put(g.id, g));
        this.next_id = next_id;
    }

    public static final int MAX_LINKED_LENGTH = 5;

    public StorageSystemMembersS2C getPositions() {
        return new StorageSystemMembersS2C(
                name,
                members.values().stream().toList()
        );
    }

    protected boolean remove(Position pos) {
        return members.remove(pos) != null;
    }

    public boolean unlink(Position pos) {
        var member = members.get(pos);
        if(member==null)return false;
        if(member.linked==null)return false;
        member.linked = null;
        return true;
    }


    protected boolean addMember(Position pos){
        if(members.containsKey(pos))return false;
        members.put(pos, new StorageMember(pos));
        return true;
    }

    protected void clear() {
        groups.clear();
        members.clear();
        groups.put(0, Group.defaultGroup());
    }

    private Iterable<StorageMember> inputPriorityOrdered(){
        return members.values().stream().sorted(Comparator.comparingInt(o -> this.getGroup(o).input.priority))::iterator;
    }

    private Iterable<StorageMember> outputPriorityOrdered(){
        return members.values().stream().sorted(Comparator.comparingInt(o -> this.getGroup(o).output.priority))::iterator;
    }

    public Iterable<StorageMember> unorderedMembers(){
        return members.values();
    }

    public Stream<StorageMember> streamUnorderedMembers(){
        return members.values().stream();
    }

    public void link(Position child, Position parent) {
        var start = members.get(child);
        var current = start;
        current.linkTo(parent);

        var encountered = new HashSet<Position>();
        int i = 0;
        encountered.add(current.pos);
        while(current.linked!=null){
            if(encountered.contains(current.linked)){
                current.linked = null;
                return;
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
    }

    public Group getGroup(StorageMember member){
        var linked = member;
        for(int i = 0; linked.linked!=null&&i< MAX_LINKED_LENGTH; i ++){
            linked = members.get(linked.linked);
            if(linked==null) return Group.defaultGroup();
        }
        if(!groups.containsKey(linked.group))return Group.defaultGroup();
        return groups.get(linked.group);
    }

    public enum ListKind implements StringIdentifiable {
        Blacklist,
        Whitelist;

        @Override
        public String asString() {
            return this.name().toLowerCase();
        }
    }

    public ItemStack getFromStorage(ServerPlayerEntity player, ItemData item){
        return getFromStorage(player, item, item.stackSize());
    }

    public ItemStack getFromStorage(ServerPlayerEntity player, ItemData item, int desired){
        int moved = 0;
        for (var member : outputPriorityOrdered()) {
            if(!this.getGroup(member).input.check(item))continue;
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
            if(!this.getGroup(member).input.check(new ItemData(stack)))continue;

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
