<script setup lang="ts">
import { ref, computed, watch } from 'vue'

export interface Column {
  key: string
  label: string
  sortable?: boolean
  align?: 'left' | 'center' | 'right'
  width?: string
}

const props = withDefaults(defineProps<{
  columns: Column[]
  data: Record<string, any>[]
  loading?: boolean
  searchable?: boolean
  searchPlaceholder?: string
  pageSize?: number
  totalItems?: number
  currentPage?: number
  serverSide?: boolean
  emptyMessage?: string
  emptyIcon?: string
}>(), {
  loading: false,
  searchable: true,
  searchPlaceholder: 'Search...',
  pageSize: 20,
  totalItems: 0,
  currentPage: 1,
  serverSide: false,
  emptyMessage: 'No data found.',
})

const emit = defineEmits<{
  search: [query: string]
  sort: [key: string, direction: 'asc' | 'desc']
  'page-change': [page: number]
}>()

const searchQuery = ref('')
const sortKey = ref('')
const sortDirection = ref<'asc' | 'desc'>('asc')

const totalPages = computed(() => {
  if (props.serverSide) {
    return Math.ceil(props.totalItems / props.pageSize)
  }
  return Math.ceil(filteredData.value.length / props.pageSize)
})

const filteredData = computed(() => {
  if (props.serverSide) return props.data

  let result = [...props.data]

  // Client-side search
  if (searchQuery.value.trim()) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter((row) =>
      props.columns.some((col) => {
        const val = row[col.key]
        return val != null && String(val).toLowerCase().includes(q)
      })
    )
  }

  // Client-side sort
  if (sortKey.value) {
    result.sort((a, b) => {
      const aVal = a[sortKey.value]
      const bVal = b[sortKey.value]
      if (aVal == null) return 1
      if (bVal == null) return -1
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDirection.value === 'asc' ? aVal - bVal : bVal - aVal
      }
      const cmp = String(aVal).localeCompare(String(bVal))
      return sortDirection.value === 'asc' ? cmp : -cmp
    })
  }

  return result
})

const paginatedData = computed(() => {
  if (props.serverSide) return props.data
  const start = (props.currentPage - 1) * props.pageSize
  return filteredData.value.slice(start, start + props.pageSize)
})

const displayTotal = computed(() => {
  return props.serverSide ? props.totalItems : filteredData.value.length
})

function handleSort(key: string) {
  if (sortKey.value === key) {
    sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortDirection.value = 'asc'
  }
  emit('sort', sortKey.value, sortDirection.value)
}

let searchTimeout: ReturnType<typeof setTimeout>
watch(searchQuery, (val) => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    emit('search', val)
  }, 300)
})

function goToPage(page: number) {
  if (page >= 1 && page <= totalPages.value) {
    emit('page-change', page)
  }
}

function getAlignClass(align?: string): string {
  switch (align) {
    case 'right': return 'text-right'
    case 'center': return 'text-center'
    default: return 'text-left'
  }
}
</script>

<template>
  <div>
    <!-- Search bar -->
    <div
      v-if="searchable"
      class="mb-4"
    >
      <div class="relative">
        <svg
          class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
          />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          :placeholder="searchPlaceholder"
          class="input pl-10"
        >
      </div>
    </div>

    <!-- Table -->
    <div class="table-container">
      <!-- Loading overlay -->
      <div
        v-if="loading"
        class="flex items-center justify-center py-12"
      >
        <svg
          class="h-8 w-8 animate-spin text-primary-600"
          fill="none"
          viewBox="0 0 24 24"
        >
          <circle
            class="opacity-25"
            cx="12"
            cy="12"
            r="10"
            stroke="currentColor"
            stroke-width="4"
          />
          <path
            class="opacity-75"
            fill="currentColor"
            d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
          />
        </svg>
      </div>

      <!-- Table content -->
      <table
        v-else
        class="w-full"
      >
        <thead>
          <tr>
            <th
              v-for="col in columns"
              :key="col.key"
              :class="['table-header', getAlignClass(col.align)]"
              :style="col.width ? { width: col.width } : {}"
            >
              <button
                v-if="col.sortable"
                class="inline-flex items-center gap-1 hover:text-gray-700"
                @click="handleSort(col.key)"
              >
                {{ col.label }}
                <svg
                  v-if="sortKey === col.key"
                  class="h-3 w-3"
                  :class="{ 'rotate-180': sortDirection === 'desc' }"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M5 15l7-7 7 7"
                  />
                </svg>
                <svg
                  v-else
                  class="h-3 w-3 text-gray-300"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M8 9l4-4 4 4m0 6l-4 4-4-4"
                  />
                </svg>
              </button>
              <span v-else>{{ col.label }}</span>
            </th>
            <th class="table-header text-right">
              <slot name="header-actions" />
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="paginatedData.length === 0">
            <td
              :colspan="columns.length + 1"
              class="px-4 py-12 text-center text-sm text-gray-500"
            >
              {{ emptyMessage }}
            </td>
          </tr>
          <tr
            v-for="(row, index) in paginatedData"
            :key="index"
            class="table-row"
          >
            <td
              v-for="col in columns"
              :key="col.key"
              :class="['table-cell', getAlignClass(col.align)]"
            >
              <slot
                :name="`cell-${col.key}`"
                :row="row"
                :value="row[col.key]"
              >
                {{ row[col.key] }}
              </slot>
            </td>
            <td class="table-cell text-right">
              <slot
                name="row-actions"
                :row="row"
              />
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div
        v-if="totalPages > 1"
        class="flex items-center justify-between border-t border-gray-200 px-4 py-3"
      >
        <p class="text-sm text-gray-500">
          Showing {{ (currentPage - 1) * pageSize + 1 }} to
          {{ Math.min(currentPage * pageSize, displayTotal) }}
          of {{ displayTotal }}
        </p>
        <div class="flex gap-1">
          <button
            class="btn btn-sm bg-white text-gray-600 hover:bg-gray-50 border border-gray-300"
            :disabled="currentPage <= 1"
            @click="goToPage(currentPage - 1)"
          >
            Previous
          </button>
          <button
            class="btn btn-sm bg-white text-gray-600 hover:bg-gray-50 border border-gray-300"
            :disabled="currentPage >= totalPages"
            @click="goToPage(currentPage + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
