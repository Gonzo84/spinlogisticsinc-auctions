<template>
  <div class="space-y-6">
    <!-- Clear All -->
    <div v-if="hasActiveFilters" class="flex items-center justify-between">
      <span class="text-sm font-medium text-gray-700">{{ $t('search.activeFilters') }}</span>
      <button
        class="text-sm text-primary hover:underline"
        @click="clearAll"
      >
        {{ $t('search.clearAll') }}
      </button>
    </div>

    <!-- Category Tree -->
    <div>
      <h3 class="text-sm font-semibold text-gray-900 mb-3">{{ $t('search.category') }}</h3>
      <div class="space-y-1">
        <button
          v-for="cat in categories"
          :key="cat.slug"
          class="w-full flex items-center justify-between px-3 py-2 rounded-lg text-sm transition-colors"
          :class="filters.category === cat.slug
            ? 'bg-primary-50 text-primary font-medium'
            : 'text-gray-700 hover:bg-gray-100'"
          @click="toggleCategory(cat.slug)"
        >
          <span class="flex items-center gap-2">
            <span>{{ cat.icon }}</span>
            <span>{{ cat.name }}</span>
          </span>
          <span class="text-xs text-gray-400">{{ cat.count }}</span>
        </button>
      </div>
    </div>

    <!-- Country Multi-Select -->
    <div>
      <h3 class="text-sm font-semibold text-gray-900 mb-3">{{ $t('search.country') }}</h3>
      <div class="space-y-1.5">
        <label
          v-for="country in countries"
          :key="country.code"
          class="flex items-center gap-2.5 px-3 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer"
        >
          <Checkbox
            :modelValue="selectedCountries.includes(country.code)"
            @update:modelValue="toggleCountry(country.code)"
            :binary="true"
          />
          <span class="text-base">{{ country.flag }}</span>
          <span class="text-sm text-gray-700">{{ country.name }}</span>
        </label>
      </div>
    </div>

    <!-- Price Range -->
    <div>
      <h3 class="text-sm font-semibold text-gray-900 mb-3">{{ $t('search.priceRange') }}</h3>
      <div class="flex items-center gap-2">
        <div class="flex-1">
          <InputNumber
            v-model="priceMin"
            :placeholder="$t('search.min')"
            inputClass="w-full"
            mode="currency"
            currency="EUR"
            locale="en-US"
            @blur="applyPriceRange"
          />
        </div>
        <span class="text-gray-400">-</span>
        <div class="flex-1">
          <InputNumber
            v-model="priceMax"
            :placeholder="$t('search.max')"
            inputClass="w-full"
            mode="currency"
            currency="EUR"
            locale="en-US"
            @blur="applyPriceRange"
          />
        </div>
      </div>
      <!-- Quick Price Buttons -->
      <div class="flex flex-wrap gap-1.5 mt-2">
        <button
          v-for="range in priceRanges"
          :key="range.label"
          class="px-2.5 py-1 text-xs border rounded-full hover:bg-primary-50 hover:border-primary hover:text-primary transition-colors"
          :class="{
            'bg-primary-50 border-primary text-primary': filters.priceMin === range.min && filters.priceMax === range.max
          }"
          @click="setPrice(range.min, range.max)"
        >
          {{ range.label }}
        </button>
      </div>
    </div>

    <!-- Distance -->
    <div>
      <h3 class="text-sm font-semibold text-gray-900 mb-3">{{ $t('search.distance') }}</h3>
      <Select
        v-model="distanceValue"
        :options="distanceOptions"
        optionLabel="label"
        optionValue="value"
        class="w-full"
        @change="applyDistance"
      />
    </div>

    <!-- Reserve Toggle -->
    <div>
      <h3 class="text-sm font-semibold text-gray-900 mb-3">{{ $t('search.reserveStatus') }}</h3>
      <div class="space-y-1.5">
        <label class="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
          <RadioButton
            :modelValue="reserveStatusModel"
            @update:modelValue="setReserve(undefined)"
            value=""
          />
          <span class="text-sm text-gray-700">{{ $t('search.allLots') }}</span>
        </label>
        <label class="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
          <RadioButton
            :modelValue="reserveStatusModel"
            @update:modelValue="setReserve('met')"
            value="met"
          />
          <span class="text-sm text-gray-700">{{ $t('search.reserveMet') }}</span>
        </label>
        <label class="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
          <RadioButton
            :modelValue="reserveStatusModel"
            @update:modelValue="setReserve('not_met')"
            value="not_met"
          />
          <span class="text-sm text-gray-700">{{ $t('search.reserveNotMet') }}</span>
        </label>
        <label class="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-gray-50 cursor-pointer">
          <RadioButton
            :modelValue="reserveStatusModel"
            @update:modelValue="setReserve('no_reserve')"
            value="no_reserve"
          />
          <span class="text-sm text-gray-700">{{ $t('search.noReserve') }}</span>
        </label>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { SearchFilters } from '~/types/search'

interface Props {
  filters: SearchFilters
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:filters': [filters: Partial<SearchFilters>]
}>()

const { t } = useI18n()

const priceMin = ref<number | undefined>(props.filters.priceMin)
const priceMax = ref<number | undefined>(props.filters.priceMax)
const distanceValue = ref<number>(props.filters.distance || 0)

const reserveStatusModel = computed(() => props.filters.reserveStatus || '')

const distanceOptions = computed(() => [
  { label: t('search.anyDistance'), value: 0 },
  { label: '50 km', value: 50 },
  { label: '100 km', value: 100 },
  { label: '250 km', value: 250 },
  { label: '500 km', value: 500 },
  { label: '1000 km', value: 1000 },
])

const selectedCountries = computed(() => props.filters.country || [])

const hasActiveFilters = computed(() => {
  return !!(
    props.filters.category ||
    props.filters.country?.length ||
    props.filters.priceMin ||
    props.filters.priceMax ||
    props.filters.distance ||
    props.filters.reserveStatus
  )
})

const categories = [
  { slug: 'transport', icon: '\uD83D\uDE9A', name: t('categories.transport'), count: 1240 },
  { slug: 'agriculture', icon: '\uD83D\uDE9C', name: t('categories.agriculture'), count: 890 },
  { slug: 'construction', icon: '\uD83C\uDFD7\uFE0F', name: t('categories.construction'), count: 1560 },
  { slug: 'metalworking', icon: '\u2699\uFE0F', name: t('categories.metalworking'), count: 430 },
  { slug: 'woodworking', icon: '\uD83E\uDEB5', name: t('categories.woodworking'), count: 310 },
  { slug: 'food-processing', icon: '\uD83C\uDFED', name: t('categories.foodProcessing'), count: 220 },
  { slug: 'electronics', icon: '\uD83D\uDD0C', name: t('categories.electronics'), count: 670 },
  { slug: 'warehouse', icon: '\uD83D\uDCE6', name: t('categories.warehouse'), count: 540 },
]

const countries = [
  { code: 'NL', flag: '\uD83C\uDDF3\uD83C\uDDF1', name: 'Netherlands' },
  { code: 'DE', flag: '\uD83C\uDDE9\uD83C\uDDEA', name: 'Germany' },
  { code: 'FR', flag: '\uD83C\uDDEB\uD83C\uDDF7', name: 'France' },
  { code: 'BE', flag: '\uD83C\uDDE7\uD83C\uDDEA', name: 'Belgium' },
  { code: 'PL', flag: '\uD83C\uDDF5\uD83C\uDDF1', name: 'Poland' },
  { code: 'IT', flag: '\uD83C\uDDEE\uD83C\uDDF9', name: 'Italy' },
  { code: 'RO', flag: '\uD83C\uDDF7\uD83C\uDDF4', name: 'Romania' },
  { code: 'ES', flag: '\uD83C\uDDEA\uD83C\uDDF8', name: 'Spain' },
  { code: 'AT', flag: '\uD83C\uDDE6\uD83C\uDDF9', name: 'Austria' },
]

const priceRanges = [
  { label: '< 1K', min: 0, max: 1000 },
  { label: '1K - 5K', min: 1000, max: 5000 },
  { label: '5K - 25K', min: 5000, max: 25000 },
  { label: '25K - 100K', min: 25000, max: 100000 },
  { label: '> 100K', min: 100000, max: undefined },
]

function toggleCategory(slug: string) {
  emit('update:filters', {
    category: props.filters.category === slug ? undefined : slug,
  })
}

function toggleCountry(code: string) {
  const current = [...(props.filters.country || [])]
  const index = current.indexOf(code)

  if (index >= 0) {
    current.splice(index, 1)
  } else {
    current.push(code)
  }

  emit('update:filters', {
    country: current.length > 0 ? current : undefined,
  })
}

function applyPriceRange() {
  emit('update:filters', {
    priceMin: priceMin.value || undefined,
    priceMax: priceMax.value || undefined,
  })
}

function setPrice(min: number | undefined, max: number | undefined) {
  priceMin.value = min
  priceMax.value = max
  applyPriceRange()
}

function applyDistance() {
  emit('update:filters', {
    distance: distanceValue.value || undefined,
  })
}

function setReserve(status: string | undefined) {
  emit('update:filters', {
    reserveStatus: status,
  })
}

function clearAll() {
  priceMin.value = undefined
  priceMax.value = undefined
  distanceValue.value = 0
  emit('update:filters', {
    category: undefined,
    country: undefined,
    priceMin: undefined,
    priceMax: undefined,
    distance: undefined,
    reserveStatus: undefined,
  })
}

watch(() => props.filters, (newFilters) => {
  priceMin.value = newFilters.priceMin
  priceMax.value = newFilters.priceMax
  distanceValue.value = newFilters.distance || 0
}, { deep: true })
</script>
