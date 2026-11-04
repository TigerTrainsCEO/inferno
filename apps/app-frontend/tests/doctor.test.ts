import assert from 'node:assert/strict'
import { test } from 'node:test'

import { diagnose, findModFile, memoryCeilingMb } from '../src/features/pvp/crash-doctor'
import { planTuning, recommendMemoryMb, tierFor } from '../src/features/pvp/tune'

const ctx = { memoryMb: 4096, systemMemoryMb: 16384 }

test('out of memory raises memory with a fix', () => {
	const [f] = diagnose('Exception in thread "Render thread" java.lang.OutOfMemoryError: Java heap space', ctx)
	assert.equal(f.id, 'out-of-memory')
	assert.equal(f.fixes[0].kind, 'memory')
	assert.ok(f.fixes[0].kind === 'memory' && f.fixes[0].memoryMb > 4096)
	assert.ok(f.fixes[0].kind === 'memory' && f.fixes[0].memoryMb <= memoryCeilingMb(16384))
})

test('out of memory with no headroom gives no fix and explains why', () => {
	const [f] = diagnose('java.lang.OutOfMemoryError: Java heap space', { memoryMb: 4096, systemMemoryMb: 6144 })
	assert.equal(f.id, 'out-of-memory')
	assert.equal(f.fixes.length, 0)
	assert.match(f.detail, /close other programs/i)
})

test('too much memory requested lowers it', () => {
	const [f] = diagnose('Error occurred during initialization of VM\nCould not reserve enough space for 8388608KB object heap', {
		memoryMb: 8192,
		systemMemoryMb: 8192,
	})
	assert.equal(f.id, 'heap-too-large')
	assert.ok(f.fixes[0].kind === 'memory' && f.fixes[0].memoryMb < 8192)
})

test('mixin failure names the mod and offers to turn it off', () => {
	const log = `[main/ERROR]: Mixin apply for mod inferno_client failed inferno_client.mixins.json:CameraMixin from mod inferno_client -> net.minecraft.client.render.Camera: org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException @Inject annotation on update could not find any targets`
	const found = diagnose(log, ctx)
	assert.equal(found[0].id, 'mixin')
	assert.ok(found[0].fixes.some((x) => x.kind === 'disable-mod' && x.modId === 'inferno_client'))
})

test('mixin failure with no mod id still reports something', () => {
	const found = diagnose('MixinApplyError: something went wrong', ctx)
	assert.equal(found[0].id, 'mixin-unknown')
	assert.equal(found[0].fixes.length, 0)
})

test('fabric missing dependency is explained', () => {
	const log = `Incompatible mods found!
Unmet dependency listing:
	 - Mod 'Inferno Client' (inferno_client) 1.0.0 requires version 0.100.0 or later of fabric-api, which is missing!`
	const [f] = diagnose(log, ctx)
	assert.equal(f.id, 'missing:inferno_client')
	assert.match(f.detail, /fabric-api/)
	assert.ok(f.fixes[0].kind === 'disable-mod' && f.fixes[0].modId === 'inferno_client')
})

test('fabric conflict is detected', () => {
	const log = `	 - Mod 'Foo' (foo) 1.0.0 conflicts with mod bar, any version!`
	const [f] = diagnose(log, ctx)
	assert.equal(f.id, 'conflict:foo')
})

test('wrong java version is reported without a fix', () => {
	const [f] = diagnose('java.lang.UnsupportedClassVersionError: net/minecraft/Foo has been compiled by a more recent version of the Java Runtime', ctx)
	assert.equal(f.id, 'java-version')
	assert.equal(f.fixes.length, 0)
})

test('graphics driver errors are recognised', () => {
	const [f] = diagnose('GLFW error 65542: WGL: The driver does not appear to support OpenGL', ctx)
	assert.equal(f.id, 'graphics-driver')
})

test('iris and sodium mismatch offers to turn iris off', () => {
	const log = `java.lang.NoSuchMethodError: 'void me.jellysquid.mods.sodium.client.Foo.bar()'\n\tat net.irisshaders.iris.Iris.onInit(Iris.java:1)`
	const [f] = diagnose(log, ctx)
	assert.equal(f.id, 'iris-sodium')
	assert.ok(f.fixes[0].kind === 'disable-mod' && f.fixes[0].modId === 'iris')
})

test('unknown crash falls back to the description', () => {
	const log = `---- Minecraft Crash Report ----\nDescription: Ticking entity\n\njava.lang.NullPointerException: x\nCaused by: java.lang.IllegalStateException: boom`
	const [f] = diagnose(log, ctx)
	assert.equal(f.id, 'unknown-crash')
	assert.match(f.detail, /Ticking entity/)
})

test('a clean log has no findings', () => {
	assert.deepEqual(diagnose('[main/INFO]: Setting user: Player\n[main/INFO]: Stopping!', ctx), [])
	assert.deepEqual(diagnose('   ', ctx), [])
})

test('findModFile matches by file name start and ignores lookalikes', () => {
	const paths = [
		'mods/sirius-1.0.jar',
		'mods/iris-fabric-1.8.0+mc1.21.jar',
		'mods/inferno-client-1.21.11.jar',
		'mods/fabric-api-0.141.jar',
	]
	assert.equal(findModFile(paths, 'iris'), 'mods/iris-fabric-1.8.0+mc1.21.jar')
	assert.equal(findModFile(paths, 'inferno_client'), 'mods/inferno-client-1.21.11.jar')
	assert.equal(findModFile(paths, 'fabric-api'), 'mods/fabric-api-0.141.jar')
	assert.equal(findModFile(paths, 'sodium'), null)
	assert.equal(findModFile(['mods\\iris-1.0.jar'], 'iris'), 'mods\\iris-1.0.jar')
})

test('hardware tiers', () => {
	assert.equal(tierFor({ totalMemoryMb: 4096, threads: 8 }), 'low')
	assert.equal(tierFor({ totalMemoryMb: 16384, threads: 4 }), 'low')
	assert.equal(tierFor({ totalMemoryMb: 8192, threads: 8 }), 'mid')
	assert.equal(tierFor({ totalMemoryMb: 32768, threads: 16 }), 'high')
})

test('memory is capped to half of RAM and never below the floor', () => {
	assert.equal(recommendMemoryMb({ totalMemoryMb: 16384, threads: 8 }, 6144), 6144)
	assert.equal(recommendMemoryMb({ totalMemoryMb: 8192, threads: 8 }, 6144), 4096)
	assert.equal(recommendMemoryMb({ totalMemoryMb: 4096, threads: 4 }, 4096), 2048)
	assert.equal(recommendMemoryMb({ totalMemoryMb: 2048, threads: 2 }, 4096), 1536)
	assert.equal(recommendMemoryMb({ totalMemoryMb: 16384, threads: 8 }, 3000) % 256, 0)
})

test('tuning plan flags only what changes', () => {
	const result = planTuning({ totalMemoryMb: 8192, threads: 8 }, [
		{ id: 'a', name: 'Duels', baselineMb: 4096, currentMb: 4096 },
		{ id: 'b', name: 'Clips', baselineMb: 6144, currentMb: 6144 },
	])
	assert.equal(result.tier, 'mid')
	assert.equal(result.plans[0].changed, false)
	assert.equal(result.plans[1].recommendedMb, 4096)
	assert.equal(result.plans[1].changed, true)
})
