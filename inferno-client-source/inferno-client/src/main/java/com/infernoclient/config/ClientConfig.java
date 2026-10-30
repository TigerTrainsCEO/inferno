package com.infernoclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.infernoclient.InfernoClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * All persisted settings. Field names intentionally match the previous Inferno Client config
 * (config/inferno_client.json) so existing settings keep loading; new fields simply get defaults.
 */
public final class ClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public Hud hud = new Hud();
	public Zoom zoom = new Zoom();
	public Movement movement = new Movement();
	public Perspective perspective = new Perspective();
	public Visual visual = new Visual();
	public Social social = new Social();
	public Home home = new Home();
	public Ui ui = new Ui();

	public static final class Hud {
		public boolean fps = true;
		public boolean coordinates = true;
		public boolean biome = true;
		public boolean armor = true;
		public boolean potionEffects = true;
		public boolean inventory = false;
		public boolean heldItem = false;
		public boolean keystrokes = true;
		public boolean cps = true;
		public boolean direction = true;
		public boolean lightLevel = false;
		public boolean speed = false;
		public boolean clock = false;
		public boolean ping = false;
		public boolean memory = false;
		public boolean day = false;
		public boolean dimension = false;
		public boolean chunkCoordinates = false;
		public boolean netherCoordinates = false;
		public boolean targetBlock = false;
		public boolean durabilityAlert = false;
		public boolean weather = false;
		public boolean health = false;
		public boolean info = true;

		/** Top-left positions in GUI pixels. -1 means "use the automatic default position". */
		public int x = 8;
		public int y = 8;
		public int inventoryX = -1;
		public int inventoryY = -1;
		public int heldItemX = -1;
		public int heldItemY = -1;
		public int keystrokesX = -1;
		public int keystrokesY = -1;
		public int armorX = -1;
		public int armorY = -1;
		public int potionX = -1;
		public int potionY = -1;

		public int color = -1;
		public int accentColor = 0xFF5A1F;
		public String theme = "inferno";
		public double infoScale = 1.0;
		public double keystrokesScale = 1.0;
		public double armorScale = 1.0;
		public double potionScale = 1.0;
		public double inventoryScale = 1.0;
		public double heldItemScale = 1.0;
		public double itemScale = 1.0;
	}

	public static final class Zoom {
		public boolean enabled = true;
		/** Field-of-view multiplier while zooming; smaller means more zoom. */
		public double multiplier = 0.25;
	}

	public static final class Movement {
		public boolean toggleSprint = true;
		public boolean autoRespawn = false;
		/** Ticks to wait on the death screen before respawning (20 ticks = 1 second). */
		public int autoRespawnDelay = 6;
	}

	public static final class Perspective {
		public boolean enabled = true;
	}

	public static final class Visual {
		public boolean fullbright = false;
		/** Gamma applied while fullbright is on. Vanilla maxes out at 1.0. */
		public double fullbrightGamma = 12.0;
		public boolean lowFire = true;
		/** How far the fire overlay is pushed down, in overlay units. */
		public double lowFireAmount = 0.35;
		public boolean lowShield = true;
		/** How far a raised shield is lowered, in view units. */
		public double lowShieldAmount = 0.2;
	}

	public static final class FriendEntry {
		public String uuid;
		public String name;
		public long addedAt;

		public FriendEntry() {
		}

		public static FriendEntry byName(String name) {
			FriendEntry entry = new FriendEntry();
			entry.name = name;
			entry.addedAt = System.currentTimeMillis();
			return entry;
		}

		public static FriendEntry byProfile(String uuid, String name) {
			FriendEntry entry = byName(name);
			entry.uuid = uuid;
			return entry;
		}

		public boolean matchesName(String other) {
			return other != null && name != null && name.equalsIgnoreCase(other);
		}
	}

	public static final class Social {
		/** Legacy list of names from the previous version; migrated into friendEntries on load. */
		public List<String> friends = new ArrayList<>();
		public List<FriendEntry> friendEntries = new ArrayList<>();
		public boolean highlightFriends = true;
		public boolean shulkerTooltips = true;
		public boolean elytraSwapper = true;
	}

	public static final class Home {
		public boolean enabled = false;
		public boolean news = true;
		public boolean changelog = true;
		public boolean servers = true;
		public boolean profile = true;
		public boolean stats = true;
	}

	public static final class Ui {
		public List<String> favoriteModules = new ArrayList<>();
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("inferno_client.json");
	}

	public static ClientConfig load() {
		Path file = path();
		ClientConfig loaded = null;
		if (Files.exists(file)) {
			try (BufferedReader reader = Files.newBufferedReader(file)) {
				loaded = GSON.fromJson(reader, ClientConfig.class);
			} catch (Exception e) {
				Path broken = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
				try {
					Files.move(file, broken, StandardCopyOption.REPLACE_EXISTING);
					InfernoClient.LOGGER.warn("Could not read Inferno Client config; saved the invalid file as {}", broken);
				} catch (IOException io) {
					InfernoClient.LOGGER.error("Could not read or back up Inferno Client config at {}", file, io);
				}
			}
		}
		ClientConfig config = loaded == null ? new ClientConfig() : loaded;
		config.normalize();
		return config;
	}

	/** Fills anything missing (older files) and clamps values to safe ranges. */
	private void normalize() {
		if (hud == null) hud = new Hud();
		if (zoom == null) zoom = new Zoom();
		if (movement == null) movement = new Movement();
		if (perspective == null) perspective = new Perspective();
		if (visual == null) visual = new Visual();
		if (social == null) social = new Social();
		if (home == null) home = new Home();
		if (ui == null) ui = new Ui();
		if (social.friends == null) social.friends = new ArrayList<>();
		if (social.friendEntries == null) social.friendEntries = new ArrayList<>();
		if (ui.favoriteModules == null) ui.favoriteModules = new ArrayList<>();
		if (hud.theme == null) hud.theme = "inferno";

		for (String legacy : social.friends) {
			if (legacy != null && !legacy.isBlank() && social.friendEntries.stream().noneMatch(e -> e.matchesName(legacy))) {
				social.friendEntries.add(FriendEntry.byName(legacy.trim()));
			}
		}
		social.friends.clear();

		hud.infoScale = clamp(hud.infoScale, 0.5, 2.5);
		hud.keystrokesScale = clamp(hud.keystrokesScale, 0.5, 2.5);
		hud.armorScale = clamp(hud.armorScale, 0.5, 2.5);
		hud.potionScale = clamp(hud.potionScale, 0.5, 2.5);
		hud.inventoryScale = clamp(hud.inventoryScale, 0.5, 2.5);
		hud.heldItemScale = clamp(hud.heldItemScale, 0.5, 2.5);
		hud.itemScale = clamp(hud.itemScale, 0.5, 2.5);
		zoom.multiplier = clamp(zoom.multiplier, 0.05, 0.9);
		visual.fullbrightGamma = clamp(visual.fullbrightGamma, 2.0, 16.0);
		visual.lowFireAmount = clamp(visual.lowFireAmount, 0.05, 0.6);
		visual.lowShieldAmount = clamp(visual.lowShieldAmount, 0.05, 0.5);
		movement.autoRespawnDelay = (int) clamp(movement.autoRespawnDelay, 0, 100);
	}

	public void save() {
		Path file = path();
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				GSON.toJson(this, writer);
			}
			try {
				Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException atomicFailed) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			InfernoClient.LOGGER.error("Could not save Inferno Client config at {}", file, e);
		}
	}

	public static double clamp(double value, double min, double max) {
		if (Double.isNaN(value)) return min;
		return Math.max(min, Math.min(max, value));
	}
}
