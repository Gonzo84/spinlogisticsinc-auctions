<template>
  <footer class="bg-gray-900 text-gray-300">
    <div class="max-w-7xl mx-auto px-4 py-12">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <!-- About -->
        <div>
          <div class="flex items-center gap-2 mb-4">
            <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
              <i class="pi pi-bolt text-lg text-white" />
            </div>
            <span class="text-lg font-bold text-white">EU Auction</span>
          </div>
          <p class="text-sm leading-relaxed text-gray-400 mb-4">
            {{ $t('footer.about') }}
          </p>
          <!-- Social Links -->
          <div class="flex items-center gap-3">
            <a
              href="https://linkedin.com"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-full bg-gray-800 hover:bg-primary flex items-center justify-center transition-colors"
              :title="$t('footer.linkedin')"
            >
              <i class="pi pi-linkedin w-4 h-4" />
            </a>
            <a
              href="https://twitter.com"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-full bg-gray-800 hover:bg-primary flex items-center justify-center transition-colors"
              :title="$t('footer.twitter')"
            >
              <i class="pi pi-twitter w-4 h-4" />
            </a>
            <a
              href="https://youtube.com"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-full bg-gray-800 hover:bg-primary flex items-center justify-center transition-colors"
              :title="$t('footer.youtube')"
            >
              <i class="pi pi-youtube w-4 h-4" />
            </a>
          </div>
        </div>

        <!-- Quick Links -->
        <div>
          <h3 class="text-white font-semibold mb-4">{{ $t('footer.quickLinks') }}</h3>
          <ul class="space-y-2">
            <li>
              <NuxtLink to="/search" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.browseAuctions') }}
              </NuxtLink>
            </li>
            <li>
              <NuxtLink to="/search?sort=closing_soonest" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.closingSoon') }}
              </NuxtLink>
            </li>
            <li>
              <NuxtLink to="/search?featured=true" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.featuredLots') }}
              </NuxtLink>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.howItWorks') }}
              </a>
            </li>
          </ul>
        </div>

        <!-- Support -->
        <div>
          <h3 class="text-white font-semibold mb-4">{{ $t('footer.support') }}</h3>
          <ul class="space-y-2">
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.helpCenter') }}
              </a>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.contact') }}
              </a>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.faq') }}
              </a>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.buyerGuide') }}
              </a>
            </li>
          </ul>
        </div>

        <!-- Legal -->
        <div>
          <h3 class="text-white font-semibold mb-4">{{ $t('footer.legal') }}</h3>
          <ul class="space-y-2">
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.terms') }}
              </a>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.privacy') }}
              </a>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.cookies') }}
              </a>
            </li>
            <li>
              <a href="#" class="text-sm hover:text-white transition-colors">
                {{ $t('footer.auctionTerms') }}
              </a>
            </li>
          </ul>
        </div>
      </div>

      <!-- Bottom Bar -->
      <div class="border-t border-gray-800 mt-10 pt-6 flex flex-col sm:flex-row items-center justify-between gap-4">
        <p class="text-sm text-gray-500">
          {{ $t('footer.copyright', { year: new Date().getFullYear() }) }}
        </p>
        <div class="flex items-center gap-4 text-xs text-gray-500">
          <span>{{ $t('footer.vatNotice') }}</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup lang="ts">
</script>
