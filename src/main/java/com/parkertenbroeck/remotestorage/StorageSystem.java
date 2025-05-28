package com.parkertenbroeck.remotestorage;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.Component;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class StorageSystem {
    public String name = "";
    public final HashMap<Position, StorageMember> members = new HashMap<>();
    public final HashMap<String, Group> groups = new HashMap<>();

    public record Position(BlockPos pos, Identifier world){
        BlockEntity blockEntityAt(MinecraftServer server){
            var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.world));
            if(world==null)return null;
            return world.getBlockEntity(this.pos);
        }
    }

    public StorageSystem(){
        clear();
    }

    public void add_default(BlockPos pos, Identifier world){
        members.computeIfAbsent(new Position(pos, world), p -> new StorageMember(p, groups.get(null)));
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
//            for(var filter : group.list){
//                switch (filter){
//                    case ItemExact(var exact) -> {
//                        return item.getComponents().equals(exact.getComponents());
//                    }
//                    case ItemLoose itemLoose -> {
//                        return item.getComponents().contains()
//                    }
//                    case Tag tag -> {
//                        item.get
//                    }
//                }
//            }
            return true;
        }

        public boolean canRemoveItem(ItemData item){
            return true;
        }

        public Group getGroup(){
            if(linked!=null&&members.containsKey(linked))return members.get(linked).getGroupNoLinked();
            return getGroupNoLinked();
        }

        private Group getGroupNoLinked() {
            if(group==null)return new Group();
            return group;
        }
    }
    public enum ListKind {
        Blacklist,
        Whitelist;
    }
    public sealed interface ElementKind{}
    public record TagMatch(String tag) implements ElementKind {}
    public record ItemMatch(String name) implements ElementKind {}
    public record ComponentMatch(Component<?> component) implements ElementKind {}
    public record ItemDataMatch(ItemData exact) implements ElementKind {}
}
