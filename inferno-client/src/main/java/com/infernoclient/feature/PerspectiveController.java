package com.infernoclient.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;

/** Freelook: look around your character in third person without turning it. */
public final class PerspectiveController {
	private static boolean active;
	private static float yaw;
	private static float pitch;
	private static Perspective previous = Perspective.FIRST_PERSON;

	private PerspectiveController() {
	}

	public static boolean isActive() {
		return active;
	}

	public static float yaw() {
		return yaw;
	}

	public static float pitch() {
		return pitch;
	}

	public static void update(MinecraftClient client, boolean wanted, boolean enabled) {
		boolean should = wanted && enabled && client.player != null && client.currentScreen == null;
		if (should && !active) {
			active = true;
			yaw = client.player.getYaw();
			pitch = client.player.getPitch();
			previous = client.options.getPerspective();
			client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
		} else if (!should && active) {
			active = false;
			client.options.setPerspective(previous);
		}
	}

	/** Same scaling vanilla uses for mouse look. */
	public static void updateCamera(double cursorDeltaX, double cursorDeltaY) {
		yaw += (float) (cursorDeltaX * 0.15);
		pitch = (float) Math.max(-90.0, Math.min(90.0, pitch + cursorDeltaY * 0.15));
	}
}
