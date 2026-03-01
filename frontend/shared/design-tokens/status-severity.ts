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

  // Info states
  sold: 'info',
  completed: 'info',
  exported: 'info',

  // Warning states
  pending: 'warn',
  pending_review: 'warn',
  processing: 'warn',
  review: 'warn',
  investigating: 'warn',
  in_progress: 'warn',

  // Secondary states
  draft: 'secondary',
  inactive: 'secondary',
  new: 'secondary',
  dismissed: 'secondary',

  // Danger states
  rejected: 'danger',
  unsold: 'danger',
  fraud: 'danger',
  cancelled: 'danger',
  blocked: 'danger',
  failed: 'danger',
  expired: 'danger',
  overdue: 'danger',
}

export function getStatusSeverity(status: string): string | undefined {
  return statusSeverityMap[status.toLowerCase()] || undefined
}

export function formatStatusLabel(status: string): string {
  return status
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}
