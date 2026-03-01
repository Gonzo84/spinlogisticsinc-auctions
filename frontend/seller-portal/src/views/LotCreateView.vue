<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useLots } from '@/composables/useLots'
import type { LotFormData } from '@/types'
import type { AxiosError } from 'axios'
import LotForm from '@/components/lots/LotForm.vue'

const router = useRouter()
const { createLot, loading } = useLots()

const submitError = ref<string | null>(null)

async function handleSubmit(data: LotFormData) {
  submitError.value = null
  try {
    const lot = await createLot(data)
    router.push({ name: 'lot-detail', params: { id: lot.id } })
  } catch (err: unknown) {
    const axiosErr = err as AxiosError<{ message?: string }>
    submitError.value = axiosErr?.response?.data?.message ?? 'Failed to create lot. Please try again.'
  }
}

function handleCancel() {
  router.push({ name: 'lots' })
}
</script>

<template>
  <div>
    <!-- Page header -->
    <div class="mb-6">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <router-link
          to="/lots"
          class="hover:text-primary-600"
        >
          My Lots
        </router-link>
        <svg
          class="h-4 w-4"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M9 5l7 7-7 7"
          />
        </svg>
        <span class="text-gray-700">Create Lot</span>
      </div>
      <h1 class="mt-2 text-2xl font-bold text-gray-900">
        Create New Lot
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Fill in the details below to list a new item for auction. You can save as a draft and submit for review later.
      </p>
    </div>

    <!-- Error banner -->
    <div
      v-if="submitError"
      class="mb-6 rounded-lg border border-red-200 bg-red-50 p-4"
    >
      <div class="flex items-start gap-3">
        <svg
          class="mt-0.5 h-5 w-5 shrink-0 text-red-400"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
          />
        </svg>
        <div>
          <p class="text-sm font-medium text-red-800">
            {{ submitError }}
          </p>
        </div>
      </div>
    </div>

    <!-- Form -->
    <div class="mx-auto max-w-3xl">
      <LotForm
        submit-label="Create Lot"
        :is-submitting="loading"
        @submit="handleSubmit"
        @cancel="handleCancel"
      />
    </div>
  </div>
</template>
