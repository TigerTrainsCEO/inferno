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

	public ClientFeatures(ClientConfig config) {
		this.config = config;
	}

	public void tick(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null) {
			deadTicks = 0;
			sprintForced = false;
			return;
		}
		tickAutoRespawn(client, player);
		tickToggleSprint(client, player);
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
