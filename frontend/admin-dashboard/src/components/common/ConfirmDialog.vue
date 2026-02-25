<script setup lang="ts">
const props = withDefaults(defineProps<{
  open: boolean
  title: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  variant?: 'danger' | 'warning' | 'info'
  loading?: boolean
}>(), {
  confirmLabel: 'Confirm',
  cancelLabel: 'Cancel',
  variant: 'danger',
  loading: false,
})

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()

const btnClass = {
  danger: 'btn-danger',
  warning: 'btn-warning',
  info: 'btn-primary',
}

const iconBg = {
  danger: 'bg-red-100',
  warning: 'bg-amber-100',
  info: 'bg-blue-100',
}

const iconColor = {
  danger: 'text-red-600',
  warning: 'text-amber-600',
  info: 'text-blue-600',
}
</script>

<template>
  <teleport to="body">
    <transition
      enter-active-class="transition-opacity duration-200"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition-opacity duration-150"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div
        v-if="open"
        class="fixed inset-0 z-[100] flex items-center justify-center p-4"
      >
        <!-- Backdrop -->
        <div
          class="absolute inset-0 bg-gray-900/50"
          @click="emit('cancel')"
        />

        <!-- Dialog -->
        <div class="relative w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
          <div class="flex items-start gap-4">
            <!-- Icon -->
            <div :class="['flex h-10 w-10 shrink-0 items-center justify-center rounded-full', iconBg[variant]]">
              <svg
                v-if="variant === 'danger'"
                :class="['h-5 w-5', iconColor[variant]]"
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
              <svg
                v-else-if="variant === 'warning'"
                :class="['h-5 w-5', iconColor[variant]]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <svg
                v-else
                :class="['h-5 w-5', iconColor[variant]]"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>

            <!-- Content -->
            <div class="flex-1">
              <h3 class="text-lg font-semibold text-gray-900">
                {{ title }}
              </h3>
              <p class="mt-2 text-sm text-gray-500">
                {{ message }}
              </p>

              <!-- Slot for extra content (e.g., reason input) -->
              <div class="mt-3">
                <slot />
              </div>
            </div>
          </div>

          <!-- Actions -->
          <div class="mt-6 flex justify-end gap-3">
            <button
              class="btn-secondary"
              :disabled="loading"
              @click="emit('cancel')"
            >
              {{ cancelLabel }}
            </button>
            <button
              :class="btnClass[variant]"
              :disabled="loading"
              @click="emit('confirm')"
            >
              <svg
                v-if="loading"
                class="h-4 w-4 animate-spin"
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
              {{ confirmLabel }}
            </button>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>
