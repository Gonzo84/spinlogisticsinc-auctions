<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import ImageUploader from './ImageUploader.vue'
import { useLots } from '@/composables/useLots'
import type { LotFormData, Category } from '@/types'

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

const { fetchCategories } = useLots()
const categories = ref<Category[]>([])

onMounted(async () => {
  categories.value = await fetchCategories()
})

const form = reactive<LotFormData>({
  brand: props.initialData?.brand ?? '',
  title: props.initialData?.title ?? '',
  description: props.initialData?.description ?? '',
  categoryId: props.initialData?.categoryId ?? '',
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
  images: props.initialData?.images ?? [],
})

const errors = ref<Record<string, string>>({})


const countries = [
  { code: 'AT', name: 'Austria' },
  { code: 'BE', name: 'Belgium' },
  { code: 'BG', name: 'Bulgaria' },
  { code: 'HR', name: 'Croatia' },
  { code: 'CY', name: 'Cyprus' },
  { code: 'CZ', name: 'Czech Republic' },
  { code: 'DK', name: 'Denmark' },
  { code: 'EE', name: 'Estonia' },
  { code: 'FI', name: 'Finland' },
  { code: 'FR', name: 'France' },
  { code: 'DE', name: 'Germany' },
  { code: 'GR', name: 'Greece' },
  { code: 'HU', name: 'Hungary' },
  { code: 'IE', name: 'Ireland' },
  { code: 'IT', name: 'Italy' },
  { code: 'LV', name: 'Latvia' },
  { code: 'LT', name: 'Lithuania' },
  { code: 'LU', name: 'Luxembourg' },
  { code: 'MT', name: 'Malta' },
  { code: 'NL', name: 'Netherlands' },
  { code: 'PL', name: 'Poland' },
  { code: 'PT', name: 'Portugal' },
  { code: 'RO', name: 'Romania' },
  { code: 'SK', name: 'Slovakia' },
  { code: 'SI', name: 'Slovenia' },
  { code: 'ES', name: 'Spain' },
  { code: 'SE', name: 'Sweden' },
]

const categoryOptions = computed(() => [
  { label: 'Select a category', value: '' },
  ...categories.value.map((cat) => ({ label: cat.name, value: cat.id })),
])

const countryOptions = [
  { label: 'Select country', value: '' },
  ...countries.map((c) => ({ label: c.name, value: c.code })),
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

  if (!form.brand.trim()) errors.value.brand = 'Brand is required'

  if (!form.categoryId) errors.value.category = 'Category is required'

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
  <form
    class="space-y-8"
    @submit.prevent="handleSubmit"
  >
    <!-- Basic Information -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">
        Basic Information
      </h3>
      <div class="space-y-4">
        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <label
              class="label"
              for="lot-title"
            >Lot Title *</label>
            <InputText
              id="lot-title"
              v-model="form.title"
              :invalid="!!errors.title"
              placeholder="e.g., Komatsu PC200-8 Hydraulic Excavator"
              class="w-full"
            />
            <p
              v-if="errors.title"
              class="mt-1 text-sm text-red-600"
            >
              {{ errors.title }}
            </p>
          </div>

          <div>
            <label
              class="label"
              for="lot-brand"
            >Brand / Manufacturer *</label>
            <InputText
              id="lot-brand"
              v-model="form.brand"
              :invalid="!!errors.brand"
              placeholder="e.g., Caterpillar, Komatsu, Liebherr"
              class="w-full"
            />
            <p
              v-if="errors.brand"
              class="mt-1 text-sm text-red-600"
            >
              {{ errors.brand }}
            </p>
          </div>
        </div>

        <div>
          <label
            class="label"
            for="lot-description"
          >Description *</label>
          <Textarea
            id="lot-description"
            v-model="form.description"
            rows="5"
            :invalid="!!errors.description"
            placeholder="Detailed description of the item including condition, history, and any defects..."
            class="w-full"
          />
          <div class="mt-1 flex justify-between">
            <p
              v-if="errors.description"
              class="text-sm text-red-600"
            >
              {{ errors.description }}
            </p>
            <p class="ml-auto text-xs text-gray-400">
              {{ form.description.length }} characters
            </p>
          </div>
        </div>

        <div>
          <label
            class="label"
            for="lot-category"
          >Category *</label>
          <Select
            id="lot-category"
            v-model="form.categoryId"
            :options="categoryOptions"
            optionLabel="label"
            optionValue="value"
            :invalid="!!errors.category"
            placeholder="Select a category"
            class="w-full"
          />
          <p
            v-if="errors.category"
            class="mt-1 text-sm text-red-600"
          >
            {{ errors.category }}
          </p>
        </div>
      </div>
    </div>

    <!-- Specifications -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">
        Specifications
      </h3>
      <div
        v-if="specEntries.length > 0"
        class="mb-4 space-y-2"
      >
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
            <i class="pi pi-times" style="font-size: 0.875rem" />
          </button>
        </div>
      </div>

      <div class="flex gap-2">
        <InputText
          v-model="newSpecKey"
          placeholder="e.g., Weight"
          class="flex-1"
          @keyup.enter="addSpecification"
        />
        <InputText
          v-model="newSpecValue"
          placeholder="e.g., 2,500 kg"
          class="flex-1"
          @keyup.enter="addSpecification"
        />
        <Button
          type="button"
          label="Add"
          severity="secondary"
          size="small"
          @click="addSpecification"
        />
      </div>
    </div>

    <!-- Pricing -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">
        Pricing
      </h3>
      <div class="grid gap-4 sm:grid-cols-2">
        <div>
          <label
            class="label"
            for="lot-starting-bid"
          >Starting Bid (EUR) *</label>
          <InputNumber
            id="lot-starting-bid"
            v-model="form.startingBid"
            mode="currency"
            currency="EUR"
            locale="en-US"
            :min="1"
            :invalid="!!errors.startingBid"
            class="w-full"
          />
          <p
            v-if="errors.startingBid"
            class="mt-1 text-sm text-red-600"
          >
            {{ errors.startingBid }}
          </p>
        </div>

        <div>
          <div class="mb-1.5 flex items-center justify-between">
            <label
              class="text-sm font-medium text-gray-700"
              for="lot-reserve-price"
            >Reserve Price (EUR)</label>
            <label class="flex items-center gap-2">
              <Checkbox
                v-model="hasReserve"
                :binary="true"
                @change="toggleReserve"
              />
              <span class="text-xs text-gray-500">Set reserve</span>
            </label>
          </div>
          <InputNumber
            id="lot-reserve-price"
            v-model="form.reservePrice"
            mode="currency"
            currency="EUR"
            locale="en-US"
            :min="0"
            :invalid="!!errors.reservePrice"
            :disabled="!hasReserve"
            class="w-full"
          />
          <p
            v-if="errors.reservePrice"
            class="mt-1 text-sm text-red-600"
          >
            {{ errors.reservePrice }}
          </p>
          <p
            v-else
            class="mt-1 text-xs text-gray-400"
          >
            Minimum price you are willing to accept
          </p>
        </div>
      </div>
    </div>

    <!-- Location -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">
        Item Location
      </h3>
      <div class="grid gap-4 sm:grid-cols-2">
        <div class="sm:col-span-2">
          <label
            class="label"
            for="lot-address"
          >Street Address</label>
          <InputText
            id="lot-address"
            v-model="form.location.address"
            placeholder="123 Industrial Road"
            class="w-full"
          />
        </div>

        <div>
          <label
            class="label"
            for="lot-city"
          >City *</label>
          <InputText
            id="lot-city"
            v-model="form.location.city"
            :invalid="!!errors.city"
            placeholder="Amsterdam"
            class="w-full"
          />
          <p
            v-if="errors.city"
            class="mt-1 text-sm text-red-600"
          >
            {{ errors.city }}
          </p>
        </div>

        <div>
          <label
            class="label"
            for="lot-country"
          >Country *</label>
          <Select
            id="lot-country"
            v-model="form.location.country"
            :options="countryOptions"
            optionLabel="label"
            optionValue="value"
            :invalid="!!errors.country"
            placeholder="Select country"
            class="w-full"
          />
          <p
            v-if="errors.country"
            class="mt-1 text-sm text-red-600"
          >
            {{ errors.country }}
          </p>
        </div>
      </div>
    </div>

    <!-- Images -->
    <div class="card">
      <h3 class="mb-4 text-lg font-semibold text-gray-900">
        Images
      </h3>
      <p class="mb-4 text-sm text-gray-500">
        Upload up to 10 images. The first image will be used as the primary thumbnail. You can reorder and set a different primary image after uploading.
      </p>
      <ImageUploader
        v-model="form.imageIds"
        :max-files="10"
        @update:images="form.images = $event"
      />
    </div>

    <!-- Actions -->
    <div class="flex items-center justify-end gap-3 border-t border-gray-200 pt-6">
      <Button
        type="button"
        label="Cancel"
        severity="secondary"
        @click="emit('cancel')"
      />
      <Button
        type="submit"
        :label="isSubmitting ? 'Saving...' : submitLabel"
        :icon="isSubmitting ? 'pi pi-spin pi-spinner' : undefined"
        :disabled="isSubmitting"
        @click.prevent="handleSubmit"
      />
    </div>
  </form>
</template>
