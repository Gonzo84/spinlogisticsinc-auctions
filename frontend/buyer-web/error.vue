<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <div class="flex-1 flex items-center justify-center px-4">
      <div class="text-center max-w-md">
        <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-primary-100 mb-6">
          <span class="text-4xl font-bold text-primary">{{ error?.statusCode || 500 }}</span>
        </div>

        <h1 class="text-2xl font-bold text-gray-900 mb-3">
          {{ title }}
        </h1>

        <p class="text-gray-500 mb-8">
          {{ description }}
        </p>

        <div class="flex flex-col sm:flex-row items-center justify-center gap-3">
          <button
            class="px-6 py-2.5 bg-primary text-white font-medium rounded-lg hover:bg-primary-800 transition-colors"
            @click="handleError"
          >
            {{ $t('error.backToHome') }}
          </button>
          <button
            v-if="error?.statusCode !== 404"
            class="px-6 py-2.5 border font-medium rounded-lg hover:bg-gray-50 transition-colors"
            @click="retry"
          >
            {{ $t('error.tryAgain') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { NuxtError } from '#app'

const { t } = useI18n()

const props = defineProps<{
  error: NuxtError
}>()

const title = computed(() => {
  if (props.error?.statusCode === 404) {
    return t('error.notFoundTitle')
  }
  return t('error.serverErrorTitle')
})

const description = computed(() => {
  if (props.error?.statusCode === 404) {
    return t('error.notFoundDescription')
  }
  return t('error.serverErrorDescription')
})

function handleError() {
  clearError({ redirect: '/' })
}

function retry() {
  clearError()
  window.location.reload()
}

useHead({
  title: `${props.error?.statusCode || 'Error'} | EU Auction Platform`,
})
</script>
