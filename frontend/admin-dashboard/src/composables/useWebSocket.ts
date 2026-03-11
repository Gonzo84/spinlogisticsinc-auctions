import { ref, readonly, onUnmounted } from 'vue'
import { useAuth } from './useAuth'

type EventHandler = (data: Record<string, unknown>) => void

interface WebSocketMessage {
  type: string
  data?: Record<string, unknown>
  serverTime?: string
}

const MAX_RECONNECT_DELAY_MS = 30_000
const INITIAL_RECONNECT_DELAY_MS = 1_000

let ws: WebSocket | null = null
const connected = ref(false)
const handlers = new Map<string, Set<EventHandler>>()
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempt = 0
let intentionalClose = false

export function useWebSocket() {
  const { token } = useAuth()

  function connect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    intentionalClose = false
    const wsBase = import.meta.env.VITE_WS_URL || 'ws://localhost:8080'
    const url = `${wsBase}/ws/user?token=${token.value}`

    ws = new WebSocket(url)

    ws.onopen = () => {
      connected.value = true
      reconnectAttempt = 0
    }

    ws.onmessage = (event: MessageEvent) => {
      try {
        const msg: WebSocketMessage = JSON.parse(event.data as string)
        const eventHandlers = handlers.get(msg.type)
        if (eventHandlers) {
          eventHandlers.forEach(fn => fn(msg.data ?? {}))
        }
      } catch {
        // Ignore malformed messages
      }
    }

    ws.onclose = () => {
      connected.value = false
      ws = null
      if (!intentionalClose) {
        scheduleReconnect()
      }
    }

    ws.onerror = () => {
      // onclose will fire after onerror
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) return

    const delay = Math.min(
      INITIAL_RECONNECT_DELAY_MS * Math.pow(2, reconnectAttempt) + Math.random() * 1000,
      MAX_RECONNECT_DELAY_MS
    )
    reconnectAttempt++

    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, delay)
  }

  function on(event: string, handler: EventHandler) {
    if (!handlers.has(event)) {
      handlers.set(event, new Set())
    }
    handlers.get(event)!.add(handler)
  }

  function off(event: string, handler: EventHandler) {
    handlers.get(event)?.delete(handler)
  }

  function disconnect() {
    intentionalClose = true
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    ws?.close()
    ws = null
    connected.value = false
  }

  onUnmounted(() => {
    // Don't disconnect on unmount — the WebSocket is shared across the app.
    // Only disconnect explicitly when logging out.
  })

  return {
    connect,
    disconnect,
    on,
    off,
    connected: readonly(connected),
  }
}
