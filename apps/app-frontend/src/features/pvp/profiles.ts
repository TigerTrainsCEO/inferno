/**
 * Inferno Launcher - pre-built PvP profiles.
 *
 * Design rules for everything in this file:
 *  - NO hacked-client / cheat mods (no reach, killaura, autoclickers, velocity, ESP/X-ray, ...).
 *  - Only performance, visibility and quality-of-life mods that are client-side and widely accepted.
 *  - The Inferno Client jar is bundled with the launcher (see apps/app/resources/inferno) and is
 *    added as a local file; every other mod is resolved live from Modrinth at install time, so
 *    each profile always gets the newest build that actually supports its Minecraft version.
 *
 * Mod entries use Modrinth project slugs. If a slug has no compatible build for a profile's
 * Minecraft version (or was renamed) the installer skips that single mod and reports it, instead of
 * failing the whole profile. `optional: true` just changes how loudly the UI reports a skip.
 */

export type PvpGameVersion = '1.21.11' | '1.21.10'

export interface PvpMod {
	/** Modrinth project slug (or id). */
	slug: string
	/** Human label shown on the profile card. */
	label: string
	/** What it does for PvP, one short line. */
	why: string
	/** A skip is reported quietly rather than as a warning. */
	optional?: boolean
}

export interface PvpProfile {
	id: string
	name: string
	tagline: string
	description: string
	gameVersion: PvpGameVersion
	loader: 'fabric'
	/** Accent colour for the card (any CSS colour). */
	accent: string
	/** lucide-ish glyph key resolved in Arena.vue */
	glyph: 'flame' | 'bolt' | 'shield' | 'film' | 'feather'
	tags: string[]
	includeInferno: boolean
	mods: PvpMod[]
	/** Honest per-profile caveat shown on the card. */
	note?: string
}

/** Resource paths are relative to the bundled `inferno/` directory (see tauri.conf.json). */
export const INFERNO_JARS: Record<PvpGameVersion, string> = {
	'1.21.11': 'inferno/inferno-client-1.21.11.jar',
	'1.21.10': 'inferno/inferno-client-1.21.10.jar',
}

/** Shared by nearly every profile. Fabric API is also pulled in automatically as a dependency. */
const CORE_PERF: PvpMod[] = [
	{
		slug: 'sodium',
		label: 'Sodium',
		why: 'Rewrites the renderer for a big FPS and frame-time boost.',
	},
	{
		slug: 'lithium',
		label: 'Lithium',
		why: 'Game-logic optimisations; smoother ticks, no gameplay changes.',
	},
	{
		slug: 'modernfix',
		label: 'ModernFix',
		why: 'Faster startup, lower memory use, fewer stutters.',
		optional: true,
	},
]

const MOD_MENU: PvpMod = {
	slug: 'modmenu',
	label: 'Mod Menu',
	why: 'See and configure all your mods in one screen.',
	optional: true,
}

const INFERNO_NOTE =
	'Inferno includes Freelook and Fullbright. Some servers ban those in PvP - turn them off in the Inferno menu before joining servers with strict rules.'

function build(version: PvpGameVersion): PvpProfile[] {
	const v = version
	const suffix = v === '1.21.11' ? '' : ` (${v})`
	const idSuffix = v === '1.21.11' ? '' : `-${v.replace(/\./g, '')}`

	return [
		{
			id: `inferno-duels${idSuffix}`,
			name: `Inferno Duels${suffix}`,
			tagline: 'The all-round PvP loadout',
			description:
				'Balanced for sword, axe and mace duels: top-tier FPS, a clean HUD with CPS and armour readouts, and zoom/freelook from Inferno.',
			gameVersion: v,
			loader: 'fabric',
			accent: '#ff5a1f',
			glyph: 'flame',
			tags: ['Duels', 'Balanced', 'Fabric'],
			includeInferno: true,
			mods: [
				...CORE_PERF,
				{
					slug: 'entityculling',
					label: 'Entity Culling',
					why: 'Skips rendering what you cannot see; helps in crowded fights.',
				},
				{
					slug: 'immediatelyfast',
					label: 'ImmediatelyFast',
					why: 'Faster HUD, text and particle drawing.',
					optional: true,
				},
				{
					slug: 'sodium-extra',
					label: 'Sodium Extra',
					why: 'Extra video options: fine control over particles, fog and more.',
					optional: true,
				},
				MOD_MENU,
			],
			note: INFERNO_NOTE,
		},
		{
			id: `max-fps${idSuffix}`,
			name: `Max FPS Arena${suffix}`,
			tagline: 'Every frame counts',
			description:
				'For crowded KitPvP, bedwars-style rushes and low-end PCs. Stacks every safe performance mod so your fights never hitch.',
			gameVersion: v,
			loader: 'fabric',
			accent: '#ffb800',
			glyph: 'bolt',
			tags: ['FPS', 'Low-end PCs', 'Fabric'],
			includeInferno: true,
			mods: [
				...CORE_PERF,
				{
					slug: 'ferrite-core',
					label: 'FerriteCore',
					why: 'Cuts memory use so the game stays smooth for long sessions.',
					optional: true,
				},
				{
					slug: 'entityculling',
					label: 'Entity Culling',
					why: 'Skips rendering hidden entities and block entities.',
				},
				{
					slug: 'immediatelyfast',
					label: 'ImmediatelyFast',
					why: 'Faster immediate-mode rendering.',
					optional: true,
				},
				{
					slug: 'more_culling',
					label: 'More Culling',
					why: 'Extra occlusion culling on top of vanilla.',
					optional: true,
				},
				{
					slug: 'dynamic-fps',
					label: 'Dynamic FPS',
					why: 'Drops resource use while the window is in the background.',
					optional: true,
				},
				{
					slug: 'sodium-extra',
					label: 'Sodium Extra',
					why: 'Turn off costly effects with one click.',
					optional: true,
				},
				MOD_MENU,
			],
			note: INFERNO_NOTE,
		},
		{
			id: `clean-slate${idSuffix}`,
			name: `Clean Slate${suffix}`,
			tagline: 'Just Inferno and a renderer',
			description:
				'The smallest possible setup: Inferno plus Sodium. Ideal when a server or tournament keeps a strict allowed-mods list.',
			gameVersion: v,
			loader: 'fabric',
			accent: '#8d96a0',
			glyph: 'feather',
			tags: ['Minimal', 'Tournament-safe', 'Fabric'],
			includeInferno: true,
			mods: [
				{
					slug: 'sodium',
					label: 'Sodium',
					why: 'FPS boost with no gameplay impact.',
				},
			],
			note: 'Always check the specific server or tournament rules for the mods you intend to use.',
		},
		{
			id: `clip-lab${idSuffix}`,
			name: `Clip Lab${suffix}`,
			tagline: 'Look good, record better',
			description:
				'For content creators: performance mods plus Iris shader support so your PvP clips look great without tanking frame rate.',
			gameVersion: v,
			loader: 'fabric',
			accent: '#c04bff',
			glyph: 'film',
			tags: ['Shaders', 'Content', 'Fabric'],
			includeInferno: true,
			mods: [
				...CORE_PERF,
				{
					slug: 'iris',
					label: 'Iris Shaders',
					why: 'Shader pack support built on Sodium.',
				},
				{
					slug: 'entityculling',
					label: 'Entity Culling',
					why: 'Keeps frame rate steady in busy scenes.',
				},
				{
					slug: 'immediatelyfast',
					label: 'ImmediatelyFast',
					why: 'Faster HUD and text rendering.',
					optional: true,
				},
				MOD_MENU,
			],
			note: 'Shaders are a visual change; they are not allowed on every server. Drop a shader pack into this profile and enable it in Video Settings.',
		},
	]
}

export const PVP_PROFILES: PvpProfile[] = [...build('1.21.11'), ...build('1.21.10')]

export function profilesForVersion(version: PvpGameVersion): PvpProfile[] {
	return PVP_PROFILES.filter((p) => p.gameVersion === version)
}
