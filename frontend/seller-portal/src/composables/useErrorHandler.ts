import axios from 'axios'
import { useToast } from 'primevue/usetoast'

function extractErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    const msg = (err.response?.data as Record<string, unknown> | undefined)?.message
    return typeof msg === 'string' ? msg : fallback
  }
  return err instanceof Error ? err.message : fallback
}

function isHttpStatus(err: unknown, status: number): boolean {
  return axios.isAxiosError(err) && err.response?.status === status
}

export function useErrorHandler() {
  const toast = useToast()

  /**
   * Handle a real API error: extract a human-readable message, show a PrimeVue
   * toast, and return the message string so the caller can also set an error ref.
   */
  function handleApiError(err: unknown, fallbackMsg: string): string {
    const msg = extractErrorMessage(err, fallbackMsg)
    toast.add({ severity: 'error', summary: 'Error', detail: msg, life: 5000 })
    return msg
  }

  /**
   * Handle expected unavailability (e.g. 404 for an endpoint that doesn't exist
   * yet). Logs to console.debug and does NOT show a toast — the caller should
   * display an empty state in the UI instead.
   */
  function handleGracefulDegradation(context: string): void {
    console.debug(`[${context}] Endpoint not available, showing empty state`)
  }

  /**
   * Convenience: returns true when the error is a 404 (expected unavailability).
   */
  function is404(err: unknown): boolean {
    return isHttpStatus(err, 404)
  }

  return {
    handleApiError,
    handleGracefulDegradation,
    is404,
  }
}
