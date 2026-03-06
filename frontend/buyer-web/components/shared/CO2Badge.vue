<template>
  <div
    class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold"
    :class="sizeClasses"
  >
    <i class="pi pi-globe text-secondary shrink-0" :class="iconSizeClass" />
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
  return props.size === 'md' ? 'text-base' : 'text-sm'
})
</script>
