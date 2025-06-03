package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.AddToRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.LinkRemoteStorageMemberC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.OpenRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.c2s.RemoveFromRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemPositionsS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

import static com.parkertenbroeck.remotestorage.RemoteStorage.MOD_ID;
import static com.parkertenbroeck.remotestorage.RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE;


@Environment(EnvType.CLIENT)
public class RemoteStorageClient implements ClientModInitializer {
	private static KeyBinding openRemoteStorage;
	private static KeyBinding toggleEditMode;
	private static boolean attackPressed = false;
	private static boolean usePressed = false;

	private static StorageSystemPositionsS2C system = new StorageSystemPositionsS2C("default", List.of());
	private static boolean editMode = false;
	public static BlockPos linkTarget = null;

	@Override
	public void onInitializeClient() {
		openRemoteStorage = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key."+ MOD_ID+".open_inv",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_X,
				"category." + MOD_ID
		));
		toggleEditMode = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key."+ MOD_ID+".edit_mode",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				"category." + MOD_ID
		));

		HandledScreens.register(REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE, RemoteStorageScreen::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(openRemoteStorage.wasPressed()){
				ClientPlayNetworking.send(new OpenRemoteStorageC2S());
			}
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(toggleEditMode.wasPressed()){
                editMode = !editMode;
				client.player.sendMessage(Text.of((editMode?"Entered":"Exited")+" remote storage edit mode"), true);
				linkTarget = null;
			}
			outer:
			if(client.options.useKey.isPressed()&&!usePressed){
				if(!editMode)break outer;
				if(client.crosshairTarget==null)break outer;
				if(client.crosshairTarget.getType() == HitResult.Type.BLOCK){
					var pos = ((BlockHitResult)client.crosshairTarget).getBlockPos();
					if(shiftHeld(client)){
						linkTarget = pos;
					}else{
						var position = StorageSystem.Position.of(client.player, pos);
						if(system.members().stream().noneMatch(m -> m.pos().equals(position)))
							addBlock(client.world, pos);
						else
							autoLink(client.world, pos);
						if(linkTarget!=null){
							ClientPlayNetworking.send(new LinkRemoteStorageMemberC2S(pos, Optional.of(linkTarget)));

						}
					}
				}else if(shiftHeld(client)){
					linkTarget = null;
				}
			}

			outer:
			if (client.options.attackKey.isPressed()&&!attackPressed){
				if(!editMode)break outer;
				if(client.crosshairTarget==null)break outer;
				if(client.crosshairTarget.getType() == HitResult.Type.BLOCK){
					var pos = ((BlockHitResult)client.crosshairTarget).getBlockPos();
					if(ctrlHeld(client)){
						ClientPlayNetworking.send(new RemoveFromRemoteStorageC2S(pos));
					}else{
						ClientPlayNetworking.send(new LinkRemoteStorageMemberC2S(pos, Optional.empty()));
					}
				}
			}

			attackPressed = client.options.attackKey.isPressed();
			usePressed = client.options.useKey.isPressed();
		});

		UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
			if(!editMode) return ActionResult.PASS;
			return ActionResult.FAIL;
		});

		AttackBlockCallback.EVENT.register((playerEntity, world, hand, pos, direction) -> {
			if(!editMode) return ActionResult.PASS;
			return ActionResult.FAIL;
		});

		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
			if (editMode) OutlineRenderer.render(context, system);
		});

		ClientPlayNetworking.registerGlobalReceiver(StorageSystemPositionsS2C.ID, (packet, context) -> system = packet);

		ClientPlayNetworking.registerGlobalReceiver(OpenRemoteStorageS2C.ID, (packet, context) -> {
			var handler = new RemoteStorageScreenHandler(packet.syncId(), context.player().getInventory(), packet);
			context.player().currentScreenHandler = handler;
			context.client().setScreen(new RemoteStorageScreen(handler, context.player().getInventory(), packet.title()));
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

	public static boolean shiftHeld(MinecraftClient client){
		return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT);
	}

	public static boolean ctrlHeld(MinecraftClient client){
		return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_CONTROL);
	}

	public static void addBlock(World world, BlockPos pos){
		ClientPlayNetworking.send(new AddToRemoteStorageC2S(pos));
		if(world.getBlockState(pos).getBlock() == Blocks.CHEST){
			if(world.getBlockState(pos).get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE){
				var other = pos.offset(ChestBlock.getFacing(world.getBlockState(pos)));
				ClientPlayNetworking.send(new AddToRemoteStorageC2S(other));
			}
		}
		autoLink(world, pos);
	}

	public static void autoLink(World world, BlockPos pos){
		if(world.getBlockState(pos).getBlock() == Blocks.CHEST){
			if(world.getBlockState(pos).get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE){
				var other = pos.offset(ChestBlock.getFacing(world.getBlockState(pos)));
				ClientPlayNetworking.send(new LinkRemoteStorageMemberC2S(other, Optional.of(pos)));
			}
		}
	}
}