package com.infernoclient.feature;

import com.infernoclient.InfernoClient;
import com.infernoclient.config.ClientConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.List;
import java.util.UUID;

public final class FriendManager {
	private FriendManager() {
	}

	private static List<ClientConfig.FriendEntry> entries() {
		return InfernoClient.config().social.friendEntries;
	}

	public static boolean isFriend(String name, UUID uuid) {
		for (ClientConfig.FriendEntry entry : entries()) {
			if (uuid != null && entry.uuid != null && entry.uuid.equalsIgnoreCase(uuid.toString())) {
				return true;
			}
			if (entry.matchesName(name)) {
				return true;
			}
		}
		return false;
	}

	public static boolean add(String name, UUID uuid) {
		if (name == null || name.isBlank() || isFriend(name, uuid)) {
			return false;
		}
		entries().add(ClientConfig.FriendEntry.byProfile(uuid == null ? null : uuid.toString(), name.trim()));
		InfernoClient.config().save();
		return true;
	}

	public static void remove(ClientConfig.FriendEntry entry) {
		entries().remove(entry);
		InfernoClient.config().save();
	}

	public static void clear() {
		entries().clear();
		InfernoClient.config().save();
	}

	/** Adds the player under your crosshair. Returns the name added, or null. */
	public static String addTargeted(MinecraftClient client) {
		if (client.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity target) {
			String name = target.getName().getString();
			return add(name, target.getUuid()) ? name : null;
		}
		return null;
	}
}
