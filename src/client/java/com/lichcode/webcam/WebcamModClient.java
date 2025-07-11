package com.lichcode.webcam;

import com.lichcode.webcam.render.PlayerFaceRenderer;

import com.lichcode.webcam.screen.SettingsScreen;
import com.lichcode.webcam.video.VideoManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;



public class WebcamModClient implements ClientModInitializer {
	private static KeyBinding toggleKey;
	private static boolean streaming = true;
	@Override
	public void onInitializeClient() {
		registerSettingsCommand();

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.webcammod.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_V,
				"category.webcammod.controls"
		));

		// Listen for client ticks to check key presses
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.wasPressed()) {
				streaming = !streaming;
				if (streaming) {
					startCapture();
					client.player.sendMessage(Text.of("Webcam stream enabled"), false);
				} else {
					stopCapture();
					client.player.sendMessage(Text.of("Webcam stream disabled"), false);
				}
			}
		});


		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
			if (entityRenderer instanceof PlayerEntityRenderer) {
				registrationHelper.register(new PlayerFaceRenderer((PlayerEntityRenderer) entityRenderer));
			}
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			VideoManager.startCameraLoop();
		});

		ClientPlayConnectionEvents.DISCONNECT.register(((handler, client) -> {
			VideoManager.stopThread();
		}));

		ClientPlayNetworking.registerGlobalReceiver(VideoFramePayload.ID, ((payload, context) -> {
			PlayerFeeds.update(payload.video());
		}));
	}

	private static void startCapture() {
		VideoManager.startCameraLoop();
	}

	private static void stopCapture() {
		VideoManager.stopThread();
	}

	private void registerSettingsCommand() {
		ClientCommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess) -> {
			commandDispatcher.register(ClientCommandManager.literal("webcam-settings").executes(context ->  {
				MinecraftClient client = context.getSource().getClient();

				client.send(() -> client.setScreen(new SettingsScreen()));
				return 1;
			}));
		}));
	}
}
