import type { Config } from 'tailwindcss'
import sharedPreset from '@auction-platform/design-tokens/tailwind'
import primeui from 'tailwindcss-primeui'

export default <Config>{
  presets: [sharedPreset],
  content: [
    './components/**/*.{js,vue,ts}',
    './layouts/**/*.vue',
    './pages/**/*.vue',
    './composables/**/*.{js,ts}',
    './plugins/**/*.{js,ts}',
    './app.vue',
  ],
  theme: {
    extend: {
      colors: {
        // Legacy aliases — kept for backward compatibility during migration
        secondary: sharedPreset.theme!.extend!.colors!.success,
        accent: sharedPreset.theme!.extend!.colors!.warning,
      },
    },
  },
  plugins: [primeui],
}
