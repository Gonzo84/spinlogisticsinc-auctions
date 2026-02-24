<script setup lang="ts">
import { computed } from 'vue'
import { Bar } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  Title,
  Tooltip,
  Legend,
  type ChartOptions,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

const props = withDefaults(defineProps<{
  labels: string[]
  data: number[]
  label?: string
  color?: string
  height?: number
}>(), {
  label: 'Revenue (EUR)',
  color: '#2563eb',
  height: 300,
})

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      label: props.label,
      data: props.data,
      backgroundColor: props.color + '33',
      borderColor: props.color,
      borderWidth: 2,
      borderRadius: 6,
      borderSkipped: false as const,
    },
  ],
}))

const chartOptions: ChartOptions<'bar'> = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      backgroundColor: '#1f2937',
      padding: 12,
      cornerRadius: 8,
      callbacks: {
        label: (ctx) => {
          return `EUR ${(ctx.parsed.y ?? 0).toLocaleString('de-DE', { minimumFractionDigits: 2 })}`
        },
      },
    },
  },
  scales: {
    x: {
      grid: { display: false },
      ticks: { font: { size: 11 }, color: '#6b7280' },
    },
    y: {
      grid: { color: '#f3f4f6' },
      ticks: {
        font: { size: 11 },
        color: '#6b7280',
        callback: (value) => `EUR ${Number(value).toLocaleString('de-DE')}`,
      },
      beginAtZero: true,
    },
  },
}
</script>

<template>
  <div :style="{ height: height + 'px' }">
    <Bar :data="chartData" :options="chartOptions" />
  </div>
</template>
