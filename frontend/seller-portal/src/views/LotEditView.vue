<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLots } from '@/composables/useLots'
import type { LotFormData, LotImage } from '@/types'
import type { AxiosError } from 'axios'
import LotForm from '@/components/lots/LotForm.vue'

const route = useRoute()
const router = useRouter()
const { currentLot, loading, fetchLot, updateLot } = useLots()

const lotId = computed(() => route.params.id as string)
const submitError = ref<string | null>(null)
const initialDataReady = ref(false)

const breadcrumbItems = computed(() => [
  { label: 'My Lots', route: '/lots' },
  { label: currentLot.value?.title ?? 'Lot', route: `/lots/${lotId.value}` },
  { label: 'Edit' },
])

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
      <Breadcrumb :model="breadcrumbItems">
        <template #item="{ item }">
          <router-link
            v-if="item.route"
            :to="item.route"
            class="text-sm text-gray-500 hover:text-primary-600"
          >
            {{ item.label }}
          </router-link>
          <span v-else class="text-sm text-gray-700">{{ item.label }}</span>
        </template>
      </Breadcrumb>
      <h1 class="mt-2 text-2xl font-bold text-gray-900">
        Edit Lot
      </h1>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !initialDataReady"
      class="py-12 text-center"
    >
      <ProgressSpinner strokeWidth="4" style="width: 2rem; height: 2rem" />
    </div>

    <!-- Error banner -->
    <Message
      v-if="submitError"
      severity="error"
      :closable="false"
      class="mb-6"
    >
      {{ submitError }}
    </Message>

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
