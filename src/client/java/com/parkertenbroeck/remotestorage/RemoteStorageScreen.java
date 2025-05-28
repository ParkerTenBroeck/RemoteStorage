package com.parkertenbroeck.remotestorage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parkertenbroeck.remotestorage.packets.c2s.RemoteStorageActionC2S;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class RemoteStorageScreen extends HandledScreen<RemoteStorageScreenHandler> {
    private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller_disabled");
    private static final Identifier INVENTORY_TEXTURE = Identifier.of(RemoteStorage.MOD_ID, "textures/gui/remote_storage.png");

    public RemoteStorageScreen(RemoteStorageScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 214;
        this.backgroundWidth = 195;

    }

    @Override
    protected void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType) {
        if(slot instanceof RemoteStorageScreenHandler.RemoteStorageSlot s){
            System.out.println(button + " " + actionType);
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
                case QUICK_MOVE -> quickMoveOut(slot);
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

    void quickMoveOut(Slot stack){
        var item = new ItemData(stack.getStack());
        handler.quickMoveOut(item, item.stackSize());
        ClientPlayNetworking.send(RemoteStorageActionC2S.quickMoveOut(handler, item));
    }

    void storageIntoCursor(Slot stack){
        var item = new ItemData(stack.getStack());
        var amount = Math.min((stack.getStack().getCount()+1)/2, (stack.getStack().getMaxCount()+1)/2);
        handler.storageIntoCursor(item, amount);
        ClientPlayNetworking.send(RemoteStorageActionC2S.storageIntoCursor(handler, item, amount));
    }

    void storageIntoCursorHalf(Slot stack){
        var item = new ItemData(stack.getStack());
        var amount = Math.min((stack.getStack().getCount()+1)/2, (stack.getStack().getMaxCount()+1)/2);
        handler.storageIntoCursor(item, amount);
        ClientPlayNetworking.send(RemoteStorageActionC2S.storageIntoCursor(handler, item, amount));
    }

    void cursorIntoStorage(Slot stack, int amount){
        var item = new ItemData(stack.getStack());
        handler.cursorIntoStorage(amount);
        ClientPlayNetworking.send(RemoteStorageActionC2S.cursorIntoStorage(handler, item, amount));
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        renderBackground(drawContext, mouseX, mouseY, delta);
        super.render(drawContext, mouseX, mouseY, delta);
        drawMouseoverTooltip(drawContext, mouseX, mouseY);
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawContext.drawTexture(RenderLayer::getGuiTextured, INVENTORY_TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
    }
}
