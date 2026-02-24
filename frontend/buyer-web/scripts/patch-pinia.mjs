/**
 * Patches pinia's shouldHydrate function to use Object.prototype.hasOwnProperty.call()
 * instead of obj.hasOwnProperty(). This fixes SSR crashes when null-prototype objects
 * (e.g. from @nuxtjs/i18n) are passed through the payload serializer.
 *
 * See: https://github.com/vuejs/pinia/issues/2798
 */
import { readFileSync, writeFileSync } from 'fs'
import { resolve, dirname } from 'path'
import { fileURLToPath } from 'url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const piniaPath = resolve(__dirname, '../node_modules/pinia/dist/pinia.mjs')

try {
  let code = readFileSync(piniaPath, 'utf8')
  const buggy = '!obj.hasOwnProperty(skipHydrateSymbol)'
  const fixed = '!Object.prototype.hasOwnProperty.call(obj, skipHydrateSymbol)'

  if (code.includes(buggy)) {
    code = code.replace(buggy, fixed)
    writeFileSync(piniaPath, code)
    console.log('[patch-pinia] Patched shouldHydrate in pinia.mjs')
  } else if (code.includes(fixed)) {
    console.log('[patch-pinia] Already patched')
  } else {
    console.log('[patch-pinia] Pattern not found — pinia may have been updated')
  }
} catch (e) {
  // Silently skip if pinia is not installed yet (e.g. during first npm install)
  if (e.code !== 'ENOENT') {
    console.warn('[patch-pinia] Warning:', e.message)
  }
}
