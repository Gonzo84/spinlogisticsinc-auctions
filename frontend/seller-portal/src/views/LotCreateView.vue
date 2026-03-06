<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLots } from '@/composables/useLots'
import type { LotFormData } from '@/types'
import type { AxiosError } from 'axios'
import LotForm from '@/components/lots/LotForm.vue'

const router = useRouter()
const { createLot, loading } = useLots()

const submitError = ref<string | null>(null)

const breadcrumbItems = computed(() => [
  { label: 'My Lots', route: '/lots' },
  { label: 'Create Lot' },
])

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
        Create New Lot
      </h1>
      <p class="mt-1 text-sm text-gray-500">
        Fill in the details below to list a new item for auction. You can save as a draft and submit for review later.
      </p>
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
