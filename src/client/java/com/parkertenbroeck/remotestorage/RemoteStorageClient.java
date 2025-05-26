package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.AddToRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.OpenRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;


@Environment(EnvType.CLIENT)
public class RemoteStorageClient implements ClientModInitializer {

	private static KeyBinding openRemoteStorage;
	private static KeyBinding addToRemoteStorage;

	@Override
	public void onInitializeClient() {
		openRemoteStorage = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key."+RemoteStorage.MOD_ID+".open_inv",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_X,
				"category." + RemoteStorage.MOD_ID
		));
		addToRemoteStorage = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key."+RemoteStorage.MOD_ID+".add_inv",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				"category." + RemoteStorage.MOD_ID
		));

		HandledScreens.register(RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, RemoteStorageScreen::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(openRemoteStorage.wasPressed()){
				ClientPlayNetworking.send(new OpenRemoteStorageC2S());
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(addToRemoteStorage.wasPressed()){
                if (client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
					ClientPlayNetworking.send(new AddToRemoteStorageC2S(((BlockHitResult)client.crosshairTarget).getBlockPos()));
                }
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(RemoteStorageContentsS2C.ID, (payload, context) -> {
			RemoteStorage.LOGGER.info(payload.toString());
		});
	}
}