<script setup lang="ts">
import { ref, computed } from 'vue'
import { useLots } from '@/composables/useLots'

interface UploadedImage {
  id: string
  url: string
  filename: string
  isPrimary: boolean
  progress: number
  status: 'uploading' | 'complete' | 'error'
}

const props = withDefaults(defineProps<{
  modelValue: string[]
  maxFiles?: number
}>(), {
  maxFiles: 10,
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const { getPresignedUploadUrl } = useLots()

const images = ref<UploadedImage[]>([])
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

const canAddMore = computed(() => images.value.length < props.maxFiles)

function handleDragOver(e: DragEvent) {
  e.preventDefault()
  isDragging.value = true
}

function handleDragLeave() {
  isDragging.value = false
}

function handleDrop(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files) processFiles(files)
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files) processFiles(input.files)
  input.value = ''
}

function openFilePicker() {
  fileInputRef.value?.click()
}

async function processFiles(files: FileList) {
  const validTypes = ['image/jpeg', 'image/png', 'image/webp']
  const maxSize = 10 * 1024 * 1024 // 10 MB

  for (let i = 0; i < files.length; i++) {
    if (!canAddMore.value) break

    const file = files[i]
    if (!validTypes.includes(file.type)) continue
    if (file.size > maxSize) continue

    const tempId = crypto.randomUUID()
    const isPrimary = images.value.length === 0

    const uploadedImage: UploadedImage = {
      id: tempId,
      url: URL.createObjectURL(file),
      filename: file.name,
      isPrimary,
      progress: 0,
      status: 'uploading',
    }

    images.value.push(uploadedImage)

    try {
      const { uploadUrl, imageId, publicUrl } = await getPresignedUploadUrl(file.name, file.type)

      // Simulate progress
      const progressInterval = setInterval(() => {
        const img = images.value.find((im) => im.id === tempId)
        if (img && img.progress < 90) {
          img.progress += 10
        }
      }, 100)

      await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: { 'Content-Type': file.type },
      })

      clearInterval(progressInterval)

      const img = images.value.find((im) => im.id === tempId)
      if (img) {
        img.id = imageId
        img.url = publicUrl
        img.progress = 100
        img.status = 'complete'
      }

      emitUpdate()
    } catch {
      const img = images.value.find((im) => im.id === tempId)
      if (img) {
        img.status = 'error'
        img.progress = 0
      }
    }
  }
}

function removeImage(id: string) {
  const wasPrimary = images.value.find((im) => im.id === id)?.isPrimary
  images.value = images.value.filter((im) => im.id !== id)
  if (wasPrimary && images.value.length > 0) {
    images.value[0].isPrimary = true
  }
  emitUpdate()
}

function setPrimary(id: string) {
  images.value.forEach((img) => {
    img.isPrimary = img.id === id
  })
  emitUpdate()
}

function moveImage(fromIndex: number, toIndex: number) {
  if (toIndex < 0 || toIndex >= images.value.length) return
  const item = images.value.splice(fromIndex, 1)[0]
  images.value.splice(toIndex, 0, item)
  emitUpdate()
}

function retryUpload(id: string) {
  const img = images.value.find((im) => im.id === id)
  if (img) {
    img.status = 'uploading'
    img.progress = 0
    // In a real app, we'd re-upload the file
    setTimeout(() => {
      img.progress = 100
      img.status = 'complete'
      emitUpdate()
    }, 1500)
  }
}

function emitUpdate() {
  const ids = images.value
    .filter((im) => im.status === 'complete')
    .map((im) => im.id)
  emit('update:modelValue', ids)
}
</script>

<template>
  <div>
    <!-- Drop zone -->
    <div
      v-if="canAddMore"
      :class="[
        'relative rounded-xl border-2 border-dashed p-8 text-center transition-colors',
        isDragging
          ? 'border-primary-400 bg-primary-50'
          : 'border-gray-300 bg-gray-50 hover:border-gray-400 hover:bg-gray-100',
      ]"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
      @click="openFilePicker"
    >
      <input
        ref="fileInputRef"
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple
        class="hidden"
        @change="handleFileSelect"
      >
      <svg
        class="mx-auto h-12 w-12 text-gray-400"
        fill="none"
        viewBox="0 0 24 24"
        stroke="currentColor"
        stroke-width="1.5"
      >
        <path
          stroke-linecap="round"
          stroke-linejoin="round"
          d="M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z"
        />
      </svg>
      <p class="mt-3 text-sm font-medium text-gray-700">
        <span class="text-primary-600">Click to upload</span> or drag and drop
      </p>
      <p class="mt-1 text-xs text-gray-500">
        JPEG, PNG, or WebP up to 10MB ({{ images.length }}/{{ maxFiles }})
      </p>
    </div>

    <!-- Image grid -->
    <div
      v-if="images.length > 0"
      class="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4"
    >
      <div
        v-for="(image, index) in images"
        :key="image.id"
        :class="[
          'group relative overflow-hidden rounded-lg border-2',
          image.isPrimary ? 'border-primary-500 ring-2 ring-primary-200' : 'border-gray-200',
          image.status === 'error' && 'border-red-300 bg-red-50',
        ]"
      >
        <!-- Image preview -->
        <div class="aspect-square">
          <img
            :src="image.url"
            :alt="image.filename"
            class="h-full w-full object-cover"
          >
        </div>

        <!-- Upload progress overlay -->
        <div
          v-if="image.status === 'uploading'"
          class="absolute inset-0 flex flex-col items-center justify-center bg-black/50"
        >
          <div class="mb-2 h-1.5 w-3/4 overflow-hidden rounded-full bg-white/30">
            <div
              class="h-full rounded-full bg-white transition-all duration-300"
              :style="{ width: image.progress + '%' }"
            />
          </div>
          <p class="text-xs font-medium text-white">
            {{ image.progress }}%
          </p>
        </div>

        <!-- Error overlay -->
        <div
          v-if="image.status === 'error'"
          class="absolute inset-0 flex flex-col items-center justify-center bg-red-900/50"
        >
          <svg
            class="h-6 w-6 text-white"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="2"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          <button
            class="mt-1 text-xs font-medium text-white underline"
            @click.stop="retryUpload(image.id)"
          >
            Retry
          </button>
        </div>

        <!-- Actions overlay -->
        <div
          v-if="image.status === 'complete'"
          class="absolute inset-0 flex items-start justify-end gap-1 bg-gradient-to-b from-black/40 to-transparent p-2 opacity-0 transition-opacity group-hover:opacity-100"
        >
          <button
            v-if="!image.isPrimary"
            class="rounded-md bg-white/90 p-1 text-xs text-gray-700 shadow-sm hover:bg-white"
            title="Set as primary"
            @click.stop="setPrimary(image.id)"
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
                d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"
              />
            </svg>
          </button>
          <button
            v-if="index > 0"
            class="rounded-md bg-white/90 p-1 text-xs text-gray-700 shadow-sm hover:bg-white"
            title="Move left"
            @click.stop="moveImage(index, index - 1)"
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
                d="M15 19l-7-7 7-7"
              />
            </svg>
          </button>
          <button
            v-if="index < images.length - 1"
            class="rounded-md bg-white/90 p-1 text-xs text-gray-700 shadow-sm hover:bg-white"
            title="Move right"
            @click.stop="moveImage(index, index + 1)"
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
                d="M9 5l7 7-7 7"
              />
            </svg>
          </button>
          <button
            class="rounded-md bg-white/90 p-1 text-xs text-red-600 shadow-sm hover:bg-white"
            title="Remove"
            @click.stop="removeImage(image.id)"
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
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        </div>

        <!-- Primary badge -->
        <div
          v-if="image.isPrimary && image.status === 'complete'"
          class="absolute bottom-0 left-0 right-0 bg-primary-600 py-1 text-center text-xs font-medium text-white"
        >
          Primary
        </div>
      </div>
    </div>
  </div>
</template>
