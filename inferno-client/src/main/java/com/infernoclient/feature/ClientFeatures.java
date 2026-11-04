package com.infernoclient.feature;

import com.infernoclient.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;

/** Per-tick gameplay helpers: auto respawn and toggle sprint. */
public final class ClientFeatures {
	private final ClientConfig config;
	private int deadTicks;
	private boolean sprintForced;
	private int adaptTicks;
	private int adaptCooldown;
	private long fpsSum;
	private int fpsSamples;

	public ClientFeatures(ClientConfig config) {
		this.config = config;
	}

	public void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			deadTicks = 0;
			sprintForced = false;
			adaptTicks = 0;
			fpsSum = 0;
			fpsSamples = 0;
			return;
		}
		tickAutoRespawn(client, player);
		tickToggleSprint(client, player);
		tickAdaptiveRenderDistance(client);
	}

	/**
	 * Keeps frame rate near a target by nudging render distance. It samples FPS once a second, looks at
	 * five seconds at a time, and waits after each change so chunk reloads do not skew the next reading.
	 */
	private void tickAdaptiveRenderDistance(MinecraftClient client) {
		ClientConfig.Performance perf = config.performance;
		if (!perf.adaptiveRenderDistance || client.currentScreen != null) {
			adaptTicks = 0;
			fpsSum = 0;
			fpsSamples = 0;
			return;
		}
		if (adaptCooldown > 0) {
			adaptCooldown--;
			return;
		}
		adaptTicks++;
		if (adaptTicks % 20 == 0) {
			fpsSum += client.getCurrentFps();
			fpsSamples++;
		}
		if (adaptTicks < 100 || fpsSamples == 0) {
			return;
		}
		double average = (double) fpsSum / fpsSamples;
		adaptTicks = 0;
		fpsSum = 0;
		fpsSamples = 0;

		int current = client.options.getViewDistance().getValue();
		int next = current;
		if (average < perf.targetFps * 0.9 && current > perf.minDistance) {
			next = current - 1;
		} else if (average > perf.targetFps * 1.4 && current < perf.maxDistance) {
			next = current + 1;
		}
		if (next != current) {
			client.options.getViewDistance().setValue(next);
			adaptCooldown = 200;
		}
	}

	/**
	 * Respawns as soon as the death screen is up. Unlike a single one-shot request this keeps asking
	 * every second until the server answers, so a dropped or ignored request cannot leave you stuck.
	 */
	private void tickAutoRespawn(MinecraftClient client, ClientPlayerEntity player) {
		boolean dead = player.getHealth() <= 0.0f || client.currentScreen instanceof DeathScreen;
		if (!config.movement.autoRespawn || !dead) {
			deadTicks = 0;
			return;
		}
		deadTicks++;
		int delay = config.movement.autoRespawnDelay;
		if (deadTicks >= delay && (deadTicks - delay) % 20 == 0) {
			player.requestRespawn();
		}
	}

	/** Holds the vanilla sprint key while you walk forward, so every vanilla sprint rule still applies. */
	private void tickToggleSprint(MinecraftClient client, ClientPlayerEntity player) {
		GameOptions options = client.options;
		boolean want = config.movement.toggleSprint
			&& client.currentScreen == null
			&& options.forwardKey.isPressed()
			&& !player.isSneaking();
		if (want) {
			if (!options.sprintKey.isPressed()) {
				options.sprintKey.setPressed(true);
				sprintForced = true;
			}
		} else if (sprintForced) {
			options.sprintKey.setPressed(false);
			sprintForced = false;
		}
	}
}
