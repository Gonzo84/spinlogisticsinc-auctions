export const COUNTRY_FLAGS: Record<string, string> = {
  NL: '\uD83C\uDDF3\uD83C\uDDF1',
  DE: '\uD83C\uDDE9\uD83C\uDDEA',
  FR: '\uD83C\uDDEB\uD83C\uDDF7',
  BE: '\uD83C\uDDE7\uD83C\uDDEA',
  PL: '\uD83C\uDDF5\uD83C\uDDF1',
  IT: '\uD83C\uDDEE\uD83C\uDDF9',
  RO: '\uD83C\uDDF7\uD83C\uDDF4',
  ES: '\uD83C\uDDEA\uD83C\uDDF8',
  AT: '\uD83C\uDDE6\uD83C\uDDF9',
  GB: '\uD83C\uDDEC\uD83C\uDDE7',
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
  { code: 'NL', flag: COUNTRY_FLAGS.NL, name: 'Netherlands' },
  { code: 'DE', flag: COUNTRY_FLAGS.DE, name: 'Germany' },
  { code: 'FR', flag: COUNTRY_FLAGS.FR, name: 'France' },
  { code: 'BE', flag: COUNTRY_FLAGS.BE, name: 'Belgium' },
  { code: 'PL', flag: COUNTRY_FLAGS.PL, name: 'Poland' },
  { code: 'IT', flag: COUNTRY_FLAGS.IT, name: 'Italy' },
  { code: 'RO', flag: COUNTRY_FLAGS.RO, name: 'Romania' },
  { code: 'ES', flag: COUNTRY_FLAGS.ES, name: 'Spain' },
  { code: 'AT', flag: COUNTRY_FLAGS.AT, name: 'Austria' },
]

export interface CategoryEntry {
  slug: string
  icon: string
  i18nKey: string
}

export const CATEGORIES: CategoryEntry[] = [
  { slug: 'transport', icon: '\uD83D\uDE9A', i18nKey: 'categories.transport' },
  { slug: 'agriculture', icon: '\uD83D\uDE9C', i18nKey: 'categories.agriculture' },
  { slug: 'construction', icon: '\uD83C\uDFD7\uFE0F', i18nKey: 'categories.construction' },
  { slug: 'metalworking', icon: '\u2699\uFE0F', i18nKey: 'categories.metalworking' },
  { slug: 'woodworking', icon: '\uD83E\uDEB5', i18nKey: 'categories.woodworking' },
  { slug: 'food-processing', icon: '\uD83C\uDFED', i18nKey: 'categories.foodProcessing' },
  { slug: 'electronics', icon: '\uD83D\uDD0C', i18nKey: 'categories.electronics' },
  { slug: 'warehouse', icon: '\uD83D\uDCE6', i18nKey: 'categories.warehouse' },
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
