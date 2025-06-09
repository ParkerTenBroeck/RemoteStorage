package com.parkertenbroeck.remotestorage.system;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record Position(BlockPos pos, Identifier world) {
    public static final PacketCodec<RegistryByteBuf, Position> PACKET_CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, Position::pos,
            Identifier.PACKET_CODEC, Position::world,
            Position::new
    );

    public BlockEntity blockEntityAt(MinecraftServer server) {
        var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, this.world));
        if (world == null) return null;
        return world.getBlockEntity(this.pos);
    }

    public static Position of(PlayerEntity player, BlockPos pos) {
        return new Position(pos, player.getWorld().getRegistryKey().getValue());
    }
}
