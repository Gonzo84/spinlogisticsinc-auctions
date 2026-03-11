import type { Config } from 'tailwindcss'
import sharedPreset from '@auction-platform/design-tokens/tailwind'
import primeui from 'tailwindcss-primeui'

export default {
  presets: [sharedPreset],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  plugins: [primeui],
} satisfies Config
