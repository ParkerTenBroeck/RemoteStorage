package com.parkertenbroeck.remotestorage;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.parkertenbroeck.remotestorage.packets.c2s.AddToRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.OpenRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.impl.renderer.RendererManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.GLFW;


@Environment(EnvType.CLIENT)
public class RemoteStorageClient implements ClientModInitializer {

	private static KeyBinding openRemoteStorage;
	private static KeyBinding addToRemoteStorage;

	private static GpuBuffer vertexBuffer;
	private static int indexCount = 0;
	private static final RenderSystem.ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.LINES);

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

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			OutlineRenderer.render(context);
		});

		ClientPlayNetworking.registerGlobalReceiver(RemoteStorageContentsDeltaS2C.ID, (payload, context) -> {
			if(context.player().currentScreenHandler.syncId == payload.syncId()){
				if(context.player().currentScreenHandler instanceof RemoteStorageScreenHandler s){
					s.receiveContentsDelta(payload);
				}
			}else{
				RemoteStorage.LOGGER.warn("Received remote storage contents packet that doesn't match sync ID");
			}
		});
	}
}