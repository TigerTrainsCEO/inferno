package com.infernoclient.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;

/** Small immediate-mode toolkit: draw every frame, and register click regions as you go. */
public final class Ui {
	private static final String[] FLAME = {
		"....#.....",
		"...##.....",
		"...###..#.",
		"..####.##.",
		".###o####.",
		".##ooo###.",
		"###ooyoo##",
		"##ooyyyoo#",
		"##oyyyyyo#",
		"##oyyyyyo#",
		".##oyyyo##",
		"..##ooo##.",
	};

	private Ui() {
	}

	public static TextRenderer font() {
		return MinecraftClient.getInstance().textRenderer;
	}

	public static int width(String text) {
		return font().getWidth(text);
	}

	public static boolean hover(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public static void rect(DrawContext c, int x, int y, int w, int h, int color) {
		c.fill(x, y, x + w, y + h, color);
	}

	/** Rectangle with notched corners, which reads as softly rounded at GUI scale. */
	public static void round(DrawContext c, int x, int y, int w, int h, int color) {
		c.fill(x + 1, y, x + w - 1, y + h, color);
		c.fill(x, y + 1, x + 1, y + h - 1, color);
		c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	public static void border(DrawContext c, int x, int y, int w, int h, int color) {
		c.fill(x, y, x + w, y + 1, color);
		c.fill(x, y + h - 1, x + w, y + h, color);
		c.fill(x, y + 1, x + 1, y + h - 1, color);
		c.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	public static void text(DrawContext c, String s, int x, int y, int color) {
		c.drawText(font(), s, x, y, color, false);
	}

	public static void shadow(DrawContext c, String s, int x, int y, int color) {
		c.drawText(font(), s, x, y, color, true);
	}

	public static void centered(DrawContext c, String s, int cx, int y, int color) {
		c.drawText(font(), s, cx - width(s) / 2, y, color, false);
	}

	public static void scaled(DrawContext c, String s, int x, int y, int color, float scale, boolean shadow) {
		c.getMatrices().pushMatrix();
		c.getMatrices().translate(x, y);
		c.getMatrices().scale(scale, scale);
		c.drawText(font(), s, 0, 0, color, shadow);
		c.getMatrices().popMatrix();
	}

	public static void flame(DrawContext c, int x, int y, int scale) {
		for (int row = 0; row < FLAME.length; row++) {
			String line = FLAME[row];
			for (int col = 0; col < line.length(); col++) {
				int color;
				switch (line.charAt(col)) {
					case '#' -> color = 0xFFE8431A;
					case 'o' -> color = 0xFFFF8A1F;
					case 'y' -> color = 0xFFFFD36B;
					default -> {
						continue;
					}
				}
				c.fill(x + col * scale, y + row * scale, x + (col + 1) * scale, y + (row + 1) * scale, color);
			}
		}
	}

	public static int flameWidth(int scale) {
		return FLAME[0].length() * scale;
	}

	public static int flameHeight(int scale) {
		return FLAME.length * scale;
	}

	/** Rising embers, derived from the clock so no state is needed. */
	public static void embers(DrawContext c, int w, int h, int count) {
		double t = System.currentTimeMillis() / 1000.0;
		for (int i = 0; i < count; i++) {
			double r1 = frac(Math.sin(i * 12.9898) * 43758.5453);
			double r2 = frac(Math.sin(i * 78.233) * 12345.6789);
			double r3 = frac(Math.sin(i * 39.425) * 9876.5432);
			double speed = 10 + r2 * 28;
			double y = h - ((t * speed + r3 * h) % (h + 20));
			double x = r1 * w + Math.sin(t * (0.4 + r2) + i) * 12;
			int size = r3 > 0.8 ? 2 : 1;
			float life = (float) Math.max(0.0, Math.min(1.0, y / h));
			int color = Theme.mix(Theme.EMBER, Theme.accent(), 1.0f - life);
			int a = (int) (60 + 150 * life * (0.5 + 0.5 * Math.sin(t * 3 + i)));
			c.fill((int) x, (int) y, (int) x + size, (int) y + size, Theme.alpha(color, Math.max(20, Math.min(220, a))));
		}
	}

	private static double frac(double v) {
		return v - Math.floor(v);
	}

	public record Region(int x, int y, int w, int h, Runnable click, DoubleConsumer drag) {
		boolean contains(double mx, double my) {
			return hover(mx, my, x, y, w, h);
		}
	}

	public static final class Regions {
		private final List<Region> list = new ArrayList<>();
		private Region dragging;

		public void clear() {
			list.clear();
		}

		public void add(int x, int y, int w, int h, Runnable click) {
			list.add(new Region(x, y, w, h, click, null));
		}

		public void addDrag(int x, int y, int w, int h, DoubleConsumer drag) {
			list.add(new Region(x, y, w, h, null, drag));
		}

		public boolean press(double mx, double my) {
			for (int i = list.size() - 1; i >= 0; i--) {
				Region r = list.get(i);
				if (r.contains(mx, my)) {
					if (r.drag != null) {
						dragging = r;
						r.drag.accept(mx);
					}
					if (r.click != null) {
						r.click.run();
					}
					return true;
				}
			}
			return false;
		}

		public boolean drag(double mx) {
			if (dragging == null) {
				return false;
			}
			dragging.drag.accept(mx);
			return true;
		}

		public boolean release() {
			boolean was = dragging != null;
			dragging = null;
			return was;
		}
	}

	public static void button(DrawContext c, Regions regions, String label, int x, int y, int w, int h, int mx, int my, boolean primary, Runnable action) {
		boolean over = hover(mx, my, x, y, w, h);
		int bg = primary ? (over ? Theme.mix(Theme.accent(), 0xFFFFFFFF, 0.15f) : Theme.accent()) : (over ? Theme.CARD_HOVER : Theme.CARD);
		round(c, x, y, w, h, bg);
		if (!primary) {
			c.fill(x + 1, y + h - 1, x + w - 1, y + h, over ? Theme.accent() : Theme.LINE);
		}
		centered(c, label, x + w / 2, y + (h - 8) / 2, primary ? 0xFF1A0E08 : Theme.TEXT);
		regions.add(x, y, w, h, action);
	}

	public static void toggle(DrawContext c, Regions regions, int x, int y, boolean on, Runnable action) {
		round(c, x, y, 26, 12, on ? Theme.accent() : 0xFF3A302A);
		int knob = on ? x + 15 : x + 1;
		round(c, knob, y + 1, 10, 10, 0xFFFFFFFF);
		regions.add(x - 2, y - 2, 30, 16, action);
	}

	public static void slider(DrawContext c, Regions regions, String label, int x, int y, int w, double value, double min, double max, String format, java.util.function.DoubleConsumer setter) {
		String shown = String.format(format, value);
		text(c, label, x, y, Theme.MUTED);
		text(c, shown, x + w - width(shown), y, Theme.TEXT);
		int ty = y + 14;
		round(c, x, ty, w, 4, 0xFF3A302A);
		double t = (value - min) / (max - min);
		t = Math.max(0.0, Math.min(1.0, t));
		round(c, x, ty, Math.max(2, (int) (w * t)), 4, Theme.accent());
		round(c, x + (int) (w * t) - 3, ty - 3, 7, 10, 0xFFFFFFFF);
		regions.addDrag(x - 3, ty - 5, w + 6, 14, mx -> {
			double f = Math.max(0.0, Math.min(1.0, (mx - x) / w));
			setter.accept(min + f * (max - min));
		});
	}
}
