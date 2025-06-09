package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.*;
import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemPositionsS2C;
import com.parkertenbroeck.remotestorage.system.Position;
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
import net.minecraft.inventory.Inventory;
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
		PayloadTypeRegistry.playS2C().register(StorageSystemPositionsS2C.ID, StorageSystemPositionsS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(OpenRemoteStorageC2S.ID, OpenRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AddToRemoteStorageC2S.ID, AddToRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(LinkRemoteStorageMemberC2S.ID, LinkRemoteStorageMemberC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoteStorageActionC2S.ID, RemoteStorageActionC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(RemoveFromRemoteStorageC2S.ID, RemoveFromRemoteStorageC2S.CODEC);

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
			sender.sendPacket(RemoteStorageSavedState.get(handler.player).getPositions());
		});

		ServerPlayNetworking.registerGlobalReceiver(OpenRemoteStorageC2S.ID, (payload, context) -> {
			openRemoteStorageScreen(context.player());
		});

		ServerPlayNetworking.registerGlobalReceiver(LinkRemoteStorageMemberC2S.ID, (payload, context) -> {
			var child = Position.of(context.player(), payload.child());
			if(payload.parent().isEmpty()){
				if(RemoteStorageSavedState.unlinkMember(context.player(), child)){
					RemoteStorageSavedState.syncWithPlayer(context.player());
				}
				return;
			}
			var parent = Position.of(context.player(), payload.parent().get());
			switch(RemoteStorageSavedState.linkMember(context.player(), child, parent)){
                case Success ->
						RemoteStorageSavedState.syncWithPlayer(context.player());
                case CannotLinkToSelf ->
						context.player().sendMessage(Text.of("Cannot link to self"));
                case ChildIsNotMember ->
						context.player().sendMessage(Text.of("Child is not a member of the system"));
                case ParentIsNotMember ->
						context.player().sendMessage(Text.of("Parent is not a member of the system"));
            }
		});

		ServerPlayNetworking.registerGlobalReceiver(RemoveFromRemoteStorageC2S.ID, (payload, context) -> {
			if(RemoteStorageSavedState.removeMember(context.player(), Position.of(context.player(), payload.blockPos()))){
				RemoteStorageSavedState.syncWithPlayer(context.player());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(AddToRemoteStorageC2S.ID, (payload, context) -> {
			var entity = context.player().getServerWorld().getBlockEntity(payload.blockPos());
			if(entity instanceof Inventory){
				if(RemoteStorageSavedState.addMember(context.player(), Position.of(context.player(), payload.blockPos()))) {
					context.player().sendMessage(Text.of("Target block " + payload.blockPos().toShortString() + " added to system"));
					RemoteStorageSavedState.syncWithPlayer(context.player());
				}
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
}