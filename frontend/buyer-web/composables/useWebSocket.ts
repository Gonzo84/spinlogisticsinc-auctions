import { useAuthStore } from '~/stores/auth'

export type WebSocketEvent =
  | 'bid_placed'
  | 'auction_extended'
  | 'auction_closed'
  | 'overbid'
  | 'reserve_met'
  | 'notification'

export interface WebSocketMessage {
  event: WebSocketEvent
  data: any
  auctionId?: string
  timestamp: string
}

type EventHandler = (data: any) => void

export function useWebSocket() {
  const config = useRuntimeConfig()
  const authStore = useAuthStore()

  let socket: WebSocket | null = null
  let heartbeatInterval: ReturnType<typeof setInterval> | null = null
  let reconnectTimeout: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 10
  const baseReconnectDelay = 1000

  const isConnected = ref(false)
  const isReconnecting = ref(false)

  const eventHandlers = new Map<WebSocketEvent, Set<EventHandler>>()

  function getWebSocketUrl(): string {
    const base = config.public.wsBaseUrl
    const token = authStore.token
    return token ? `${base}?token=${token}` : base
  }

  function connect() {
    if (socket?.readyState === WebSocket.OPEN) return

    try {
      socket = new WebSocket(getWebSocketUrl())

      socket.onopen = () => {
        isConnected.value = true
        isReconnecting.value = false
        reconnectAttempts = 0
        startHeartbeat()
      }

      socket.onmessage = (event: MessageEvent) => {
        try {
          const message: WebSocketMessage = JSON.parse(event.data)

          if (message.event === 'pong') return

          const handlers = eventHandlers.get(message.event)
          if (handlers) {
            handlers.forEach((handler) => handler(message.data))
          }
        } catch {
          console.error('Failed to parse WebSocket message')
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
        console.error('WebSocket error')
      }
    } catch {
      console.error('Failed to create WebSocket connection')
      scheduleReconnect()
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

  function send(data: any) {
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
      send({ event: 'ping', timestamp: new Date().toISOString() })
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

  function onBidPlaced(handler: EventHandler) {
    addHandler('bid_placed', handler)
  }

  function onAuctionExtended(handler: EventHandler) {
    addHandler('auction_extended', handler)
  }

  function onAuctionClosed(handler: EventHandler) {
    addHandler('auction_closed', handler)
  }

  function onOverbid(handler: EventHandler) {
    addHandler('overbid', handler)
  }

  function onReserveMet(handler: EventHandler) {
    addHandler('reserve_met', handler)
  }

  function onNotification(handler: EventHandler) {
    addHandler('notification', handler)
  }

  function addHandler(event: WebSocketEvent, handler: EventHandler) {
    if (!eventHandlers.has(event)) {
      eventHandlers.set(event, new Set())
    }
    eventHandlers.get(event)!.add(handler)
  }

  function removeHandler(event: WebSocketEvent, handler: EventHandler) {
    eventHandlers.get(event)?.delete(handler)
  }

  function off(event: WebSocketEvent, handler: EventHandler) {
    removeHandler(event, handler)
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
