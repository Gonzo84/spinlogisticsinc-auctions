<template>
  <div
    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold"
    :class="sizeClasses"
  >
    <!-- Leaf Icon -->
    <svg
      class="text-secondary shrink-0"
      :class="iconSizeClass"
      fill="none"
      stroke="currentColor"
      viewBox="0 0 24 24"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        stroke-width="2"
        d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z"
      />
    </svg>
    <span class="text-secondary-800">
      {{ formattedAmount }} kg CO&#8322; {{ $t('common.avoided') }}
    </span>
  </div>
</template>

<script setup lang="ts">
interface Props {
  amount: number
  size?: 'sm' | 'md'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'sm',
})

const formattedAmount = computed(() => {
  if (props.amount >= 1000) {
    return `${(props.amount / 1000).toFixed(1)}t`
  }
  return props.amount.toLocaleString()
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'md':
      return 'bg-secondary-100 text-sm px-3 py-1.5'
    default:
      return 'bg-secondary-100/80 backdrop-blur-sm text-xs px-2 py-1'
  }
})

const iconSizeClass = computed(() => {
  return props.size === 'md' ? 'w-4 h-4' : 'w-3.5 h-3.5'
})
</script>
