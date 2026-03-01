declare module '@nuxt/test-utils/config' {
  import type { UserConfig } from 'vitest/config'
  export function defineVitestConfig(config: UserConfig): UserConfig
}
