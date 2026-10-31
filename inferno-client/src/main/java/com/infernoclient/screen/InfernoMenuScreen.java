package com.infernoclient.screen;

import com.infernoclient.InfernoClient;
import com.infernoclient.config.ClientConfig;
import com.infernoclient.feature.FriendManager;
import com.infernoclient.ui.Modules;
import com.infernoclient.ui.Theme;
import com.infernoclient.ui.Ui;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class InfernoMenuScreen extends Screen {
	private static final String[] TABS = {"Favorites", "HUD", "Gameplay", "Visual", "Social", "Friends", "Appearance"};
	private static final String[] THEMES = {"inferno", "glass", "minimal"};
	private static final int[] ACCENTS = {0xFF5A1F, 0xFF8A1F, 0xFFC247, 0xE8431A, 0x4FA3FF, 0x5BD68A, 0xC05BFF, 0xF2F2F2};
	private static String tab = "HUD";

	private final ClientConfig cfg;
	private final Screen parent;
	private final Ui.Regions regions = new Ui.Regions();
	private final List<Modules.Module> modules;
	private Modules.Module selected;
	private int scroll;
	private int maxScroll;
	private TextFieldWidget friendField;

	public InfernoMenuScreen(ClientConfig cfg, Screen parent) {
		super(Text.translatable("screen.inferno_client.menu"));
		this.cfg = cfg;
		this.parent = parent;
		this.modules = Modules.all(cfg);
	}

	@Override
	protected void init() {
		friendField = new TextFieldWidget(textRenderer, 0, 0, 140, 18, Text.literal("Player name"));
		friendField.setMaxLength(16);
		friendField.setPlaceholder(Text.literal("Player name"));
		friendField.setVisible(false);
		addDrawableChild(friendField);
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

	private boolean isFavorite(Modules.Module m) {
		return cfg.ui.favoriteModules.contains(m.id());
	}

	private void toggleFavorite(Modules.Module m) {
		if (!cfg.ui.favoriteModules.remove(m.id())) {
			cfg.ui.favoriteModules.add(m.id());
		}
		cfg.save();
	}

	private List<Modules.Module> visibleModules() {
		List<Modules.Module> out = new ArrayList<>();
		for (Modules.Module m : modules) {
			if (tab.equals("Favorites") ? isFavorite(m) : m.category().equals(tab)) {
				out.add(m);
			}
		}
		return out;
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		regions.clear();
		boolean inGame = client.world != null;
		if (inGame) {
			ctx.fill(0, 0, width, height, 0xC00C0A09);
		} else {
			ctx.fill(0, 0, width, height, Theme.BG);
			Ui.embers(ctx, width, height, 40);
		}

		int pw = Math.min(width - 24, 780);
		int ph = Math.min(height - 24, 440);
		int px = (width - pw) / 2;
		int py = (height - ph) / 2;
		Ui.round(ctx, px, py, pw, ph, Theme.PANEL);
		Ui.border(ctx, px, py, pw, ph, Theme.LINE);

		int sideW = 124;
		Ui.round(ctx, px + 1, py + 1, sideW, ph - 2, 0xFF110D0B);
		Ui.flame(ctx, px + 12, py + 12, 2);
		Ui.scaled(ctx, "INFERNO", px + 38, py + 12, Theme.TEXT, 1.0f, true);
		Ui.text(ctx, "CLIENT " + InfernoClient.VERSION, px + 38, py + 24, Theme.MUTED);

		int ty = py + 48;
		for (String t : TABS) {
			boolean active = t.equals(tab);
			boolean over = Ui.hover(mouseX, mouseY, px + 8, ty, sideW - 16, 20);
			if (active) {
				Ui.round(ctx, px + 8, ty, sideW - 16, 20, Theme.CARD_HOVER);
				ctx.fill(px + 8, ty + 3, px + 10, ty + 17, Theme.accent());
			} else if (over) {
				Ui.round(ctx, px + 8, ty, sideW - 16, 20, Theme.CARD);
			}
			Ui.text(ctx, t, px + 18, ty + 6, active ? Theme.TEXT : Theme.MUTED);
			final String target = t;
			regions.add(px + 8, ty, sideW - 16, 20, () -> {
				tab = target;
				scroll = 0;
				selected = null;
			});
			ty += 22;
		}
		Ui.button(ctx, regions, "Close", px + 10, py + ph - 30, sideW - 20, 20, mouseX, mouseY, false, this::close);

		int cx = px + sideW + 12;
		int cw = pw - sideW - 24;
		friendField.setVisible(false);

		if (tab.equals("Friends")) {
			renderFriends(ctx, cx, py + 14, cw, ph - 28, mouseX, mouseY);
		} else if (tab.equals("Appearance")) {
			renderAppearance(ctx, cx, py + 14, cw, ph - 28, mouseX, mouseY);
		} else {
			renderModules(ctx, cx, py + 14, cw, ph - 28, mouseX, mouseY);
		}
	}

	private void renderModules(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
		boolean detail = w > 420;
		int detailW = detail ? 190 : 0;
		int gridW = w - detailW - (detail ? 12 : 0);

		Ui.scaled(ctx, tab, x, y, Theme.TEXT, 1.5f, false);
		List<Modules.Module> list = visibleModules();
		Ui.text(ctx, list.size() + " modules", x, y + 18, Theme.MUTED);

		int gy = y + 36;
		int gh = h - 36;
		int cardW = 150;
		int cols = Math.max(1, (gridW + 8) / (cardW + 8));
		cardW = (gridW - (cols - 1) * 8) / cols;
		int cardH = 48;
		int rows = (list.size() + cols - 1) / cols;
		maxScroll = Math.max(0, rows * (cardH + 8) - gh);
		scroll = Math.max(0, Math.min(maxScroll, scroll));

		if (list.isEmpty()) {
			Ui.text(ctx, tab.equals("Favorites") ? "Star a module to pin it here." : "Nothing here.", x, gy + 6, Theme.MUTED);
		}

		ctx.enableScissor(x, gy, x + gridW, gy + gh);
		for (int i = 0; i < list.size(); i++) {
			Modules.Module m = list.get(i);
			int cx = x + (i % cols) * (cardW + 8);
			int cy = gy + (i / cols) * (cardH + 8) - scroll;
			if (cy + cardH < gy || cy > gy + gh) continue;
			boolean over = Ui.hover(mx, my, cx, cy, cardW, cardH) && Ui.hover(mx, my, x, gy, gridW, gh);
			boolean sel = m == selected;
			Ui.round(ctx, cx, cy, cardW, cardH, over || sel ? Theme.CARD_HOVER : Theme.CARD);
			if (sel) Ui.border(ctx, cx, cy, cardW, cardH, Theme.accent());
			boolean on = m.get().getAsBoolean();
			ctx.fill(cx + 1, cy + cardH - 2, cx + cardW - 1, cy + cardH - 1, on ? Theme.accent() : Theme.LINE);
			Ui.text(ctx, m.name(), cx + 8, cy + 8, Theme.TEXT);
			Ui.text(ctx, on ? "On" : "Off", cx + 8, cy + 24, on ? Theme.GOOD : Theme.MUTED);
			Ui.toggle(ctx, regions, cx + cardW - 34, cy + 8, on, () -> {
				m.set().accept(!m.get().getAsBoolean());
				cfg.save();
			});
			final Modules.Module target = m;
			if (Ui.hover(mx, my, x, gy, gridW, gh)) {
				regions.add(cx, cy, cardW - 40, cardH, () -> selected = target);
			}
		}
		ctx.disableScissor();

		if (maxScroll > 0) {
			int trackH = gh;
			int thumb = Math.max(20, trackH * gh / (gh + maxScroll));
			int ty = gy + (trackH - thumb) * scroll / maxScroll;
			Ui.round(ctx, x + gridW + 2, ty, 3, thumb, Theme.LINE);
		}

		if (detail) {
			int dx = x + gridW + 12;
			Ui.round(ctx, dx, y, detailW, h, Theme.CARD);
			if (selected == null) {
				Ui.text(ctx, "Select a module", dx + 12, y + 14, Theme.MUTED);
				Ui.text(ctx, "to see its settings.", dx + 12, y + 26, Theme.MUTED);
			} else {
				Modules.Module m = selected;
				Ui.scaled(ctx, m.name(), dx + 12, y + 12, Theme.TEXT, 1.2f, false);
				List<net.minecraft.text.OrderedText> wrapped = textRenderer.wrapLines(Text.literal(m.description()), detailW - 24);
				int ly = y + 32;
				for (net.minecraft.text.OrderedText line : wrapped) {
					ctx.drawText(textRenderer, line, dx + 12, ly, Theme.MUTED, false);
					ly += 10;
				}
				ly += 8;
				boolean on = m.get().getAsBoolean();
				Ui.text(ctx, "Enabled", dx + 12, ly + 2, Theme.TEXT);
				Ui.toggle(ctx, regions, dx + detailW - 38, ly, on, () -> {
					m.set().accept(!m.get().getAsBoolean());
					cfg.save();
				});
				ly += 24;
				for (Modules.Slider s : m.sliders()) {
					Ui.slider(ctx, regions, s.label(), dx + 12, ly, detailW - 24, s.get().getAsDouble(), s.min(), s.max(), s.format(), s.set());
					ly += 34;
				}
				Ui.button(ctx, regions, isFavorite(m) ? "Unfavorite" : "Add to Favorites", dx + 12, y + h - 30, detailW - 24, 20, mx, my, false, () -> toggleFavorite(m));
			}
		}
	}

	private void renderFriends(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
		Ui.scaled(ctx, "Friends", x, y, Theme.TEXT, 1.5f, false);
		Ui.text(ctx, "Friends are marked in the player list. Type a name to add someone.", x, y + 18, Theme.MUTED);

		friendField.setVisible(true);
		friendField.setX(x);
		friendField.setY(y + 40);
		friendField.setWidth(Math.min(180, w - 90));
		friendField.render(ctx, mx, my, 0);
		Ui.button(ctx, regions, "Add", x + friendField.getWidth() + 8, y + 40, 60, 18, mx, my, true, () -> {
			if (FriendManager.add(friendField.getText(), null)) {
				friendField.setText("");
			}
		});

		int ly = y + 70;
		List<ClientConfig.FriendEntry> entries = new ArrayList<>(cfg.social.friendEntries);
		if (entries.isEmpty()) {
			Ui.text(ctx, "No friends added yet.", x, ly, Theme.MUTED);
		}
		for (ClientConfig.FriendEntry e : entries) {
			if (ly > y + h - 20) break;
			Ui.round(ctx, x, ly, w, 22, Theme.CARD);
			Ui.text(ctx, e.name == null ? "?" : e.name, x + 10, ly + 7, Theme.TEXT);
			Ui.button(ctx, regions, "Remove", x + w - 64, ly + 2, 60, 18, mx, my, false, () -> FriendManager.remove(e));
			ly += 26;
		}
	}

	private void renderAppearance(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
		Ui.scaled(ctx, "Appearance", x, y, Theme.TEXT, 1.5f, false);

		int ly = y + 32;
		Ui.text(ctx, "HUD style", x, ly, Theme.MUTED);
		int bx = x;
		for (String t : THEMES) {
			boolean on = t.equals(cfg.hud.theme);
			Ui.button(ctx, regions, Character.toUpperCase(t.charAt(0)) + t.substring(1), bx, ly + 14, 84, 20, mx, my, on, () -> {
				cfg.hud.theme = t;
				cfg.save();
			});
			bx += 90;
		}

		ly += 52;
		Ui.text(ctx, "Accent color", x, ly, Theme.MUTED);
		bx = x;
		for (int color : ACCENTS) {
			boolean on = (cfg.hud.accentColor & 0xFFFFFF) == color;
			Ui.round(ctx, bx, ly + 14, 24, 24, 0xFF000000 | color);
			if (on) Ui.border(ctx, bx - 2, ly + 12, 28, 28, 0xFFFFFFFF);
			regions.add(bx, ly + 14, 24, 24, () -> {
				cfg.hud.accentColor = color;
				cfg.save();
			});
			bx += 32;
		}

		ly += 58;
		Ui.text(ctx, "Inferno home screen", x, ly + 2, Theme.TEXT);
		Ui.toggle(ctx, regions, x + 150, ly, cfg.home.enabled, () -> {
			cfg.home.enabled = !cfg.home.enabled;
			cfg.save();
		});

		ly += 30;
		if (client.world != null) {
			Ui.button(ctx, regions, "Edit HUD layout", x, ly, 130, 22, mx, my, true, () -> client.setScreen(new HudEditScreen(cfg, this)));
		} else {
			Ui.text(ctx, "Join a world to edit the HUD layout.", x, ly + 6, Theme.MUTED);
		}
	}

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0 && regions.press(click.x(), click.y())) {
			return true;
		}
		return super.mouseClicked(click, doubled);
	}

	@Override
	public boolean mouseDragged(Click click, double offsetX, double offsetY) {
		if (regions.drag(click.x())) {
			return true;
		}
		return super.mouseDragged(click, offsetX, offsetY);
	}

	@Override
	public boolean mouseReleased(Click click) {
		if (regions.release()) {
			cfg.save();
		}
		return super.mouseReleased(click);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (vertical * 18)));
		return true;
	}
}
