<template>
  <Toast />
  <ConfirmDialog />
  <NuxtLayout>
    <NuxtPage />
  </NuxtLayout>
</template>

<script setup lang="ts">
useHead({
  titleTemplate: (titleChunk) => {
    return titleChunk ? `${titleChunk} | SPC Aukcije` : 'SPC Aukcije'
  },
})
</script>
