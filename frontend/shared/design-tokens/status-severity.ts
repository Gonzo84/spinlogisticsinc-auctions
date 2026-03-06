/**
 * Maps platform status strings to PrimeVue Tag severity values.
 * Used across seller-portal and admin-dashboard for consistent status badges.
 */

const statusSeverityMap: Record<string, string> = {
  // Success states
  active: 'success',
  approved: 'success',
  paid: 'success',
  verified: 'success',
  resolved: 'success',
  healthy: 'success',
  released: 'success',

  // Info states
  sold: 'info',
  completed: 'info',
  exported: 'info',
  held: 'info',
  refunded: 'info',
  scheduled: 'info',

  // Warning states
  pending: 'warn',
  pending_review: 'warn',
  processing: 'warn',
  review: 'warn',
  investigating: 'warn',
  in_progress: 'warn',
  closing: 'warn',
  degraded: 'warn',
  disputed: 'warn',

  // Secondary states
  draft: 'secondary',
  inactive: 'secondary',
  new: 'secondary',
  dismissed: 'secondary',
  closed: 'secondary',
  withdrawn: 'secondary',
  not_started: 'secondary',
  false_positive: 'secondary',
  unknown: 'secondary',

  // Danger states
  rejected: 'danger',
  unsold: 'danger',
  fraud: 'danger',
  cancelled: 'danger',
  blocked: 'danger',
  failed: 'danger',
  expired: 'danger',
  overdue: 'danger',
  forfeited: 'danger',
  down: 'danger',
  high: 'danger',
  suspended: 'danger',

  // Severity levels
  medium: 'warn',
  low: 'info',

  // Account types
  buyer: 'info',
  seller: 'success',
  both: 'warn',
}

export function getStatusSeverity(status: string): string | undefined {
  return statusSeverityMap[status.toLowerCase()] || undefined
}

export function formatStatusLabel(status: string): string {
  return status
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}
