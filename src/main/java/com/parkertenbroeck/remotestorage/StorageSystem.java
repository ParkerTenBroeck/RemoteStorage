package com.parkertenbroeck.remotestorage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemPositionsS2C;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.Component;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Stream;

public class StorageSystem {
    public String name = "default";
    public final HashMap<Position, StorageMember> members = new HashMap<>();
    public final HashMap<String, Group> groups = new HashMap<>();

    public static final Codec<StorageSystem> CODEC = RecordCodecBuilder.create(
        instance ->
            instance.group(
                    Codec.sizeLimitedString(50).fieldOf("name").forGetter(i -> i.name)
            ).apply(instance, StorageSystem::new)
    );

    private StorageSystem(String name){
        this.name = name;
    }

    public static final int MAX_GROUP_LINKED_RECURSION = 5;

    public StorageSystemPositionsS2C getPositions() {
        return new StorageSystemPositionsS2C(
                name,
                members.entrySet()
                        .stream()
                        .map(e -> new StorageSystemPositionsS2C.Member(
                                e.getKey(),
                                e.getValue().linked,
                                0
                        )).toList()
        );
    }

    public record Position(BlockPos pos, Identifier world){
        public static final PacketCodec<RegistryByteBuf, Position> PACKET_CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, Position::pos,
                Identifier.PACKET_CODEC, Position::world,
                Position::new
        );

        BlockEntity blockEntityAt(MinecraftServer server){
            var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.world));
            if(world==null)return null;
            return world.getBlockEntity(this.pos);
        }

        public static Position of(ServerPlayerEntity player, BlockPos pos){
            return new Position(pos, player.getServerWorld().getRegistryKey().getValue());
        }
    }

    public StorageSystem(){
        clear();
    }

    public boolean add_default(BlockPos pos, Identifier world){
        var p = new Position(pos, world);
        if(members.containsKey(p))return false;
        members.put(p, new StorageMember(p, groups.get(null)));
        return true;
    }

    public void clear() {
        groups.clear();
        members.clear();
        groups.put(null, new Group());
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
        private Group group;

        public StorageMember(Position pos, Group group) {
            this.pos = pos;
            this.group = group;
        }

        public void setGroup(){
            this.linked = null;
        }

        public void linkTo(Position pos){
            this.group = null;
            this.linked = pos;
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
            for(int i = 0; linked.group==null&&i<MAX_GROUP_LINKED_RECURSION; i ++){
                if(linked.group!=null)return linked.group;
                if(linked.linked==null) return new Group();
                linked = members.get(linked.linked);
                if(linked==null) return new Group();
            }
            return new Group();
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
