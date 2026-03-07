<template>
  <footer class="bg-gray-900 text-gray-300">
    <div class="max-w-7xl mx-auto px-4 py-12">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        <!-- About -->
        <div>
          <div class="flex items-center gap-2 mb-4">
            <img src="/images/spc-logo.png" alt="SPC" class="h-9 w-auto brightness-0 invert" />
            <span class="text-lg font-bold text-white">SPC Aukcije</span>
          </div>
          <p class="text-sm leading-relaxed text-gray-400 mb-4">
            {{ $t('footer.about') }}
          </p>
          <!-- Contact Info -->
          <div class="space-y-1.5 text-sm text-gray-400 mb-4">
            <div class="flex items-center gap-2">
              <i class="pi pi-phone w-4 h-4" />
              <a href="tel:+38617777666" class="hover:text-white transition-colors">+386 1 77 77 666</a>
            </div>
            <div class="flex items-center gap-2">
              <i class="pi pi-envelope w-4 h-4" />
              <a href="mailto:info@spc.si" class="hover:text-white transition-colors">info@spc.si</a>
            </div>
            <div class="flex items-center gap-2">
              <i class="pi pi-map-marker w-4 h-4" />
              <span>Premrlova ulica 11, 1000 Ljubljana</span>
            </div>
          </div>
          <!-- Social Links -->
          <div class="flex items-center gap-3">
            <a
              href="https://facebook.com/SPContainer"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-full bg-gray-800 hover:bg-primary flex items-center justify-center transition-colors"
              title="Facebook"
            >
              <i class="pi pi-facebook w-4 h-4" />
            </a>
            <a
              href="https://instagram.com/spcontainers"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-full bg-gray-800 hover:bg-primary flex items-center justify-center transition-colors"
              title="Instagram"
            >
              <i class="pi pi-instagram w-4 h-4" />
            </a>
            <a
              href="https://linkedin.com"
              target="_blank"
              rel="noopener noreferrer"
              class="w-9 h-9 rounded-full bg-gray-800 hover:bg-primary flex items-center justify-center transition-colors"
              :title="$t('footer.linkedin')"
            >
              <i class="pi pi-linkedin w-4 h-4" />
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
