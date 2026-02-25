<template>
  <section class="py-16 px-4 bg-white">
    <div class="max-w-5xl mx-auto">
      <h2 class="text-3xl font-bold text-gray-900 text-center mb-12">{{ $t('home.howItWorks') }}</h2>
      <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
        <div
          v-for="(step, index) in steps"
          :key="index"
          class="text-center"
        >
          <div class="inline-flex items-center justify-center w-14 h-14 rounded-full bg-primary-100 text-primary font-bold text-xl mb-4">
            {{ index + 1 }}
          </div>
          <h3 class="font-semibold text-gray-900 mb-2">{{ step.title }}</h3>
          <p class="text-sm text-gray-600">{{ step.description }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
const { t } = useI18n()

const steps = computed(() => [
  { title: t('home.step1Title'), description: t('home.step1Desc') },
  { title: t('home.step2Title'), description: t('home.step2Desc') },
  { title: t('home.step3Title'), description: t('home.step3Desc') },
  { title: t('home.step4Title'), description: t('home.step4Desc') },
])
</script>
