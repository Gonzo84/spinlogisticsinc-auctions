<template>
  <div
    class="inline-flex items-center gap-1"
    :class="[containerClasses, { 'animate-pulse-fast': isPulsing }]"
  >
    <!-- Extended flash -->
    <Transition
      enter-active-class="transition ease-out duration-300"
      enter-from-class="opacity-0 scale-90"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition ease-in duration-200"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-90"
    >
      <span
        v-if="showExtendedFlash"
        class="px-2 py-0.5 bg-accent text-white text-xs font-bold rounded-full mr-1"
      >
        {{ $t('auction.extended') }}
      </span>
    </Transition>

    <template v-if="!hasValidEndTime">
      <span :class="compact ? 'text-xs' : 'text-sm'" class="font-medium text-gray-400">
        --:--:--
      </span>
    </template>

    <template v-else-if="isExpired">
      <span :class="compact ? 'text-xs' : 'text-sm'" class="font-medium text-gray-500">
        {{ $t('auction.ended') }}
      </span>
    </template>

    <template v-else>
      <!-- Timer Icon -->
      <i v-if="!compact" class="pi pi-clock text-base shrink-0" :class="iconColor" />

      <!-- Compact mode -->
      <div v-if="compact" class="flex items-center gap-0.5 text-xs font-mono font-semibold" :class="textColor">
        <template v-if="days > 0">
          <span>{{ days }}d</span>
          <span>{{ padZero(hours) }}h</span>
        </template>
        <template v-else>
          <span>{{ padZero(hours) }}:{{ padZero(minutes) }}:{{ padZero(seconds) }}</span>
        </template>
      </div>

      <!-- Full mode -->
      <div v-else class="flex items-center gap-1.5">
        <div v-if="days > 0" class="text-center">
          <span class="text-lg font-bold" :class="textColor">{{ days }}</span>
          <span class="text-[10px] text-gray-500 ml-0.5">{{ $t('auction.days') }}</span>
        </div>
        <div class="text-center">
          <span class="text-lg font-bold" :class="textColor">{{ padZero(hours) }}</span>
          <span class="text-[10px] text-gray-500 ml-0.5">{{ $t('auction.hours') }}</span>
        </div>
        <span class="text-lg font-bold" :class="textColor">:</span>
        <div class="text-center">
          <span class="text-lg font-bold" :class="textColor">{{ padZero(minutes) }}</span>
          <span class="text-[10px] text-gray-500 ml-0.5">{{ $t('auction.mins') }}</span>
        </div>
        <span class="text-lg font-bold" :class="textColor">:</span>
        <div class="text-center">
          <span class="text-lg font-bold" :class="textColor">{{ padZero(seconds) }}</span>
          <span class="text-[10px] text-gray-500 ml-0.5">{{ $t('auction.secs') }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
interface Props {
  endTime: string
  compact?: boolean
  extended?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  compact: false,
  extended: false,
})

const emit = defineEmits<{
  expired: []
}>()

const now = ref(Date.now())
const showExtendedFlash = ref(false)
let intervalId: ReturnType<typeof setInterval> | null = null

const endTimestamp = computed(() => {
  if (!props.endTime) return 0
  const ts = new Date(props.endTime).getTime()
  return isNaN(ts) ? 0 : ts
})

const hasValidEndTime = computed(() => endTimestamp.value > 0)

const totalSeconds = computed(() => {
  if (!hasValidEndTime.value) return 0
  const diff = endTimestamp.value - now.value
  return Math.max(0, Math.floor(diff / 1000))
})

const isExpired = computed(() => hasValidEndTime.value && totalSeconds.value <= 0)

const days = computed(() => Math.floor(totalSeconds.value / 86400))
const hours = computed(() => Math.floor((totalSeconds.value % 86400) / 3600))
const minutes = computed(() => Math.floor((totalSeconds.value % 3600) / 60))
const seconds = computed(() => totalSeconds.value % 60)

const totalMinutesLeft = computed(() => totalSeconds.value / 60)

// Amber when less than 1 hour
const isAmber = computed(() => !isExpired.value && totalMinutesLeft.value < 60 && totalMinutesLeft.value >= 2)

// Red pulsing when less than 2 minutes (anti-sniping zone)
const isRed = computed(() => !isExpired.value && totalMinutesLeft.value < 2)

const isPulsing = computed(() => isRed.value)

const textColor = computed(() => {
  if (isRed.value) return 'text-warning'
  if (isAmber.value) return 'text-accent'
  return 'text-gray-900'
})

const iconColor = computed(() => {
  if (isRed.value) return 'text-warning'
  if (isAmber.value) return 'text-accent'
  return 'text-gray-400'
})

const containerClasses = computed(() => {
  if (isRed.value) return 'bg-warning-50 rounded-lg px-2 py-1'
  if (isAmber.value) return 'bg-accent-50 rounded-lg px-2 py-1'
  return ''
})

function padZero(n: number): string {
  return n.toString().padStart(2, '0')
}

// Watch for extension - flash "Extended!" badge
watch(() => props.endTime, (newVal, oldVal) => {
  if (newVal !== oldVal && oldVal) {
    showExtendedFlash.value = true
    setTimeout(() => {
      showExtendedFlash.value = false
    }, 3000)
  }
})

watch(isExpired, (expired) => {
  if (expired) {
    emit('expired')
    if (intervalId) {
      clearInterval(intervalId)
      intervalId = null
    }
  }
})

onMounted(() => {
  intervalId = setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (intervalId) {
    clearInterval(intervalId)
  }
})
</script>
