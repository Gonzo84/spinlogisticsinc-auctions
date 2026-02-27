<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useApi } from '@/composables/useApi'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

interface PendingLot {
  id: string
  title: string
  description: string
  category: string
  sellerName: string
  sellerId: string
  startingBid: number
  reservePrice: number | null
  location: { city: string; country: string }
  imageCount: number
  primaryImage: string | null
  specifications: Record<string, string>
  submittedAt: string
}

const { get, post } = useApi()

const pendingLots = ref<PendingLot[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const expandedLotId = ref<string | null>(null)

// Cache for seller name lookups to avoid repeated API calls
const sellerNameCache = new Map<string, string>()
// Cache for category name lookups
const categoryNameCache = new Map<string, string>()

// Rejection dialog
const showRejectDialog = ref(false)
const rejectingLotId = ref<string | null>(null)
const rejectReason = ref('')

onMounted(() => {
  fetchPendingLots()
})

async function fetchCategories() {
  if (categoryNameCache.size > 0) return
  try {
    const raw = await get<any>('/categories')
    const categories = raw?.data ?? raw
    if (Array.isArray(categories)) {
      for (const cat of categories) {
        categoryNameCache.set(cat.id, cat.name ?? cat.slug ?? cat.id)
      }
    }
  } catch {
    // Silently fail — category names will remain as UUIDs
  }
}

function resolveCategoryName(categoryId: string): string {
  return categoryNameCache.get(categoryId) ?? categoryId
}

async function resolveSellerName(sellerId: string): Promise<string> {
  if (!sellerId) return 'Unknown'
  if (sellerNameCache.has(sellerId)) return sellerNameCache.get(sellerId)!
  try {
    // sellerId from catalog is a Keycloak UUID, not a user-service internal ID
    const raw = await get<any>(`/users/by-keycloak-id/${sellerId}`)
    const userData = raw?.data ?? raw
    const user = userData?.user ?? userData
    const name = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.fullName || user?.email || sellerId
    sellerNameCache.set(sellerId, name)
    return name
  } catch {
    // If user lookup fails, fall back to showing the sellerId
    sellerNameCache.set(sellerId, sellerId)
    return sellerId
  }
}

async function fetchPendingLots() {
  loading.value = true
  error.value = null
  try {
    const raw = await get<any>('/lots', { params: { status: 'PENDING_REVIEW', page: 0, pageSize: 50 } })
    // Unwrap ApiResponse wrapper if present
    const response = raw?.data && typeof raw.data === 'object' && !Array.isArray(raw.data) ? raw.data : raw
    const items = response.items ?? []
    pendingLots.value = items.map((lot: any) => ({
      id: lot.id,
      title: lot.title ?? '',
      description: lot.description ?? '',
      category: lot.category ?? lot.categoryId ?? '',
      sellerName: lot.sellerName ?? lot.sellerId ?? 'Unknown',
      sellerId: lot.sellerId ?? '',
      startingBid: lot.startingBid ?? 0,
      reservePrice: lot.reservePrice ?? null,
      location: lot.location ?? {
        city: lot.locationCity ?? '',
        country: lot.locationCountry ?? '',
      },
      imageCount: lot.imageCount ?? (lot.images?.length ?? 0),
      primaryImage: lot.primaryImageUrl ?? lot.primaryImage ?? null,
      specifications: lot.specifications ?? {},
      submittedAt: lot.updatedAt ?? lot.createdAt ?? new Date().toISOString(),
    }))

    // Resolve seller names and category names in parallel
    const uniqueSellerIds = [...new Set(pendingLots.value.map((l) => l.sellerId).filter(Boolean))]
    await Promise.all([
      ...uniqueSellerIds.map((id) => resolveSellerName(id)),
      fetchCategories(),
    ])
    // Update lot entries with resolved names
    pendingLots.value = pendingLots.value.map((lot) => ({
      ...lot,
      sellerName: sellerNameCache.get(lot.sellerId) ?? lot.sellerName,
      category: resolveCategoryName(lot.category),
    }))
  } catch (err: any) {
    error.value = err.response?.data?.message ?? 'Failed to fetch pending lots'
  } finally {
    loading.value = false
  }
}

function toggleExpand(lotId: string) {
  expandedLotId.value = expandedLotId.value === lotId ? null : lotId
}

async function approveLot(lotId: string) {
  try {
    await post(`/lots/${lotId}/approve`)
    pendingLots.value = pendingLots.value.filter((l) => l.id !== lotId)
  } catch (err: any) {
    error.value = err.response?.data?.message ?? 'Failed to approve lot'
  }
}

function openRejectDialog(lotId: string) {
  rejectingLotId.value = lotId
  rejectReason.value = ''
  showRejectDialog.value = true
}

async function confirmReject() {
  if (!rejectingLotId.value || !rejectReason.value.trim()) return
  try {
    await post(`/lots/${rejectingLotId.value}/admin-withdraw`, { reason: rejectReason.value })
    pendingLots.value = pendingLots.value.filter((l) => l.id !== rejectingLotId.value)
    showRejectDialog.value = false
  } catch (err: any) {
    error.value = err.response?.data?.message ?? 'Failed to reject lot'
  }
}

function formatCurrency(value: number | null): string {
  if (value === null) return '--'
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">
          Lot Approval Queue
        </h1>
        <p class="mt-1 text-sm text-gray-500">
          Review and approve seller lot submissions before they go live.
        </p>
      </div>
      <div class="flex items-center gap-2">
        <span class="inline-flex items-center rounded-full bg-amber-100 px-3 py-1 text-sm font-medium text-amber-800">
          {{ pendingLots.length }} pending
        </span>
        <button
          class="btn-secondary btn-sm"
          @click="fetchPendingLots"
        >
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
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          Refresh
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="loading"
      class="py-12 text-center"
    >
      <svg
        class="mx-auto h-8 w-8 animate-spin text-primary-600"
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
    </div>

    <!-- Error -->
    <div
      v-else-if="error"
      class="card border-red-200 bg-red-50 text-center"
    >
      <p class="text-sm text-red-600">
        {{ error }}
      </p>
    </div>

    <!-- Empty -->
    <div
      v-else-if="pendingLots.length === 0"
      class="card py-12 text-center"
    >
      <svg
        class="mx-auto h-12 w-12 text-green-300"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="1.5"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
        />
      </svg>
      <h3 class="mt-4 text-lg font-medium text-gray-900">
        All caught up!
      </h3>
      <p class="mt-1 text-sm text-gray-500">
        No lots pending approval.
      </p>
    </div>

    <!-- Lot cards -->
    <div
      v-else
      class="space-y-4"
    >
      <div
        v-for="lot in pendingLots"
        :key="lot.id"
        class="card overflow-hidden p-0"
      >
        <!-- Summary row -->
        <div
          class="flex cursor-pointer items-center gap-4 p-4 hover:bg-gray-50"
          @click="toggleExpand(lot.id)"
        >
          <!-- Thumbnail -->
          <div class="h-16 w-16 shrink-0 overflow-hidden rounded-lg bg-gray-100">
            <img
              v-if="lot.primaryImage"
              :src="lot.primaryImage"
              :alt="lot.title"
              class="h-full w-full object-cover"
            >
            <div
              v-else
              class="flex h-full w-full items-center justify-center"
            >
              <svg
                class="h-6 w-6 text-gray-400"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                />
              </svg>
            </div>
          </div>

          <!-- Info -->
          <div class="min-w-0 flex-1">
            <h3 class="text-sm font-semibold text-gray-900">
              {{ lot.title }}
            </h3>
            <p class="text-xs text-gray-500">
              {{ lot.category }} &middot; {{ lot.sellerName }} &middot; {{ lot.location.city }}, {{ lot.location.country }}
            </p>
            <p class="text-xs text-gray-400">
              Submitted {{ formatDate(lot.submittedAt) }}
            </p>
          </div>

          <!-- Price -->
          <div class="hidden text-right sm:block">
            <p class="text-sm font-medium text-gray-900">
              {{ formatCurrency(lot.startingBid) }}
            </p>
            <p class="text-xs text-gray-500">
              Starting bid
            </p>
          </div>

          <!-- Actions -->
          <div
            class="flex gap-2"
            @click.stop
          >
            <button
              class="btn-success btn-sm"
              @click="approveLot(lot.id)"
            >
              Approve
            </button>
            <button
              class="btn-danger btn-sm"
              @click="openRejectDialog(lot.id)"
            >
              Reject
            </button>
          </div>

          <!-- Expand icon -->
          <svg
            :class="['h-5 w-5 text-gray-400 transition-transform', expandedLotId === lot.id && 'rotate-180']"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M19 9l-7 7-7-7"
            />
          </svg>
        </div>

        <!-- Expanded details -->
        <div
          v-if="expandedLotId === lot.id"
          class="border-t border-gray-100 bg-gray-50 p-4"
        >
          <div class="grid gap-4 md:grid-cols-2">
            <div>
              <h4 class="mb-2 text-xs font-semibold uppercase text-gray-500">
                Description
              </h4>
              <p class="text-sm text-gray-700">
                {{ lot.description }}
              </p>
            </div>
            <div>
              <h4 class="mb-2 text-xs font-semibold uppercase text-gray-500">
                Details
              </h4>
              <dl class="space-y-1 text-sm">
                <div class="flex justify-between">
                  <dt class="text-gray-500">
                    Starting Bid
                  </dt>
                  <dd class="font-medium text-gray-900">
                    {{ formatCurrency(lot.startingBid) }}
                  </dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">
                    Reserve Price
                  </dt>
                  <dd class="font-medium text-gray-900">
                    {{ formatCurrency(lot.reservePrice) }}
                  </dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">
                    Images
                  </dt>
                  <dd class="font-medium text-gray-900">
                    {{ lot.imageCount }}
                  </dd>
                </div>
                <div class="flex justify-between">
                  <dt class="text-gray-500">
                    Seller
                  </dt>
                  <dd>
                    <router-link
                      :to="`/users/${lot.sellerId}`"
                      class="font-medium text-primary-600 hover:text-primary-700"
                    >
                      {{ lot.sellerName }}
                    </router-link>
                  </dd>
                </div>
              </dl>
            </div>
          </div>

          <!-- Specifications -->
          <div
            v-if="Object.keys(lot.specifications).length > 0"
            class="mt-4"
          >
            <h4 class="mb-2 text-xs font-semibold uppercase text-gray-500">
              Specifications
            </h4>
            <div class="flex flex-wrap gap-2">
              <span
                v-for="(value, key) in lot.specifications"
                :key="String(key)"
                class="rounded-lg bg-white px-3 py-1 text-xs text-gray-700 border border-gray-200"
              >
                <span class="font-medium">{{ key }}:</span> {{ value }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Reject Dialog -->
    <ConfirmDialog
      :open="showRejectDialog"
      title="Reject Lot"
      message="Please provide a reason for rejecting this lot. The seller will be notified."
      confirm-label="Reject Lot"
      variant="danger"
      @confirm="confirmReject"
      @cancel="showRejectDialog = false"
    >
      <div>
        <label class="label">Rejection Reason *</label>
        <textarea
          v-model="rejectReason"
          rows="3"
          class="input"
          placeholder="e.g., Images are blurry, description is incomplete..."
        />
      </div>
    </ConfirmDialog>
  </div>
</template>
