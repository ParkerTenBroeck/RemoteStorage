package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.*;
import com.parkertenbroeck.remotestorage.packets.c2s.edit.*;
import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemResyncS2C;
import com.parkertenbroeck.remotestorage.system.RemoteStorageSavedState;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RemoteStorage implements ModInitializer {
	public static final String MOD_ID = "remote_storage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ScreenHandlerType<RemoteStorageScreenHandler> REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE = new ExtendedScreenHandlerType<>(RemoteStorageScreenHandler::new, OpenRemoteStorageS2C.CODEC);


	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(RemoteStorageContentsDeltaS2C.ID, RemoteStorageContentsDeltaS2C.CODEC);
		PayloadTypeRegistry.playS2C().register(OpenRemoteStorageS2C.ID, OpenRemoteStorageS2C.CODEC);
		PayloadTypeRegistry.playS2C().register(StorageSystemResyncS2C.ID, StorageSystemResyncS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(OpenRemoteStorageC2S.ID, OpenRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AddToRemoteStorageC2S.ID, AddToRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(LinkRemoteStorageMemberC2S.ID, LinkRemoteStorageMemberC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoteStorageActionC2S.ID, RemoteStorageActionC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoveFromRemoteStorageC2S.ID, RemoveFromRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateGroupRemoteStorageC2S.ID, UpdateGroupRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SetGroupRemoteStorageC2S.ID, SetGroupRemoteStorageC2S.CODEC);

//      breaks when vanilla clients or clients without this mod join so we send our own packet
//		Registry.register(Registries.SCREEN_HANDLER, NetworkingUtils.createIdentifier(RemoteStorageScreenHandler.class), REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE);

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for(var player : server.getPlayerManager().getPlayerList()){
				if(player.currentScreenHandler instanceof RemoteStorageScreenHandler s){
					s.serverTick();
				}
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			RemoteStorageSavedState.resyncWithPlayer(handler.player);
		});

		ServerPlayNetworking.registerGlobalReceiver(OpenRemoteStorageC2S.ID, (payload, context) -> {
			openRemoteStorageScreen(context.player());
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

		systemEditPackets();
	}

	public void openRemoteStorageScreen(ServerPlayerEntity player){
		var system = RemoteStorageSavedState.get(player);
		player.openHandledScreen(
				new ExtendedScreenHandlerFactory<>(){
					OpenRemoteStorageS2C packet;
					@Override
					public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity p) {
						packet = new OpenRemoteStorageS2C(syncId, getDisplayName());
						return new RemoteStorageScreenHandler(syncId, playerInventory, system);
					}

					@Override
					public Text getDisplayName() {
						return Text.of(system.name);
					}

					@Override
					public OpenRemoteStorageS2C getScreenOpeningData(ServerPlayerEntity player) {
						return packet;
					}
				}
		);
	}

	private void systemEditPackets(){
		ServerPlayNetworking.registerGlobalReceiver(LinkRemoteStorageMemberC2S.ID, (payload, context) -> {
			if(payload.parent().isEmpty()){
				if(!RemoteStorageSavedState.get(context.player()).unlink(payload.child())){
					RemoteStorageSavedState.resyncWithPlayer(context.player());
				}
			}else if(!RemoteStorageSavedState.get(context.player()).link(payload.child(), payload.parent().get()).modified){
				RemoteStorageSavedState.resyncWithPlayer(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(RemoveFromRemoteStorageC2S.ID, (payload, context) -> {
			if(!RemoteStorageSavedState.get(context.player()).remove(payload.pos())){
				RemoteStorageSavedState.resyncWithPlayer(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(AddToRemoteStorageC2S.ID, (payload, context) -> {
			if(!RemoteStorageSavedState.get(context.player()).add(payload.pos(), context.player().getWorld())) {
				RemoteStorageSavedState.resyncWithPlayer(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(UpdateGroupRemoteStorageC2S.ID, (payload, context) -> {
			RemoteStorageSavedState.get(context.player()).updateGroup(payload.group());
		});

		ServerPlayNetworking.registerGlobalReceiver(SetGroupRemoteStorageC2S.ID, (payload, context) -> {
			RemoteStorageSavedState.get(context.player()).setGroup(payload.memberPos(), payload.groupId());
		});
	}
}