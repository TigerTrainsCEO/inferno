package com.infernoclient.hud;

import com.infernoclient.config.ClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Replaces the vanilla crosshair with the configurable one when it is switched on. */
public final class CrosshairRenderer {
	private CrosshairRenderer() {
	}

	public static void register(ClientConfig config) {
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, original -> (context, tickCounter) -> {
			if (!config.crosshair.enabled) {
				original.render(context, tickCounter);
				return;
			}
			MinecraftClient client = MinecraftClient.getInstance();
			if (!client.options.getPerspective().isFirstPerson()) {
				return;
			}
			int cx = (context.getScaledWindowWidth() - 1) / 2;
			int cy = (context.getScaledWindowHeight() - 1) / 2;
			draw(context, config.crosshair, cx, cy);
		});
	}

	/** Draws the crosshair so that pixel (px, py) is its centre. Shared with the menu preview. */
	public static void draw(DrawContext ctx, ClientConfig.Crosshair c, int px, int py) {
		int alpha = (int) Math.round(ClientConfig.clamp(c.opacity, 0.1, 1.0) * 255.0);
		int color = (alpha << 24) | (c.color & 0xFFFFFF);
		int outline = (alpha << 24) | (c.outlineColor & 0xFFFFFF);
		int t = c.thickness;
		int o = c.outline ? c.outlineThickness : 0;
		int len = c.length;
		int gap = c.gap;

		if ("circle".equals(c.shape)) {
			drawRing(ctx, px, py, len, t, o, color, outline);
		} else {
			List<int[]> rects = new ArrayList<>();
			int sx = px - (t - 1) / 2;
			int sy = py - (t - 1) / 2;
			switch (c.shape) {
				case "cross" -> {
					rects.add(new int[]{sx - gap - len, sy, sx - gap, sy + t});
					rects.add(new int[]{sx + t + gap, sy, sx + t + gap + len, sy + t});
					rects.add(new int[]{sx, sy - gap - len, sx + t, sy - gap});
					rects.add(new int[]{sx, sy + t + gap, sx + t, sy + t + gap + len});
				}
				case "square" -> {
					int r = len;
					rects.add(new int[]{px - r, py - r, px + r + 1, py - r + t});
					rects.add(new int[]{px - r, py + r + 1 - t, px + r + 1, py + r + 1});
					rects.add(new int[]{px - r, py - r + t, px - r + t, py + r + 1 - t});
					rects.add(new int[]{px + r + 1 - t, py - r + t, px + r + 1, py + r + 1 - t});
				}
				default -> {
				}
			}
			if (c.centerDot || "dot".equals(c.shape)) {
				int d = c.dotSize;
				int dx = px - (d - 1) / 2;
				int dy = py - (d - 1) / 2;
				rects.add(new int[]{dx, dy, dx + d, dy + d});
			}
			if (o > 0) {
				for (int[] r : rects) {
					ctx.fill(r[0] - o, r[1] - o, r[2] + o, r[3] + o, outline);
				}
			}
			for (int[] r : rects) {
				ctx.fill(r[0], r[1], r[2], r[3], color);
			}
		}

		if ("circle".equals(c.shape) && c.centerDot) {
			int d = c.dotSize;
			int dx = px - (d - 1) / 2;
			int dy = py - (d - 1) / 2;
			if (o > 0) {
				ctx.fill(dx - o, dy - o, dx + d + o, dy + d + o, outline);
			}
			ctx.fill(dx, dy, dx + d, dy + d, color);
		}
	}

	private static void drawRing(DrawContext ctx, int px, int py, int radius, int thickness, int outlineSize, int color, int outline) {
		int reach = radius + thickness + outlineSize + 1;
		double half = thickness / 2.0;
		for (int pass = 0; pass < 2; pass++) {
			if (pass == 0 && outlineSize == 0) {
				continue;
			}
			double limit = pass == 0 ? half + outlineSize : half;
			int col = pass == 0 ? outline : color;
			for (int dy = -reach; dy <= reach; dy++) {
				for (int dx = -reach; dx <= reach; dx++) {
					double dist = Math.sqrt(dx * dx + dy * dy);
					if (Math.abs(dist - radius) <= limit) {
						ctx.fill(px + dx, py + dy, px + dx + 1, py + dy + 1, col);
					}
				}
			}
		}
	}
}
