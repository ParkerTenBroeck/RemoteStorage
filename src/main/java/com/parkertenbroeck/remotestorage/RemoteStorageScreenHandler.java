package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsS2C;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;


public class RemoteStorageScreenHandler extends ScreenHandler {
    private final RSInventory inventory;
    private final Property scroll;
    private final ServerPlayerEntity player;
    private StorageSystem system;

    static class RemoteStorageSlot extends Slot{
        public RemoteStorageSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }

        @Override
        public boolean canTakePartial(PlayerEntity player) {
            return false;
        }
    }

    static class RSInventory extends SimpleInventory{
        RSInventory(int size){
            super(size);
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            this.heldStacks.set(slot, stack);
            this.markDirty();
        }

        public void checkShift(int slot_index) {
            if(this.heldStacks.get(slot_index).isEmpty()){
                for(int i = slot_index; i < this.heldStacks.size()-1;i++){
                    this.heldStacks.set(i, this.heldStacks.get(i+1));
                }
                this.heldStacks.set(this.heldStacks.size()-1, ItemStack.EMPTY);
                this.markDirty();
            }
        }
    }

    public RemoteStorageScreenHandler(int syncId, PlayerInventory playerInventory, StorageSystem system, PlayerEntity player) {
        super(RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, syncId);
        this.system = system;
        scroll = this.addProperty(Property.create());

        this.player = (ServerPlayerEntity) player;



        int width = 9;
        int height = 6;
        this.inventory = new RSInventory(height*width);
        inventory.onOpen(playerInventory.player);


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.addSlot(new RemoteStorageSlot(inventory, x + y * width, 9 + x * 18, 18 + y * 18));
            }
        }

        this.addPlayerSlots(playerInventory, 9, 132);
    }

    @Override
    public void sendContentUpdates() {
        if(this.player.getServer()!=null) {
            HashMap<Item, Integer> items = new HashMap<>();

            for (var member : system.unorderedMembers()) {
                if (member.pos.blockEntityAt(player.server) instanceof Inventory i) {
                    for(var stack : i){
                        if (stack.isEmpty()) continue;
                        items.compute(stack.getItem(),
                                (item, integer) -> (integer == null ? 0 : integer) + stack.getCount()
                        );
                    }
                }
            }

            ArrayList<ItemStack> list = new ArrayList<>();
            items.forEach((item, count) -> {
                list.add(new ItemStack(item, count));
            });
            list.sort((o1, o2) -> o1.getName().toString().compareToIgnoreCase(o2.getName().toString()));

            for(int i = 0; i < inventory.size(); i ++){
                var stack = i<list.size()?list.get(i):ItemStack.EMPTY;

                if(!ItemStack.areEqual(stack, inventory.getStack(i))){
                    getSlot(i).setStack(stack);
                    getSlot(i).markDirty();
                }
            }
        }

        super.sendContentUpdates();
    }

    public RemoteStorageScreenHandler(int syncId, PlayerInventory playerInventory, RemoteStorageContentsS2C s2c) {
        super(RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, syncId);
        scroll = this.addProperty(Property.create());
        this.player = null;

        int width = 9;
        int height = 6;
        this.inventory = new RSInventory(height*width);
        inventory.onOpen(playerInventory.player);


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.addSlot(new RemoteStorageSlot(inventory, x + y * width, 9 + x * 18, 18 + y * 18));
            }
        }

        this.addPlayerSlots(playerInventory, 9, 132);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {

        if(system!=null)System.out.println(actionType + " " + slotIndex + " " + button);
        if(slotIndex >= inventory.size() || slotIndex < 0) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }
        switch(actionType){
            case PICKUP -> {
                Slot slot = getSlot(slotIndex);
                if(button==0){
                    if(getCursorStack().isEmpty()){
                        if(slot.getStack().isEmpty())return;
                        int desired = Math.min(slot.getStack().getCount(), slot.getStack().getMaxCount());
                        var item = slot.getStack().getItem();
                        if(system!=null){
                            var got = getFromStorage(item, desired).getCount();
                            slot.getStack().setCount(slot.getStack().getCount()-got);
                            if(got!=desired)slot.markDirty();
                            desired = got;
                        }else{
                            slot.getStack().setCount(slot.getStack().getCount()-desired);
                        }
                        inventory.checkShift(slot.getIndex());
                        setCursorStack(new ItemStack(item, desired));
                    }else{
                        if(system!=null)
                            insertIntoStorage(getCursorStack());
                        else {
                            setCursorStack(ItemStack.EMPTY);
                        }
                    }
                }else if(button==1){
                    if(getCursorStack().isEmpty()){
                        var current = slot.getStack().getCount();
                        var desired = (current+1)/2;
                        if(current>slot.getStack().getMaxCount()){
                            desired = (slot.getStack().getMaxCount()+1)/2;
                        }
                        if(system!=null){
                            var got = getFromStorage(slot.getStack().getItem(), desired);
                            slot.getStack().setCount(slot.getStack().getCount()-got.getCount());
                            if(got.getCount()!=desired)slot.markDirty();
                            setCursorStack(got);
                        }else{
                            setCursorStack(slot.getStack().copyWithCount(desired));
                            slot.getStack().setCount(slot.getStack().getCount()-desired);
                        }
                    }else{
                        if(system!=null) {
                            insertIntoStorage(getCursorStack(), 1);
                            slot.getStack().setCount(slot.getStack().getCount()-1);
                        }else {
                            slot.getStack().setCount(slot.getStack().getCount()-1);
                            getCursorStack().setCount(getCursorStack().getCount() - 1);
                        }
                    }
                    inventory.checkShift(slot.getIndex());
                }

            }
            case QUICK_MOVE -> quickMove(player, slotIndex);
            case THROW -> {
                Slot slot = getSlot(slotIndex);
                if(slot.getStack().isEmpty())return;
                var amount = button==0?Math.min(slot.getStack().getCount(), 1):Math.min(slot.getStack().getCount(), slot.getStack().getMaxCount());
                if(amount<=0)return;
                if(system!=null){
                    amount = getFromStorage(slot.getStack().getItem(), amount).getCount();
                    slot.markDirty();
                }
                player.dropItem(slot.getStack().copyWithCount(amount), true);
                slot.getStack().setCount(slot.getStack().getCount()-amount);
            }

            case CLONE, SWAP, QUICK_CRAFT, PICKUP_ALL -> {}
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot_index) {
        System.out.println(player);
        Slot slot = this.slots.get(slot_index);
        if(slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        if(slot_index < inventory.size()){
            int toTransfer = Math.min(slot.getStack().getCount(), slot.getStack().getMaxCount());//TODO calculate max transfer that player inventory can accept

            ItemStack transfered;
            if(system!=null)
                transfered= getFromStorage(slot.getStack().getItem(), toTransfer);
            else
                transfered = new ItemStack(slot.getStack().getItem(), toTransfer);

            slot.getStack().setCount(slot.getStack().getCount()-transfered.getCount());
            this.insertItem(transfered, this.inventory.size(), this.slots.size(), true);

            if(system!=null&&transfered.getCount()!=0){
                RemoteStorage.LOGGER.warn("Not all items transferred!");
                insertIntoStorage(transfered);
                if(transfered.getCount()!=0){
                    this.setCursorStack(transfered);
                    RemoteStorage.LOGGER.warn("Not all items transferred BACK!!!!!");
                }
            }
        }else{
            // player inv -> storage system
            if(system==null){
                slot.setStack(ItemStack.EMPTY); // we can't check if it will succeed on client so we assume it will work
            }else{
                insertIntoStorage(slot.getStack());
                if(!slot.getStack().isEmpty())slot.markDirty();
            }
        }

        return ItemStack.EMPTY;
    }

    public ItemStack getFromStorage(Item item, int desired){
        int moved = 0;
        for (var member : system.outputPriorityOrdered()) {
            if(!member.canRemoveItem(item))continue;
            if (member.pos.blockEntityAt(player.server) instanceof Inventory inv) {
                for(var other_stack : inv){
                    if(other_stack.getItem().equals(item)){
                        var remaining = desired-moved;
                        var toMove = Math.min(remaining, other_stack.getCount());
                        if(toMove>0){
                            moved += toMove;
                            other_stack.setCount(other_stack.getCount()-toMove);
                            inv.markDirty();
                        }
                    }
                }
            }
            if(moved==desired)break;
        }
        return new ItemStack(item, moved);
    }

    public void insertIntoStorage(ItemStack stack){
        insertIntoStorage(stack, stack.getCount());
    }

    public void insertIntoStorage(ItemStack stack, int desiredAmount){
        desiredAmount = Math.min(stack.getCount(), desiredAmount);
        outer:
        for (var member : system.inputPriorityOrdered()) {
            if(desiredAmount==0)break;
            if(stack.isEmpty())break;
            if(!member.canInsertItem(stack.getItem()))continue;

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
                    } else if (other_stack.getItem().equals(stack.getItem())) {
                        var remaining = other_stack.getItem().getMaxCount() - other_stack.getCount();
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

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
