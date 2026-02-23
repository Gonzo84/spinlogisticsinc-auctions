<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import ImageUploader from './ImageUploader.vue'
import type { LotFormData } from '@/composables/useLots'

const props = withDefaults(defineProps<{
  initialData?: Partial<LotFormData>
  submitLabel?: string
  isSubmitting?: boolean
}>(), {
  submitLabel: 'Create Lot',
  isSubmitting: false,
})

const emit = defineEmits<{
  submit: [data: LotFormData]
  cancel: []
}>()

const form = reactive<LotFormData>({
  title: props.initialData?.title ?? '',
  description: props.initialData?.description ?? '',
  category: props.initialData?.category ?? '',
  specifications: props.initialData?.specifications ?? {},
  startingBid: props.initialData?.startingBid ?? 0,
  reservePrice: props.initialData?.reservePrice ?? null,
  location: {
    address: props.initialData?.location?.address ?? '',
    city: props.initialData?.location?.city ?? '',
    country: props.initialData?.location?.country ?? '',
    lat: props.initialData?.location?.lat ?? 0,
    lng: props.initialData?.location?.lng ?? 0,
  },
  imageIds: props.initialData?.imageIds ?? [],
})

const errors = ref<Record<string, string>>({})

const categories = [
  'Industrial Equipment',
  'Construction Machinery',
  'Vehicles & Transport',
  'Electronics & IT',
  'Office Equipment',
  'Agricultural Machinery',
  'Medical Equipment',
  'Catering Equipment',
  'Warehouse & Logistics',
  'Other',
]

const countries = [
  'Austria', 'Belgium', 'Bulgaria', 'Croatia', 'Cyprus', 'Czech Republic',
  'Denmark', 'Estonia', 'Finland', 'France', 'Germany', 'Greece',
  'Hungary', 'Ireland', 'Italy', 'Latvia', 'Lithuania', 'Luxembourg',
  'Malta', 'Netherlands', 'Poland', 'Portugal', 'Romania', 'Slovakia',
  'Slovenia', 'Spain', 'Sweden',
]

// Specification management
const newSpecKey = ref('')
const newSpecValue = ref('')

function addSpecification() {
  if (newSpecKey.value.trim() && newSpecValue.value.trim()) {
    form.specifications[newSpecKey.value.trim()] = newSpecValue.value.trim()
    newSpecKey.value = ''
    newSpecValue.value = ''
  }
}

function removeSpecification(key: string) {
  delete form.specifications[key]
}

const specEntries = computed(() => Object.entries(form.specifications))

// Reserve price toggle
const hasReserve = ref(form.reservePrice !== null)
function toggleReserve() {
  if (hasReserve.value) {
    form.reservePrice = form.startingBid || 0
  } else {
    form.reservePrice = null
  }
}

// Validation
function validate(): boolean {
  errors.value = {}

  if (!form.title.trim()) errors.value.title = 'Title is required'
  else if (form.title.length < 5) errors.value.title = 'Title must be at least 5 characters'

  if (!form.description.trim()) errors.value.description = 'Description is required'
  else if (form.description.length < 20) errors.value.description = 'Description must be at least 20 characters'

  if (!form.category) errors.value.category = 'Category is required'

  if (!form.startingBid || form.startingBid <= 0) errors.value.startingBid = 'Starting bid must be greater than 0'

  if (hasReserve.value && form.reservePrice !== null) {
    if (form.reservePrice < form.startingBid) {
      errors.value.reservePrice = 'Reserve price must be at or above starting bid'
    }
  }

  if (!form.location.city.trim()) errors.value.city = 'City is required'
  if (!form.location.country) errors.value.country = 'Country is required'

  return Object.keys(errors.value).length === 0
}

function handleSubmit() {
  if (validate()) {
    emit('submit', { ...form })
  }
}
</script>

<template>
  <form class="space-y-8" @submit.prevent="handleSubmit">
    <!-- Basic Information -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">Basic Information</h3>
      <div class="space-y-4">
        <div>
          <label class="label" for="lot-title">Lot Title *</label>
          <input
            id="lot-title"
            v-model="form.title"
            type="text"
            class="input"
            :class="errors.title && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
            placeholder="e.g., Komatsu PC200-8 Hydraulic Excavator"
          />
          <p v-if="errors.title" class="mt-1 text-sm text-red-600">{{ errors.title }}</p>
        </div>

        <div>
          <label class="label" for="lot-description">Description *</label>
          <textarea
            id="lot-description"
            v-model="form.description"
            rows="5"
            class="input resize-y"
            :class="errors.description && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
            placeholder="Detailed description of the item including condition, history, and any defects..."
          />
          <div class="mt-1 flex justify-between">
            <p v-if="errors.description" class="text-sm text-red-600">{{ errors.description }}</p>
            <p class="ml-auto text-xs text-gray-400">{{ form.description.length }} characters</p>
          </div>
        </div>

        <div>
          <label class="label" for="lot-category">Category *</label>
          <select
            id="lot-category"
            v-model="form.category"
            class="input"
            :class="errors.category && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
          >
            <option value="">Select a category</option>
            <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
          </select>
          <p v-if="errors.category" class="mt-1 text-sm text-red-600">{{ errors.category }}</p>
        </div>
      </div>
    </div>

    <!-- Specifications -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">Specifications</h3>
      <div v-if="specEntries.length > 0" class="mb-4 space-y-2">
        <div
          v-for="[key, value] in specEntries"
          :key="key"
          class="flex items-center gap-3 rounded-lg bg-gray-50 px-3 py-2"
        >
          <span class="text-sm font-medium text-gray-700">{{ key }}:</span>
          <span class="flex-1 text-sm text-gray-600">{{ value }}</span>
          <button
            type="button"
            class="text-gray-400 hover:text-red-500"
            @click="removeSpecification(key)"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>

      <div class="flex gap-2">
        <input
          v-model="newSpecKey"
          type="text"
          class="input flex-1"
          placeholder="e.g., Weight"
          @keyup.enter="addSpecification"
        />
        <input
          v-model="newSpecValue"
          type="text"
          class="input flex-1"
          placeholder="e.g., 2,500 kg"
          @keyup.enter="addSpecification"
        />
        <button
          type="button"
          class="btn-secondary btn-sm"
          @click="addSpecification"
        >
          Add
        </button>
      </div>
    </div>

    <!-- Pricing -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">Pricing</h3>
      <div class="grid gap-4 sm:grid-cols-2">
        <div>
          <label class="label" for="lot-starting-bid">Starting Bid (EUR) *</label>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-500">EUR</span>
            <input
              id="lot-starting-bid"
              v-model.number="form.startingBid"
              type="number"
              min="1"
              step="0.01"
              class="input pl-12"
              :class="errors.startingBid && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
              placeholder="0.00"
            />
          </div>
          <p v-if="errors.startingBid" class="mt-1 text-sm text-red-600">{{ errors.startingBid }}</p>
        </div>

        <div>
          <div class="mb-1.5 flex items-center justify-between">
            <label class="text-sm font-medium text-gray-700" for="lot-reserve-price">Reserve Price (EUR)</label>
            <label class="flex items-center gap-2">
              <input
                v-model="hasReserve"
                type="checkbox"
                class="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                @change="toggleReserve"
              />
              <span class="text-xs text-gray-500">Set reserve</span>
            </label>
          </div>
          <div class="relative">
            <span class="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-500">EUR</span>
            <input
              id="lot-reserve-price"
              v-model.number="form.reservePrice"
              type="number"
              min="0"
              step="0.01"
              class="input pl-12"
              :class="errors.reservePrice && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
              :disabled="!hasReserve"
              placeholder="0.00"
            />
          </div>
          <p v-if="errors.reservePrice" class="mt-1 text-sm text-red-600">{{ errors.reservePrice }}</p>
          <p v-else class="mt-1 text-xs text-gray-400">Minimum price you are willing to accept</p>
        </div>
      </div>
    </div>

    <!-- Location -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">Item Location</h3>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label class="label" for="lot-address">Street Address</label>
          <input
            id="lot-address"
            v-model="form.location.address"
            type="text"
            class="input"
            placeholder="123 Industrial Road"
          />
        </div>

        <div>
          <label class="label" for="lot-city">City *</label>
          <input
            id="lot-city"
            v-model="form.location.city"
            type="text"
            class="input"
            :class="errors.city && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
            placeholder="Amsterdam"
          />
          <p v-if="errors.city" class="mt-1 text-sm text-red-600">{{ errors.city }}</p>
        </div>

        <div>
          <label class="label" for="lot-country">Country *</label>
          <select
            id="lot-country"
            v-model="form.location.country"
            class="input"
            :class="errors.country && 'border-red-300 focus:border-red-500 focus:ring-red-500/20'"
          >
            <option value="">Select country</option>
            <option v-for="c in countries" :key="c" :value="c">{{ c }}</option>
          </select>
          <p v-if="errors.country" class="mt-1 text-sm text-red-600">{{ errors.country }}</p>
        </div>
      </div>
    </div>

    <!-- Images -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">Images</h3>
      <p class="mb-4 text-sm text-gray-500">
        Upload up to 10 images. The first image will be used as the primary thumbnail. You can reorder and set a different primary image after uploading.
      </p>
      <ImageUploader v-model="form.imageIds" :max-files="10" />
    </div>

    <!-- Actions -->
    <div class="flex items-center justify-end gap-3 border-t border-gray-200 pt-6">
      <button
        type="button"
        class="btn-secondary"
        @click="emit('cancel')"
      >
        Cancel
      </button>
      <button
        type="submit"
        class="btn-primary"
        :disabled="isSubmitting"
      >
        <svg v-if="isSubmitting" class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
        {{ isSubmitting ? 'Saving...' : submitLabel }}
      </button>
    </div>
  </form>
</template>
