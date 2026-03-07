import { definePreset } from '@primeuix/themes'
import Aura from '@primeuix/themes/aura'

export const AuctionPlatformPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#e6f2f8',
      100: '#b3d9e8',
      200: '#80bfd8',
      300: '#4da6c8',
      400: '#268db5',
      500: '#0077a3',
      600: '#006a92',
      700: '#004d71',
      800: '#003e5a',
      900: '#002f44',
      950: '#001f2e',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.700}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.800}',
          activeColor: '{primary.900}',
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
        borderRadius: '9999px',
      },
    },
    card: {
      root: {
        borderRadius: '{border.radius.xl}',
        shadow: '6px 6px 9px rgba(0, 0, 0, 0.12)',
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
        borderRadius: '{border.radius.xl}',
      },
    },
    select: {
      root: {
        borderRadius: '{border.radius.xl}',
      },
    },
    tag: {
      root: {
        borderRadius: '9999px',
      },
    },
    datepicker: {
      root: {
        borderRadius: '{border.radius.xl}',
      },
    },
    textarea: {
      root: {
        borderRadius: '{border.radius.xl}',
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
        borderRadius: '9999px',
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
