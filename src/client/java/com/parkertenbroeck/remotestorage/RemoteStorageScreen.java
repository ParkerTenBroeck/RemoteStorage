package com.parkertenbroeck.remotestorage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.parkertenbroeck.remotestorage.packets.c2s.RemoteStorageActionC2S;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.AbstractCraftingRecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.display.RecipeDisplay;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public class RemoteStorageScreen extends HandledScreen<RemoteStorageScreenHandler> implements RecipeBookProvider {
    private static final Identifier SCROLLER_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller");
    private static final Identifier SCROLLER_DISABLED_TEXTURE = Identifier.ofVanilla("container/creative_inventory/scroller_disabled");
    private static final Identifier INVENTORY_TEXTURE = Identifier.of(RemoteStorage.MOD_ID, "textures/gui/remote_storage.png");

    private final RecipeBookWidget<?> recipeBook;
    private TextFieldWidget searchField;

    private final int tw = 227;
    private final int th = 283;

    public RemoteStorageScreen(RemoteStorageScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        recipeBook = new AbstractCraftingRecipeBookWidget(handler);
        this.backgroundWidth = tw;
        this.backgroundHeight = th;
    }

    private void addRecipeBook() {
        ScreenPos screenPos = this.getRecipeBookButtonPos();
        this.addDrawableChild(new TexturedButtonWidget(screenPos.x(), screenPos.y(), 20, 18, RecipeBookWidget.BUTTON_TEXTURES, button -> {
            this.recipeBook.toggleOpen();
            this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
            ScreenPos screenPosx = this.getRecipeBookButtonPos();
            button.setPosition(screenPosx.x(), screenPosx.y());
            this.onRecipeBookToggled();
        }));
        this.addSelectableChild(this.recipeBook);
    }

    protected ScreenPos getRecipeBookButtonPos(){
        return new ScreenPos(this.x+this.backgroundWidth/2-16, this.y+this.backgroundHeight/2+30);
    }

    protected void onRecipeBookToggled() {
        searchField.setPosition(this.x+80, this.y+6);
    }


    @Override
    protected void init() {
        super.init();

        this.recipeBook.initialize(this.width-50, this.height+58*2, this.client, false);
        this.x = this.recipeBook.findLeftEdge(this.width, this.backgroundWidth);
        this.addRecipeBook();

        // Center the title
        titleX = 6;
        titleY = 6;


        // Center inventory text
        playerInventoryTitleX = 6;
        playerInventoryTitleY = 190;

        this.searchField = new TextFieldWidget(this.textRenderer, this.x+80, this.y+6, 80, 9, Text.of("meow!"));
        this.searchField.setMaxLength(50);
        this.searchField.setDrawsBackground(false);
        this.searchField.setVisible(false);
        this.searchField.setEditableColor(16777215);
        this.addDrawableChild(this.searchField);

        this.searchField.setVisible(true);
        setFocused(this.searchField);
    }

    @Override
    protected void drawSlots(DrawContext context) {
        super.drawSlots(context);
        this.recipeBook.drawGhostSlots(context, true);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        searchField.render(context, mouseX, mouseY, delta);
        this.recipeBook.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        this.recipeBook.drawTooltip(context, mouseX, mouseY, this.focusedSlot);
    }



    @Override
    public void refreshRecipeBook() {
        this.recipeBook.refresh();
    }

    @Override
    public void onCraftFailed(RecipeDisplay display) {
        this.recipeBook.onCraftFailed(display);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(this.searchField.isFocused() && this.searchField.charTyped(chr, modifiers)){
            return true;
        }else if(this.recipeBook.isFocused() && this.recipeBook.charTyped(chr, modifiers)){
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == GLFW.GLFW_KEY_ESCAPE){
            return super.keyPressed(keyCode, scanCode, modifiers);
        }else if(this.searchField.isFocused()){
            return this.searchField.keyPressed(keyCode, scanCode, modifiers);
        }else if(this.recipeBook.isFocused()){
            return this.recipeBook.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void drawBackground(DrawContext drawContext, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawContext.drawTexture(RenderLayer::getGuiTextured, INVENTORY_TEXTURE, this.x, this.y, 0, 0, backgroundWidth, backgroundHeight, tw, th);
        searchField.drawsBackground();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {

        System.out.println(horizontalAmount + " "+  verticalAmount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.recipeBook.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.recipeBook);
            return true;
        } else if(this.searchField.mouseClicked(mouseX, mouseY, button)) {
            this.setFocused(this.searchField);
            return true;
        }else{
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        boolean bl = mouseX < left || mouseY < top || mouseX >= left + this.backgroundWidth || mouseY >= top + this.backgroundHeight;
        return this.recipeBook.isClickOutsideBounds(mouseX, mouseY, this.x, this.y, this.backgroundWidth, this.backgroundHeight, button) && bl;
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
        this.recipeBook.onMouseClick(slot);
    }

    void doAction(RemoteStorageActionC2S payload){
        handler.acceptAction(payload, this.client.player);
        ClientPlayNetworking.send(payload);
    }



    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        this.recipeBook.update();

        var nameFilters = new ArrayList<String>();
        var tagFilters = new ArrayList<String>();
        var modFilters = new ArrayList<String>();
        for(var entry : searchField.getText().split(" ")){
            if(entry.isEmpty())continue;
            if(entry.startsWith("@"))modFilters.add(entry.substring(1).toLowerCase(Locale.ROOT));
            else if(entry.startsWith("#"))tagFilters.add(entry.substring(1).toLowerCase(Locale.ROOT));
            else nameFilters.add(entry.toLowerCase(Locale.ROOT));
        }
        var sorted = handler.currentMap.entrySet()
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
                ).sorted(Comparator.comparingInt(ItemStack::getCount).reversed())
                .toList();

        int offset = 0;
        for(int i = 0; i < handler.fakeInventory.size(); i ++){
            var slot = handler.getSlot(i+handler.fakeInvStart);
            if(i+offset<sorted.size())
                slot.setStack(sorted.get(i+offset));
            else
                slot.setStack(ItemStack.EMPTY);
        }
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
