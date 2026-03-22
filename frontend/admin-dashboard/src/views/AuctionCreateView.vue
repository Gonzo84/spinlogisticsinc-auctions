<script setup lang="ts">
import { reactive, ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuctions, type AuctionCreatePayload } from '@/composables/useAuctions'
import { useApi } from '@/composables/useApi'
import type { ApprovedLot } from '@/types/lot'
import type { ApiResponse, PagedResponse } from '@/types/api'
import { useToast } from 'primevue/usetoast'

interface RawLotSummary {
  id?: string
  title?: string
  brand?: string
  sellerId?: string
  startingBid?: number
  locationCountry?: string
  locationCity?: string
}

const router = useRouter()
const toast = useToast()
const { createAuction, loading, error } = useAuctions()
const { get } = useApi()

const approvedLots = ref<ApprovedLot[]>([])
const lotsLoading = ref(false)
const selectedLotId = ref<string | null>(null)
const selectedLot = ref<ApprovedLot | null>(null)

const PLATFORM_BRANDS = [
  { code: 'troostwijk', label: 'Troostwijk' },
  { code: 'surplex', label: 'Surplex' },
  { code: 'industrial-auctions', label: 'Industrial Auctions' },
  { code: 'spc', label: 'SPC' },
  { code: 'spin-logistics', label: 'Spin Logistics' },
  { code: 'ritchie-bros', label: 'Ritchie Bros' },
  { code: 'purple-wave', label: 'Purple Wave' },
  { code: 'bidadoo', label: 'Bidadoo' },
  { code: 'govplanet', label: 'GovPlanet' },
  { code: 'proxibid', label: 'Proxibid' },
  { code: 'iron-planet', label: 'Iron Planet' },
  { code: 'custom', label: 'Custom' },
]

const CURRENCY_OPTIONS = [
  { label: 'EUR', value: 'EUR' },
  { label: 'USD', value: 'USD' },
  { label: 'GBP', value: 'GBP' },
]

const breadcrumbItems = [
  { label: 'Auctions', to: '/auctions' },
  { label: 'Create Auction' },
]

const form = reactive({
  startTime: null as Date | null,
  endTime: null as Date | null,
  currency: 'USD',
  brand: 'spin-logistics',
})

const lotOptions = computed(() =>
  approvedLots.value.map(lot => ({
    label: `${lot.title} (${lot.brand} - ${lot.locationCity}, ${lot.locationCountry}) [${lot.id.slice(-8)}]`,
    value: lot.id,
  }))
)

const lotPlaceholder = computed(() =>
  lotsLoading.value ? 'Loading lots...' : 'Select an approved lot'
)

const isSubmitDisabled = computed(() => {
  return loading.value || submitting.value || !selectedLotId.value
})

const errors = ref<Record<string, string>>({})

onMounted(async () => {
  await fetchApprovedLots()
})

function normalizeRawLot(lot: RawLotSummary): ApprovedLot {
  return {
    id: lot.id ?? '',
    title: lot.title ?? '',
    brand: lot.brand ?? '',
    sellerId: lot.sellerId ?? '',
    startingBid: lot.startingBid ?? 1,
    locationCountry: lot.locationCountry ?? '',
    locationCity: lot.locationCity ?? '',
  }
}

async function fetchApprovedLots() {
  lotsLoading.value = true
  try {
    const raw = await get<ApiResponse<PagedResponse<RawLotSummary>> | PagedResponse<RawLotSummary>>('/lots', { params: { status: 'APPROVED', pageSize: 100 } })
    const unwrapped = raw as ApiResponse<PagedResponse<RawLotSummary>>
    const data = unwrapped?.data ?? (raw as PagedResponse<RawLotSummary>)
    const items = data?.items ?? []
    approvedLots.value = items.map(normalizeRawLot)
  } catch {
    approvedLots.value = []
  } finally {
    lotsLoading.value = false
  }
}

async function onLotSelected() {
  // PrimeVue Select may provide the value as an object or string depending on configuration
  const lotId = typeof selectedLotId.value === 'object' && selectedLotId.value !== null
    ? (selectedLotId.value as unknown as Record<string, string>).value ?? ''
    : selectedLotId.value ?? ''
  if (typeof lotId === 'string' && lotId !== selectedLotId.value) {
    selectedLotId.value = lotId
  }

  const summary = approvedLots.value.find(l => l.id === selectedLotId.value)
  if (!summary) {
    selectedLot.value = null
    return
  }
  // Fetch full lot detail to get sellerId (not included in summary response)
  try {
    const raw = await get<ApiResponse<RawLotSummary> | RawLotSummary>(`/lots/${summary.id}`)
    const unwrapped = raw as ApiResponse<RawLotSummary>
    const lot: RawLotSummary = unwrapped?.data ?? (raw as RawLotSummary)
    selectedLot.value = {
      id: lot.id ?? summary.id,
      title: lot.title ?? summary.title,
      brand: lot.brand ?? summary.brand,
      sellerId: lot.sellerId ?? summary.sellerId,
      startingBid: lot.startingBid ?? summary.startingBid,
      locationCountry: lot.locationCountry ?? summary.locationCountry,
      locationCity: lot.locationCity ?? summary.locationCity,
    }
  } catch {
    // Fallback to summary data if detail fetch fails
    selectedLot.value = { ...summary }
  }
}

function validate(): boolean {
  errors.value = {}
  if (!selectedLotId.value || selectedLotId.value === '') errors.value.lot = 'Please select an approved lot'
  if (!form.startTime) errors.value.startTime = 'Start time is required'
  if (!form.endTime) errors.value.endTime = 'End time is required'
  if (form.startTime && form.endTime && form.startTime >= form.endTime) {
    errors.value.endTime = 'End time must be after start time'
  }
  return Object.keys(errors.value).length === 0
}

const submitting = ref(false)

async function handleSubmit() {
  if (!validate()) return
  if (submitting.value) return
  submitting.value = true

  // If selectedLot isn't populated yet, try to resolve it from selectedLotId
  if (!selectedLot.value && selectedLotId.value) {
    await onLotSelected()
  }
  if (!selectedLot.value) { submitting.value = false; return }

  // Ensure required lot fields are present — fallback to summary data if detail fetch missed them
  const lot = selectedLot.value
  const sellerId = lot.sellerId || approvedLots.value.find(l => l.id === lot.id)?.sellerId || ''
  const startingBid = lot.startingBid || approvedLots.value.find(l => l.id === lot.id)?.startingBid || 1

  if (!sellerId) {
    errors.value = { lot: 'Could not determine seller for this lot. Please re-select the lot.' }
    submitting.value = false
    return
  }

  const payload: AuctionCreatePayload = {
    lotId: lot.id,
    brand: form.brand,
    startTime: form.startTime!.toISOString(),
    endTime: form.endTime!.toISOString(),
    startingBid,
    currency: form.currency,
    sellerId,
  }

  try {
    const auction = await createAuction(payload)
    if (auction) {
      toast.add({
        severity: 'success',
        summary: 'Auction Created',
        detail: `Auction for "${selectedLot.value?.title}" has been created successfully.`,
        life: 4000,
      })
      await router.push('/auctions')
    }
  } catch {
    // Error is stored in the reactive `error` ref by useAuctions
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/auctions')
}
</script>

<template>
  <div>
    <div class="mb-6">
      <Breadcrumb :model="breadcrumbItems">
        <template #item="{ item }">
          <router-link v-if="item.to" :to="item.to" class="text-primary-600 hover:text-primary-700">
            {{ item.label }}
          </router-link>
          <span v-else class="text-gray-700">{{ item.label }}</span>
        </template>
      </Breadcrumb>
      <h1 class="mt-2 page-title">
        Create Auction
      </h1>
    </div>

    <!-- Error banner -->
    <Message v-if="error" severity="error" :closable="false" class="mb-6">
      {{ error }}
    </Message>

    <div class="mx-auto max-w-2xl">
      <form
        class="space-y-6"
        @submit.prevent="handleSubmit"
      >
        <div class="card">
          <h2 class="section-title">
            Select Approved Lot
          </h2>
          <div class="space-y-4">
            <div>
              <label
                class="label"
                for="lot"
              >Approved Lot *</label>
              <Select
                id="lot"
                v-model="selectedLotId"
                :options="lotOptions"
                optionLabel="label"
                optionValue="value"
                :placeholder="lotPlaceholder"
                class="w-full"
                :class="errors.lot && 'p-invalid'"
                @change="onLotSelected"
              />
              <p
                v-if="errors.lot"
                class="mt-1 text-sm text-red-600"
              >
                {{ errors.lot }}
              </p>
              <p
                v-if="approvedLots.length === 0 && !lotsLoading"
                class="mt-1 text-sm text-yellow-600"
              >
                No approved lots available. Approve a lot first in Lot Approval.
              </p>
            </div>

            <!-- Lot info display when selected -->
            <div
              v-if="selectedLot"
              class="rounded-lg border border-gray-200 bg-gray-50 p-4"
            >
              <h3 class="text-sm font-medium text-gray-700">
                Lot Details
              </h3>
              <dl class="mt-2 grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <dt class="text-gray-500">
                  Title
                </dt>
                <dd class="text-gray-900">
                  {{ selectedLot.title }}
                </dd>
                <dt class="text-gray-500">
                  Brand
                </dt>
                <dd class="text-gray-900">
                  {{ selectedLot.brand }}
                </dd>
                <dt class="text-gray-500">
                  Starting Bid
                </dt>
                <dd class="text-gray-900">
                  USD {{ selectedLot.startingBid }}
                </dd>
                <dt class="text-gray-500">
                  Location
                </dt>
                <dd class="text-gray-900">
                  {{ selectedLot.locationCity }}, {{ selectedLot.locationCountry }}
                </dd>
              </dl>
            </div>
          </div>
        </div>

        <div class="card">
          <h2 class="section-title">
            Auction Schedule
          </h2>
          <div class="space-y-4">
            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label
                  class="label"
                  for="startTime"
                >Start Date & Time *</label>
                <DatePicker
                  id="startTime"
                  v-model="form.startTime"
                  showTime
                  hourFormat="24"
                  dateFormat="yy-mm-dd"
                  class="w-full"
                  :class="errors.startTime && 'p-invalid'"
                />
                <p
                  v-if="errors.startTime"
                  class="mt-1 text-sm text-red-600"
                >
                  {{ errors.startTime }}
                </p>
              </div>
              <div>
                <label
                  class="label"
                  for="endTime"
                >End Date & Time *</label>
                <DatePicker
                  id="endTime"
                  v-model="form.endTime"
                  showTime
                  hourFormat="24"
                  dateFormat="yy-mm-dd"
                  class="w-full"
                  :class="errors.endTime && 'p-invalid'"
                />
                <p
                  v-if="errors.endTime"
                  class="mt-1 text-sm text-red-600"
                >
                  {{ errors.endTime }}
                </p>
              </div>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label
                  class="label"
                  for="brand"
                >Platform Brand *</label>
                <Select
                  id="brand"
                  v-model="form.brand"
                  :options="PLATFORM_BRANDS"
                  optionLabel="label"
                  optionValue="code"
                  class="w-full"
                />
              </div>
              <div>
                <label
                  class="label"
                  for="currency"
                >Currency</label>
                <Select
                  id="currency"
                  v-model="form.currency"
                  :options="CURRENCY_OPTIONS"
                  optionLabel="label"
                  optionValue="value"
                  class="w-full"
                />
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3">
          <Button
            type="button"
            label="Cancel"
            severity="secondary"
            @click="handleCancel"
          />
          <Button
            type="submit"
            :label="loading || submitting ? 'Creating...' : 'Create Auction'"
            :loading="loading || submitting"
            :disabled="isSubmitDisabled"
          />
        </div>
      </form>
    </div>
  </div>
</template>
