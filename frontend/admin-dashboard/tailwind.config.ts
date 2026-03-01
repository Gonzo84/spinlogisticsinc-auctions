import type { Config } from 'tailwindcss'
import sharedPreset from '@auction-platform/design-tokens/tailwind'

export default {
  presets: [sharedPreset],
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  plugins: [],
} satisfies Config
