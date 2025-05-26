package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.packets.NetworkingConstants;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record RemoteStorageContentsS2C(List<ItemStack> items) implements CustomPayload, Inventory {
    public static final CustomPayload.Id<RemoteStorageContentsS2C> ID = new CustomPayload.Id<>(NetworkingConstants.REMOTE_STORAGE_CONTENTS_ID);
    public static final PacketCodec<RegistryByteBuf, RemoteStorageContentsS2C> CODEC = PacketCodec.tuple(
            ItemStack.OPTIONAL_LIST_PACKET_CODEC,
            RemoteStorageContentsS2C::items,
            RemoteStorageContentsS2C::new
    );

    @Override
    public Id<RemoteStorageContentsS2C> getId() {
        return ID;
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getStack(int slot) {
        if(slot>=items.size())return ItemStack.EMPTY;
        return items.get(slot).copy();
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if(slot>=items.size())return ItemStack.EMPTY;
        var item = items.get(slot);
        int amm = Math.min(Math.min(item.getMaxCount(), amount), item.getCount());
        var ret = new ItemStack(item.getItem(), amm);
        item.setCount(item.getCount()-amm);
        if(item.isEmpty())items.remove(slot);
        return ret;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if(slot>=items.size())return ItemStack.EMPTY;
        return removeStack(slot, items.get(slot).getMaxCount());
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if(slot>=items.size())return;
        items.set(slot, stack.copy());
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        items.clear();
    }
}
