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
  valueFormatter?: (value: number) => string
}>(), {
  label: 'Revenue (EUR)',
  color: '#2563eb',
  height: 300,
  valueFormatter: undefined,
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

const defaultFormatter = (value: number) => `EUR ${value.toLocaleString('de-DE', { minimumFractionDigits: 2 })}`
const defaultTickFormatter = (value: number) => `EUR ${value.toLocaleString('de-DE')}`

const chartOptions = computed<ChartOptions<'bar'>>(() => {
  const fmt = props.valueFormatter ?? defaultFormatter
  const tickFmt = props.valueFormatter ?? defaultTickFormatter
  return {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
      tooltip: {
        backgroundColor: '#1f2937',
        titleFont: { size: 13, weight: 'bold' },
        bodyFont: { size: 12 },
        padding: 12,
        cornerRadius: 8,
        callbacks: {
          label: (ctx) => {
            const value = ctx.parsed.y ?? 0
            return fmt(value)
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
          callback: (value) => tickFmt(Number(value)),
        },
        beginAtZero: true,
      },
    },
  }
})
</script>

<template>
  <div :style="{ height: height + 'px' }">
    <Bar
      :data="chartData"
      :options="chartOptions"
    />
  </div>
</template>
