package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.RemoteStorageActionC2S;
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
    public final RSInventory fakeInventory;
    private final ServerPlayerEntity player;
    private StorageSystem system;

    private final int width = 9;
    private final int height = 6;
    public final int playerInventorySize;

    public int storageRevision = 0;

    public Map<ItemData, Integer> currentMap = new HashMap<>();
    private Map<ItemData, Integer> lastMap = new HashMap<>();

    public void acceptAction(RemoteStorageActionC2S payload, PlayerEntity player) {
        if(payload.isTakingFromStorage()){
            if(payload.kind()==0) {
                storageIntoCursor(payload.item(), payload.amount());
            }else if(payload.kind()==1){
                quickMoveOut(payload.item(), payload.amount());
            }
        }else{
            if(payload.kind()==0) {
                cursorIntoStorage(payload.amount());
            }else if(payload.kind()==1) {
                quickMove(player, payload.slot());
            }
        }
    }

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
//            this.markDirty();
        }
    }

    public RemoteStorageScreenHandler(int syncId, PlayerInventory playerInventory, StorageSystem system, PlayerEntity player) {
        super(RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, syncId);
        this.system = system;

        playerInventorySize = playerInventory.getMainStacks().size();
        this.player = (ServerPlayerEntity) player;
        fakeInventory = null;

        this.addPlayerSlots(playerInventory, 9, 132);
//        serverTick();// gets the contents packet there faster... should probably put it in the custom payload?
    }

    public void serverTick(){
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
        System.out.println(contents);
        if(contents.revision()!=storageRevision){
            currentMap = contents.map();
            storageRevision = contents.revision();
        }else{
            contents.map().forEach(this::setLocalItemCount);
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

    void modifyLocalItemCount(ItemData item, int count){
        var result = currentMap.compute(item, (ignore, old_c) -> (old_c==null?0:old_c)+count);
        if(result<=0)currentMap.remove(item);
    }

    void setLocalItemCount(ItemData item, int count){
        if(count<=0)currentMap.remove(item);
        else currentMap.put(item, count);
    }

    void storageIntoCursor(ItemData item, int amount){
        if(!getCursorStack().isEmpty()&&!item.equals(getCursorStack())) return;

        var stackSize = item.stackSize();
        int capped = Math.min(amount, stackSize-getCursorStack().getCount());
        if(capped<=0)return;
        if(isServer()){
            var got = getFromStorage(item, capped);
            if(got.getCount()!=capped)makeDirty(item);
            capped = got.getCount();
        }else{
            modifyLocalItemCount(item, -capped);
        }
        if(getCursorStack().isEmpty())
            setCursorStack(item.withCount(capped));
        else
            getCursorStack().setCount(getCursorStack().getCount()+capped);
    }

    void cursorIntoStorage(int amount){
        if(getCursorStack().isEmpty())return;

        int capped = Math.min(getCursorStack().getCount(), amount);
        var item = new ItemData(getCursorStack());
        if(isServer()){
            var old = getCursorStack().getCount();
            insertIntoStorage(getCursorStack(), capped);
            var diff = old-getCursorStack().getCount();
            if(diff!=capped) makeDirty(item);
        }else{
            modifyLocalItemCount(item, capped);
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
        Slot slot = this.slots.get(slot_index);
        if(slot == null || !slot.hasStack()) return ItemStack.EMPTY;

        if(slot_index >= playerInventorySize){
            RemoteStorage.LOGGER.warn("This should not be ran");
        }else{
            // player inv -> storage system
            if(isClient()){
                modifyLocalItemCount(new ItemData(slot.getStack()), slot.getStack().getCount());
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
