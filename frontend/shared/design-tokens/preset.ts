import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

export const AuctionPlatformPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{blue.50}',
      100: '{blue.100}',
      200: '{blue.200}',
      300: '{blue.300}',
      400: '{blue.400}',
      500: '{blue.500}',
      600: '{blue.600}',
      700: '{blue.700}',
      800: '{blue.800}',
      900: '{blue.900}',
      950: '{blue.950}',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.600}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.700}',
          activeColor: '{primary.800}',
        },
        highlight: {
          background: '{primary.50}',
          focusBackground: '{primary.100}',
          color: '{primary.700}',
          focusColor: '{primary.800}',
        },
        surface: {
          0: '#ffffff',
          50: '{slate.50}',
          100: '{slate.100}',
          200: '{slate.200}',
          300: '{slate.300}',
          400: '{slate.400}',
          500: '{slate.500}',
          600: '{slate.600}',
          700: '{slate.700}',
          800: '{slate.800}',
          900: '{slate.900}',
          950: '{slate.950}',
        },
      },
      dark: {
        primary: {
          color: '{primary.400}',
          contrastColor: '{surface.900}',
          hoverColor: '{primary.300}',
          activeColor: '{primary.200}',
        },
        highlight: {
          background: 'color-mix(in srgb, {primary.400}, transparent 84%)',
          focusBackground: 'color-mix(in srgb, {primary.400}, transparent 76%)',
          color: 'rgba(255,255,255,0.87)',
          focusColor: 'rgba(255,255,255,0.87)',
        },
        surface: {
          0: '#ffffff',
          50: '{zinc.50}',
          100: '{zinc.100}',
          200: '{zinc.200}',
          300: '{zinc.300}',
          400: '{zinc.400}',
          500: '{zinc.500}',
          600: '{zinc.600}',
          700: '{zinc.700}',
          800: '{zinc.800}',
          900: '{zinc.900}',
          950: '{zinc.950}',
        },
      },
    },
  },
  components: {
    button: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    card: {
      root: {
        borderRadius: '{border.radius.xl}',
        shadow: '0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1)',
      },
    },
    datatable: {
      headerCell: {
        borderColor: '{surface.200}',
      },
    },
    dialog: {
      root: {
        borderRadius: '{border.radius.xl}',
      },
    },
    inputtext: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    select: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    tag: {
      root: {
        borderRadius: '{border.radius.xl}',
      },
    },
    datepicker: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    textarea: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    drawer: {
      root: {
        borderRadius: '0',
      },
    },
    message: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    accordion: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    tabs: {
      root: {
        borderRadius: '{border.radius.lg}',
      },
    },
    progressbar: {
      root: {
        borderRadius: '{border.radius.xl}',
        height: '0.5rem',
      },
    },
    avatar: {
      root: {
        borderRadius: '{border.radius.xl}',
      },
    },
    badge: {
      root: {
        borderRadius: '{border.radius.xl}',
      },
    },
  } as Record<string, Record<string, Record<string, string>>>,
})

export const themeConfig = {
  preset: AuctionPlatformPreset,
  options: {
    prefix: 'p',
    darkModeSelector: '.app-dark',
    cssLayer: {
      name: 'primevue',
      order: 'tailwind-base, primevue, tailwind-utilities',
    },
  },
}
