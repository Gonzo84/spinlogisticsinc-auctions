import { themeConfig, globalPT } from '@auction-platform/design-tokens'

export default defineNuxtConfig({
  ssr: true,

  modules: [
    '@pinia/nuxt',
    '@vueuse/nuxt',
    '@nuxtjs/tailwindcss',
    '@nuxtjs/i18n',
    '@nuxt/eslint',
    '@primevue/nuxt-module',
  ],

  primevue: {
    options: { theme: themeConfig, pt: globalPT },
    autoImport: true,
  },

  css: ['~/assets/css/main.css'],

  i18n: {
    locales: [
      { code: 'sl', name: 'Slovenščina', file: 'sl.json' },
      { code: 'hr', name: 'Hrvatski', file: 'hr.json' },
      { code: 'de', name: 'Deutsch', file: 'de.json' },
      { code: 'en', name: 'English', file: 'en.json' },
      { code: 'it', name: 'Italiano', file: 'it.json' },
      { code: 'sr', name: 'Srpski', file: 'sr.json' },
      { code: 'hu', name: 'Magyar', file: 'hu.json' },
    ],
    defaultLocale: 'sl',
    // @ts-expect-error lazy is valid but not in generated types for this i18n version
    lazy: true,
    strategy: 'prefix_except_default',
    detectBrowserLanguage: {
      useCookie: true,
      cookieKey: 'i18n_redirected',
      redirectOn: 'root',
    },
  },

  tailwindcss: {
    configPath: 'tailwind.config.ts',
  },

  runtimeConfig: {
    public: {
      apiBaseUrl: process.env.NUXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1',
      wsBaseUrl: process.env.NUXT_PUBLIC_WS_BASE_URL || 'ws://localhost:8080/ws',
      keycloakUrl: process.env.NUXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8180',
      keycloakRealm: process.env.NUXT_PUBLIC_KEYCLOAK_REALM || 'auction-platform',
      keycloakClientId: process.env.NUXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'buyer-web',
    },
  },

  app: {
    head: {
      title: 'SPC Aukcije - Kontejnerji in oprema na dražbi',
      charset: 'utf-8',
      viewport: 'width=device-width, initial-scale=1',
      meta: [
        { name: 'description', content: 'SPC Aukcije - B2B platforma za spletne dražbe kontejnerjev, klimatske opreme in gradbenih strojev. Dražite in kupujte rabljeno industrijsko opremo v srednji Evropi.' },
        { name: 'format-detection', content: 'telephone=no' },
        { property: 'og:site_name', content: 'SPC Aukcije' },
        { property: 'og:locale', content: 'sl_SI' },
        { property: 'og:locale:alternate', content: 'hr_HR' },
        { property: 'og:locale:alternate', content: 'de_DE' },
        { property: 'og:locale:alternate', content: 'en_GB' },
      ],
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;500;600;700&family=Raleway:wght@500;600;700&display=swap' },
      ],
      htmlAttrs: {
        lang: 'sl',
      },
    },
  },

  routeRules: {
    '/': { prerender: true },
    '/search': { swr: 300 },
    '/lots/**': { ssr: false },
    '/my/**': { ssr: false },
    '/checkout/**': { ssr: false },
    '/auth/**': { ssr: false },
    '/profile/**': { ssr: false },
  },

  nitro: {
    compressPublicAssets: true,
    routeRules: {
      '/**': {
        headers: {
          'X-Content-Type-Options': 'nosniff',
          'X-Frame-Options': 'DENY',
          'X-XSS-Protection': '1; mode=block',
          'Referrer-Policy': 'strict-origin-when-cross-origin',
          'Permissions-Policy': 'camera=(), microphone=(), geolocation=(self)',
          'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
        },
      },
      '/silent-check-sso.html': {
        headers: {
          'X-Frame-Options': 'SAMEORIGIN',
        },
      },
    },
  },

  devtools: { enabled: true },

  compatibilityDate: '2024-11-01',
})
