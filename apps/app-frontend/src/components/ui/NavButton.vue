<template>
	<component
		:is="isLink ? RouterLink : 'button'"
		:to="isLink ? to : undefined"
		v-bind="$attrs"
		:disabled="isLink ? undefined : disabled"
		:active-class="isLink && isSubpage ? '' : undefined"
		:class="{
			'router-link-active': isPrimary && isPrimary(route),
			'subpage-active': isSubpage && isSubpage(route),
			disabled: disabled,
		}"
		class="nav-button border-none text-primary cursor-pointer w-14 min-h-[3.25rem] rounded-md flex flex-col gap-0.5 items-center justify-center text-2xl transition-all bg-transparent hover:bg-button-bg hover:text-contrast"
		@click="onClick"
	>
		<slot />
		<span v-if="label" class="nav-label">{{ label }}</span>
	</component>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()

type RouteFunction = (route: RouteLocationNormalizedLoaded) => boolean

const props = withDefaults(
	defineProps<{
		to: (() => void) | string
		isPrimary?: RouteFunction
		isSubpage?: RouteFunction
		highlightOverride?: boolean
		disabled?: boolean
		/** Optional tiny caption under the icon (Inferno nav style). */
		label?: string
	}>(),
	{
		disabled: false,
	},
)

const isLink = computed(() => typeof props.to === 'string')

function onClick() {
	if (typeof props.to === 'function') {
		props.to()
	}
}

defineOptions({
	inheritAttrs: false,
})
</script>

<style lang="scss" scoped>
.nav-button {
	position: relative;

	&::before {
		content: '';
		position: absolute;
		inset: 0;
		background-color: var(--color-button-bg-selected);
		border-radius: var(--radius-md);
		opacity: 0;
		scale: 0.6;
		z-index: -1;
		transition:
			opacity 0.25s var(--ease-out-expo),
			scale 0.25s var(--ease-out-expo);
	}
}

.nav-label {
	font-family: var(--font-display);
	font-size: 0.625rem;
	line-height: 1;
	font-weight: 700;
	letter-spacing: 0.08em;
	text-transform: uppercase;
	opacity: 0.85;
}

.nav-button::after {
	content: '';
	position: absolute;
	left: -0.5rem;
	top: 22%;
	bottom: 22%;
	width: 3px;
	border-radius: 0 3px 3px 0;
	background: var(--color-brand);
	box-shadow: 0 0 12px var(--color-brand-shadow);
	opacity: 0;
	scale: 1 0.4;
	transition:
		opacity 0.2s var(--ease-out-expo),
		scale 0.2s var(--ease-out-expo);
}

.router-link-active::after {
	opacity: 1;
	scale: 1 1;
}

.router-link-active,
.subpage-active {
	svg {
		filter: drop-shadow(0 0 0.5rem black);
	}
}

.router-link-active {
	@apply text-[--color-button-text-selected];
	&::before {
		background-color: var(--color-button-bg-selected);
		scale: 1;
		opacity: 1;
	}
}

.subpage-active {
	@apply text-contrast bg-button-bg;
}
</style>
