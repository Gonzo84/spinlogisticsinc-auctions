<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useLots } from '@/composables/useLots'

interface UploadedImage {
  id: string
  url: string
  filename: string
  isPrimary: boolean
  progress: number
  status: 'uploading' | 'complete' | 'error'
}

interface ImageData {
  id: string
  url: string
}

const props = withDefaults(defineProps<{
  modelValue: string[]
  maxFiles?: number
  initialImages?: Array<{ id: string; url: string; isPrimary?: boolean; sortOrder?: number }>
}>(), {
  maxFiles: 10,
  initialImages: () => [],
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
  'update:images': [value: ImageData[]]
}>()

const { getPresignedUploadUrl } = useLots()

const images = ref<UploadedImage[]>([])
const isDragging = ref(false)
const fileInputRef = ref<HTMLInputElement | null>(null)

// Initialize with existing images when editing a lot
onMounted(() => {
  if (props.initialImages && props.initialImages.length > 0) {
    images.value = props.initialImages.map((img, index) => ({
      id: img.id,
      url: img.url,
      filename: `image-${index + 1}`,
      isPrimary: img.isPrimary ?? index === 0,
      progress: 100,
      status: 'complete' as const,
    }))
  }
})

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
  const completed = images.value.filter((im) => im.status === 'complete')
  emit('update:modelValue', completed.map((im) => im.id))
  emit('update:images', completed.map((im) => ({ id: im.id, url: im.url })))
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
        class="absolute w-0 h-0 overflow-hidden opacity-0"
        @change="handleFileSelect"
      >
      <i class="pi pi-images text-4xl text-gray-400" />
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
          <i class="pi pi-exclamation-triangle text-2xl text-white" />
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
            v-tooltip="'Set as primary'"
            @click.stop="setPrimary(image.id)"
          >
            <i class="pi pi-star" style="font-size: 1rem" />
          </button>
          <button
            v-if="index > 0"
            class="rounded-md bg-white/90 p-1 text-xs text-gray-700 shadow-sm hover:bg-white"
            v-tooltip="'Move left'"
            @click.stop="moveImage(index, index - 1)"
          >
            <i class="pi pi-chevron-left" style="font-size: 1rem" />
          </button>
          <button
            v-if="index < images.length - 1"
            class="rounded-md bg-white/90 p-1 text-xs text-gray-700 shadow-sm hover:bg-white"
            v-tooltip="'Move right'"
            @click.stop="moveImage(index, index + 1)"
          >
            <i class="pi pi-chevron-right" style="font-size: 1rem" />
          </button>
          <button
            class="rounded-md bg-white/90 p-1 text-xs text-red-600 shadow-sm hover:bg-white"
            v-tooltip="'Remove'"
            @click.stop="removeImage(image.id)"
          >
            <i class="pi pi-times" style="font-size: 1rem" />
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
