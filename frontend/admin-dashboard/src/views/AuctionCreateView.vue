<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuctions, type AuctionCreatePayload } from '@/composables/useAuctions'
import { useApi } from '@/composables/useApi'

const router = useRouter()
const { createAuction, loading, error } = useAuctions()
const { get } = useApi()

interface ApprovedLot {
  id: string
  title: string
  brand: string
  sellerId: string
  startingBid: number
  locationCountry: string
  locationCity: string
}

const approvedLots = ref<ApprovedLot[]>([])
const lotsLoading = ref(false)
const selectedLotId = ref('')
const selectedLot = ref<ApprovedLot | null>(null)

const PLATFORM_BRANDS = [
  { code: 'troostwijk', label: 'Troostwijk' },
  { code: 'surplex', label: 'Surplex' },
  { code: 'industrial-auctions', label: 'Industrial Auctions' },
  { code: 'custom', label: 'Custom' },
]

const form = reactive({
  startTime: '',
  endTime: '',
  currency: 'EUR',
  brand: 'troostwijk',
})

const errors = ref<Record<string, string>>({})

onMounted(async () => {
  await fetchApprovedLots()
})

async function fetchApprovedLots() {
  lotsLoading.value = true
  try {
    const raw = await get<any>('/lots', { params: { status: 'APPROVED', pageSize: 100 } })
    const data = raw?.data ?? raw
    const items = data?.items ?? []
    approvedLots.value = items.map((lot: any) => ({
      id: lot.id ?? '',
      title: lot.title ?? '',
      brand: lot.brand ?? '',
      sellerId: lot.sellerId ?? '',
      startingBid: lot.startingBid ?? 1,
      locationCountry: lot.locationCountry ?? '',
      locationCity: lot.locationCity ?? '',
    }))
  } catch {
    approvedLots.value = []
  } finally {
    lotsLoading.value = false
  }
}

async function onLotSelected() {
  const summary = approvedLots.value.find(l => l.id === selectedLotId.value)
  if (!summary) {
    selectedLot.value = null
    return
  }
  // Fetch full lot detail to get sellerId (not included in summary response)
  try {
    const raw = await get<any>(`/lots/${summary.id}`)
    const lot = raw?.data ?? raw
    selectedLot.value = {
      id: lot.id ?? summary.id,
      title: lot.title ?? summary.title,
      brand: lot.brand ?? summary.brand,
      sellerId: lot.sellerId ?? '',
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
  if (!selectedLotId.value) errors.value.lot = 'Please select an approved lot'
  if (!form.startTime) errors.value.startTime = 'Start time is required'
  if (!form.endTime) errors.value.endTime = 'End time is required'
  if (form.startTime && form.endTime && new Date(form.startTime) >= new Date(form.endTime)) {
    errors.value.endTime = 'End time must be after start time'
  }
  return Object.keys(errors.value).length === 0
}

async function handleSubmit() {
  if (!validate() || !selectedLot.value) return

  const payload: AuctionCreatePayload = {
    lotId: selectedLot.value.id,
    brand: form.brand,
    startTime: new Date(form.startTime).toISOString(),
    endTime: new Date(form.endTime).toISOString(),
    startingBid: selectedLot.value.startingBid,
    currency: form.currency,
    sellerId: selectedLot.value.sellerId,
  }

  const auction = await createAuction(payload)
  if (auction) {
    router.push({ name: 'auctions' })
  }
}

function handleCancel() {
  router.push({ name: 'auctions' })
}
</script>

<template>
  <div>
    <div class="mb-6">
      <div class="flex items-center gap-2 text-sm text-gray-500">
        <router-link
          to="/auctions"
          class="hover:text-primary-600"
        >
          Auctions
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
        <span class="text-gray-700">Create Auction</span>
      </div>
      <h1 class="mt-2 page-title">
        Create Auction
      </h1>
    </div>

    <!-- Error banner -->
    <div
      v-if="error"
      class="mb-6 rounded-lg border border-red-200 bg-red-50 p-4"
    >
      <p class="text-sm font-medium text-red-800">
        {{ error }}
      </p>
    </div>

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
              <select
                id="lot"
                v-model="selectedLotId"
                class="select"
                :class="errors.lot && 'border-red-300'"
                @change="onLotSelected"
              >
                <option value="">
                  {{ lotsLoading ? 'Loading lots...' : 'Select an approved lot' }}
                </option>
                <option
                  v-for="lot in approvedLots"
                  :key="lot.id"
                  :value="lot.id"
                >
                  {{ lot.title }} ({{ lot.brand }} - {{ lot.locationCity }}, {{ lot.locationCountry }})
                </option>
              </select>
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
                  EUR {{ selectedLot.startingBid }}
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
                <input
                  id="startTime"
                  v-model="form.startTime"
                  type="datetime-local"
                  class="input"
                  :class="errors.startTime && 'border-red-300'"
                >
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
                <input
                  id="endTime"
                  v-model="form.endTime"
                  type="datetime-local"
                  class="input"
                  :class="errors.endTime && 'border-red-300'"
                >
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
                <select
                  id="brand"
                  v-model="form.brand"
                  class="select"
                >
                  <option
                    v-for="b in PLATFORM_BRANDS"
                    :key="b.code"
                    :value="b.code"
                  >
                    {{ b.label }}
                  </option>
                </select>
              </div>
              <div>
                <label
                  class="label"
                  for="currency"
                >Currency</label>
                <select
                  id="currency"
                  v-model="form.currency"
                  class="select"
                >
                  <option value="EUR">
                    EUR
                  </option>
                  <option value="USD">
                    USD
                  </option>
                  <option value="GBP">
                    GBP
                  </option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3">
          <button
            type="button"
            class="btn-secondary"
            @click="handleCancel"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn-primary"
            :disabled="loading || !selectedLotId"
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
            {{ loading ? 'Creating...' : 'Create Auction' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
