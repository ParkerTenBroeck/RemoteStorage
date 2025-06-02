package com.parkertenbroeck.remotestorage.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.parkertenbroeck.remotestorage.RemoteStorageScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.impl.screenhandler.Networking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Networking.class)
public class ScreenHandlerNetworkingMixin {

    @Inject(
            method="sendOpenPacket",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void sendOpenPacketInjectedHead(CallbackInfo info, @Local ServerPlayerEntity player, @Local ExtendedScreenHandlerFactory<?> factory, @Local ScreenHandler handler){
        if(handler instanceof RemoteStorageScreenHandler) {
            ServerPlayNetworking.send(player, (CustomPayload) factory.getScreenOpeningData(player));
            info.cancel();
        }
    }
}