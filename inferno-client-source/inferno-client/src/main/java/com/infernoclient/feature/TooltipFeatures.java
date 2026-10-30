package com.infernoclient.feature;

import com.infernoclient.config.ClientConfig;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Adds a compact contents summary to container items such as shulker boxes. */
public final class TooltipFeatures {
	private static final int MAX_LINES = 6;

	private TooltipFeatures() {
	}

	public static void register(ClientConfig config) {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			if (!config.social.shulkerTooltips) {
				return;
			}
			ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
			if (container == null) {
				return;
			}

			Map<String, Integer> totals = new LinkedHashMap<>();
			List<ItemStack> contents = new ArrayList<>();
			container.streamNonEmpty().forEach(contents::add);
			for (ItemStack inside : contents) {
				totals.merge(inside.getName().getString(), inside.getCount(), Integer::sum);
			}

			lines.add(Text.literal("Contents").formatted(Formatting.GOLD));
			if (totals.isEmpty()) {
				lines.add(Text.literal("  Empty").formatted(Formatting.DARK_GRAY));
				return;
			}
			int shown = 0;
			for (Map.Entry<String, Integer> entry : totals.entrySet()) {
				if (shown == MAX_LINES) {
					lines.add(Text.literal("  +" + (totals.size() - MAX_LINES) + " more").formatted(Formatting.DARK_GRAY));
					break;
				}
				lines.add(Text.literal("  " + entry.getKey() + " x" + entry.getValue()).formatted(Formatting.GRAY));
				shown++;
			}
		});
	}
}
