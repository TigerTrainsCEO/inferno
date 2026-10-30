package com.infernoclient.ui;

import com.infernoclient.config.ClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** Every toggleable feature, described once so the menu can render and edit it generically. */
public final class Modules {
	private Modules() {
	}

	public record Slider(String label, double min, double max, DoubleSupplier get, DoubleConsumer set, String format) {
	}

	public record Module(String id, String name, String category, String description, BooleanSupplier get, Consumer<Boolean> set, List<Slider> sliders) {
	}

	public static final String[] CATEGORIES = {"HUD", "Gameplay", "Visual", "Social"};

	private static Module m(String id, String name, String category, String description, BooleanSupplier get, Consumer<Boolean> set, Slider... sliders) {
		return new Module(id, name, category, description, get, set, List.of(sliders));
	}

	public static List<Module> all(ClientConfig c) {
		ClientConfig.Hud h = c.hud;
		List<Module> list = new ArrayList<>();

		list.add(m("info", "Info Panel", "HUD", "The stacked readout of everything toggled below.", () -> h.info, v -> h.info = v,
			new Slider("Scale", 0.5, 2.5, () -> h.infoScale, v -> h.infoScale = v, "%.1fx")));
		list.add(m("fps", "FPS", "HUD", "Frames per second.", () -> h.fps, v -> h.fps = v));
		list.add(m("coordinates", "Coordinates", "HUD", "Your block position.", () -> h.coordinates, v -> h.coordinates = v));
		list.add(m("biome", "Biome", "HUD", "The biome you are standing in.", () -> h.biome, v -> h.biome = v));
		list.add(m("direction", "Direction", "HUD", "Compass facing.", () -> h.direction, v -> h.direction = v));
		list.add(m("cps", "CPS", "HUD", "Left and right clicks per second.", () -> h.cps, v -> h.cps = v));
		list.add(m("ping", "Ping", "HUD", "Latency to the server.", () -> h.ping, v -> h.ping = v));
		list.add(m("speed", "Speed", "HUD", "Horizontal speed in blocks per second.", () -> h.speed, v -> h.speed = v));
		list.add(m("health", "Health", "HUD", "Hearts including absorption.", () -> h.health, v -> h.health = v));
		list.add(m("lightLevel", "Light Level", "HUD", "Light at your feet.", () -> h.lightLevel, v -> h.lightLevel = v));
		list.add(m("clock", "Clock", "HUD", "Real time.", () -> h.clock, v -> h.clock = v));
		list.add(m("memory", "Memory", "HUD", "Java heap in use.", () -> h.memory, v -> h.memory = v));
		list.add(m("day", "World Day", "HUD", "In-game day count.", () -> h.day, v -> h.day = v));
		list.add(m("dimension", "Dimension", "HUD", "Overworld, Nether or End.", () -> h.dimension, v -> h.dimension = v));
		list.add(m("chunkCoordinates", "Chunk Coords", "HUD", "Chunk position.", () -> h.chunkCoordinates, v -> h.chunkCoordinates = v));
		list.add(m("netherCoordinates", "Nether Coords", "HUD", "Position converted to the other dimension.", () -> h.netherCoordinates, v -> h.netherCoordinates = v));
		list.add(m("targetBlock", "Target Block", "HUD", "The block under your crosshair.", () -> h.targetBlock, v -> h.targetBlock = v));
		list.add(m("weather", "Weather", "HUD", "Clear, rain or thunder.", () -> h.weather, v -> h.weather = v));
		list.add(m("keystrokes", "Keystrokes", "HUD", "WASD, mouse buttons and jump.", () -> h.keystrokes, v -> h.keystrokes = v,
			new Slider("Scale", 0.5, 2.5, () -> h.keystrokesScale, v -> h.keystrokesScale = v, "%.1fx")));
		list.add(m("armor", "Armor Status", "HUD", "Armor pieces with durability.", () -> h.armor, v -> h.armor = v,
			new Slider("Scale", 0.5, 2.5, () -> h.armorScale, v -> h.armorScale = v, "%.1fx")));
		list.add(m("potionEffects", "Potion Effects", "HUD", "Active effects and time left.", () -> h.potionEffects, v -> h.potionEffects = v,
			new Slider("Scale", 0.5, 2.5, () -> h.potionScale, v -> h.potionScale = v, "%.1fx")));
		list.add(m("inventory", "Inventory", "HUD", "Your main inventory on screen.", () -> h.inventory, v -> h.inventory = v,
			new Slider("Scale", 0.5, 2.5, () -> h.inventoryScale, v -> h.inventoryScale = v, "%.1fx")));
		list.add(m("heldItem", "Held Item", "HUD", "What is in your hand.", () -> h.heldItem, v -> h.heldItem = v,
			new Slider("Scale", 0.5, 2.5, () -> h.heldItemScale, v -> h.heldItemScale = v, "%.1fx")));
		list.add(m("durabilityAlert", "Durability Alert", "HUD", "Warns when gear is about to break.", () -> h.durabilityAlert, v -> h.durabilityAlert = v));

		list.add(m("zoom", "Zoom", "Gameplay", "Hold the zoom key to look far away.", () -> c.zoom.enabled, v -> c.zoom.enabled = v,
			new Slider("Zoom strength", 0.05, 0.9, () -> c.zoom.multiplier, v -> c.zoom.multiplier = v, "%.2f")));
		list.add(m("toggleSprint", "Toggle Sprint", "Gameplay", "Sprint automatically while walking forward.", () -> c.movement.toggleSprint, v -> c.movement.toggleSprint = v));
		list.add(m("autoRespawn", "Auto Respawn", "Gameplay", "Respawns for you, retrying until it works.", () -> c.movement.autoRespawn, v -> c.movement.autoRespawn = v,
			new Slider("Delay (ticks)", 0, 60, () -> c.movement.autoRespawnDelay, v -> c.movement.autoRespawnDelay = (int) Math.round(v), "%.0f")));
		list.add(m("freelook", "Freelook", "Gameplay", "Hold the key to look around without turning.", () -> c.perspective.enabled, v -> c.perspective.enabled = v));
		list.add(m("elytraSwapper", "Elytra Swapper", "Gameplay", "One key swaps elytra and chestplate.", () -> c.social.elytraSwapper, v -> c.social.elytraSwapper = v));

		list.add(m("fullbright", "Fullbright", "Visual", "Everything is fully lit.", () -> c.visual.fullbright, v -> c.visual.fullbright = v,
			new Slider("Brightness", 2.0, 16.0, () -> c.visual.fullbrightGamma, v -> c.visual.fullbrightGamma = v, "%.0f")));
		list.add(m("lowFire", "Low Fire", "Visual", "Lowers the burning overlay.", () -> c.visual.lowFire, v -> c.visual.lowFire = v,
			new Slider("Lowering", 0.05, 0.6, () -> c.visual.lowFireAmount, v -> c.visual.lowFireAmount = v, "%.2f")));
		list.add(m("lowShield", "Low Shield", "Visual", "Lowers your held shield.", () -> c.visual.lowShield, v -> c.visual.lowShield = v,
			new Slider("Lowering", 0.05, 0.5, () -> c.visual.lowShieldAmount, v -> c.visual.lowShieldAmount = v, "%.2f")));
		list.add(m("shulkerTooltips", "Container Tooltips", "Visual", "Shows shulker box contents in tooltips.", () -> c.social.shulkerTooltips, v -> c.social.shulkerTooltips = v));

		list.add(m("highlightFriends", "Friend Highlight", "Social", "Marks friends in the player list.", () -> c.social.highlightFriends, v -> c.social.highlightFriends = v));
		return list;
	}
}
