package com.infernoclient;

import com.infernoclient.config.ClientConfig;
import com.infernoclient.feature.ClientFeatures;
import com.infernoclient.feature.ElytraSwapper;
import com.infernoclient.feature.PerspectiveController;
import com.infernoclient.feature.TooltipFeatures;
import com.infernoclient.hud.ClientHud;
import com.infernoclient.screen.InfernoHomeScreen;
import com.infernoclient.screen.InfernoMenuScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InfernoClient implements ClientModInitializer {
	public static final String ID = "inferno_client";
	public static final String VERSION = "1.0.0";
	public static final Logger LOGGER = LoggerFactory.getLogger("Inferno Client");

	private static ClientConfig config = new ClientConfig();
	private static ClientFeatures features;

	private static KeyBinding menuKey;
	private static KeyBinding zoomKey;
	private static KeyBinding perspectiveKey;
	private static KeyBinding elytraSwapKey;

	private static boolean zooming;
	private static float zoomPrevious = 1.0f;
	private static float zoomCurrent = 1.0f;

	@Override
	public void onInitializeClient() {
		config = ClientConfig.load();
		config.save();
		features = new ClientFeatures(config);

		KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(ID, "keys"));
		menuKey = register("key.inferno_client.menu", 344, category);
		zoomKey = register("key.inferno_client.zoom", 67, category);
		perspectiveKey = register("key.inferno_client.perspective", 342, category);
		elytraSwapKey = register("key.inferno_client.elytra_swap", 71, category);

		ClientHud.register(config);
		TooltipFeatures.register(config);
		ClientTickEvents.END_CLIENT_TICK.register(InfernoClient::onTick);
		LOGGER.info("Inferno Client {} loaded", VERSION);
	}

	private static KeyBinding register(String translationKey, int glfwKey, KeyBinding.Category category) {
		return KeyBindingHelper.registerKeyBinding(new KeyBinding(translationKey, InputUtil.Type.KEYSYM, glfwKey, category));
	}

	public static ClientConfig config() {
		return config;
	}

	public static boolean isZooming() {
		return zooming;
	}

	/** Field-of-view multiplier, interpolated between ticks so zooming eases in and out. */
	public static float fovMultiplier(float tickDelta) {
		return zoomPrevious + (zoomCurrent - zoomPrevious) * tickDelta;
	}

	public static boolean zoomActive() {
		return zooming || zoomCurrent < 0.999f || zoomPrevious < 0.999f;
	}

	private static void onTick(MinecraftClient client) {
		ClientConfig cfg = config;

		zooming = cfg.zoom.enabled && zoomKey.isPressed() && client.currentScreen == null;
		float target = zooming ? (float) cfg.zoom.multiplier : 1.0f;
		zoomPrevious = zoomCurrent;
		zoomCurrent += (target - zoomCurrent) * 0.45f;
		if (Math.abs(zoomCurrent - target) < 0.002f) {
			zoomCurrent = target;
		}

		while (menuKey.wasPressed()) {
			if (client.currentScreen == null) {
				client.setScreen(new InfernoMenuScreen(cfg, null));
			}
		}
		while (elytraSwapKey.wasPressed()) {
			if (cfg.social.elytraSwapper) {
				ElytraSwapper.swap(client);
			}
		}

		boolean wantFreelook = perspectiveKey.isPressed();
		PerspectiveController.update(client, wantFreelook, cfg.perspective.enabled);

		features.tick(client);

		if (cfg.home.enabled && client.world == null && client.currentScreen instanceof TitleScreen) {
			if (!InfernoHomeScreen.classicRequested()) {
				client.setScreen(new InfernoHomeScreen(cfg));
			}
		} else if (client.world != null) {
			InfernoHomeScreen.resetClassicRequest();
		}
	}
}
