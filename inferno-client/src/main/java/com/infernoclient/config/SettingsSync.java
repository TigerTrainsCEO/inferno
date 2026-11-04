package com.infernoclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.infernoclient.InfernoClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Shares HUD layout and crosshair between every Inferno profile on this computer through one file in
 * the user's home folder. Keybinds and video settings are synced by the launcher itself.
 */
public final class SettingsSync {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final class Shared {
		ClientConfig.Hud hud;
		ClientConfig.Crosshair crosshair;
	}

	private SettingsSync() {
	}

	private static Path path() {
		return Path.of(System.getProperty("user.home"), ".inferno-client", "shared.json");
	}

	private static Shared read() {
		Path file = path();
		if (!Files.exists(file)) {
			return new Shared();
		}
		try (BufferedReader reader = Files.newBufferedReader(file)) {
			Shared shared = GSON.fromJson(reader, Shared.class);
			return shared == null ? new Shared() : shared;
		} catch (Exception e) {
			InfernoClient.LOGGER.warn("Could not read shared Inferno settings at {}", file, e);
			return new Shared();
		}
	}

	/** Loads the shared HUD and crosshair into this profile's config. Call once at startup. */
	public static void pull(ClientConfig config) {
		if (!config.sync.enabled) {
			return;
		}
		Shared shared = read();
		if (config.sync.hud && shared.hud != null) {
			config.hud = shared.hud;
		}
		if (config.sync.crosshair && shared.crosshair != null) {
			config.crosshair = shared.crosshair;
		}
		config.normalize();
	}

	/** Writes this profile's HUD and crosshair to the shared file, for the parts that are being synced. */
	public static void push(ClientConfig config) {
		if (!config.sync.enabled || (!config.sync.hud && !config.sync.crosshair)) {
			return;
		}
		Shared shared = read();
		if (config.sync.hud) {
			shared.hud = config.hud;
		}
		if (config.sync.crosshair) {
			shared.crosshair = config.crosshair;
		}
		Path file = path();
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
				GSON.toJson(shared, writer);
			}
			try {
				Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException atomicFailed) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			InfernoClient.LOGGER.warn("Could not write shared Inferno settings at {}", file, e);
		}
	}
}
