package com.infernoclient.screen;

import com.infernoclient.InfernoClient;
import com.infernoclient.config.ClientConfig;
import com.infernoclient.ui.Theme;
import com.infernoclient.ui.Ui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public final class InfernoHomeScreen extends Screen {
	private static boolean classic;

	private final ClientConfig cfg;
	private final Ui.Regions regions = new Ui.Regions();
	private final String gameVersion;

	public InfernoHomeScreen(ClientConfig cfg) {
		super(Text.translatable("screen.inferno_client.home"));
		this.cfg = cfg;
		this.gameVersion = FabricLoader.getInstance().getModContainer("minecraft")
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
	}

	public static boolean classicRequested() {
		return classic;
	}

	public static void resetClassicRequest() {
		classic = false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		regions.clear();
		ctx.fillGradient(0, 0, width, height, 0xFF160D08, 0xFF050303);
		int glowY = height - height / 3;
		ctx.fillGradient(0, glowY, width, height, Theme.alpha(Theme.accent(), 0), Theme.alpha(Theme.accent(), 60));
		Ui.embers(ctx, width, height, 90);

		int cx = width / 2;
		int top = Math.max(16, height / 2 - 118);

		int flameScale = 4;
		Ui.flame(ctx, cx - Ui.flameWidth(flameScale) / 2, top, flameScale);
		int ty = top + Ui.flameHeight(flameScale) + 8;
		String name = "INFERNO";
		float s = 3.0f;
		int nameW = (int) (Ui.width(name) * s);
		Ui.scaled(ctx, name, cx - nameW / 2 + 2, ty + 2, Theme.alpha(Theme.accent(), 160), s, false);
		Ui.scaled(ctx, name, cx - nameW / 2, ty, Theme.TEXT, s, false);
		Ui.centered(ctx, "CLIENT " + InfernoClient.VERSION + (gameVersion.isEmpty() ? "" : "  ·  Minecraft " + gameVersion), cx, ty + 32, Theme.MUTED);

		int bw = 210;
		int bx = cx - bw / 2;
		int by = ty + 52;
		Ui.button(ctx, regions, "Singleplayer", bx, by, bw, 24, mouseX, mouseY, true, () -> client.setScreen(new SelectWorldScreen(this)));
		by += 30;
		Ui.button(ctx, regions, "Multiplayer", bx, by, bw, 24, mouseX, mouseY, true, () -> client.setScreen(new MultiplayerScreen(this)));
		by += 30;
		Ui.button(ctx, regions, "Inferno Menu", bx, by, bw, 24, mouseX, mouseY, false, () -> client.setScreen(new InfernoMenuScreen(cfg, this)));
		by += 30;
		int half = (bw - 6) / 2;
		Ui.button(ctx, regions, "Options", bx, by, half, 22, mouseX, mouseY, false, () -> client.setScreen(new OptionsScreen(this, client.options)));
		Ui.button(ctx, regions, "Quit", bx + half + 6, by, half, 22, mouseX, mouseY, false, () -> client.scheduleStop());
		by += 28;
		Ui.button(ctx, regions, "Classic menu", bx, by, bw, 18, mouseX, mouseY, false, () -> {
			classic = true;
			client.setScreen(new TitleScreen());
		});

		if (width >= 700) {
			int cardW = Math.min(190, (width - bw) / 2 - 30);
			int cardY = top + 10;
			if (cfg.home.profile) {
				profileCard(ctx, 24, cardY, cardW);
			}
			if (cfg.home.changelog) {
				changelogCard(ctx, width - 24 - cardW, cardY, cardW);
			}
			if (cfg.home.news) {
				tipsCard(ctx, width - 24 - cardW, cardY + 112, cardW);
			}
			if (cfg.home.stats) {
				statsCard(ctx, 24, cardY + 78, cardW);
			}
		}
	}

	private void card(DrawContext ctx, int x, int y, int w, int h, String title) {
		Ui.round(ctx, x, y, w, h, 0xB0120D0A);
		ctx.fill(x, y + 1, x + 2, y + h - 1, Theme.accent());
		Ui.text(ctx, title, x + 10, y + 8, Theme.accent());
	}

	private void profileCard(DrawContext ctx, int x, int y, int w) {
		card(ctx, x, y, w, 62, "PLAYER");
		String user = client.getSession().getUsername();
		Ui.round(ctx, x + 10, y + 24, 28, 28, Theme.accent());
		Ui.centered(ctx, user.isEmpty() ? "?" : user.substring(0, 1).toUpperCase(), x + 24, y + 34, 0xFF1A0E08);
		Ui.text(ctx, user, x + 46, y + 28, Theme.TEXT);
		Ui.text(ctx, "Ready to fight", x + 46, y + 40, Theme.MUTED);
	}

	private void statsCard(DrawContext ctx, int x, int y, int w) {
		int enabled = 0;
		ClientConfig.Hud h = cfg.hud;
		boolean[] flags = {h.fps, h.cps, h.keystrokes, h.armor, h.potionEffects, cfg.visual.fullbright, cfg.visual.lowFire, cfg.visual.lowShield, cfg.zoom.enabled, cfg.movement.toggleSprint, cfg.movement.autoRespawn};
		for (boolean f : flags) if (f) enabled++;
		card(ctx, x, y, w, 62, "LOADOUT");
		Ui.text(ctx, enabled + " of " + flags.length + " key modules on", x + 10, y + 26, Theme.TEXT);
		Ui.text(ctx, "Style: " + cfg.hud.theme, x + 10, y + 40, Theme.MUTED);
	}

	private void changelogCard(DrawContext ctx, int x, int y, int w) {
		card(ctx, x, y, w, 100, "WHAT'S NEW");
		String[] lines = {"Auto Respawn retries", "Fullbright rebuilt", "Low Fire / Shield fixed", "New HUD + menu", "New home screen"};
		int ly = y + 24;
		for (String l : lines) {
			Ui.text(ctx, "+ " + l, x + 10, ly, Theme.TEXT);
			ly += 13;
		}
	}

	private void tipsCard(DrawContext ctx, int x, int y, int w) {
		card(ctx, x, y, w, 76, "KEYS");
		Ui.text(ctx, "Right Shift  menu", x + 10, y + 24, Theme.TEXT);
		Ui.text(ctx, "C  zoom", x + 10, y + 37, Theme.TEXT);
		Ui.text(ctx, "Left Alt  freelook", x + 10, y + 50, Theme.TEXT);
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0 && regions.press(click.x(), click.y())) {
			return true;
		}
		return super.mouseClicked(click, doubled);
	}
}
