import { get_projects, toggle_disable_project } from '@/helpers/instance'
import {
	get_latest_log_cursor,
	get_logs,
	get_output_by_filename,
} from '@/helpers/logs.js'

import { findModFile } from './crash-doctor'

const MAX_LOG_CHARS = 400_000
const CRASH_REPORT_MAX_AGE_MS = 3 * 24 * 60 * 60 * 1000

interface LogEntry {
	log_type: string
	filename: string
	age: number
}

/** Newest crash report (if recent) followed by the tail of latest.log. */
export async function loadProblemText(instanceId: string): Promise<string> {
	const parts: string[] = []

	try {
		const logs = ((await get_logs(instanceId, true)) ?? []) as LogEntry[]
		const crash = logs.find((l) => l.log_type === 'CrashReport')
		if (crash) {
			const ageMs = crash.age > 1e11 ? crash.age : crash.age * 1000
			if (Date.now() - ageMs <= CRASH_REPORT_MAX_AGE_MS) {
				const report = await get_output_by_filename(instanceId, 'CrashReport', crash.filename)
				if (report) parts.push(String(report))
			}
		}
	} catch {
		/* no crash reports yet */
	}

	try {
		const latest = await get_latest_log_cursor(instanceId, 0)
		const output = String(latest?.output ?? '')
		parts.push(output.length > MAX_LOG_CHARS ? output.slice(-MAX_LOG_CHARS) : output)
	} catch {
		/* the game has never been started */
	}

	return parts.join('\n')
}

/** Turns a mod off by its Fabric id. Throws if the mod is not in the instance. */
export async function disableModById(instanceId: string, modId: string): Promise<void> {
	const files = await get_projects(instanceId)
	const path = findModFile(Object.keys(files), modId)
	if (!path) throw new Error(`Could not find "${modId}" in this profile`)
	await toggle_disable_project(instanceId, path, false)
}
