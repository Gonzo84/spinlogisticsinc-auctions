import { useAuthStore } from '~/stores/auth'
import type { Bid } from '~/types/auction'
import type { Notification } from '~/types/notification'

export type WebSocketEvent =
  | 'bid_placed'
  | 'auction_extended'
  | 'auction_closed'
  | 'overbid'
  | 'reserve_met'
  | 'notification'
  | 'pong'

export interface WebSocketMessage {
  event: WebSocketEvent
  data: unknown
  auctionId?: string
  timestamp: string
}

interface BidPlacedData {
  bid: Bid
  auctionId: string
}

interface AuctionExtendedData {
  auctionId: string
  newEndTime: string
}

interface AuctionClosedData {
  auctionId: string
}

interface OverbidData {
  lotTitle?: string
  auctionId?: string
  newBidAmount?: number
}

interface ReserveMetData {
  auctionId: string
}

// Internal handler type uses unknown; callers narrow via typed on* methods
type InternalHandler = (data: unknown) => void

export function useWebSocket() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  let socket: WebSocket | null = null
  let heartbeatInterval: ReturnType<typeof setInterval> | null = null
  let reconnectTimeout: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 1
  const baseReconnectDelay = 5000

  const isConnected = ref(false)
  const isReconnecting = ref(false)

  const eventHandlers = new Map<WebSocketEvent, Set<InternalHandler>>()

  let currentAuctionId: string | null = null

  function getWebSocketUrl(auctionId?: string): string {
    const base = config.public.wsBaseUrl
    const token = authStore.token
    // Backend expects /ws/auctions/{auctionId} path, not a generic /ws
    const path = auctionId ? `${base}/auctions/${auctionId}` : base
    return token ? `${path}?token=${token}` : path
  }

  function connect(auctionId?: string) {
    if (socket?.readyState === WebSocket.OPEN) return
    if (!authStore.token) return
    if (auctionId) currentAuctionId = auctionId
    // Backend requires auctionId in the WebSocket path — skip if none provided
    const effectiveAuctionId = auctionId || currentAuctionId
    if (!effectiveAuctionId) return

    try {
      socket = new WebSocket(getWebSocketUrl(effectiveAuctionId))

      socket.onopen = () => {
        isConnected.value = true
        isReconnecting.value = false
        reconnectAttempts = 0
        startHeartbeat()
      }

      socket.onmessage = (event: MessageEvent) => {
        try {
          const message = JSON.parse(event.data as string) as Record<string, unknown>

          // Gateway sends "type" field; normalize to match our event types
          let eventType = (message.type ?? message.event) as string | undefined
          if (!eventType) return
          if (eventType === 'pong' || eventType === 'connected' || eventType === 'heartbeat') return

          // Normalize gateway event names to frontend event names
          if (eventType === 'lot_extended') eventType = 'auction_extended'
          if (eventType === 'lot_closed') eventType = 'auction_closed'
          if (eventType === 'lot_awarded') eventType = 'auction_closed'

          const handlers = eventHandlers.get(eventType as WebSocketEvent)
          if (handlers) {
            // Include auctionId in data for handlers that need it
            const data = message.data ?? message
            if (typeof data === 'object' && data !== null && message.auctionId) {
              (data as Record<string, unknown>).auctionId = message.auctionId
            }
            handlers.forEach((handler) => handler(data))
          }
        } catch {
          // Silently ignore malformed messages
        }
      }

      socket.onclose = (event: CloseEvent) => {
        isConnected.value = false
        stopHeartbeat()

        if (!event.wasClean && reconnectAttempts < maxReconnectAttempts) {
          scheduleReconnect()
        }
      }

      socket.onerror = () => {
        // Silently handle - onclose will fire and handle reconnect
      }
    } catch {
      // WebSocket endpoint not available - fail silently
    }
  }

  function disconnect() {
    reconnectAttempts = maxReconnectAttempts // Prevent reconnection
    stopHeartbeat()

    if (reconnectTimeout) {
      clearTimeout(reconnectTimeout)
      reconnectTimeout = null
    }

    if (socket) {
      socket.close(1000, 'Client disconnect')
      socket = null
    }

    isConnected.value = false
    isReconnecting.value = false
  }

  function send(data: Record<string, unknown>) {
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(data))
    }
  }

  function subscribe(auctionId: string) {
    send({ action: 'subscribe', auctionId })
  }

  function unsubscribe(auctionId: string) {
    send({ action: 'unsubscribe', auctionId })
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatInterval = setInterval(() => {
      if (socket?.readyState === WebSocket.OPEN) {
        socket.send('ping')
      }
    }, 30000)
  }

  function stopHeartbeat() {
    if (heartbeatInterval) {
      clearInterval(heartbeatInterval)
      heartbeatInterval = null
    }
  }

  function scheduleReconnect() {
    if (reconnectAttempts >= maxReconnectAttempts) return

    isReconnecting.value = true
    reconnectAttempts++

    const delay = Math.min(
      baseReconnectDelay * Math.pow(2, reconnectAttempts - 1),
      30000,
    )

    reconnectTimeout = setTimeout(() => {
      connect()
    }, delay)
  }

  function onBidPlaced(handler: (data: BidPlacedData) => void) {
    addHandler('bid_placed', handler as InternalHandler)
  }

  function onAuctionExtended(handler: (data: AuctionExtendedData) => void) {
    addHandler('auction_extended', handler as InternalHandler)
  }

  function onAuctionClosed(handler: (data: AuctionClosedData) => void) {
    addHandler('auction_closed', handler as InternalHandler)
  }

  function onOverbid(handler: (data: OverbidData) => void) {
    addHandler('overbid', handler as InternalHandler)
  }

  function onReserveMet(handler: (data: ReserveMetData) => void) {
    addHandler('reserve_met', handler as InternalHandler)
  }

  function onNotification(handler: (data: Notification) => void) {
    addHandler('notification', handler as InternalHandler)
  }

  function addHandler(event: WebSocketEvent, handler: InternalHandler) {
    if (!eventHandlers.has(event)) {
      eventHandlers.set(event, new Set())
    }
    eventHandlers.get(event)!.add(handler)
  }

  function removeHandler(event: WebSocketEvent, handler: InternalHandler) {
    eventHandlers.get(event)?.delete(handler)
  }

  // Handler reference removal — accepts the original typed handler for reference equality
  function off<T>(event: WebSocketEvent, handler: (data: T) => void) {
    removeHandler(event, handler as unknown as InternalHandler)
  }

  onBeforeUnmount(() => {
    disconnect()
    eventHandlers.clear()
  })

  return {
    isConnected: readonly(isConnected),
    isReconnecting: readonly(isReconnecting),
    connect,
    disconnect,
    send,
    subscribe,
    unsubscribe,
    onBidPlaced,
    onAuctionExtended,
    onAuctionClosed,
    onOverbid,
    onReserveMet,
    onNotification,
    off,
  }
}
