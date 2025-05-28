package com.parkertenbroeck.remotestorage;

import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;


/**
 * An item + its data but without having a count
 */
public record ItemData(ItemStack item) {
    public static final PacketCodec<RegistryByteBuf, ItemData> ITEM_DATA_PACKET_CODEC = new PacketCodec<>() {
        public ItemData decode(RegistryByteBuf buf) {
            return new ItemData(ItemStack.OPTIONAL_PACKET_CODEC.decode(buf));
        }

        public void encode(RegistryByteBuf buf, ItemData data) {
            ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, data.item);
        }
    };

    public ItemData(ItemStack item){
        this.item = item.copyWithCount(1);
    }

    public ItemStack withCount(int count){
        return item.copyWithCount(count);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ItemStack stack)
            return equals(this.item, stack);
        if(obj instanceof ItemData data)
            return equals(this.item, data.item);
        return false;
    }

    @Override
    public int hashCode() {
        return ItemStack.hashCode(item);
    }

    public static boolean equals(ItemStack s1, ItemStack s2){
        return ItemStack.areItemsAndComponentsEqual(s1, s2);
    }

    public int stackSize() {
        return item.getMaxCount();
    }
}
