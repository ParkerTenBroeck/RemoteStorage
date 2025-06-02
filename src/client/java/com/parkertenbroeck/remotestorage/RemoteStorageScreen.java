package com.parkertenbroeck.remotestorage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parkertenbroeck.remotestorage.packets.c2s.RemoteStorageActionC2S;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class RemoteStorageScreen extends HandledScreen<RemoteStorageScreenHandler> {
    private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller_disabled");
    private static final Identifier INVENTORY_TEXTURE = Identifier.of(RemoteStorage.MOD_ID, "textures/gui/remote_storage.png");

    private TextFieldWidget searchField;

    public RemoteStorageScreen(RemoteStorageScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 214;
        this.backgroundWidth = 195;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;

        this.searchField = new TextFieldWidget(this.textRenderer, this.x+82, this.y+6, 80, 9, Text.of("meow!"));
        this.searchField.setMaxLength(50);
        this.searchField.setDrawsBackground(false);
        this.searchField.setVisible(false);
        this.searchField.setEditableColor(16777215);
        this.addSelectableChild(this.searchField);

        this.searchField.setVisible(true);
        this.searchField.setFocusUnlocked(false);
        this.searchField.setFocused(true);
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        renderBackground(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
        searchField.render(drawContext, mouseX, mouseY, delta);
        drawMouseoverTooltip(drawContext, mouseX, mouseY);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.searchField.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(!this.searchField.keyPressed(keyCode, scanCode, modifiers)){
            return this.searchField.isFocused() && this.searchField.isVisible() && keyCode != GLFW.GLFW_KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return true;
    }

    @Override
    protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawContext.drawTexture(RenderLayer::getGuiTextured, INVENTORY_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
        searchField.drawsBackground();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

        System.out.println(horizontalAmount + " "+  verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        System.out.println(slot + " " + button + " " + actionType);
        if(slot instanceof RemoteStorageScreenHandler.RemoteStorageSlot s){
            switch(actionType){
                case PICKUP -> {
                    if(button==0){
                        if(handler.getCursorStack().isEmpty())storageIntoCursor(slot);
                        else cursorIntoStorage(slot, handler.getCursorStack().getCount());
                    }else if(button==1){
                        if(handler.getCursorStack().isEmpty())storageIntoCursorHalf(slot);
                        else cursorIntoStorage(slot, 1);
                    }
                }
                case QUICK_MOVE -> quickMoveOutStack(new ItemData(slot.getStack()));
                case SWAP -> {}
                case CLONE -> {}
                case THROW -> {}
                case QUICK_CRAFT -> {}
                case PICKUP_ALL -> {}
            }
            return;
        }
        super.onMouseClick(slot, slotId, button, actionType);
    }

    void doAction(RemoteStorageActionC2S payload){
        handler.acceptAction(payload, this.client.player);
        ClientPlayNetworking.send(payload);
    }

    @Override
    protected void handledScreenTick() {
        var nameFilters = new ArrayList<String>();
        var tagFilters = new ArrayList<String>();
        var modFilters = new ArrayList<String>();
        for(var entry : searchField.getText().split(" ")){
            if(entry.isEmpty())continue;
            if(entry.startsWith("@"))modFilters.add(entry.substring(1).toLowerCase(Locale.ROOT));
            else if(entry.startsWith("#"))tagFilters.add(entry.substring(1).toLowerCase(Locale.ROOT));
            else nameFilters.add(entry.toLowerCase(Locale.ROOT));
        }
        var stream = handler.currentMap.entrySet()
                .stream()
                .map(entry -> entry.getKey().withCount(entry.getValue()))
                .filter(i -> nameFilters.stream().allMatch(name ->
                        i.getName().getString().toLowerCase(Locale.ROOT).contains(name)
                        || i.getComponents().stream().anyMatch(c ->
                                c.value().toString().toLowerCase(Locale.ROOT).contains(name))
                        )
                )
                .filter(i -> tagFilters.stream().allMatch(tag ->
                        i.streamTags().anyMatch(t ->
                                t.getName().getString().toLowerCase(Locale.ROOT).startsWith(tag)
                        )
                ))
                .filter(i -> modFilters.stream().allMatch(mod ->
                        i.getRegistryEntry().getKey().map(k ->
                                k.getValue().getNamespace().toLowerCase(Locale.ROOT).contains(mod)).orElse(false)
                        )
                );
        stream = stream.sorted(Comparator.comparingInt(ItemStack::getCount).reversed());

        var sorted = stream.toList();
        int offset = 0;
        for(int i = 0; i < handler.fakeInventory.size(); i ++){
            var slot = handler.getSlot(i+handler.playerInventorySize);
            if(i+offset<sorted.size())
                slot.setStack(sorted.get(i+offset));
            else
                slot.setStack(ItemStack.EMPTY);
        }
        super.handledScreenTick();
    }

    void quickMoveOutStack(ItemData item){
        if(item.withCount(1).isEmpty())return;
        doAction(RemoteStorageActionC2S.quickMoveOut(handler, item, item.stackSize()));
    }

    void quickMoveOutOne(ItemData item){
        if(item.withCount(1).isEmpty())return;
        doAction(RemoteStorageActionC2S.quickMoveOut(handler, item, 1));
    }

    void storageIntoCursor(Slot stack){
        var item = new ItemData(stack.getStack());
        var amount = Math.min(stack.getStack().getCount(), stack.getStack().getMaxCount());
        doAction(RemoteStorageActionC2S.storageIntoCursor(handler, item, amount));
    }

    void storageIntoCursorHalf(Slot stack){
        var item = new ItemData(stack.getStack());
        var amount = Math.min((stack.getStack().getCount()+1)/2, (stack.getStack().getMaxCount()+1)/2);
        doAction(RemoteStorageActionC2S.storageIntoCursor(handler, item, amount));
    }

    void cursorIntoStorage(Slot stack, int amount){
        var item = new ItemData(handler.getCursorStack());
        doAction(RemoteStorageActionC2S.cursorIntoStorage(handler, item, amount));
    }
}
