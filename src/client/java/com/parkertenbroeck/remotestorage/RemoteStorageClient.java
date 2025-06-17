package com.parkertenbroeck.remotestorage;

import com.parkertenbroeck.remotestorage.packets.c2s.edit.*;
import com.parkertenbroeck.remotestorage.packets.c2s.OpenRemoteStorageC2S;
import com.parkertenbroeck.remotestorage.packets.s2c.OpenRemoteStorageS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.RemoteStorageContentsDeltaS2C;
import com.parkertenbroeck.remotestorage.packets.s2c.StorageSystemResyncS2C;
import com.parkertenbroeck.remotestorage.system.Group;
import com.parkertenbroeck.remotestorage.system.Position;
import com.parkertenbroeck.remotestorage.system.StorageSystem;
import com.parkertenbroeck.remotestorage.system.StorageSystemContext;
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
import net.minecraft.world.World;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

import static com.parkertenbroeck.remotestorage.RemoteStorage.MOD_ID;
import static com.parkertenbroeck.remotestorage.RemoteStorage.REMOTE_STORAGE_SCREEN_HANDLER_SCREEN_HANDLER_TYPE;


@Environment(EnvType.CLIENT)
public class RemoteStorageClient implements ClientModInitializer {
	private static KeyBinding openRemoteStorage;
	private static KeyBinding toggleEditMode;
	private static boolean attackPressed = false;
	private static boolean usePressed = false;

	public static StorageSystem system = new StorageSystem();
	public static StorageSystemContext context = new StorageSystemContext() {
		@Override
		public void unlink(Position pos) {
			ClientPlayNetworking.send(new LinkRemoteStorageMemberC2S(pos, Optional.empty()));
		}

		@Override
		public void add(Position pos) {
			ClientPlayNetworking.send(new AddToRemoteStorageC2S(pos));
		}

		@Override
		public void clear() {
			throw new IllegalStateException();
		}

		@Override
		public void remove(Position pos) {
			ClientPlayNetworking.send(new RemoveFromRemoteStorageC2S(pos));
		}

		@Override
		public void link(Position child, Position parent) {
			ClientPlayNetworking.send(new LinkRemoteStorageMemberC2S(child, Optional.of(parent)));
		}

		@Override
		public void updateGroup(Group group) {
			ClientPlayNetworking.send(new UpdateGroupRemoteStorageC2S(group));
		}

		@Override
		public void setGroup(Position member, int group) {
			ClientPlayNetworking.send(new SetGroupRemoteStorageC2S(member, group));
		}
	};

	private static boolean editMode = false;
	public static Position linkTarget = null;

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

		system.setContext(context);

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

			var target = client.crosshairTarget==null?null:client.crosshairTarget.getType() != HitResult.Type.BLOCK?null:((BlockHitResult)client.crosshairTarget).getBlockPos();

			if(client.options.useKey.isPressed()&&!usePressed){
				if(shiftHeld(client)){
					linkTarget = Position.of(client.player, target);
				}else if(ctrlHeld(client)){
					var member = system.member(Position.of(client.world, target));
					if(member != null)
						client.setScreen(new EditGroupScreen(system.group(member)));
				}else if(target !=null){
					var position = Position.of(client.player, target);
					if(system.unorderedMembers().stream().noneMatch(m -> m.pos().equals(position)))
						addBlock(client.world, position);
					else
						autoLink(client.world, position);
					if(linkTarget!=null)
						link(position, linkTarget, false);
				}
			}

			if (client.options.attackKey.isPressed()&&!attackPressed){
				if(shiftHeld(client)){
					client.setScreen(new GroupListScreen());
				}if(ctrlHeld(client)){
					if(target!=null)
						system.remove(Position.of(client.player, target));
				}else{
					if(target!=null)
						system.unlink(Position.of(client.player, target));
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

		ClientPlayNetworking.registerGlobalReceiver(StorageSystemResyncS2C.ID, (packet, context) -> {
			system = packet.system();
			system.setContext(RemoteStorageClient.context);
		});

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

	public static void link(Position child, Position parent, boolean suppressCycleMessage){
		var player = MinecraftClient.getInstance().player;
		switch(system.link(child, parent)){
			case Success -> {}
			case CannotCreateCircularLink -> {
				if(!suppressCycleMessage)
					player.sendMessage(Text.of("Cannot create circular links"), false);
			}
			case CannotLinkToSelf ->
					player.sendMessage(Text.of("Cannot link to self"), false);
			case ChildIsNotMember ->
					player.sendMessage(Text.of("Child is not a member of the system"), false);
			case ParentIsNotMember ->
					player.sendMessage(Text.of("Parent is not a member of the system"), false);
        }
	}

	public static boolean shiftHeld(MinecraftClient client){
		return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_SHIFT);
	}

	public static boolean ctrlHeld(MinecraftClient client){
		return InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.GLFW_KEY_LEFT_CONTROL);
	}

	public static void addBlock(World world, Position pos){
		system.add(pos, world);
		if(!world.getRegistryKey().getValue().equals(pos.world()))return;
		if(world.getBlockState(pos.pos()).getBlock() == Blocks.CHEST){
			if(world.getBlockState(pos.pos()).get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE){
				var other = pos.pos().offset(ChestBlock.getFacing(world.getBlockState(pos.pos())));
				system.add(Position.of(world, other), world);
			}
		}
		autoLink(world, pos);
	}

	public static void autoLink(World world, Position pos){
		if(!world.getRegistryKey().getValue().equals(pos.world()))return;
		if(world.getBlockState(pos.pos()).getBlock() == Blocks.CHEST){
			if(world.getBlockState(pos.pos()).get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE){
				var other = pos.pos().offset(ChestBlock.getFacing(world.getBlockState(pos.pos())));
				link(pos, Position.of(world, other), true);
			}
		}
	}
}