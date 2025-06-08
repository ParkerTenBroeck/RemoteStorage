package com.parkertenbroeck.remotestorage.packets.s2c;

import com.parkertenbroeck.remotestorage.ItemData;
import com.parkertenbroeck.remotestorage.Utils;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.HashMap;
import java.util.Map;

public record RemoteStorageContentsDeltaS2C(int syncId, int revision, Map<ItemData, Integer> map) implements CustomPayload {
    public static final CustomPayload.Id<RemoteStorageContentsDeltaS2C> ID = Utils.createId(RemoteStorageContentsDeltaS2C.class);
    public static final PacketCodec<RegistryByteBuf, Map<ItemData, Integer>> ITEM_DATA_COUNT_MAP_PACKET = PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.size());
                value.forEach((data, count) -> {
                    ItemData.ITEM_DATA_PACKET_CODEC.encode(buf, data);
                    buf.writeInt(count);
                });
            },
            buf -> {
                var length = buf.readInt();
                var map = new HashMap<ItemData, Integer>(length);
                for(int i = 0; i < length; i ++){
                    map.put(ItemData.ITEM_DATA_PACKET_CODEC.decode(buf), buf.readInt());
                }
                return map;
            }
    );

    public static final PacketCodec<RegistryByteBuf, RemoteStorageContentsDeltaS2C> CODEC = PacketCodec.tuple(
            PacketCodecs.SYNC_ID,
            RemoteStorageContentsDeltaS2C::syncId,
            PacketCodecs.VAR_INT,
            RemoteStorageContentsDeltaS2C::revision,
            ITEM_DATA_COUNT_MAP_PACKET,
            RemoteStorageContentsDeltaS2C::map,
            RemoteStorageContentsDeltaS2C::new
    );

    @Override
    public Id<RemoteStorageContentsDeltaS2C> getId() {
        return ID;
    }
}
