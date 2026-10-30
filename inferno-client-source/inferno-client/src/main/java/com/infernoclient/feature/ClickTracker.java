package com.infernoclient.feature;

import java.util.ArrayDeque;

/** Counts mouse clicks in a rolling one second window for the CPS readout. */
public final class ClickTracker {
	private static final ArrayDeque<Long> LEFT = new ArrayDeque<>();
	private static final ArrayDeque<Long> RIGHT = new ArrayDeque<>();

	private ClickTracker() {
	}

	public static synchronized void record(int button, int action) {
		if (action != 1) {
			return;
		}
		long now = System.currentTimeMillis();
		if (button == 0) {
			LEFT.addLast(now);
		} else if (button == 1) {
			RIGHT.addLast(now);
		}
		prune(now);
	}

	public static synchronized int leftCps() {
		prune(System.currentTimeMillis());
		return LEFT.size();
	}

	public static synchronized int rightCps() {
		prune(System.currentTimeMillis());
		return RIGHT.size();
	}

	private static void prune(long now) {
		while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000L) {
			LEFT.removeFirst();
		}
		while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000L) {
			RIGHT.removeFirst();
		}
	}
}
