import axios from 'axios'
import { useToast } from 'primevue/usetoast'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

export function useErrorHandler() {
  const toast = useToast()

  /**
   * Handle a real API error: extract a human-readable message, show a PrimeVue
   * error toast, and return the message string so the caller can set an error ref.
   */
  function handleApiError(err: unknown, fallbackMsg: string): string {
    const msg = extractErrorMessage(err, fallbackMsg)
    toast.add({ severity: 'error', summary: 'Error', detail: msg, life: 5000 })
    return msg
  }

  /**
   * Handle expected unavailability (e.g. 404 for a feature endpoint that does
   * not exist yet). Logs to console.debug without showing a toast — the caller
   * should display an empty/default state in the UI.
   */
  function handleGracefulDegradation(context: string): void {
    console.debug(`[${context}] Endpoint not available, showing empty state`)
  }

  /**
   * Show an informational toast for features that are not yet implemented.
   */
  function handleFeatureNotAvailable(featureName: string): void {
    toast.add({
      severity: 'info',
      summary: 'Not Available',
      detail: `${featureName} is not yet available`,
      life: 5000,
    })
  }

  /** Returns true when the error represents a 404 Not Found response. */
  function is404(err: unknown): boolean {
    return axios.isAxiosError(err) && err.response?.status === 404
  }

  /** Returns true when the error is a network failure (no response at all). */
  function isNetworkError(err: unknown): boolean {
    return axios.isAxiosError(err) && !err.response
  }

  return {
    handleApiError,
    handleGracefulDegradation,
    handleFeatureNotAvailable,
    is404,
    isNetworkError,
  }
}
