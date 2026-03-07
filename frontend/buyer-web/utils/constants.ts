export const COUNTRY_FLAGS: Record<string, string> = {
  SI: '\uD83C\uDDF8\uD83C\uDDEE',
  HR: '\uD83C\uDDED\uD83C\uDDF7',
  AT: '\uD83C\uDDE6\uD83C\uDDF9',
  DE: '\uD83C\uDDE9\uD83C\uDDEA',
  IT: '\uD83C\uDDEE\uD83C\uDDF9',
  BA: '\uD83C\uDDE7\uD83C\uDDE6',
  RS: '\uD83C\uDDF7\uD83C\uDDF8',
  HU: '\uD83C\uDDED\uD83C\uDDFA',
}

export function getCountryFlag(code: string): string {
  return COUNTRY_FLAGS[code] || code
}

export interface CountryEntry {
  code: string
  flag: string
  name: string
}

export const COUNTRIES: CountryEntry[] = [
  { code: 'SI', flag: COUNTRY_FLAGS.SI, name: 'Slovenija' },
  { code: 'HR', flag: COUNTRY_FLAGS.HR, name: 'Hrvatska' },
  { code: 'AT', flag: COUNTRY_FLAGS.AT, name: 'Avstrija' },
  { code: 'DE', flag: COUNTRY_FLAGS.DE, name: 'Nemčija' },
  { code: 'IT', flag: COUNTRY_FLAGS.IT, name: 'Italija' },
  { code: 'BA', flag: COUNTRY_FLAGS.BA, name: 'BiH' },
  { code: 'RS', flag: COUNTRY_FLAGS.RS, name: 'Srbija' },
  { code: 'HU', flag: COUNTRY_FLAGS.HU, name: 'Madžarska' },
]

export interface CategoryEntry {
  slug: string
  icon: string
  i18nKey: string
}

export const CATEGORIES: CategoryEntry[] = [
  { slug: 'office-containers', icon: '\uD83C\uDFE2', i18nKey: 'categories.officeContainers' },
  { slug: 'shipping-containers', icon: '\uD83D\uDCE6', i18nKey: 'categories.shippingContainers' },
  { slug: 'sanitary-containers', icon: '\uD83D\uDEBF', i18nKey: 'categories.sanitaryContainers' },
  { slug: 'storage-containers', icon: '\uD83D\uDD12', i18nKey: 'categories.storageContainers' },
  { slug: 'modular-structures', icon: '\uD83C\uDFD7\uFE0F', i18nKey: 'categories.modularStructures' },
  { slug: 'climate-control', icon: '\u2744\uFE0F', i18nKey: 'categories.climateControl' },
  { slug: 'construction-equipment', icon: '\uD83D\uDD27', i18nKey: 'categories.constructionEquipment' },
  { slug: 'fencing', icon: '\uD83D\uDEE1\uFE0F', i18nKey: 'categories.fencing' },
]

export const DISTANCE_OPTIONS = [0, 50, 100, 250, 500, 1000]

export const PRICE_RANGES = [
  { label: '< 1K', min: undefined, max: 1000 },
  { label: '1K-5K', min: 1000, max: 5000 },
  { label: '5K-25K', min: 5000, max: 25000 },
  { label: '25K-100K', min: 25000, max: 100000 },
  { label: '> 100K', min: 100000, max: undefined },
]

export const TARGET_CO2_SAVINGS = 2_847_350
