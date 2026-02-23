<script setup lang="ts">
import { computed } from 'vue'
import { Doughnut } from 'vue-chartjs'
import {
  Chart as ChartJS,
  ArcElement,
  Tooltip,
  Legend,
  type ChartOptions,
} from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend)

const props = withDefaults(defineProps<{
  sold: number
  unsold: number
  height?: number
}>(), {
  height: 250,
})

const rate = computed(() => {
  const total = props.sold + props.unsold
  if (total === 0) return 0
  return Math.round((props.sold / total) * 100)
})

const chartData = computed(() => ({
  labels: ['Sold', 'Unsold'],
  datasets: [
    {
      data: [props.sold, props.unsold],
      backgroundColor: ['#22c55e', '#e5e7eb'],
      borderColor: ['#16a34a', '#d1d5db'],
      borderWidth: 2,
      hoverOffset: 6,
    },
  ],
}))

const chartOptions: ChartOptions<'doughnut'> = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '70%',
  plugins: {
    legend: {
      position: 'bottom',
      labels: {
        padding: 16,
        usePointStyle: true,
        pointStyleWidth: 10,
        font: { size: 12 },
        color: '#374151',
      },
    },
    tooltip: {
      backgroundColor: '#1f2937',
      padding: 10,
      cornerRadius: 8,
      callbacks: {
        label: (ctx) => {
          const total = props.sold + props.unsold
          const pct = total > 0 ? Math.round((ctx.parsed / total) * 100) : 0
          return `${ctx.label}: ${ctx.parsed} lots (${pct}%)`
        },
      },
    },
  },
}
</script>

<template>
  <div class="relative">
    <div :style="{ height: height + 'px' }">
      <Doughnut :data="chartData" :options="chartOptions" />
    </div>
    <!-- Center label -->
    <div class="pointer-events-none absolute inset-0 flex items-center justify-center" style="margin-bottom: 30px;">
      <div class="text-center">
        <p class="text-3xl font-bold text-gray-900">{{ rate }}%</p>
        <p class="text-xs text-gray-500">Sell-through</p>
      </div>
    </div>
  </div>
</template>
