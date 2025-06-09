package com.parkertenbroeck.remotestorage.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.ItemData;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemPositionsS2C;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.component.Component;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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
                    Codec.sizeLimitedString(50).fieldOf("name").forGetter(i -> i.name)
            ).apply(instance, StorageSystem::new)
    );


    public StorageSystem(){
        next_id = 1;
        clear();
    }

    private StorageSystem(String name){
        this.name = name;
        next_id = 1;
    }

    public static final int MAX_LINKED_LENGTH = 5;

    public StorageSystemPositionsS2C getPositions() {
        return new StorageSystemPositionsS2C(
                name,
                members.entrySet()
                        .stream()
                        .map(e -> new StorageSystemPositionsS2C.Member(
                                e.getKey(),
                                e.getValue().linked,
                                e.getValue().group
                        )).toList()
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
        groups.put(0, new Group());
    }

    public Iterable<StorageMember> inputPriorityOrdered(){
        return members.values().stream().sorted(Comparator.comparingInt(o -> o.getGroup().inputPriority))::iterator;
    }

    public Iterable<StorageMember> outputPriorityOrdered(){
        return members.values().stream().sorted(Comparator.comparingInt(o -> o.getGroup().outputPriority))::iterator;
    }

    public Iterable<StorageMember> unorderedMembers(){
        return members.values();
    }

    public Stream<StorageMember> streamUnorderedMembers(){
        return members.values().stream();
    }

    public final class Group {
        public String name;
        public ListKind kind = ListKind.Blacklist;
        private int inputPriority;
        private int outputPriority;
        public final ArrayList<ElementKind> list = new ArrayList<>();

        public void setInputPriority(int inputPriority){
            this.inputPriority = inputPriority;
        }

        public void setOutputPriority(int outputPriority){
            this.outputPriority = outputPriority;
        }
    }

    public final class StorageMember {
        public final Position pos;
        private Position linked;
        private int group;

        public StorageMember(Position pos) {
            this.pos = pos;
        }

        public void setGroup(int group){
            this.linked = null;
            this.group = group;
        }

        public void linkTo(Position pos){
            this.group = 0;
            this.linked = pos;

            var encountered = new HashSet<Position>();
            int i = 0;
            var current = this;
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
                    linked = null;
                    break;
                }
                i++;
            }
        }

        public boolean canInsertItem(ItemData item){
            var group = getGroup();
            for(var filter : group.list){
                var match = filter.matches(item);
                if(match)return group.kind != ListKind.Blacklist;
            }
            return group.kind == ListKind.Blacklist;
        }

        public boolean canRemoveItem(ItemData item){
            return true;
        }

        public Group getGroup(){
            var linked = this;
            for(int i = 0; linked.linked!=null&&i< MAX_LINKED_LENGTH; i ++){
                linked = members.get(linked.linked);
                if(linked==null) return new Group();
            }
            if(!groups.containsKey(linked.group))return new Group();
            return groups.get(linked.group);
        }


    }
    public enum ListKind {
        Blacklist,
        Whitelist;
    }
    public sealed interface ElementKind{
        boolean matches(ItemData item);
    }
    public record TagMatch(Identifier tag) implements ElementKind {
        @Override
        public boolean matches(ItemData item) {
            return item.item().streamTags().anyMatch(tag -> tag.id().equals(this.tag));
        }
    }
    public record ItemMatch(Item item) implements ElementKind {
        @Override
        public boolean matches(ItemData item) {
            return item.item().getItem() == this.item;
        }
    }
    public record ComponentMatch(Component<?> component) implements ElementKind {
        @Override
        public boolean matches(ItemData item) {
            return Objects.equals(item.item().getComponents().get(component.type()), component.value());
        }
    }
    public record ItemDataMatch(ItemData exact) implements ElementKind {
        @Override
        public boolean matches(ItemData item) {
            return item.equals(exact);
        }
    }

    public ItemStack getFromStorage(ServerPlayerEntity player, ItemData item){
        return getFromStorage(player, item, item.stackSize());
    }

    public ItemStack getFromStorage(ServerPlayerEntity player, ItemData item, int desired){
        int moved = 0;
        for (var member : outputPriorityOrdered()) {
            if(!member.canRemoveItem(item))continue;
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
            if(!member.canInsertItem(new ItemData(stack)))continue;

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
