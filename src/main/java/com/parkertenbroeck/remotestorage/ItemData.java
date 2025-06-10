package com.parkertenbroeck.remotestorage;

import com.mojang.serialization.Codec;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;


/**
 * An item + its data but without having a count
 */
public record ItemData(ItemStack item) {
    public static final PacketCodec<RegistryByteBuf, ItemData> PACKET_CODEC = new PacketCodec<>() {
        public ItemData decode(RegistryByteBuf buf) {
            return new ItemData(ItemStack.OPTIONAL_PACKET_CODEC.decode(buf));
        }

        public void encode(RegistryByteBuf buf, ItemData data) {
            ItemStack.OPTIONAL_PACKET_CODEC.encode(buf, data.item);
        }
    };
    public static final Codec<ItemData> CODEC = ItemStack.CODEC.xmap(ItemData::new, ItemData::item);

    public ItemData(Item item){
        this(new ItemStack(item));
    }

    public ItemData(ItemStack item){
        this.item = item.copyWithCount(1);
    }

    public ItemStack withCount(int count){
        return item.copyWithCount(count);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ItemStack stack) return this.equals(stack);
        if(obj instanceof ItemData data) return this.equals(data.item);
        return false;
    }

    public boolean equals(ItemData data){
        return equals(this.item, data.item);
    }

    public boolean equals(ItemStack stack){
        return equals(this.item, stack);
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
