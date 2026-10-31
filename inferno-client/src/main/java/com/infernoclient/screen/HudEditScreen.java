package com.infernoclient.screen;

import com.infernoclient.config.ClientConfig;
import com.infernoclient.hud.ClientHud;
import com.infernoclient.ui.Theme;
import com.infernoclient.ui.Ui;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** Drag HUD elements anywhere; scroll over one to resize it. */
public final class HudEditScreen extends Screen {
	private final ClientConfig cfg;
	private final Screen parent;
	private final Ui.Regions regions = new Ui.Regions();
	private String dragId;
	private int grabX;
	private int grabY;

	public HudEditScreen(ClientConfig cfg, Screen parent) {
		super(Text.translatable("screen.inferno_client.hud_edit"));
		this.cfg = cfg;
		this.parent = parent;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		cfg.save();
		client.setScreen(parent);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		regions.clear();
		ctx.fill(0, 0, width, height, 0x90000000);
		ClientHud.render(ctx, client, true);

		for (String id : ClientHud.ELEMENTS) {
			int[] b = ClientHud.bounds(id);
			if (b == null) continue;
			boolean over = Ui.hover(mouseX, mouseY, b[0], b[1], b[2], b[3]) || id.equals(dragId);
			ctx.drawBorder(b[0] - 2, b[1] - 2, b[2] + 4, b[3] + 4, over ? Theme.accent() : Theme.alpha(Theme.accent(), 110));
			Ui.shadow(ctx, label(id), b[0], b[1] - 12, over ? Theme.TEXT : Theme.MUTED);
		}

		Ui.centered(ctx, "Drag to move  -  scroll to resize  -  Esc to finish", width / 2, 14, Theme.TEXT);
		Ui.button(ctx, regions, "Done", width / 2 - 70, 28, 64, 18, mouseX, mouseY, true, this::close);
		Ui.button(ctx, regions, "Reset", width / 2 + 6, 28, 64, 18, mouseX, mouseY, false, this::reset);
	}

	private static String label(String id) {
		return switch (id) {
			case "info" -> "Info";
			case "keystrokes" -> "Keystrokes";
			case "armor" -> "Armor";
			case "potion" -> "Effects";
			case "inventory" -> "Inventory";
			default -> "Held item";
		};
	}

	private void reset() {
		for (String id : ClientHud.ELEMENTS) {
			ClientHud.setPosition(id, -1, -1);
			ClientHud.setScale(id, 1.0);
		}
		cfg.hud.x = 8;
		cfg.hud.y = 8;
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0) {
			if (regions.press(click.x(), click.y())) {
				return true;
			}
			String[] ids = ClientHud.ELEMENTS;
			for (int i = ids.length - 1; i >= 0; i--) {
				int[] b = ClientHud.bounds(ids[i]);
				if (b != null && Ui.hover(click.x(), click.y(), b[0], b[1], b[2], b[3])) {
					dragId = ids[i];
					grabX = (int) click.x() - b[0];
					grabY = (int) click.y() - b[1];
					return true;
				}
			}
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (dragId != null) {
			int x = Math.max(0, Math.min(width - 10, (int) click.x() - grabX));
			int y = Math.max(0, Math.min(height - 10, (int) click.y() - grabY));
			ClientHud.setPosition(dragId, x, y);
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		dragId = null;
		cfg.save();
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		for (String id : ClientHud.ELEMENTS) {
			int[] b = ClientHud.bounds(id);
			if (b != null && Ui.hover(mouseX, mouseY, b[0], b[1], b[2], b[3])) {
				ClientHud.setScale(id, Math.round((ClientHud.getScale(id) + Math.signum(vertical) * 0.1) * 10.0) / 10.0);
				return true;
			}
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}
}
