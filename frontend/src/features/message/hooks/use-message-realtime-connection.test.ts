import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { DisplayName, EmailAddress, Username } from '@/features/auth/model/AuthValues'
import type { SessionResponse } from '@/features/auth/model/SessionResponse'

class MockEventSource {
  static instances: MockEventSource[] = []

  readonly url: string
  readonly withCredentials: boolean
  closed = false
  private readonly listeners = new Map<string, Set<(event: MessageEvent) => void>>()

  constructor(url: string, init?: EventSourceInit) {
    this.url = url
    this.withCredentials = init?.withCredentials ?? false
    MockEventSource.instances.push(this)
  }

  addEventListener(type: string, listener: EventListenerOrEventListenerObject | null) {
    if (!listener) {
      return
    }
    const callback =
      typeof listener === 'function'
        ? listener
        : ((event: Event) => {
            listener.handleEvent(event)
          })
    const listeners = this.listeners.get(type) ?? new Set<(event: MessageEvent) => void>()
    listeners.add(callback as (event: MessageEvent) => void)
    this.listeners.set(type, listeners)
  }

  removeEventListener(type: string, listener: EventListenerOrEventListenerObject | null) {
    if (!listener) {
      return
    }
    const listeners = this.listeners.get(type)
    if (!listeners) {
      return
    }
    listeners.forEach((candidate) => {
      if (candidate === listener) {
        listeners.delete(candidate)
      }
    })
  }

  close() {
    this.closed = true
  }

  emit(type: string, payload: unknown) {
    const event = new MessageEvent(type, { data: JSON.stringify(payload) })
    this.listeners.get(type)?.forEach((listener) => {
      listener(event)
    })
  }
}

type Harness = Awaited<ReturnType<typeof loadHarness>>

function createSession(): SessionResponse {
  return {
    displayName: 'Alice' as DisplayName,
    username: 'alice' as Username,
    email: 'alice@example.com' as EmailAddress,
    preferences: {
      displayMode: 'display_name',
      locale: 'en',
      problemTitleDisplayMode: 'title',
    },
    siteManager: false,
    problemManager: false,
  }
}

async function loadHarness() {
  vi.resetModules()
  MockEventSource.instances = []
  globalThis.EventSource = MockEventSource as unknown as typeof EventSource

  const authStoreModule = await import('@/features/auth/stores/use-auth-store')
  const messageStoreModule = await import('@/features/message/stores/use-message-store')
  const hookModule = await import('@/features/message/hooks/use-message-realtime-connection')

  return {
    useAuthStore: authStoreModule.useAuthStore,
    useMessageStore: messageStoreModule.useMessageStore,
    useMessageRealtimeConnection: hookModule.useMessageRealtimeConnection,
    messageStreamEventName: hookModule.messageStreamEventName,
  }
}

function configureLoggedInStores(harness: Harness, refreshInbox = vi.fn().mockResolvedValue(undefined), clear = vi.fn()) {
  harness.useAuthStore.setState({ session: createSession() })
  harness.useMessageStore.setState({
    hasLoadedInbox: false,
    refreshInbox,
    clear,
  })
  return { refreshInbox, clear }
}

describe('useMessageRealtimeConnection', () => {
  beforeEach(() => {
    MockEventSource.instances = []
  })

  afterEach(() => {
    vi.resetModules()
    MockEventSource.instances = []
  })

  it('creates a single shared EventSource and refreshes inbox on first connection', async () => {
    const harness = await loadHarness()
    const { refreshInbox } = configureLoggedInStores(harness)

    const first = renderHook(() => harness.useMessageRealtimeConnection(), { reactStrictMode: false })
    const second = renderHook(() => harness.useMessageRealtimeConnection(), { reactStrictMode: false })
    await act(async () => {})

    const eventSource = MockEventSource.instances[0]
    expect(MockEventSource.instances).toHaveLength(1)
    expect(eventSource?.url).toBe('/api/messages/events')
    expect(eventSource?.withCredentials).toBe(true)
    expect(refreshInbox).toHaveBeenCalledTimes(1)

    second.unmount()
    expect(eventSource?.closed).toBe(false)

    first.unmount()
    expect(eventSource?.closed).toBe(true)
  }, 15000)

  it('does not refresh inbox on mount after inbox has already loaded', async () => {
    const harness = await loadHarness()
    const refreshInbox = vi.fn().mockResolvedValue(undefined)
    harness.useAuthStore.setState({ session: createSession() })
    harness.useMessageStore.setState({
      hasLoadedInbox: true,
      refreshInbox,
      clear: vi.fn(),
    })

    const rendered = renderHook(() => harness.useMessageRealtimeConnection(), { reactStrictMode: false })

    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    expect(refreshInbox).not.toHaveBeenCalled()

    rendered.unmount()
  })

  it('refreshes inbox and dispatches stream events for each supported server event', async () => {
    const harness = await loadHarness()
    const { refreshInbox } = configureLoggedInStores(harness)
    const receivedEvents: CustomEvent[] = []
    window.addEventListener(harness.messageStreamEventName, (event) => {
      receivedEvents.push(event as CustomEvent)
    })

    const rendered = renderHook(() => harness.useMessageRealtimeConnection(), { reactStrictMode: false })
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    const eventSource = MockEventSource.instances[0]

    eventSource?.emit('message_received', { kind: 'message' })
    eventSource?.emit('conversation_read', { kind: 'read' })
    eventSource?.emit('inbox_changed', { kind: 'inbox' })

    await waitFor(() => expect(refreshInbox).toHaveBeenCalledTimes(4))
    expect(receivedEvents).toHaveLength(3)
    expect(receivedEvents.map((event) => event.detail)).toEqual([
      { type: 'message_received', payload: { kind: 'message' } },
      { type: 'conversation_read', payload: { kind: 'read' } },
      { type: 'inbox_changed', payload: { kind: 'inbox' } },
    ])

    rendered.unmount()
  })

  it('clears stores and tears down the connection when the user logs out', async () => {
    const harness = await loadHarness()
    const { clear } = configureLoggedInStores(harness)

    renderHook(() => harness.useMessageRealtimeConnection(), { reactStrictMode: false })
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    const eventSource = MockEventSource.instances[0]

    act(() => {
      harness.useAuthStore.setState({ session: null })
    })

    await waitFor(() => expect(clear).toHaveBeenCalledTimes(1))
    expect(eventSource?.closed).toBe(true)
  })
})
