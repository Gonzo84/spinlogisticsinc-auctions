<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useAuctions } from '@/composables/useAuctions'
import { useConfirm } from 'primevue/useconfirm'
import { useToast } from 'primevue/usetoast'
import Tag from 'primevue/tag'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import { getStatusSeverity, formatStatusLabel } from '@/composables/useStatusSeverity'
import LiveBidChart from '@/components/charts/LiveBidChart.vue'
import Dialog from 'primevue/dialog'
import Button from 'primevue/button'
import Textarea from 'primevue/textarea'

const confirm = useConfirm()
const toast = useToast()

const route = useRoute()
const {
  currentAuction,
  auctionLots,
  liveBids,
  loading,
  fetchAuction,
  fetchAuctionLots,
  fetchLiveBids,
  cancelAuction,
  closeAuction,
} = useAuctions()

const auctionId = computed(() => route.params.id as string)

const showCancelDialog = ref(false)
const cancelReason = ref('')

onMounted(async () => {
  await Promise.all([
    fetchAuction(auctionId.value),
    fetchAuctionLots(auctionId.value),
    fetchLiveBids(auctionId.value),
  ])
})

function formatDate(dateStr: string): string {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '—'
  return d.toLocaleDateString('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value)
}

async function handleCancel() {
  if (!cancelReason.value.trim()) return
  const ok = await cancelAuction(auctionId.value, cancelReason.value)
  if (ok) {
    showCancelDialog.value = false
    await fetchAuction(auctionId.value)
  }
}

function handleClose() {
  confirm.require({
    message: 'Are you sure you want to close this auction?',
    header: 'Close Auction',
    icon: 'pi pi-exclamation-triangle',
    acceptClass: 'p-button-warning',
    accept: async () => {
      const ok = await closeAuction(auctionId.value)
      if (ok) {
        toast.add({ severity: 'success', summary: 'Closed', detail: 'Auction has been closed', life: 3000 })
        await fetchAuction(auctionId.value)
      }
    },
  })
}

const bidChartLabels = computed(() =>
  liveBids.value.slice(-20).map((b) =>
    new Date(b.timestamp).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })
  )
)
const bidChartData = computed(() =>
  liveBids.value.slice(-20).map((b) => b.amount)
)
</script>

<template>
  <div>
    <!-- Breadcrumb -->
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
        <span class="text-gray-700">{{ currentAuction?.title ?? 'Detail' }}</span>
      </div>
    </div>

    <!-- Loading -->
    <div
      v-if="loading && !currentAuction"
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

    <template v-else-if="currentAuction">
      <!-- Header -->
      <div class="page-header">
        <div>
          <div class="flex items-center gap-3">
            <h1 class="page-title">
              {{ currentAuction.title }}
            </h1>
            <Tag :value="formatStatusLabel(currentAuction.status)" :severity="getStatusSeverity(currentAuction.status)" />
          </div>
          <p class="mt-1 text-sm text-gray-500">
            {{ currentAuction.brand }} &middot; {{ currentAuction.country }}
          </p>
        </div>
        <div class="flex gap-2">
          <Button
            v-if="currentAuction.status === 'active'"
            label="Close Auction"
            severity="warn"
            @click="handleClose"
          />
          <Button
            v-if="currentAuction.status !== 'cancelled' && currentAuction.status !== 'closed'"
            label="Cancel Auction"
            severity="danger"
            @click="showCancelDialog = true"
          />
        </div>
      </div>

      <!-- Info cards -->
      <div class="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <div class="card">
          <p class="text-sm text-gray-500">
            Lots
          </p>
          <p class="text-2xl font-bold text-gray-900">
            {{ currentAuction.lotCount }}
          </p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">
            Total Bids
          </p>
          <p class="text-2xl font-bold text-gray-900">
            {{ currentAuction.totalBids }}
          </p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">
            Buyer Premium
          </p>
          <p class="text-2xl font-bold text-gray-900">
            {{ currentAuction.buyerPremiumPercent }}%
          </p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">
            Duration
          </p>
          <p class="text-sm font-medium text-gray-900">
            {{ formatDate(currentAuction.startDate) }}<br>
            to {{ formatDate(currentAuction.endDate) }}
          </p>
        </div>
      </div>

      <!-- Bid Activity -->
      <div
        v-if="liveBids.length > 0"
        class="card mb-6"
      >
        <h2 class="section-title">
          Bid Activity
        </h2>
        <LiveBidChart
          :labels="bidChartLabels"
          :data="bidChartData"
          label="Bid Amount (EUR)"
          :height="250"
        />
      </div>

      <!-- Lots table -->
      <div class="card">
        <h2 class="section-title">
          Lots ({{ auctionLots.length }})
        </h2>
        <DataTable :value="auctionLots" stripedRows>
          <template #empty>
            <div class="text-center py-8 text-gray-500">No lots assigned to this auction.</div>
          </template>
          <Column field="lotNumber" header="#">
            <template #body="{ data }">
              <span class="font-medium">{{ data.lotNumber }}</span>
            </template>
          </Column>
          <Column field="title" header="Title">
            <template #body="{ data }">
              <span class="font-medium text-gray-900">{{ data.title }}</span>
            </template>
          </Column>
          <Column field="status" header="Status">
            <template #body="{ data }">
              <Tag :value="formatStatusLabel(data.status)" :severity="getStatusSeverity(data.status)" />
            </template>
          </Column>
          <Column field="currentBid" header="Current Bid" headerStyle="text-align: right" bodyStyle="text-align: right">
            <template #body="{ data }">
              {{ formatCurrency(data.currentBid) }}
            </template>
          </Column>
          <Column field="bidCount" header="Bids" headerStyle="text-align: right" bodyStyle="text-align: right" />
          <Column field="closingTime" header="Closing">
            <template #body="{ data }">
              <span class="text-gray-500">{{ formatDate(data.closingTime) }}</span>
            </template>
          </Column>
        </DataTable>
      </div>

      <!-- Recent bids -->
      <div
        v-if="liveBids.length > 0"
        class="card mt-6"
      >
        <h2 class="section-title">
          Recent Bids
        </h2>
        <div class="space-y-2">
          <div
            v-for="bid in liveBids.slice(0, 10)"
            :key="bid.id"
            class="flex items-center justify-between rounded-lg bg-gray-50 px-4 py-2"
          >
            <div>
              <p class="text-sm font-medium text-gray-900">
                {{ bid.lotTitle }}
              </p>
              <p class="text-xs text-gray-500">
                by {{ bid.bidderName }}
              </p>
            </div>
            <div class="text-right">
              <p class="text-sm font-bold text-gray-900">
                {{ formatCurrency(bid.amount) }}
              </p>
              <p class="text-xs text-gray-400">
                {{ new Date(bid.timestamp).toLocaleTimeString('en-GB') }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- Cancel Dialog -->
    <Dialog
      v-model:visible="showCancelDialog"
      header="Cancel Auction"
      :modal="true"
      :closable="true"
      :style="{ width: '28rem' }"
    >
      <p class="mb-4 text-sm text-gray-500">
        Are you sure you want to cancel this auction? All active bids will be voided.
      </p>
      <div>
        <label class="label">Reason for cancellation</label>
        <Textarea
          v-model="cancelReason"
          rows="3"
          class="w-full"
          placeholder="Provide a reason..."
        />
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <Button
            label="Cancel"
            severity="secondary"
            :disabled="loading"
            @click="showCancelDialog = false"
          />
          <Button
            label="Cancel Auction"
            severity="danger"
            :loading="loading"
            :disabled="loading"
            @click="handleCancel"
          />
        </div>
      </template>
    </Dialog>
  </div>
</template>
