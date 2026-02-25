/**
 * Formats a number as EUR currency string.
 * Uses Irish locale (en-IE) for EU-standard EUR formatting.
 */
export function formatCurrency(amount: number, maximumFractionDigits: number = 0): string {
  return new Intl.NumberFormat('en-IE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 0,
    maximumFractionDigits,
  }).format(amount)
}

/**
 * Formats a date string as a relative time ago string.
 * Returns i18n-friendly key + value pairs for the caller to translate.
 */
export function formatTimeAgo(dateStr: string): { key: string; value?: number } {
  const now = Date.now()
  const date = new Date(dateStr).getTime()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return { key: 'common.timeAgo.justNow' }
  if (minutes < 60) return { key: 'common.timeAgo.minutesAgo', value: minutes }
  if (hours < 24) return { key: 'common.timeAgo.hoursAgo', value: hours }
  return { key: 'common.timeAgo.daysAgo', value: days }
}

/**
 * Formats a date string as a localized date-time string.
 */
export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleString()
}
