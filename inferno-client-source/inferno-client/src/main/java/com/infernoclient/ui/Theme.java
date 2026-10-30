package com.infernoclient.ui;

import com.infernoclient.InfernoClient;

public final class Theme {
	public static final int BG = 0xFF0C0A09;
	public static final int PANEL = 0xFF151110;
	public static final int CARD = 0xFF1D1815;
	public static final int CARD_HOVER = 0xFF29211B;
	public static final int LINE = 0xFF30261E;
	public static final int TEXT = 0xFFF4EDE7;
	public static final int MUTED = 0xFF9B8E84;
	public static final int GOOD = 0xFF5BD68A;
	public static final int WARN = 0xFFFFC857;
	public static final int BAD = 0xFFFF5A5A;
	public static final int EMBER = 0xFFFFB347;

	private Theme() {
	}

	public static int accent() {
		return 0xFF000000 | InfernoClient.config().hud.accentColor;
	}

	public static int alpha(int color, int alpha) {
		return (alpha << 24) | (color & 0xFFFFFF);
	}

	public static int mix(int a, int b, float t) {
		t = Math.max(0.0f, Math.min(1.0f, t));
		int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
		int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
		return ((int) (aa + (ba - aa) * t) << 24)
			| ((int) (ar + (br - ar) * t) << 16)
			| ((int) (ag + (bg - ag) * t) << 8)
			| (int) (ab + (bb - ab) * t);
	}

	public static int hudText() {
		return 0xFF000000 | (InfernoClient.config().hud.color & 0xFFFFFF);
	}
}
