<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConfirm } from 'primevue/useconfirm'
import { useLots } from '@/composables/useLots'
import RevenueChart from '@/components/charts/RevenueChart.vue'

const route = useRoute()
const router = useRouter()
const confirm = useConfirm()
const { currentLot, lotBids, loading, error, fetchLot, fetchLotBids, submitForReview, deleteLot, fetchCategories, categories } = useLots()

const lotId = computed(() => route.params.id as string)
const selectedImageIndex = ref(0)

const breadcrumbItems = computed(() => [
  { label: 'My Lots', route: '/lots' },
  { label: lot.value?.title ?? 'Lot Detail' },
])

// Resolve category UUID to human-readable name whenever lot data changes
function resolveCategoryName() {
  if (currentLot.value && categories.value.length > 0) {
    const categoryValue = currentLot.value.category
    // Only resolve if the category looks like a UUID (not already a name)
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(categoryValue)
    if (isUuid) {
      const cat = categories.value.find((c) => c.id === categoryValue)
      if (cat) {
        currentLot.value = { ...currentLot.value, category: cat.name }
      }
    }
  }
}

// Watch for lot changes and re-resolve category name
watch(() => currentLot.value?.category, () => {
  resolveCategoryName()
})

onMounted(async () => {
  const lotData = await fetchLot(lotId.value)
  // Fetch categories and resolve UUID to name
  await fetchCategories()
  resolveCategoryName()
  // Only fetch bids if the lot has an associated auction (active/sold status or has auctionStart)
  const hasAuction = lotData.status === 'active' || lotData.status === 'sold' || lotData.auctionStart
  if (hasAuction) {
    try {
      await fetchLotBids(lotId.value, lotData.auctionId)
    } catch {
      // Clear the shared error ref so it doesn't hide the lot detail
      error.value = null
    }
  }
})

const lot = computed(() => currentLot.value)

function getStatusSeverity(status: string): string | undefined {
  const map: Record<string, string> = {
    draft: 'secondary',
    pending: 'warn',
    pending_review: 'warn',
    active: 'success',
    sold: 'info',
    completed: 'info',
    unsold: 'danger',
    rejected: 'danger',
    paid: 'success',
    processing: 'warn',
  }
  return map[status] || undefined
}

const statusConfig = computed(() => {
  if (!lot.value) return { severity: 'secondary', label: 'Unknown' }
  const map: Record<string, { severity: string | undefined; label: string }> = {
    draft: { severity: getStatusSeverity('draft'), label: 'Draft' },
    pending_review: { severity: getStatusSeverity('pending_review'), label: 'Pending Review' },
    active: { severity: getStatusSeverity('active'), label: 'Active' },
    sold: { severity: getStatusSeverity('sold'), label: 'Sold' },
    unsold: { severity: getStatusSeverity('unsold'), label: 'Unsold' },
    rejected: { severity: getStatusSeverity('rejected'), label: 'Rejected' },
  }
  return map[lot.value.status] ?? { severity: getStatusSeverity(lot.value.status), label: lot.value.status }
})

function formatCurrency(value: number | null): string {
  if (value === null) return '--'
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

// Build bid activity chart data from bids (chronological order for time-series)
const bidsChronological = computed(() =>
  [...lotBids.value].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
)

const bidChartLabels = computed(() => {
  if (bidsChronological.value.length === 0) return []
  return bidsChronological.value.slice(-20).map((b) =>
    new Date(b.timestamp).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  )
})

const bidChartData = computed(() => {
  if (bidsChronological.value.length === 0) return []
  return bidsChronological.value.slice(-20).map((b) => b.amount)
})

async function handleSubmitForReview() {
  if (!lot.value) return
  confirm.require({
    message: 'Submit this lot for review?',
    header: 'Confirm Submission',
    acceptLabel: 'Submit',
    rejectLabel: 'Cancel',
    acceptProps: {
      severity: 'success',
    },
    rejectProps: {
      severity: 'secondary',
      outlined: true,
    },
    accept: async () => {
      await submitForReview(lot.value!.id)
      await fetchLot(lotId.value)
    },
  })
}

function handleDelete() {
  if (!lot.value) return
  confirm.require({
    message: `Delete "${lot.value.title}"? This cannot be undone.`,
    header: 'Confirm Delete',
    acceptClass: 'p-button-danger',
    acceptLabel: 'Delete',
    rejectLabel: 'Cancel',
    accept: async () => {
      await deleteLot(lot.value!.id)
      router.push({ name: 'lots' })
    },
  })
}

function selectImage(index: number) {
  selectedImageIndex.value = index
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
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !lot"
      class="py-12 text-center"
    >
      <ProgressSpinner strokeWidth="4" style="width: 2rem; height: 2rem" />
    </div>

    <!-- Error -->
    <Message
      v-else-if="error && !lot"
      severity="error"
      :closable="false"
      class="mb-4"
    >
      {{ error }}
      <Button
        label="Retry"
        severity="secondary"
        size="small"
        class="ml-3"
        @click="fetchLot(lotId)"
      />
    </Message>

    <!-- Lot content -->
    <div v-else-if="lot">
      <!-- Header with status and actions -->
      <div class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div class="flex items-center gap-3">
            <h1 class="text-2xl font-bold text-gray-900">
              {{ lot.title }}
            </h1>
            <Tag :value="statusConfig.label" :severity="statusConfig.severity" />
          </div>
          <p class="mt-1 text-sm text-gray-500">
            {{ lot.category }} &middot; {{ lot.location.city }}, {{ lot.location.country }}
          </p>
        </div>
        <div class="flex gap-2">
          <router-link
            v-if="lot.status === 'draft'"
            :to="`/lots/${lot.id}/edit`"
            custom
            v-slot="{ navigate }"
          >
            <Button
              label="Edit Lot"
              severity="secondary"
              @click="navigate"
            />
          </router-link>
          <Button
            v-if="lot.status === 'draft'"
            label="Submit for Review"
            severity="success"
            @click="handleSubmitForReview"
          />
          <Button
            v-if="lot.status === 'draft'"
            label="Delete"
            severity="danger"
            @click="handleDelete"
          />
        </div>
      </div>

      <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
        <!-- Left column - Images and Details -->
        <div class="space-y-6 lg:col-span-2">
          <!-- Images -->
          <div
            v-if="lot.images.length > 0"
            class="card p-0 overflow-hidden"
          >
            <div class="aspect-video overflow-hidden bg-gray-100">
              <img
                :src="lot.images[selectedImageIndex]?.url"
                :alt="lot.title"
                class="h-full w-full object-contain"
              >
            </div>
            <div
              v-if="lot.images.length > 1"
              class="flex gap-2 overflow-x-auto p-3"
            >
              <button
                v-for="(img, idx) in lot.images"
                :key="img.id"
                :class="[
                  'h-16 w-16 shrink-0 overflow-hidden rounded-lg border-2 transition-colors',
                  idx === selectedImageIndex ? 'border-primary-500' : 'border-transparent hover:border-gray-300',
                ]"
                @click="selectImage(idx)"
              >
                <img
                  :src="img.thumbnailUrl || img.url"
                  :alt="`Image ${idx + 1}`"
                  class="h-full w-full object-cover"
                >
              </button>
            </div>
          </div>

          <!-- No images placeholder -->
          <div
            v-else
            class="card flex items-center justify-center py-12"
          >
            <div class="text-center">
              <i class="pi pi-image mx-auto text-gray-300" style="font-size: 3rem" />
              <p class="mt-2 text-sm text-gray-500">
                No images uploaded
              </p>
            </div>
          </div>

          <!-- Description -->
          <div class="card">
            <h2 class="mb-3 text-lg font-semibold text-gray-900">
              Description
            </h2>
            <p class="whitespace-pre-line text-sm leading-relaxed text-gray-700">
              {{ lot.description }}
            </p>
          </div>

          <!-- Specifications -->
          <div
            v-if="Object.keys(lot.specifications).length > 0"
            class="card"
          >
            <h2 class="mb-3 text-lg font-semibold text-gray-900">
              Specifications
            </h2>
            <dl class="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div
                v-for="(value, key) in lot.specifications"
                :key="key"
                class="rounded-lg bg-gray-50 px-4 py-3"
              >
                <dt class="text-xs font-medium uppercase text-gray-500">
                  {{ key }}
                </dt>
                <dd class="mt-1 text-sm font-medium text-gray-900">
                  {{ value }}
                </dd>
              </div>
            </dl>
          </div>

          <!-- Bid Activity Chart -->
          <div
            v-if="lotBids.length > 0"
            class="card"
          >
            <h2 class="mb-3 text-lg font-semibold text-gray-900">
              Bid Activity
            </h2>
            <RevenueChart
              :labels="bidChartLabels"
              :data="bidChartData"
              label="Bid Amount (EUR)"
              color="#22c55e"
              :height="250"
            />
          </div>
        </div>

        <!-- Right column - Key Info and Bid History -->
        <div class="space-y-6">
          <!-- Key info -->
          <div class="card">
            <h2 class="mb-4 text-lg font-semibold text-gray-900">
              Auction Info
            </h2>
            <dl class="space-y-3">
              <div class="flex justify-between">
                <dt class="text-sm text-gray-500">
                  Starting Bid
                </dt>
                <dd class="text-sm font-medium text-gray-900">
                  {{ formatCurrency(lot.startingBid) }}
                </dd>
              </div>
              <div
                v-if="lot.reservePrice"
                class="flex justify-between"
              >
                <dt class="text-sm text-gray-500">
                  Reserve Price
                </dt>
                <dd class="text-sm font-medium text-gray-900">
                  {{ formatCurrency(lot.reservePrice) }}
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-sm text-gray-500">
                  Current Bid
                </dt>
                <dd class="text-sm font-bold text-seller-600">
                  {{ formatCurrency(lot.currentBid) }}
                </dd>
              </div>
              <div
                v-if="lot.hammerPrice"
                class="flex justify-between"
              >
                <dt class="text-sm text-gray-500">
                  Hammer Price
                </dt>
                <dd class="text-sm font-bold text-green-600">
                  {{ formatCurrency(lot.hammerPrice) }}
                </dd>
              </div>
              <hr class="border-gray-100">
              <div class="flex justify-between">
                <dt class="text-sm text-gray-500">
                  Total Bids
                </dt>
                <dd class="text-sm font-medium text-gray-900">
                  {{ lot.bidCount }}
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-sm text-gray-500">
                  Viewers
                </dt>
                <dd class="flex items-center gap-1 text-sm font-medium text-gray-900">
                  <i class="pi pi-eye text-gray-400" />
                  {{ lot.viewerCount }}
                </dd>
              </div>
              <hr class="border-gray-100">
              <div
                v-if="lot.auctionStart"
                class="flex justify-between"
              >
                <dt class="text-sm text-gray-500">
                  Auction Start
                </dt>
                <dd class="text-sm text-gray-900">
                  {{ formatDateTime(lot.auctionStart) }}
                </dd>
              </div>
              <div
                v-if="lot.auctionEnd"
                class="flex justify-between"
              >
                <dt class="text-sm text-gray-500">
                  Auction End
                </dt>
                <dd class="text-sm text-gray-900">
                  {{ formatDateTime(lot.auctionEnd) }}
                </dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-sm text-gray-500">
                  Created
                </dt>
                <dd class="text-sm text-gray-900">
                  {{ formatDate(lot.createdAt) }}
                </dd>
              </div>
            </dl>
          </div>

          <!-- Location -->
          <div class="card">
            <h2 class="mb-3 text-lg font-semibold text-gray-900">
              Location
            </h2>
            <div class="flex items-start gap-2">
              <i class="pi pi-map-marker mt-0.5 shrink-0 text-gray-400" style="font-size: 1.25rem" />
              <div>
                <p
                  v-if="lot.location.address"
                  class="text-sm text-gray-700"
                >
                  {{ lot.location.address }}
                </p>
                <p class="text-sm text-gray-700">
                  {{ lot.location.city }}, {{ lot.location.country }}
                </p>
              </div>
            </div>
          </div>

          <!-- Bid History -->
          <div class="card">
            <h2 class="mb-3 text-lg font-semibold text-gray-900">
              Bid History
            </h2>
            <div
              v-if="lotBids.length === 0"
              class="py-4 text-center text-sm text-gray-500"
            >
              No bids yet
            </div>
            <div
              v-else
              class="max-h-80 space-y-2 overflow-y-auto scrollbar-thin"
            >
              <div
                v-for="(bid, index) in lotBids"
                :key="bid.id"
                :class="[
                  'flex items-center justify-between rounded-lg px-3 py-2',
                  index === 0 ? 'bg-seller-50' : 'bg-gray-50',
                ]"
              >
                <div>
                  <p class="text-sm font-medium text-gray-900">
                    {{ bid.bidderAlias }}
                  </p>
                  <p class="text-xs text-gray-500">
                    {{ formatDateTime(bid.timestamp) }}
                  </p>
                </div>
                <div class="text-right">
                  <p :class="['text-sm font-bold', index === 0 ? 'text-seller-700' : 'text-gray-700']">
                    {{ formatCurrency(bid.amount) }}
                  </p>
                  <p
                    v-if="index === 0"
                    class="text-xs font-medium text-seller-600"
                  >
                    Highest
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
