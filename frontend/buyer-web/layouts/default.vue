<template>
  <div class="min-h-screen flex flex-col bg-gray-50">
    <SharedNavbar />
    <main class="flex-1">
      <slot />
    </main>
    <SharedFooter />
  </div>
</template>

<script setup lang="ts">
</script>
