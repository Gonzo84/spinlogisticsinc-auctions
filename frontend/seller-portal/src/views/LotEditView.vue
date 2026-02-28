<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLots } from '@/composables/useLots'
import type { LotFormData, LotImage } from '@/types'
import type { AxiosError } from 'axios'
import LotForm from '@/components/lots/LotForm.vue'

const route = useRoute()
const router = useRouter()
const { currentLot, loading, error, fetchLot, updateLot } = useLots()

const lotId = computed(() => route.params.id as string)
const submitError = ref<string | null>(null)
const initialDataReady = ref(false)

const initialData = computed<Partial<LotFormData> | undefined>(() => {
  if (!currentLot.value) return undefined
  const lot = currentLot.value
  return {
    brand: lot.brand ?? '',
    title: lot.title,
    description: lot.description,
    categoryId: lot.category ?? '',
    specifications: { ...(lot.specifications ?? {}) },
    startingBid: lot.startingBid,
    reservePrice: lot.reservePrice,
    location: lot.location ? { ...lot.location } : {
      address: '',
      city: '',
      country: '',
      lat: 0,
      lng: 0,
    },
    imageIds: (lot.images ?? []).map((img: LotImage) => img.id),
  }
})

onMounted(async () => {
  await fetchLot(lotId.value)
  initialDataReady.value = true
})

async function handleSubmit(data: LotFormData) {
  submitError.value = null
  try {
    await updateLot(lotId.value, data)
    router.push({ name: 'lot-detail', params: { id: lotId.value } })
  } catch (err: unknown) {
    const axiosErr = err as AxiosError<{ message?: string }>
    submitError.value = axiosErr?.response?.data?.message ?? 'Failed to update lot. Please try again.'
  }
}

function handleCancel() {
  router.push({ name: 'lot-detail', params: { id: lotId.value } })
}
</script>

<template>
  <div>
    <!-- Breadcrumb -->
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
        <router-link
          :to="`/lots/${lotId}`"
          class="hover:text-primary-600"
        >
          {{ currentLot?.title ?? 'Lot' }}
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
        <span class="text-gray-700">Edit</span>
      </div>
      <h1 class="mt-2 text-2xl font-bold text-gray-900">
        Edit Lot
      </h1>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !initialDataReady"
      class="py-12 text-center"
    >
      <svg
        class="mx-auto h-8 w-8 animate-spin text-primary-600"
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

    <!-- Error banner -->
    <div
      v-if="submitError"
      class="mb-6 rounded-lg border border-red-200 bg-red-50 p-4"
    >
      <p class="text-sm font-medium text-red-800">
        {{ submitError }}
      </p>
    </div>

    <!-- Form -->
    <div
      v-if="initialDataReady && initialData"
      class="mx-auto max-w-3xl"
    >
      <LotForm
        :initial-data="initialData"
        submit-label="Save Changes"
        :is-submitting="loading"
        @submit="handleSubmit"
        @cancel="handleCancel"
      />
    </div>
  </div>
</template>
