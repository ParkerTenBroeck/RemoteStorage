package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.AddToRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.OpenRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsS2C;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RemoteStorage implements ModInitializer {
	public static final String MOD_ID = "remote_storage";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ScreenHandlerType<RemoteStorageScreenHandler> REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE = new ExtendedScreenHandlerType<>(RemoteStorageScreenHandler::new, RemoteStorageContentsS2C.CODEC);

	public final StorageSystem system = new StorageSystem();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(RemoteStorageContentsS2C.ID, RemoteStorageContentsS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenRemoteStorageC2S.ID, OpenRemoteStorageC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AddToRemoteStorageC2S.ID, AddToRemoteStorageC2S.CODEC);

		Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MOD_ID, "remote_storage"), REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE);

		ServerLifecycleEvents.SERVER_STARTED.register(ms -> {
			system.clear();
		});
//		UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
//			var result = world.getBlockEntity(blockHitResult.getBlockPos());
//			if(result instanceof Inventory i){
//				var item = Registries.ITEM.get((int) (Registries.ITEM.size() * Math.random()));
//				i.setStack(0, new ItemStack(item, (int) (item.getMaxCount() * Math.random())));
//				i.markDirty();
//				LOGGER.info(i + " ");
//			}
//
//			if(result!=null) LOGGER.info(result.toString() + world.isClient());
//			return ActionResult.PASS;
//		});

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
					public RemoteStorageContentsS2C getScreenOpeningData(ServerPlayerEntity player) {
						return new RemoteStorageContentsS2C(List.of());
					}
				}
			);

		});
		ServerPlayNetworking.registerGlobalReceiver(AddToRemoteStorageC2S.ID, (payload, context) -> {
			LOGGER.info(payload.toString());
			var entity = context.player().getServerWorld().getBlockEntity(payload.blockPos());
			if(entity instanceof Inventory){
				system.add_default(payload.blockPos(), context.player().getServerWorld().getRegistryKey().getValue());
			}else{
				context.player().sendMessage(Text.of("Target block " + payload.blockPos().toShortString() + " is not a valid inventory"));
			}
		});
	}
}