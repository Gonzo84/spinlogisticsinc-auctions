<!--
  @deprecated Use PrimeVue's Tag component with getStatusSeverity() and formatStatusLabel()
  from '@/composables/useStatusSeverity' instead. This component is no longer used and will
  be removed in a future cleanup.
-->
<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  status: string
  size?: 'sm' | 'md'
}>(), {
  size: 'md',
})

const colorMap: Record<string, { bg: string; text: string }> = {
  // General
  active: { bg: 'bg-green-100', text: 'text-green-800' },
  approved: { bg: 'bg-green-100', text: 'text-green-800' },
  paid: { bg: 'bg-emerald-100', text: 'text-emerald-800' },
  completed: { bg: 'bg-green-100', text: 'text-green-800' },

  // Pending/Processing
  pending: { bg: 'bg-yellow-100', text: 'text-yellow-800' },
  pending_review: { bg: 'bg-yellow-100', text: 'text-yellow-800' },
  processing: { bg: 'bg-amber-100', text: 'text-amber-800' },
  scheduled: { bg: 'bg-blue-100', text: 'text-blue-800' },
  not_started: { bg: 'bg-gray-100', text: 'text-gray-600' },

  // Active states
  closing: { bg: 'bg-orange-100', text: 'text-orange-800' },
  held: { bg: 'bg-blue-100', text: 'text-blue-800' },

  // Draft/Neutral
  draft: { bg: 'bg-gray-100', text: 'text-gray-700' },
  none: { bg: 'bg-gray-100', text: 'text-gray-600' },

  // Sold
  sold: { bg: 'bg-blue-100', text: 'text-blue-800' },
  won: { bg: 'bg-green-100', text: 'text-green-800' },

  // Negative states
  unsold: { bg: 'bg-red-100', text: 'text-red-700' },
  rejected: { bg: 'bg-red-100', text: 'text-red-800' },
  blocked: { bg: 'bg-red-100', text: 'text-red-800' },
  suspended: { bg: 'bg-red-100', text: 'text-red-800' },
  cancelled: { bg: 'bg-red-100', text: 'text-red-700' },
  overdue: { bg: 'bg-red-100', text: 'text-red-800' },
  forfeited: { bg: 'bg-red-100', text: 'text-red-800' },
  withdrawn: { bg: 'bg-gray-100', text: 'text-gray-600' },
  failed: { bg: 'bg-red-100', text: 'text-red-800' },
  disputed: { bg: 'bg-red-100', text: 'text-red-800' },
  closed: { bg: 'bg-gray-100', text: 'text-gray-700' },

  // Special
  released: { bg: 'bg-teal-100', text: 'text-teal-800' },
  refunded: { bg: 'bg-purple-100', text: 'text-purple-800' },
  outbid: { bg: 'bg-amber-100', text: 'text-amber-800' },
  lost: { bg: 'bg-gray-100', text: 'text-gray-600' },

  // Fraud
  high: { bg: 'bg-red-100', text: 'text-red-800' },
  medium: { bg: 'bg-amber-100', text: 'text-amber-800' },
  low: { bg: 'bg-yellow-100', text: 'text-yellow-800' },
  resolved: { bg: 'bg-green-100', text: 'text-green-800' },
  investigating: { bg: 'bg-blue-100', text: 'text-blue-800' },
}

const colors = computed(() => {
  return colorMap[props.status] ?? { bg: 'bg-gray-100', text: 'text-gray-700' }
})

const label = computed(() => {
  return props.status
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (l) => l.toUpperCase())
})

const sizeClasses = computed(() => {
  return props.size === 'sm'
    ? 'px-2 py-0.5 text-[10px]'
    : 'px-2.5 py-0.5 text-xs'
})
</script>

<template>
  <span
    :class="[
      'inline-flex items-center rounded-full font-medium',
      colors.bg,
      colors.text,
      sizeClasses,
    ]"
  >
    {{ label }}
  </span>
</template>
