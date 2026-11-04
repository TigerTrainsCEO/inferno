/**
 * "Tune my PC": picks how much memory each PvP profile should get from the computer's real
 * hardware, so a 4 GB laptop and a 32 GB desktop do not run the same settings.
 */

export interface Hardware {
	/** Total system memory in MB. */
	totalMemoryMb: number
	/** Logical CPU threads. */
	threads: number
}

export type Tier = 'low' | 'mid' | 'high'

export interface TuneProfile {
	id: string
	name: string
	/** The memory this profile is normally built with, in MB. */
	baselineMb: number
	/** What the instance has right now, in MB. */
	currentMb: number
}

export interface TunePlan {
	id: string
	name: string
	currentMb: number
	recommendedMb: number
	changed: boolean
}

export interface TuneResult {
	tier: Tier
	summary: string
	plans: TunePlan[]
}

const STEP = 256
const MIN_HEAP_MB = 1536

export function tierFor(hw: Hardware): Tier {
	if (hw.totalMemoryMb < 6144 || hw.threads <= 4) return 'low'
	if (hw.totalMemoryMb >= 15360 && hw.threads >= 8) return 'high'
	return 'mid'
}

/** Most heap the game may take: at most half of the RAM, always leaving 2 GB for everything else. */
export function heapCeilingMb(totalMemoryMb: number): number {
	return Math.max(MIN_HEAP_MB, Math.min(totalMemoryMb * 0.5, totalMemoryMb - 2048))
}

export function recommendMemoryMb(hw: Hardware, baselineMb: number): number {
	const capped = Math.min(baselineMb, heapCeilingMb(hw.totalMemoryMb))
	return Math.max(MIN_HEAP_MB, Math.floor(capped / STEP) * STEP)
}

export function planTuning(hw: Hardware, profiles: TuneProfile[]): TuneResult {
	const tier = tierFor(hw)
	const plans = profiles.map((p) => {
		const recommendedMb = recommendMemoryMb(hw, p.baselineMb)
		return {
			id: p.id,
			name: p.name,
			currentMb: p.currentMb,
			recommendedMb,
			changed: recommendedMb !== p.currentMb,
		}
	})
	const gb = Math.round(hw.totalMemoryMb / 1024)
	const hardware = `${gb} GB RAM, ${hw.threads} threads`
	const summary =
		tier === 'low'
			? `${hardware}: a lighter setup. Memory is capped to keep your system responsive. Turn on Adaptive Render Distance in the Inferno menu to hold your FPS.`
			: tier === 'high'
				? `${hardware}: plenty of headroom. Every profile gets its full memory.`
				: `${hardware}: a balanced setup. Profiles get their normal memory.`
	return { tier, summary, plans }
}
