package com.parkertenbroeck.remotestorage.packets.c2s;

import com.parkertenbroeck.remotestorage.ItemData;
import com.parkertenbroeck.remotestorage.RemoteStorageScreenHandler;
import com.parkertenbroeck.remotestorage.packets.NetworkingUtils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RemoteStorageActionC2S(ItemData item, int syncId, int revision, boolean isTakingFromStorage, byte kind, int amount, int slot) implements CustomPayload{
    public static final CustomPayload.Id<RemoteStorageActionC2S> ID = NetworkingUtils.createId(RemoteStorageActionC2S.class);
    public static final PacketCodec<RegistryByteBuf, RemoteStorageActionC2S> CODEC = PacketCodec.of(
        (value, buf) -> {
            ItemData.ITEM_DATA_PACKET_CODEC.encode(buf, value.item);
            PacketCodecs.SYNC_ID.encode(buf, value.syncId);
            buf.writeVarInt(value.revision)
                    .writeByte((value.isTakingFromStorage ?0b10000000:0)+value.kind)
                    .writeVarInt(value.amount)
                    .writeVarInt(value.amount);
        }, buf -> {
            var item = ItemData.ITEM_DATA_PACKET_CODEC.decode(buf);
            var syncId = PacketCodecs.SYNC_ID.decode(buf);
            var revision = buf.readVarInt();
            var rk = buf.readByte();
            var remove = (rk&0b10000000)!=0;
            var kind = rk&~0b10000000;
            var amount = buf.readVarInt();
            var slot = buf.readVarInt();
            return new RemoteStorageActionC2S(item, syncId, revision, remove, (byte) kind, amount, slot);
        }
    );

    public static RemoteStorageActionC2S quickMoveOut(RemoteStorageScreenHandler handler, ItemData item, int amount){
        return new RemoteStorageActionC2S(item, handler.syncId, handler.storageRevision, true, (byte)1, amount, 0);
    }

    public static RemoteStorageActionC2S storageIntoCursor(RemoteStorageScreenHandler handler, ItemData item, int amount){
        return new RemoteStorageActionC2S(item, handler.syncId, handler.storageRevision, true, (byte)0, amount, 0);
    }

    public static RemoteStorageActionC2S cursorIntoStorage(RemoteStorageScreenHandler handler, ItemData item, int amount){
        return new RemoteStorageActionC2S(item, handler.syncId, handler.storageRevision, false, (byte)0, amount, 0);
    }

    public boolean takingFromStorage(){
        return isTakingFromStorage;
    }

    public boolean puttingIntoStorage(){
        return !isTakingFromStorage;
    }

    @Override
    public CustomPayload.Id<RemoteStorageActionC2S> getId() {
        return ID;
    }
}
