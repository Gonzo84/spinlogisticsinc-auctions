<script setup lang="ts">
import { ref } from 'vue'
import { useLotIntake } from '@/composables/useLotIntake'
import { useToast } from 'primevue/usetoast'
import LotIntakeForm from '@/components/intake/LotIntakeForm.vue'
import type { LotIntakeRequest } from '@/types'

const toast = useToast()
const { loading, submitIntake } = useLotIntake()
const formRef = ref<InstanceType<typeof LotIntakeForm> | null>(null)

async function handleSubmit(data: LotIntakeRequest) {
  const result = await submitIntake(data)
  if (result) {
    toast.add({
      severity: 'success',
      summary: 'Lot Intake Submitted',
      detail: `Lot "${result.title}" has been submitted successfully.`,
      life: 5000,
    })
    formRef.value?.resetForm()
  } else {
    toast.add({
      severity: 'error',
      summary: 'Submission Failed',
      detail: 'Failed to submit lot intake. Please try again.',
      life: 5000,
    })
  }
}
</script>

<template>
  <div>
    <!-- Header -->
    <div class="mb-6">
      <h1 class="text-2xl font-bold text-gray-900">Lot Intake</h1>
      <p class="mt-1 text-sm text-gray-500">
        Submit a new lot for intake processing
      </p>
    </div>

    <!-- Form -->
    <div class="card">
      <LotIntakeForm
        ref="formRef"
        :loading="loading"
        @submit="handleSubmit"
      />
    </div>
  </div>
</template>
