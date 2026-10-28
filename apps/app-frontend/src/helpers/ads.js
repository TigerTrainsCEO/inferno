// Inferno Launcher ships WITHOUT Modrinth's ad webview. Every ad entry point is a
// no-op so no third-party ad content or consent flow is ever initialised.
// (The original invoke() bindings were removed deliberately.)

export async function init_ads_window() {}
export async function take_ads_window_hold() {}
export async function release_ads_window_hold() {}
export async function hide_ads_window() {}
export async function should_show_ads_consent_popup() {
	return false
}
export async function perform_ads_consent_action() {}
export async function open_ads_consent_preferences() {}
export async function record_ads_click() {}
export async function open_ads_link() {}
export async function get_ads_debug_state() {
	return null
}
