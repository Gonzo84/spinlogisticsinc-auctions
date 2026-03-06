<template>
  <div class="relative" ref="containerRef">
    <form @submit.prevent="handleSearch">
      <div class="relative">
        <i class="pi pi-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
        <input
          ref="inputRef"
          v-model="query"
          type="text"
          :placeholder="$t('search.placeholder')"
          class="w-full pl-10 pr-10 py-2.5 bg-gray-100 border border-transparent rounded-xl text-sm focus:bg-white focus:border-primary focus:ring-2 focus:ring-primary/20 transition-all"
          autocomplete="off"
          @input="handleInput"
          @focus="showDropdown = true"
          @keydown.enter.prevent="handleSearch"
          @keydown.down.prevent="navigateSuggestion(1)"
          @keydown.up.prevent="navigateSuggestion(-1)"
          @keydown.escape="closeDropdown"
        >
        <button
          v-if="query"
          type="button"
          class="absolute right-3 top-1/2 -translate-y-1/2 p-0.5 text-gray-400 hover:text-gray-600"
          @click="clearQuery"
        >
          <i class="pi pi-times w-4 h-4" />
        </button>
      </div>
    </form>

    <!-- Autocomplete Dropdown -->
    <Transition
      enter-active-class="transition ease-out duration-100"
      enter-from-class="transform opacity-0 scale-95"
      enter-to-class="transform opacity-100 scale-100"
      leave-active-class="transition ease-in duration-75"
      leave-from-class="transform opacity-100 scale-100"
      leave-to-class="transform opacity-0 scale-95"
    >
      <div
        v-if="showDropdown && (suggestions.length > 0 || query.length >= 2)"
        class="absolute top-full left-0 right-0 mt-1 bg-white rounded-xl shadow-lg border z-50 overflow-hidden"
      >
        <!-- Suggestions -->
        <div v-if="suggestions.length > 0" class="py-1">
          <button
            v-for="(suggestion, index) in suggestions"
            :key="index"
            class="w-full flex items-center gap-3 px-4 py-2.5 text-left hover:bg-gray-50 transition-colors"
            :class="{ 'bg-gray-50': highlightedIndex === index }"
            @click="selectSuggestion(suggestion)"
            @mouseenter="highlightedIndex = index"
          >
            <!-- Suggestion icon based on type -->
            <div v-if="suggestion.imageUrl" class="w-8 h-8 rounded bg-gray-100 overflow-hidden shrink-0">
              <img :src="suggestion.imageUrl" :alt="suggestion.text" class="w-full h-full object-cover">
            </div>
            <div v-else class="w-8 h-8 rounded bg-gray-100 flex items-center justify-center shrink-0">
              <i v-if="suggestion.type === 'category'" class="pi pi-tag w-4 h-4 text-gray-400" />
              <i v-else class="pi pi-search w-4 h-4 text-gray-400" />
            </div>
            <div class="min-w-0">
              <p class="text-sm text-gray-900 truncate">{{ suggestion.text }}</p>
              <p class="text-xs text-gray-400 capitalize">{{ suggestion.type }}</p>
            </div>
          </button>
        </div>

        <!-- No results hint -->
        <div v-else-if="query.length >= 2 && !searchLoading" class="px-4 py-6 text-center">
          <p class="text-sm text-gray-500">{{ $t('search.noSuggestions') }}</p>
          <button
            class="mt-2 text-sm text-primary hover:underline"
            @click="handleSearch"
          >
            {{ $t('search.searchFor', { query }) }}
          </button>
        </div>

        <!-- Loading -->
        <div v-if="searchLoading && query.length >= 2" class="px-4 py-3 text-center">
          <div class="inline-flex items-center gap-2 text-sm text-gray-500">
            <ProgressSpinner style="width: 1rem; height: 1rem" strokeWidth="4" />
            {{ $t('common.loading') }}
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import type { SearchSuggestion } from '~/types/search'

const route = useRoute()
const { suggest, suggestions, loading: searchLoading, clearSuggestions } = useSearch()

const query = ref((route.query.q as string) || '')
const showDropdown = ref(false)
const highlightedIndex = ref(-1)
const containerRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)

function handleInput() {
  highlightedIndex.value = -1
  if (query.value.length >= 2) {
    suggest(query.value)
  } else {
    clearSuggestions()
  }
}

async function handleSearch() {
  // If a suggestion is highlighted, select it instead
  if (highlightedIndex.value >= 0 && highlightedIndex.value < suggestions.value.length) {
    selectSuggestion(suggestions.value[highlightedIndex.value])
    return
  }

  if (!query.value.trim()) return

  closeDropdown()
  clearSuggestions()

  // Blur the input to dismiss any open overlays/keyboards
  inputRef.value?.blur()

  await navigateTo({
    path: '/search',
    query: { q: query.value.trim() },
  })
}

function selectSuggestion(suggestion: SearchSuggestion) {
  closeDropdown()
  clearSuggestions()

  if (suggestion.type === 'lot' && suggestion.id) {
    navigateTo(`/lots/${suggestion.id}`)
  } else if (suggestion.type === 'category') {
    navigateTo({
      path: '/search',
      query: { category: suggestion.text.toLowerCase() },
    })
  } else {
    query.value = suggestion.text
    handleSearch()
  }
}

function navigateSuggestion(direction: number) {
  if (suggestions.value.length === 0) return

  highlightedIndex.value += direction
  if (highlightedIndex.value < 0) {
    highlightedIndex.value = suggestions.value.length - 1
  }
  if (highlightedIndex.value >= suggestions.value.length) {
    highlightedIndex.value = 0
  }
}

function closeDropdown() {
  showDropdown.value = false
  highlightedIndex.value = -1
}

function clearQuery() {
  query.value = ''
  clearSuggestions()
  inputRef.value?.focus()
}

// Close dropdown on outside click
function handleClickOutside(event: MouseEvent) {
  if (containerRef.value && !containerRef.value.contains(event.target as Node)) {
    closeDropdown()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})

// Update query when route changes
watch(() => route.query.q, (newQ) => {
  if (typeof newQ === 'string') {
    query.value = newQ
  }
})
</script>
