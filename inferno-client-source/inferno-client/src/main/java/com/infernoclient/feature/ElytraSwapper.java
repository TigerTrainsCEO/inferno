package com.infernoclient.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

/**
 * Swaps your chest slot between an elytra and a chestplate using normal inventory clicks, so the
 * server sees an ordinary, validated inventory action.
 */
public final class ElytraSwapper {
	private static final int CHEST_SLOT = 6;

	private ElytraSwapper() {
	}

	public static void swap(MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || client.interactionManager == null || client.currentScreen != null) {
			return;
		}

		PlayerInventory inventory = player.getInventory();
		boolean wearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);

		int found = -1;
		int limit = Math.min(inventory.size(), 36);
		for (int i = 0; i < limit; i++) {
			ItemStack stack = inventory.getStack(i);
			if (stack.isEmpty()) {
				continue;
			}
			boolean match = wearingElytra ? isChestplate(stack) : isUsableElytra(stack);
			if (match) {
				found = i;
				break;
			}
		}

		if (found < 0) {
			player.sendMessage(Text.literal(wearingElytra ? "No chestplate in your inventory" : "No elytra in your inventory"), true);
			return;
		}

		int slot = found < 9 ? 36 + found : found;
		int syncId = player.playerScreenHandler.syncId;
		client.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, player);
		client.interactionManager.clickSlot(syncId, CHEST_SLOT, 0, SlotActionType.PICKUP, player);
		client.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, player);
		player.sendMessage(Text.literal(wearingElytra ? "Equipped chestplate" : "Equipped elytra"), true);
	}

	private static boolean isUsableElytra(ItemStack stack) {
		if (!stack.isOf(Items.ELYTRA)) {
			return false;
		}
		return !(stack.isDamageable() && stack.getDamage() >= stack.getMaxDamage() - 1);
	}

	private static boolean isChestplate(ItemStack stack) {
		if (stack.isOf(Items.ELYTRA)) {
			return false;
		}
		EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
	}
}
