package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.AddToRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.OpenRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.RemoteStorageActionC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RemoteStorage implements ModInitializer {
	public static final String MOD_ID = "remote_storage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ScreenHandlerType<RemoteStorageScreenHandler> REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE = new ExtendedScreenHandlerType<>(RemoteStorageScreenHandler::new, OpenRemoteStorageS2C.CODEC);

	public final StorageSystem system = new StorageSystem();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(RemoteStorageContentsDeltaS2C.ID, RemoteStorageContentsDeltaS2C.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenRemoteStorageS2C.ID, OpenRemoteStorageS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(OpenRemoteStorageC2S.ID, OpenRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AddToRemoteStorageC2S.ID, AddToRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoteStorageActionC2S.ID, RemoteStorageActionC2S.CODEC);

		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "remote_storage"), REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE);

		ServerLifecycleEvents.SERVER_STARTED.register(ms -> {
			system.clear();
		});

		ServerPlayNetworking.registerGlobalReceiver(OpenRemoteStorageC2S.ID, (payload, context) -> {
			context.player().openHandledScreen(
				new ExtendedScreenHandlerFactory<>(){
					@Override
					public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
						return new RemoteStorageScreenHandler(syncId, playerInventory, system, player);
					}

					@Override
					public Text getDisplayName() {
						return Text.empty();
					}

					@Override
					public OpenRemoteStorageS2C getScreenOpeningData(ServerPlayerEntity player) {
						return new OpenRemoteStorageS2C();
					}
				}
			);

		});
		ServerPlayNetworking.registerGlobalReceiver(AddToRemoteStorageC2S.ID, (payload, context) -> {
			var entity = context.player().getServerWorld().getBlockEntity(payload.blockPos());
			if(entity instanceof Inventory){
				system.add_default(payload.blockPos(), context.player().getServerWorld().getRegistryKey().getValue());
			}else{
				context.player().sendMessage(Text.of("Target block " + payload.blockPos().toShortString() + " is not a valid inventory"));
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(RemoteStorageActionC2S.ID, (payload, context) -> {
			if(context.player().currentScreenHandler.syncId == payload.syncId()){
				if(context.player().currentScreenHandler instanceof RemoteStorageScreenHandler s){
					s.acceptAction(payload, context.player());
				}
			}else{
				RemoteStorage.LOGGER.warn("Received remote storage contents packet that doesn't match sync ID");
			}
		});
	}
}