<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuctions, type AuctionCreatePayload } from '@/composables/useAuctions'

const router = useRouter()
const { createAuction, loading, error } = useAuctions()

const form = reactive<AuctionCreatePayload>({
  title: '',
  brand: '',
  description: '',
  country: '',
  buyerPremiumPercent: 15,
  startDate: '',
  endDate: '',
})

const errors = ref<Record<string, string>>({})

const countries = [
  'Austria', 'Belgium', 'Bulgaria', 'Croatia', 'Cyprus', 'Czech Republic',
  'Denmark', 'Estonia', 'Finland', 'France', 'Germany', 'Greece',
  'Hungary', 'Ireland', 'Italy', 'Latvia', 'Lithuania', 'Luxembourg',
  'Malta', 'Netherlands', 'Poland', 'Portugal', 'Romania', 'Slovakia',
  'Slovenia', 'Spain', 'Sweden',
]

function validate(): boolean {
  errors.value = {}
  if (!form.title.trim()) errors.value.title = 'Title is required'
  if (!form.brand.trim()) errors.value.brand = 'Brand is required'
  if (!form.description.trim()) errors.value.description = 'Description is required'
  if (!form.country) errors.value.country = 'Country is required'
  if (form.buyerPremiumPercent < 0 || form.buyerPremiumPercent > 30) {
    errors.value.buyerPremiumPercent = 'Buyer premium must be between 0% and 30%'
  }
  if (!form.startDate) errors.value.startDate = 'Start date is required'
  if (!form.endDate) errors.value.endDate = 'End date is required'
  if (form.startDate && form.endDate && new Date(form.startDate) >= new Date(form.endDate)) {
    errors.value.endDate = 'End date must be after start date'
  }
  return Object.keys(errors.value).length === 0
}

async function handleSubmit() {
  if (!validate()) return

  const auction = await createAuction(form)
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
            Auction Details
          </h2>
          <div class="space-y-4">
            <div>
              <label
                class="label"
                for="title"
              >Auction Title *</label>
              <input
                id="title"
                v-model="form.title"
                type="text"
                class="input"
                :class="errors.title && 'border-red-300'"
                placeholder="e.g., Industrial Equipment Auction - March 2026"
              >
              <p
                v-if="errors.title"
                class="mt-1 text-sm text-red-600"
              >
                {{ errors.title }}
              </p>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label
                  class="label"
                  for="brand"
                >Brand / Organizer *</label>
                <input
                  id="brand"
                  v-model="form.brand"
                  type="text"
                  class="input"
                  :class="errors.brand && 'border-red-300'"
                  placeholder="e.g., TradEx"
                >
                <p
                  v-if="errors.brand"
                  class="mt-1 text-sm text-red-600"
                >
                  {{ errors.brand }}
                </p>
              </div>
              <div>
                <label
                  class="label"
                  for="country"
                >Country *</label>
                <select
                  id="country"
                  v-model="form.country"
                  class="select"
                  :class="errors.country && 'border-red-300'"
                >
                  <option value="">
                    Select country
                  </option>
                  <option
                    v-for="c in countries"
                    :key="c"
                    :value="c"
                  >
                    {{ c }}
                  </option>
                </select>
                <p
                  v-if="errors.country"
                  class="mt-1 text-sm text-red-600"
                >
                  {{ errors.country }}
                </p>
              </div>
            </div>

            <div>
              <label
                class="label"
                for="description"
              >Description *</label>
              <textarea
                id="description"
                v-model="form.description"
                rows="4"
                class="input resize-y"
                :class="errors.description && 'border-red-300'"
                placeholder="Description of the auction..."
              />
              <p
                v-if="errors.description"
                class="mt-1 text-sm text-red-600"
              >
                {{ errors.description }}
              </p>
            </div>
          </div>
        </div>

        <div class="card">
          <h2 class="section-title">
            Schedule & Pricing
          </h2>
          <div class="space-y-4">
            <div>
              <label
                class="label"
                for="premium"
              >Buyer Premium (%)</label>
              <input
                id="premium"
                v-model.number="form.buyerPremiumPercent"
                type="number"
                min="0"
                max="30"
                step="0.5"
                class="input w-32"
                :class="errors.buyerPremiumPercent && 'border-red-300'"
              >
              <p
                v-if="errors.buyerPremiumPercent"
                class="mt-1 text-sm text-red-600"
              >
                {{ errors.buyerPremiumPercent }}
              </p>
              <p
                v-else
                class="mt-1 text-xs text-gray-400"
              >
                Commission charged to buyers on top of the hammer price.
              </p>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <div>
                <label
                  class="label"
                  for="startDate"
                >Start Date & Time *</label>
                <input
                  id="startDate"
                  v-model="form.startDate"
                  type="datetime-local"
                  class="input"
                  :class="errors.startDate && 'border-red-300'"
                >
                <p
                  v-if="errors.startDate"
                  class="mt-1 text-sm text-red-600"
                >
                  {{ errors.startDate }}
                </p>
              </div>
              <div>
                <label
                  class="label"
                  for="endDate"
                >End Date & Time *</label>
                <input
                  id="endDate"
                  v-model="form.endDate"
                  type="datetime-local"
                  class="input"
                  :class="errors.endDate && 'border-red-300'"
                >
                <p
                  v-if="errors.endDate"
                  class="mt-1 text-sm text-red-600"
                >
                  {{ errors.endDate }}
                </p>
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
            :disabled="loading"
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
