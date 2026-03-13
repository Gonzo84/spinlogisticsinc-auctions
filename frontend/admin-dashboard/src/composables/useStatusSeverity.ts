/**
 * Maps status strings to PrimeVue Tag severity values.
 * Replaces the custom StatusBadge component with PrimeVue's Tag component.
 */

const severityMap: Record<string, string> = {
  // Success (green)
  active: 'success',
  approved: 'success',
  paid: 'success',
  completed: 'success',
  won: 'success',
  resolved: 'success',

  // Info (blue)
  awarded: 'info',
  scheduled: 'info',
  sold: 'info',
  held: 'info',
  investigating: 'info',
  released: 'info',

  // Warning (amber/yellow)
  pending: 'warn',
  pending_review: 'warn',
  processing: 'warn',
  closing: 'warn',
  outbid: 'warn',
  medium: 'warn',
  low: 'warn',

  // Danger (red)
  rejected: 'danger',
  blocked: 'danger',
  suspended: 'danger',
  cancelled: 'danger',
  overdue: 'danger',
  forfeited: 'danger',
  failed: 'danger',
  disputed: 'danger',
  unsold: 'danger',
  high: 'danger',

  // Secondary (gray)
  draft: 'secondary',
  none: 'secondary',
  not_started: 'secondary',
  withdrawn: 'secondary',
  closed: 'secondary',
  lost: 'secondary',
  inactive: 'secondary',

  // Contrast (distinct)
  refunded: 'contrast',
}

export function getStatusSeverity(status: string): string | undefined {
  return severityMap[status.toLowerCase()] || undefined
}

export function formatStatusLabel(status: string): string {
  return status
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (l) => l.toUpperCase())
}
