<script setup lang="ts">
import { ref, reactive } from 'vue'
import type { LotIntakeRequest } from '@/types'

const emit = defineEmits<{
  submit: [data: LotIntakeRequest]
}>()

defineProps<{
  loading: boolean
}>()

const form = reactive<LotIntakeRequest>({
  title: '',
  categoryId: '',
  description: '',
  reservePrice: undefined,
  startingBid: undefined,
  brand: '',
  locationAddress: '',
  locationCity: '',
  locationCountry: '',
  imageKeys: [],
})

const submitted = ref(false)

function handleSubmit() {
  submitted.value = true
  if (!form.title || !form.categoryId) return
  emit('submit', { ...form })
}

function resetForm() {
  form.title = ''
  form.categoryId = ''
  form.description = ''
  form.reservePrice = undefined
  form.startingBid = undefined
  form.brand = ''
  form.locationAddress = ''
  form.locationCity = ''
  form.locationCountry = ''
  form.imageKeys = []
  submitted.value = false
}

defineExpose({ resetForm })
</script>

<template>
  <form
    class="space-y-6"
    @submit.prevent="handleSubmit"
  >
    <!-- Title -->
    <div>
      <label
        for="title"
        class="label"
      >Lot Title *</label>
      <InputText
        id="title"
        v-model="form.title"
        placeholder="Enter lot title"
        class="w-full"
        :invalid="submitted && !form.title"
      />
      <small
        v-if="submitted && !form.title"
        class="text-red-500"
      >Title is required</small>
    </div>

    <!-- Category ID -->
    <div>
      <label
        for="categoryId"
        class="label"
      >Category ID *</label>
      <InputText
        id="categoryId"
        v-model="form.categoryId"
        placeholder="Enter category UUID"
        class="w-full"
        :invalid="submitted && !form.categoryId"
      />
      <small
        v-if="submitted && !form.categoryId"
        class="text-red-500"
      >Category ID is required</small>
    </div>

    <!-- Description -->
    <div>
      <label
        for="description"
        class="label"
      >Description</label>
      <Textarea
        id="description"
        v-model="form.description"
        placeholder="Describe the lot"
        class="w-full"
        rows="4"
        auto-resize
      />
    </div>

    <!-- Pricing -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <div>
        <label
          for="reservePrice"
          class="label"
        >Reserve Price</label>
        <InputNumber
          id="reservePrice"
          v-model="form.reservePrice"
          mode="currency"
          currency="USD"
          locale="en-US"
          placeholder="0.00"
          class="w-full"
        />
      </div>
      <div>
        <label
          for="startingBid"
          class="label"
        >Starting Bid</label>
        <InputNumber
          id="startingBid"
          v-model="form.startingBid"
          mode="currency"
          currency="USD"
          locale="en-US"
          placeholder="0.00"
          class="w-full"
        />
      </div>
    </div>

    <!-- Brand -->
    <div>
      <label
        for="brand"
        class="label"
      >Brand</label>
      <InputText
        id="brand"
        v-model="form.brand"
        placeholder="e.g. troostwijk, surplex"
        class="w-full"
      />
    </div>

    <!-- Location -->
    <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
      <div>
        <label
          for="locationAddress"
          class="label"
        >Address</label>
        <InputText
          id="locationAddress"
          v-model="form.locationAddress"
          placeholder="Street address"
          class="w-full"
        />
      </div>
      <div>
        <label
          for="locationCity"
          class="label"
        >City</label>
        <InputText
          id="locationCity"
          v-model="form.locationCity"
          placeholder="City"
          class="w-full"
        />
      </div>
      <div>
        <label
          for="locationCountry"
          class="label"
        >Country Code</label>
        <InputText
          id="locationCountry"
          v-model="form.locationCountry"
          placeholder="US, CA, etc."
          class="w-full"
          maxlength="3"
        />
      </div>
    </div>

    <!-- Actions -->
    <div class="flex items-center gap-3 border-t border-gray-200 pt-4">
      <Button
        type="submit"
        label="Submit Lot Intake"
        icon="pi pi-check"
        :loading="loading"
      />
      <Button
        type="button"
        label="Reset"
        severity="secondary"
        text
        @click="resetForm"
      />
    </div>
  </form>
</template>
