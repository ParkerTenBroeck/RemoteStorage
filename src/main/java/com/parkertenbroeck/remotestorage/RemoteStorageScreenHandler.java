package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class RemoteStorageScreenHandler extends ScreenHandler {
    private final RSInventory fakeInventory;
    private final ServerPlayerEntity player;
    private StorageSystem system;

    private final int width = 9;
    private final int height = 6;
    private final int playerInventorySize;

    public int storageRevision = 0;

    private Map<ItemData, Integer> currentMap = new HashMap<>();
    private Map<ItemData, Integer> lastMap = new HashMap<>();

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

        playerInventorySize = playerInventory.getMainStacks().size();
        this.player = (ServerPlayerEntity) player;
        fakeInventory = null;

        this.addPlayerSlots(playerInventory, 9, 132);
    }

    @Override
    public void sendContentUpdates() {
        if(isServer()) {
            lastMap.clear();

            for (var member : system.unorderedMembers()) {
                if (member.pos.blockEntityAt(player.server) instanceof Inventory i) {
                    for(var stack : i){
                        if (stack.isEmpty()) continue;
                        lastMap.compute(new ItemData(stack), (ignored, count) -> (count==null?0:count)+stack.getCount());
                    }
                }
            }

            var difference = new HashMap<ItemData, Integer>();
            lastMap.forEach((key, value) -> {
                if(!Objects.equals(currentMap.remove(key), value)) difference.put(key, value);
            });
            currentMap.forEach((key, value) -> {
                difference.put(key, 0);
            });
            currentMap.clear();
            {
                var tmp = lastMap;
                lastMap = currentMap;
                currentMap = tmp;
            }

            if(!difference.isEmpty())
                ServerPlayNetworking.send(player, new RemoteStorageContentsDeltaS2C(syncId, storageRevision, difference));
        }

        super.sendContentUpdates();
    }

    public RemoteStorageScreenHandler(int syncId, PlayerInventory playerInventory, OpenRemoteStorageS2C s2c) {
        super(RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, syncId);

        this.player = null;
        playerInventorySize = playerInventory.getMainStacks().size();
        this.addPlayerSlots(playerInventory, 9, 132);


        int width = 9;
        int height = 6;
        this.fakeInventory = new RSInventory(height*width);
        fakeInventory.onOpen(playerInventory.player);


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                this.addSlot(new RemoteStorageSlot(fakeInventory, x + y * width, 9 + x * 18, 18 + y * 18));
            }
        }
    }

    public void receiveContentsDelta(RemoteStorageContentsDeltaS2C contents){
        System.out.println(contents.map());
        if(contents.revision()!=storageRevision){
            currentMap = contents.map();
            storageRevision = contents.revision();
        }else{
            contents.map().forEach((key, value) -> {
                if(value==0)currentMap.remove(key);
                else currentMap.put(key, value);
            });
        }
        int i = 0;
        for(var entry : currentMap.entrySet()){
            var stack = entry.getKey().withCount(entry.getValue());
            getSlot(playerInventorySize+i).setStack(stack);
            i++;
            if(i>=fakeInventory.size())break;
        }
        for(; i < fakeInventory.size(); i ++){
            getSlot(playerInventorySize+i).setStack(ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private boolean isClient(){
        return system==null;
    }

    private boolean isServer(){
        return system!=null;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    //    @Override
//    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
//
//        if(system!=null)System.out.println(actionType + " " + slotIndex + " " + button);
//        if(slotIndex >= inventory.size() || slotIndex < 0) {
//            super.onSlotClick(slotIndex, button, actionType, player);
//            return;
//        }
//        switch(actionType){
//            case PICKUP -> {
//                Slot slot = getSlot(slotIndex);
//                if(button==0){
//                    if(getCursorStack().isEmpty()){
//                        if(slot.getStack().isEmpty())return;
//                        int desired = Math.min(slot.getStack().getCount(), slot.getStack().getMaxCount());
//                        var item = slot.getStack().getItem();
//                        if(isServer()){
//                            var got = getFromStorage(item, desired).getCount();
//                            slot.getStack().setCount(slot.getStack().getCount()-got);
//                            if(got!=desired)slot.markDirty();
//                            desired = got;
//                        }else{
//                            slot.getStack().setCount(slot.getStack().getCount()-desired);
//                            inventory.checkShift(slot.getIndex());
//                        }
//                        setCursorStack(new ItemStack(item, desired));
//                    }else{
//                        if(system!=null)
//                            insertIntoStorage(getCursorStack());
//                        else {
//                            setCursorStack(ItemStack.EMPTY);
//                        }
//                    }
//                }else if(button==1){
//                    if(getCursorStack().isEmpty()){
//                        var current = slot.getStack().getCount();
//                        var desired = (current+1)/2;
//                        if(current>slot.getStack().getMaxCount()){
//                            desired = (slot.getStack().getMaxCount()+1)/2;
//                        }
//                        if(system!=null){
//                            var got = getFromStorage(slot.getStack().getItem(), desired);
//                            slot.getStack().setCount(slot.getStack().getCount()-got.getCount());
//                            if(got.getCount()!=desired)slot.markDirty();
//                            setCursorStack(got);
//                        }else{
//                            setCursorStack(slot.getStack().copyWithCount(desired));
//                            slot.getStack().setCount(slot.getStack().getCount()-desired);
//                        }
//                    }else{
//                        if(system!=null) {
//                            insertIntoStorage(getCursorStack(), 1);
//                            slot.getStack().setCount(slot.getStack().getCount()-1);
//                        }else {
//                            slot.getStack().setCount(slot.getStack().getCount()-1);
//                            getCursorStack().setCount(getCursorStack().getCount() - 1);
//                        }
//                    }
//                    inventory.checkShift(slot.getIndex());
//                }
//
//            }
//            case QUICK_MOVE -> quickMove(player, slotIndex);
//            case THROW -> {
//                Slot slot = getSlot(slotIndex);
//                if(slot.getStack().isEmpty())return;
//                var amount = button==0?Math.min(slot.getStack().getCount(), 1):Math.min(slot.getStack().getCount(), slot.getStack().getMaxCount());
//                if(amount<=0)return;
//                if(system!=null){
//                    amount = getFromStorage(slot.getStack().getItem(), amount).getCount();
//                    slot.markDirty();
//                }
//                player.dropItem(slot.getStack().copyWithCount(amount), true);
//                slot.getStack().setCount(slot.getStack().getCount()-amount);
//            }
//
//            case CLONE, SWAP, QUICK_CRAFT, PICKUP_ALL -> {}
//        }
//    }

    void modifyItemLocalCount(ItemData item, int count){
        var result = currentMap.compute(item, (ignore, old_c) -> (old_c==null?0:old_c)+count);
        if(result<=0)currentMap.remove(item);
    }

    void storageIntoCursor(ItemData item, int amount){
        if(!getCursorStack().isEmpty()&&!item.equals(getCursorStack())) return;

        var stackSize = item.stackSize();
        int capped = Math.min(amount, stackSize-getCursorStack().getCount());
        if(capped<=0)return;
        if(isServer()){
            var got = getFromStorage(item, capped);
            if(got.getCount()!=capped)makeDirty(item);
            getCursorStack().setCount(getCursorStack().getCount()+got.getCount());
        }else{
            modifyItemLocalCount(item, -capped);
            getCursorStack().setCount(getCursorStack().getCount()+capped);
        }
    }

    void cursorIntoStorage(int amount){
        if(getCursorStack().isEmpty())return;

        int capped = Math.max(getCursorStack().getCount(), amount);
        var item = new ItemData(getCursorStack());
        if(isServer()){
            var old = getCursorStack().getCount();
            insertIntoStorage(getCursorStack(), capped);
            var diff = old-getCursorStack().getCount();
            if(diff!=capped) makeDirty(item);
        }else{
            modifyItemLocalCount(item, -capped);
            getCursorStack().setCount(getCursorStack().getCount()-capped);
        }
    }

    void quickMoveOut(ItemData item, int amount){
        if(isServer()) {
            //TODO calculate max transfer that player inventory can accept
            amount =  Math.min(amount, item.stackSize());

            var transferred = getFromStorage(item, amount);

            this.insertItem(transferred, 0, playerInventorySize, true);

            if(transferred.getCount()!=0){
                RemoteStorage.LOGGER.warn("Not all items transferred!");
                insertIntoStorage(transferred);
                if(transferred.getCount()!=0){
                    this.setCursorStack(transferred);
                    RemoteStorage.LOGGER.warn("Not all items transferred BACK!!!!!");
                }
                makeDirty(item);
            }
        }else {
            var count = currentMap.get(item);
            if(count==null)count=0;
            amount = Math.min(count, Math.min(amount, item.stackSize()));
            count -= amount;
            if(count<=0)currentMap.remove(item);
            else currentMap.put(item, count);

            this.insertItem(item.withCount(amount), 0, playerInventorySize, true);
        }
    }

    private void makeDirty(ItemData item){
        currentMap.put(item, -1);//ensures this gets updated
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot_index) {
        System.out.println(player);
        Slot slot = this.slots.get(slot_index);
        if(slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        if(slot_index >= playerInventorySize){
            RemoteStorage.LOGGER.warn("This should not be ran");
        }else{
            // player inv -> storage system
            if(isClient()){
                modifyItemLocalCount(new ItemData(slot.getStack()), slot.getStack().getCount());
                slot.setStack(ItemStack.EMPTY); // we can't check if it will succeed on client so we assume it will work
            }else{
                insertIntoStorage(slot.getStack());
                if(!slot.getStack().isEmpty()) {
                    slot.markDirty();
                    makeDirty(new ItemData(slot.getStack()));
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public ItemStack getFromStorage(ItemData item, int desired){
        int moved = 0;
        for (var member : system.outputPriorityOrdered()) {
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

    public void insertIntoStorage(ItemStack stack){
        insertIntoStorage(stack, stack.getCount());
    }

    public void insertIntoStorage(ItemStack stack, int desiredAmount){
        desiredAmount = Math.min(stack.getCount(), desiredAmount);
        outer:
        for (var member : system.inputPriorityOrdered()) {
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

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
    }
}
