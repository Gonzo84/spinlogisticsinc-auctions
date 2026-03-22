export interface CategoryEntry {
  slug: string
  icon: string
  i18nKey: string
}

export const CATEGORIES: CategoryEntry[] = [
  { slug: 'containers-modular', icon: '📦', i18nKey: 'categories.containersModular' },
  { slug: 'construction-equipment', icon: '🏗️', i18nKey: 'categories.constructionEquipment' },
  { slug: 'cranes-lifting', icon: '🏗️', i18nKey: 'categories.cranesLifting' },
  { slug: 'aerial-platforms', icon: '⬆️', i18nKey: 'categories.aerialPlatforms' },
  { slug: 'material-handling', icon: '🏭', i18nKey: 'categories.materialHandling' },
  { slug: 'trucks', icon: '🚛', i18nKey: 'categories.trucks' },
  { slug: 'trailers', icon: '🚚', i18nKey: 'categories.trailers' },
  { slug: 'agriculture', icon: '🌾', i18nKey: 'categories.agriculture' },
  { slug: 'forestry-logging', icon: '🌲', i18nKey: 'categories.forestryLogging' },
  { slug: 'mining-quarry', icon: '⛏️', i18nKey: 'categories.miningQuarry' },
  { slug: 'oil-gas', icon: '🛢️', i18nKey: 'categories.oilGas' },
  { slug: 'power-climate', icon: '⚡', i18nKey: 'categories.powerClimate' },
  { slug: 'metalworking', icon: '⚙️', i18nKey: 'categories.metalworking' },
  { slug: 'woodworking-plastics', icon: '🪵', i18nKey: 'categories.woodworkingPlastics' },
  { slug: 'food-processing', icon: '🍽️', i18nKey: 'categories.foodProcessing' },
  { slug: 'medical-lab', icon: '🏥', i18nKey: 'categories.medicalLab' },
  { slug: 'vehicles-fleet', icon: '🚗', i18nKey: 'categories.vehiclesFleet' },
  { slug: 'attachments-parts', icon: '🔩', i18nKey: 'categories.attachmentsParts' },
]

export const DISTANCE_OPTIONS = [0, 25, 50, 100, 250, 500]

export const PRICE_RANGES = [
  { label: '< $1K', min: undefined, max: 1000 },
  { label: '$1K-$5K', min: 1000, max: 5000 },
  { label: '$5K-$25K', min: 5000, max: 25000 },
  { label: '$25K-$100K', min: 25000, max: 100000 },
  { label: '> $100K', min: 100000, max: undefined },
]

export const TARGET_CO2_SAVINGS = 2_847_350

/**
 * Returns a flag emoji for the given country code.
 * Kept for backward compatibility with LotCard and lot detail pages.
 */
export function getCountryFlag(code: string): string {
  if (!code || code.length !== 2) return code || ''
  // Convert 2-letter country code to regional indicator emoji
  const codePoints = [...code.toUpperCase()].map(
    (c) => 0x1f1e6 + c.charCodeAt(0) - 65,
  )
  return String.fromCodePoint(...codePoints)
}
