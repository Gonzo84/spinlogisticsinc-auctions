<script setup lang="ts">
import { computed } from 'vue'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler,
  type ChartOptions,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, Filler)

const props = withDefaults(defineProps<{
  labels: string[]
  data: number[]
  label?: string
  color?: string
  height?: number
  fill?: boolean
}>(), {
  label: 'Bids / minute',
  color: '#9333ea',
  height: 300,
  fill: true,
})

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      label: props.label,
      data: props.data,
      borderColor: props.color,
      backgroundColor: props.fill ? props.color + '20' : 'transparent',
      fill: props.fill,
      tension: 0.4,
      borderWidth: 2,
      pointRadius: 2,
      pointHoverRadius: 5,
      pointBackgroundColor: props.color,
    },
  ],
}))

const chartOptions: ChartOptions<'line'> = {
  responsive: true,
  maintainAspectRatio: false,
  animation: {
    duration: 500,
  },
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      backgroundColor: '#1f2937',
      titleFont: { size: 12 },
      bodyFont: { size: 12 },
      padding: 10,
      cornerRadius: 8,
    },
  },
  scales: {
    x: {
      grid: { display: false },
      ticks: { font: { size: 10 }, color: '#9ca3af', maxTicksLimit: 12 },
    },
    y: {
      grid: { color: '#f3f4f6' },
      ticks: { font: { size: 10 }, color: '#9ca3af' },
      beginAtZero: true,
    },
  },
  interaction: {
    intersect: false,
    mode: 'index',
  },
}
</script>

<template>
  <div :style="{ height: height + 'px' }">
    <Line
      :data="chartData"
      :options="chartOptions"
    />
  </div>
</template>
