package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.RemoteStorageActionC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import com.parkertenbroeck.remotestorage.system.StorageSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.RecipeBookType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.ArmorSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;


public class RemoteStorageScreenHandler extends AbstractCraftingScreenHandler {
    public final RSInventory fakeInventory;
    private final PlayerEntity player;
    private StorageSystem system;

    public final int storageWidth = 9;
    public final int storageHeight = 6;
    public final int craftingGridOutputSlot;
    public final int craftingGridInputSlotsStart;
    public final int craftingGridInputSlotsEnd;
    public final int playerEquipmentSlotsStart;
    public final int playerEquipmentSlotsEnd;
    public final int playerMainInvSlotsStart;
    public final int playerMainInvSlotsEnd;
    public final int playerOffHandSlot;

    public final int fakeInvStart;

    public int storageRevision = 0;

    public Map<ItemData, Integer> currentMap = new HashMap<>();
    private Map<ItemData, Integer> lastMap = new HashMap<>();

    public void acceptAction(RemoteStorageActionC2S payload, PlayerEntity player) {
        if(payload.revision()!=storageRevision)
            RemoteStorage.LOGGER.warn("Storage revisions don't match");
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

    @Override
    public Slot getOutputSlot() {
        return this.slots.get(0);
    }

    @Override
    public List<Slot> getInputSlots() {
        return  this.slots.subList(1, 10);
    }

    @Override
    protected PlayerEntity getPlayer() {
        return player;
    }

    @Override
    public RecipeBookType getCategory() {
        return RecipeBookType.CRAFTING;
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
        }
    }

    public RemoteStorageScreenHandler(int syncId, PlayerInventory playerInventory, StorageSystem system) {
        this(syncId, playerInventory, system, null);
    }

    public RemoteStorageScreenHandler(int syncId, PlayerInventory playerInventory, OpenRemoteStorageS2C init) {
        this(syncId, playerInventory, null, init);
    }

    private RemoteStorageScreenHandler(int syncId, PlayerInventory inv, @Nullable StorageSystem system, @Nullable OpenRemoteStorageS2C init){
        super(RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, syncId, 3, 3);

        this.system = system;

        this.player = inv.player;

        this.craftingGridOutputSlot = this.slots.size();
        this.addResultSlot(inv.player, 134, 132+18);
        this.craftingGridInputSlotsStart = this.slots.size();
        this.addInputSlots(26, 132);
        this.craftingGridInputSlotsEnd = this.slots.size();


        this.playerEquipmentSlotsStart = this.slots.size();
        this.addSlot(new ArmorSlot(inv, inv.player,
                EquipmentSlot.HEAD,
                EquipmentSlot.HEAD.getOffsetEntitySlotId(PlayerInventory.MAIN_SIZE),
                204,
                6,
                PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE
        ));
        this.addSlot(new ArmorSlot(inv, inv.player,
                EquipmentSlot.CHEST,
                EquipmentSlot.CHEST.getOffsetEntitySlotId(PlayerInventory.MAIN_SIZE),
                204,
                6+18,
                PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE
        ));
        this.addSlot(new ArmorSlot(inv, inv.player,
                EquipmentSlot.LEGS,
                EquipmentSlot.LEGS.getOffsetEntitySlotId(PlayerInventory.MAIN_SIZE),
                204,
                6+18*2,
                PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE
        ));
        this.addSlot(new ArmorSlot(inv, inv.player,
                EquipmentSlot.FEET,
                EquipmentSlot.FEET.getOffsetEntitySlotId(PlayerInventory.MAIN_SIZE),
                204,
                6+18*3,
                PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE
        ));
        this.playerEquipmentSlotsEnd = this.slots.size();

        this.playerMainInvSlotsStart = this.slots.size();
        this.addPlayerSlots(inv, 8, 201);
        this.playerMainInvSlotsEnd = this.slots.size();

        this.playerOffHandSlot = this.slots.size();
        this.addSlot(new Slot(inv, PlayerInventory.OFF_HAND_SLOT, 204, 89){
            @Override
            public void setStack(ItemStack stack, ItemStack previousStack) {
                inv.player.onEquipStack(EquipmentSlot.OFFHAND, previousStack, stack);
                super.setStack(stack, previousStack);
            }

            @Override
            public Identifier getBackgroundSprite() {
                return PlayerScreenHandler.EMPTY_OFF_HAND_SLOT_TEXTURE;
            }
        });

        fakeInvStart = this.slots.size();

        if(system==null){
            this.fakeInventory = new RSInventory(storageHeight * storageWidth);
            fakeInventory.onOpen(inv.player);

            for (int y = 0; y < storageHeight; y++) {
                for (int x = 0; x < storageWidth; x++) {
                    this.addSlot(new RemoteStorageSlot(fakeInventory, x + y * storageWidth, 8 + x * 18, 20 + y * 18));
                }
            }
        }else{
            fakeInventory = null;
        }
    }

    protected static void updateResult(
            ScreenHandler handler,
            ServerWorld world,
            PlayerEntity player,
            RecipeInputInventory craftingInventory,
            CraftingResultInventory resultInventory,
            @Nullable RecipeEntry<CraftingRecipe> recipe
    ) {
        CraftingRecipeInput craftingRecipeInput = craftingInventory.createRecipeInput();
        ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)player;
        ItemStack itemStack = ItemStack.EMPTY;
        Optional<RecipeEntry<CraftingRecipe>> optional = world.getServer().getRecipeManager().getFirstMatch(RecipeType.CRAFTING, craftingRecipeInput, world, recipe);
        if (optional.isPresent()) {
            RecipeEntry<CraftingRecipe> recipeEntry = (RecipeEntry<CraftingRecipe>)optional.get();
            CraftingRecipe craftingRecipe = recipeEntry.value();
            if (resultInventory.shouldCraftRecipe(serverPlayerEntity, recipeEntry)) {
                ItemStack itemStack2 = craftingRecipe.craft(craftingRecipeInput, world.getRegistryManager());
                if (itemStack2.isItemEnabled(world.getEnabledFeatures())) {
                    itemStack = itemStack2;
                }
            }
        }

        resultInventory.setStack(0, itemStack);
        handler.setReceivedStack(0, itemStack);
        serverPlayerEntity.networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, itemStack));
    }

    private static int clampToMaxCount(int count, List<RegistryEntry<Item>> entries) {
        for (RegistryEntry<Item> registryEntry : entries) {
            count = Math.min(count, registryEntry.value().getMaxCount());
        }

        return count;
    }

    @Override
    public PostFillAction fillInputSlots(boolean craftAll, boolean creative, RecipeEntry<?> recipe, ServerWorld world, PlayerInventory inventory) {
        RecipeEntry<CraftingRecipe> recipeEntry = (RecipeEntry<CraftingRecipe>)recipe;
        this.onInputSlotFillStart();


        try{
            RecipeFinder recipeFinder = new RecipeFinder();
            player.getInventory().getMainStacks().forEach(recipeFinder::addInputIfUsable);
            currentMap.entrySet().stream().map(e -> e.getKey().withCount(e.getValue())).forEach(recipeFinder::addInputIfUsable);

            if(!recipeEntry.value().matches(this.craftingInventory.createRecipeInput(), getPlayer().getWorld())){
                craftingInventory.getHeldStacks().forEach(recipeFinder::addInputIfUsable);
                for(var slot : getInputSlots()){
                    insertIntoStorage(slot.getStack());
                    this.insertItem(slot.getStack(), playerMainInvSlotsStart, playerMainInvSlotsEnd, true);
                    if(!slot.getStack().isEmpty())
                        return PostFillAction.NOTHING;
                }
            }

            List<RegistryEntry<Item>> list = new ArrayList<>();
            var pre_count = recipeFinder.countCrafts(recipe.value(), null);
            if(!craftAll) pre_count = Math.min(1, pre_count);
            if(!recipeFinder.isCraftable(recipe.value(), pre_count, list::add)){
                return AbstractRecipeScreenHandler.PostFillAction.PLACE_GHOST_RECIPE;
            }
            var count = clampToMaxCount(pre_count, list);
            if(!recipeFinder.isCraftable(recipe.value(), count, list::add)){
                return AbstractRecipeScreenHandler.PostFillAction.PLACE_GHOST_RECIPE;
            }

            RecipeGridAligner.alignRecipeToGrid(
                    this.getWidth(), this.getHeight(), recipe.value(), recipe.value().getIngredientPlacement().getPlacementSlots(), (slotx, index, x, y) -> {
                        if (slotx == -1) return;

                        Slot slot = getInputSlots().get(index);
                        RegistryEntry<Item> entry = list.get(slotx);


                        var takeAmount = Math.min(count, slot.getStack().isEmpty()?entry.value().getMaxCount():slot.getStack().getMaxCount()-slot.getStack().getCount());
                        {
                            int slot_index;
                            while(takeAmount!=0&&(slot_index = player.getInventory().getMatchingSlot(entry, slot.getStack())) != -1){
                                var stack = player.getInventory().getStack(slot_index);
                                var amount = Math.min(takeAmount, stack.getCount());
                                slot.setStack(stack.copyWithCount(amount+slot.getStack().getCount()));
                                stack.setCount(stack.getCount()-amount);
                                takeAmount -= amount;
                            }
                        }
                        if(takeAmount>0){
                            var data = slot.getStack().isEmpty()?new ItemData(entry.value()):new ItemData(slot.getStack());
                            var got = getFromStorage(data, takeAmount);
                            got.increment(slot.getStack().getCount());
                            slot.setStack(got);
                        }
                    }
            );
            return PostFillAction.NOTHING;
        }finally {
            this.onInputSlotFillFinish(world, (RecipeEntry<CraftingRecipe>)recipe);
        }
    }

    @Override
    protected void onInputSlotFillStart() {
    }

    @Override
    public void onInputSlotFillFinish(ServerWorld world, RecipeEntry<CraftingRecipe> recipe) {
        updateResult(this, world, this.player, this.craftingInventory, this.craftingResultInventory, recipe);
    }

    @Override
    public void onContentChanged(Inventory inventory) {
        if(this.player==null)return;
        if (this.player.getWorld() instanceof ServerWorld serverWorld) {
            updateResult(this, serverWorld, this.player, this.craftingInventory, this.craftingResultInventory, null);
        }
    }

    @Override
    public void populateRecipeFinder(RecipeFinder finder) {
        super.populateRecipeFinder(finder);
        currentMap.entrySet().stream().map(e -> e.getKey().withCount(e.getValue())).forEach(finder::addInputIfUsable);
    }

    public void serverTick(){
        if(player instanceof ServerPlayerEntity p) {
            lastMap.clear();

            for (var member : system.unorderedMembers()) {
                if (member.pos.blockEntityAt(p.server) instanceof Inventory i) {
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
                ServerPlayNetworking.send(p, new RemoteStorageContentsDeltaS2C(syncId, storageRevision, difference));
        }
    }

    public void receiveContentsDelta(RemoteStorageContentsDeltaS2C contents){
        if(contents.revision()!=storageRevision){
            currentMap = contents.map();
            storageRevision = contents.revision();
            player.getInventory().markDirty();
        }else{
            contents.map().forEach(this::setLocalItemCount);
            if(!contents.map().isEmpty())
                player.getInventory().markDirty();
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
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
        amount = Math.min(amount, stackSize-getCursorStack().getCount());
        if(amount<=0)return;
        var got = getFromStorage(item, amount);
        if(got.getCount()!=amount)makeDirty(item);
        if(getCursorStack().isEmpty())
            setCursorStack(got);
        else
            getCursorStack().setCount(getCursorStack().getCount()+got.getCount());
    }

    void cursorIntoStorage(int amount){
        if(getCursorStack().isEmpty())return;

        amount = Math.min(getCursorStack().getCount(), amount);
        var old = getCursorStack().getCount();
        insertIntoStorage(getCursorStack(), amount);
        if(old-getCursorStack().getCount()!=amount)makeDirty(new ItemData(getCursorStack()));
    }

    void quickMoveOut(ItemData item, int amount){
        var canAccept = 0;
        for(var stack : this.slots.subList(playerMainInvSlotsStart, playerMainInvSlotsEnd)){
            if(!stack.canInsert(item.withCount(amount)))continue;
            if(stack.getStack().isEmpty()){
                canAccept += item.stackSize();
                continue;
            }
            if(!item.equals(stack.getStack()))continue;
            canAccept += stack.getStack().getMaxCount()-stack.getStack().getCount();
        }
        amount = Math.min(amount, canAccept);

        amount =  Math.min(amount, item.stackSize());
        var transferred = getFromStorage(item, amount);
        this.insertItem(transferred, playerMainInvSlotsStart, playerMainInvSlotsEnd, true);
        if(transferred.getCount()!=0){
            this.setCursorStack(transferred);
            makeDirty(item);
        }
    }

    private void makeDirty(ItemData item){
        if(player instanceof ServerPlayerEntity)
            currentMap.put(item, -1);//ensures this gets updated
    }

    private ItemStack getFromStorage(ItemData item, int amount){
        if(player instanceof ServerPlayerEntity p){
            return system.getFromStorage(p, item, amount);
        }else{
            amount = Math.min(amount, currentMap.getOrDefault(item, 0));
            modifyLocalItemCount(item, -amount);
            return item.withCount(amount);
        }
    }

    private void insertIntoStorage(ItemStack stack){
        insertIntoStorage(stack, stack.getCount());
    }

    private void insertIntoStorage(ItemStack stack, int amount){
        if(player instanceof ServerPlayerEntity p){
            system.insertIntoStorage(p, stack, amount);
        }else{
            modifyLocalItemCount(new ItemData(stack), amount);
            stack.setCount(stack.getCount()-amount); // we can't check if it will succeed on client so we assume it will work
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot_index) {
        Slot slot = this.slots.get(slot_index);
        if(!slot.hasStack()) return ItemStack.EMPTY;

        if(slot_index >= fakeInvStart){
            RemoteStorage.LOGGER.warn("This should not be ran");
            return ItemStack.EMPTY;
        }else{
            var stack = slot.getStack();
            var orig = slot.getStack().copy();

            if(slot_index==0){
                this.insertItem(stack, playerMainInvSlotsStart, playerMainInvSlotsEnd, true);
            }
            insertIntoStorage(stack);
            if(!stack.isEmpty()) {
                slot.markDirty();
                makeDirty(new ItemData(stack));
            }
            slot.onQuickTransfer(stack, orig);

            if (orig.getCount() == stack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, stack);
            if (slot_index == 0) {
                player.dropItem(stack, false);
                return orig;
            }
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.craftingResultInventory.clear();
        if (!player.getWorld().isClient) {
            for(var stack : this.craftingInventory.createRecipeInput().getStacks()){
                insertIntoStorage(stack);
            }
            this.dropInventory(player, this.craftingInventory);
        }
    }
}
