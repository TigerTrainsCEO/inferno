/**
 * Crash Doctor: reads a Minecraft log or crash report and says what went wrong in plain words,
 * with fixes the launcher can apply itself. Pure text in, findings out, so it is fully testable.
 */

export type Fix =
	| { kind: 'memory'; memoryMb: number; label: string }
	| { kind: 'disable-mod'; modId: string; label: string }

export interface Finding {
	id: string
	severity: 'error' | 'warning' | 'info'
	title: string
	detail: string
	fixes: Fix[]
}

export interface DoctorContext {
	/** The instance's current max heap in MB. */
	memoryMb: number
	/** Total system memory in MB. */
	systemMemoryMb: number
}

const MEMORY_STEP = 256

function roundDown(mb: number): number {
	return Math.floor(mb / MEMORY_STEP) * MEMORY_STEP
}

function roundUp(mb: number): number {
	return Math.ceil(mb / MEMORY_STEP) * MEMORY_STEP
}

/** Largest heap that still leaves the operating system room to breathe. */
export function memoryCeilingMb(systemMemoryMb: number): number {
	return Math.max(2048, roundDown(Math.min(systemMemoryMb * 0.6, systemMemoryMb - 2048)))
}

function prettyModName(id: string): string {
	return id.replace(/[_-]+/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase())
}

function uniq<T>(items: T[]): T[] {
	return Array.from(new Set(items))
}

function modIdsFromMixins(text: string): string[] {
	const ids: string[] = []
	for (const m of text.matchAll(/from mod ([A-Za-z0-9_.-]+)/g)) ids.push(m[1])
	for (const m of text.matchAll(/([A-Za-z0-9_.-]+)\.mixins?\.json/g)) ids.push(m[1])
	for (const m of text.matchAll(/Mixin config ([A-Za-z0-9_.-]+?)(?:\.mixins?\.json)? /gi)) ids.push(m[1])
	return uniq(
		ids
			.map((id) => id.replace(/^mixins\./, '').replace(/\.mixins?$/, ''))
			.filter((id) => id && id !== 'fabric' && id !== 'minecraft' && id !== 'mixins'),
	)
}

function memoryFindings(text: string, ctx: DoctorContext): Finding[] {
	const out: Finding[] = []
	const ceiling = memoryCeilingMb(ctx.systemMemoryMb)

	if (/Could not reserve enough space for (?:\d+KB )?object heap|Invalid maximum heap size/i.test(text)) {
		const target = Math.max(2048, Math.min(roundDown(ctx.memoryMb / 2), ceiling))
		out.push({
			id: 'heap-too-large',
			severity: 'error',
			title: 'Too much memory was requested',
			detail: `The game asked for ${ctx.memoryMb} MB but your computer could not provide it. Lowering it fixes the crash.`,
			fixes:
				target < ctx.memoryMb
					? [{ kind: 'memory', memoryMb: target, label: `Set memory to ${target} MB` }]
					: [],
		})
	} else if (/java\.lang\.OutOfMemoryError|GC overhead limit exceeded|Java heap space/i.test(text)) {
		const wanted = roundUp(Math.max(ctx.memoryMb * 1.5, ctx.memoryMb + 1024))
		const target = Math.min(wanted, ceiling)
		const canRaise = target > ctx.memoryMb
		out.push({
			id: 'out-of-memory',
			severity: 'error',
			title: 'The game ran out of memory',
			detail: canRaise
				? `It was limited to ${ctx.memoryMb} MB. Giving it more room usually fixes this.`
				: `It was limited to ${ctx.memoryMb} MB, which is already close to what your computer can spare. Close other programs or lower your render distance.`,
			fixes: canRaise ? [{ kind: 'memory', memoryMb: target, label: `Raise memory to ${target} MB` }] : [],
		})
	}

	if (/unable to create native thread/i.test(text)) {
		out.push({
			id: 'native-thread',
			severity: 'error',
			title: 'Your computer ran out of system resources',
			detail: 'Java could not start another thread. Close other programs and try again, or restart your computer.',
			fixes: [],
		})
	}
	return out
}

function mixinFindings(text: string): Finding[] {
	if (
		!/Mixin apply failed|Mixin transformation of .* failed|InvalidInjectionException|MixinApplyError|MixinTransformerError|Critical injection failure/i.test(
			text,
		)
	) {
		return []
	}
	const ids = modIdsFromMixins(text)
	if (!ids.length) {
		return [
			{
				id: 'mixin-unknown',
				severity: 'error',
				title: 'A mod could not hook into Minecraft',
				detail:
					'Two mods are fighting over the same part of the game, or one is built for a different Minecraft version. The log did not say which one.',
				fixes: [],
			},
		]
	}
	return [
		{
			id: 'mixin',
			severity: 'error',
			title: `${ids.map(prettyModName).join(' or ')} could not hook into Minecraft`,
			detail:
				'That mod conflicts with another one, or is built for a different Minecraft version. Turning it off lets the game start.',
			fixes: ids.slice(0, 3).map((modId) => ({
				kind: 'disable-mod' as const,
				modId,
				label: `Turn off ${prettyModName(modId)}`,
			})),
		},
	]
}

function dependencyFindings(text: string): Finding[] {
	const out: Finding[] = []
	const missing = Array.from(
		text.matchAll(/Mod '([^']+)' \(([^)]+)\)[^\n]*?requires (.+?), which is missing/g),
	)
	for (const m of missing) {
		out.push({
			id: `missing:${m[2]}`,
			severity: 'error',
			title: `${m[1]} is missing something it needs`,
			detail: `It requires ${m[3]}, and that is not installed. Install the missing mod from the instance's content tab, or turn ${m[1]} off.`,
			fixes: [{ kind: 'disable-mod', modId: m[2], label: `Turn off ${m[1]}` }],
		})
	}

	const conflicts = Array.from(
		text.matchAll(/Mod '([^']+)' \(([^)]+)\)[^\n]*?(?:conflicts with|is incompatible with|breaks) ([^\n!]+)/g),
	)
	for (const m of conflicts) {
		out.push({
			id: `conflict:${m[2]}`,
			severity: 'error',
			title: `${m[1]} conflicts with another mod`,
			detail: `It cannot run together with ${m[3].trim()}. Turn one of them off.`,
			fixes: [{ kind: 'disable-mod', modId: m[2], label: `Turn off ${m[1]}` }],
		})
	}
	return out
}

function environmentFindings(text: string): Finding[] {
	const out: Finding[] = []
	if (
		/UnsupportedClassVersionError|Unsupported class file major version|compiled by a more recent version of the Java Runtime/i.test(
			text,
		)
	) {
		out.push({
			id: 'java-version',
			severity: 'error',
			title: 'Wrong Java version',
			detail:
				'A mod or Minecraft needs a newer Java than the one being used. In the instance settings, set Java to automatic so the launcher picks the right one.',
			fixes: [],
		})
	}
	if (
		/GLFW error 65542|WGL: The driver does not appear to support OpenGL|Pixel format not accelerated|GLFW_VERSION_UNAVAILABLE|Failed to create (?:the GLFW )?window|OpenGL.{0,40}(?:not supported|unavailable)/i.test(
			text,
		)
	) {
		out.push({
			id: 'graphics-driver',
			severity: 'error',
			title: 'Graphics driver problem',
			detail:
				'Minecraft could not open a graphics window. Update your graphics driver, and make sure the game is not set to use a graphics chip that is turned off.',
			fixes: [],
		})
	}
	return out
}

function versionMismatchFindings(text: string): Finding[] {
	if (!/java\.lang\.(?:NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|AbstractMethodError)/.test(text)) {
		return []
	}
	if (/irisshaders|net\.coderbot\.iris/i.test(text) && /sodium/i.test(text)) {
		return [
			{
				id: 'iris-sodium',
				severity: 'error',
				title: 'Iris and Sodium do not match',
				detail:
					'Iris was built for a different Sodium version than the one installed. Turning Iris off lets the game start; re-install it afterwards to get a matching pair.',
				fixes: [{ kind: 'disable-mod', modId: 'iris', label: 'Turn off Iris' }],
			},
		]
	}
	return [
		{
			id: 'mod-mismatch',
			severity: 'error',
			title: 'Two mods are built for different versions of each other',
			detail:
				'A mod called code that does not exist in the version of another mod you have installed. Updating all of your mods usually fixes it.',
			fixes: [],
		},
	]
}

function fallbackFinding(text: string): Finding | null {
	const description = text.match(/^Description: (.+)$/m)
	const cause = Array.from(text.matchAll(/^Caused by: (.+)$/gm)).pop()
	if (!description && !cause) return null
	return {
		id: 'unknown-crash',
		severity: 'warning',
		title: 'The game crashed, but the cause is not one I recognise',
		detail: [description?.[1], cause?.[1]].filter(Boolean).join(' - '),
		fixes: [],
	}
}

const SEVERITY_ORDER = { error: 0, warning: 1, info: 2 } as const

export function diagnose(text: string, ctx: DoctorContext): Finding[] {
	if (!text.trim()) return []

	const findings = [
		...memoryFindings(text, ctx),
		...dependencyFindings(text),
		...mixinFindings(text),
		...versionMismatchFindings(text),
		...environmentFindings(text),
	]

	const seen = new Set<string>()
	const unique = findings.filter((f) => (seen.has(f.id) ? false : (seen.add(f.id), true)))

	if (!unique.length) {
		const fallback = fallbackFinding(text)
		if (fallback) unique.push(fallback)
	}
	return unique.sort((a, b) => SEVERITY_ORDER[a.severity] - SEVERITY_ORDER[b.severity])
}

function normalize(value: string): string {
	return value.toLowerCase().replace(/[^a-z0-9]/g, '')
}

/** Finds the mod file for a Fabric mod id among instance file paths. Matches on the file name's start. */
export function findModFile(paths: string[], modId: string): string | null {
	const wanted = normalize(modId)
	if (!wanted) return null
	let best: string | null = null
	for (const path of paths) {
		const name = path.split(/[\\/]/).pop() ?? path
		const stem = normalize(name.replace(/\.disabled$/i, '').replace(/\.jar$/i, ''))
		if (stem.startsWith(wanted)) {
			if (best === null || normalize(best.split(/[\\/]/).pop() ?? best).length > stem.length) best = path
		}
	}
	return best
}
