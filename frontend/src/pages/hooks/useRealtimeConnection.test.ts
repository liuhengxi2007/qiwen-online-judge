import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'

type MockEventSource = {
  readonly url: string
  readonly withCredentials: boolean
  closed: boolean
  addEventListener(type: string, listener: EventListenerOrEventListenerObject | null): void
  close(): void
  emit(type: string, payload: unknown): void
}

type MockEventSourceConstructor = {
  new (url: string, init?: EventSourceInit): MockEventSource
  instances: MockEventSource[]
}

type MockBroadcastChannelInstance = {
  readonly name: string
  readonly messages: unknown[]
  closed: boolean
  addEventListener(type: string, listener: EventListenerOrEventListenerObject | null): void
  removeEventListener(type: string, listener: EventListenerOrEventListenerObject | null): void
  postMessage(message: unknown): void
  close(): void
  emit(message: unknown): void
}

type MockBroadcastChannelConstructor = {
  new (name: string): MockBroadcastChannelInstance
  instances: MockBroadcastChannelInstance[]
}

function createMockEventSource(url: string, init?: EventSourceInit): MockEventSource {
  const listenersByType = new Map<string, Set<(event: MessageEvent) => void>>()
  const source: MockEventSource = {
    url,
    withCredentials: init?.withCredentials ?? false,
    closed: false,
    addEventListener(type, listener) {
      if (!listener) {
        return
      }
      const callback =
        typeof listener === 'function'
          ? listener
          : ((event: Event) => {
              listener.handleEvent(event)
            })
      const listeners = listenersByType.get(type) ?? new Set<(event: MessageEvent) => void>()
      listeners.add(callback as (event: MessageEvent) => void)
      listenersByType.set(type, listeners)
    },
    close() {
      source.closed = true
    },
    emit(type, payload) {
      const event = new MessageEvent(type, { data: JSON.stringify(payload) })
      listenersByType.get(type)?.forEach((listener) => {
        listener(event)
      })
    },
  }

  MockEventSource.instances.push(source)
  return source
}

const MockEventSource = Object.assign(
  function MockEventSource(url: string, init?: EventSourceInit) {
    return createMockEventSource(url, init)
  } as unknown as MockEventSourceConstructor,
  { instances: [] as MockEventSource[] },
)

function createMockBroadcastChannel(name: string): MockBroadcastChannelInstance {
  const listeners = new Set<(event: MessageEvent) => void>()
  const channel: MockBroadcastChannelInstance = {
    name,
    messages: [],
    closed: false,
    addEventListener(type, listener) {
      if (type !== 'message' || !listener) {
        return
      }
      const callback =
        typeof listener === 'function'
          ? listener
          : ((event: Event) => {
              listener.handleEvent(event)
            })
      listeners.add(callback as (event: MessageEvent) => void)
    },
    removeEventListener(type, listener) {
      if (type !== 'message' || !listener) {
        return
      }
      listeners.forEach((candidate) => {
        if (candidate === listener) {
          listeners.delete(candidate)
        }
      })
    },
    postMessage(message) {
      channel.messages.push(message)
      MockBroadcastChannel.instances.forEach((candidate) => {
        if (candidate !== channel && candidate.name === channel.name && !candidate.closed) {
          candidate.emit(message)
        }
      })
    },
    close() {
      channel.closed = true
    },
    emit(message) {
      const event = new MessageEvent('message', { data: message })
      listeners.forEach((listener) => {
        listener(event)
      })
    },
  }

  MockBroadcastChannel.instances.push(channel)
  return channel
}

const MockBroadcastChannel = Object.assign(
  function MockBroadcastChannel(name: string) {
    return createMockBroadcastChannel(name)
  } as unknown as MockBroadcastChannelConstructor,
  { instances: [] as MockBroadcastChannelInstance[] },
)

const originalBroadcastChannel = globalThis.BroadcastChannel

type Harness = Awaited<ReturnType<typeof loadHarness>>

function createSession(): SessionResponse {
  return {
    displayName: 'Alice' as DisplayName,
    username: 'alice' as Username,
    avatarUrl: null,
    email: 'alice@example.com' as EmailAddress,
    preferences: {
      displayMode: 'display_name',
      locale: 'en',
      problemTitleDisplayMode: 'title',
      autoMarkMessageRead: false,
    },
    siteManager: false,
    problemManager: false,
    contestManager: false,
  }
}

function inboxResponse() {
  return {
    conversations: [],
    totalUnreadCount: 0,
    page: 1,
    pageSize: 10,
    totalItems: 0,
  }
}

function notificationListResponse() {
  return {
    notifications: [],
    unreadCount: 1,
    page: 1,
    pageSize: 10,
    totalItems: 0,
  }
}

async function loadHarness(isLeader: boolean) {
  vi.resetModules()
  MockEventSource.instances = []
  MockBroadcastChannel.instances = []
  globalThis.EventSource = MockEventSource as unknown as typeof EventSource
  globalThis.BroadcastChannel = MockBroadcastChannel as unknown as typeof BroadcastChannel

  const sendAPI = vi.fn(async (message: { constructor: { name: string } }) => {
    switch (message.constructor.name) {
      case 'ListInbox':
        return inboxResponse()
      case 'GetNotificationUnreadCount':
        return { unreadCount: 1 }
      case 'ListNotifications':
        return notificationListResponse()
      default:
        throw new Error(`Unexpected API: ${message.constructor.name}`)
    }
  })

  vi.doMock('@/system/api/api-message', async () => {
    const actual = await vi.importActual<typeof import('@/system/api/api-message')>('@/system/api/api-message')
    return {
      ...actual,
      sendAPI,
    }
  })
  vi.doMock('@/pages/hooks/useRealtimeLeader', () => ({
    useRealtimeLeader: () => isLeader,
  }))

  const authStoreModule = await import('@/pages/stores/auth/UseAuthStore')
  const messageStoreModule = await import('@/pages/stores/message/UseMessageStore')
  const notificationStoreModule = await import('@/pages/stores/notification/UseNotificationStore')
  const realtimeModule = await import('@/pages/hooks/useRealtimeConnection')
  const messageEventsModule = await import('@/pages/hooks/messageRealtimeEvents')

  return {
    useAuthStore: authStoreModule.useAuthStore,
    useMessageStore: messageStoreModule.useMessageStore,
    useNotificationStore: notificationStoreModule.useNotificationStore,
    useRealtimeConnection: realtimeModule.useRealtimeConnection,
    messageStreamEventName: messageEventsModule.messageStreamEventName,
    sendAPI,
  }
}

function configureLoggedInStores(harness: Harness, options?: { inboxLoaded?: boolean; unreadLoaded?: boolean; notificationListLoaded?: boolean }) {
  harness.useAuthStore.setState({ session: createSession() })
  harness.useMessageStore.setState({
    hasLoadedInbox: options?.inboxLoaded ?? true,
    clear: vi.fn(),
  })
  harness.useNotificationStore.setState({
    hasLoadedUnreadCount: options?.unreadLoaded ?? true,
    hasLoadedList: options?.notificationListLoaded ?? false,
    clear: vi.fn(),
  })
}

function apiCallNames(sendAPI: Harness['sendAPI']): string[] {
  return sendAPI.mock.calls.map(([message]) => message.constructor.name)
}

const messagePayload = {
  id: '46ef1556-0d91-4a53-9cd0-ac7d71037b5b',
  conversationId: 'f47ac10b-58cc-4372-a567-0e02b2c3d479',
  sender: { username: 'alice', displayName: 'Alice' },
  recipientUsername: 'bob',
  content: 'Hello',
  createdAt: '2026-05-05T00:00:00Z',
  readAt: null,
}

describe('useRealtimeConnection', () => {
  beforeEach(() => {
    MockEventSource.instances = []
    MockBroadcastChannel.instances = []
  })

  afterEach(() => {
    vi.doUnmock('@/system/api/api-message')
    vi.doUnmock('@/pages/hooks/useRealtimeLeader')
    vi.resetModules()
    MockEventSource.instances = []
    MockBroadcastChannel.instances = []
    globalThis.BroadcastChannel = originalBroadcastChannel
  })

  it('creates a single combined EventSource when this tab is leader', async () => {
    const harness = await loadHarness(true)
    configureLoggedInStores(harness)

    const rendered = renderHook(() => harness.useRealtimeConnection(), { reactStrictMode: false })

    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    const eventSource = MockEventSource.instances[0]
    expect(eventSource?.url).toBe('/api/realtime/events')
    expect(eventSource?.withCredentials).toBe(true)
    expect(apiCallNames(harness.sendAPI)).toEqual(expect.arrayContaining(['ListInbox', 'GetNotificationUnreadCount']))

    rendered.unmount()
    expect(eventSource?.closed).toBe(true)
  })

  it('does not create EventSource as follower and handles message broadcasts', async () => {
    const harness = await loadHarness(false)
    configureLoggedInStores(harness)
    const receivedEvents: CustomEvent[] = []
    window.addEventListener(harness.messageStreamEventName, (event) => {
      receivedEvents.push(event as CustomEvent)
    })

    const rendered = renderHook(() => harness.useRealtimeConnection(), { reactStrictMode: false })
    await waitFor(() => expect(MockBroadcastChannel.instances).toHaveLength(1))
    expect(MockEventSource.instances).toHaveLength(0)
    harness.sendAPI.mockClear()

    MockBroadcastChannel.instances[0]?.emit({
      eventName: 'message_received',
      rawData: JSON.stringify(messagePayload),
    })

    await waitFor(() => expect(apiCallNames(harness.sendAPI)).toEqual(['ListInbox']))
    expect(receivedEvents).toHaveLength(1)
    expect(receivedEvents[0]?.detail.type).toBe('message_received')

    rendered.unmount()
  })

  it('broadcasts leader SSE messages after applying them locally', async () => {
    const harness = await loadHarness(true)
    configureLoggedInStores(harness)
    const receivedEvents: CustomEvent[] = []
    window.addEventListener(harness.messageStreamEventName, (event) => {
      receivedEvents.push(event as CustomEvent)
    })

    const rendered = renderHook(() => harness.useRealtimeConnection(), { reactStrictMode: false })
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    harness.sendAPI.mockClear()

    MockEventSource.instances[0]?.emit('message_received', messagePayload)

    await waitFor(() => expect(apiCallNames(harness.sendAPI)).toEqual(['ListInbox']))
    expect(receivedEvents).toHaveLength(1)
    expect(MockBroadcastChannel.instances[0]?.messages).toEqual([
      {
        eventName: 'message_received',
        rawData: JSON.stringify(messagePayload),
      },
    ])

    rendered.unmount()
  })

  it('refreshes notification count and loaded list for notification events', async () => {
    const harness = await loadHarness(true)
    configureLoggedInStores(harness, { notificationListLoaded: true })

    const rendered = renderHook(() => harness.useRealtimeConnection(), { reactStrictMode: false })
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    harness.sendAPI.mockClear()

    MockEventSource.instances[0]?.emit('notifications_changed', {})

    await waitFor(() => expect(apiCallNames(harness.sendAPI)).toEqual(['GetNotificationUnreadCount', 'ListNotifications']))
    expect(MockBroadcastChannel.instances[0]?.messages).toEqual([
      {
        eventName: 'notifications_changed',
        rawData: '{}',
      },
    ])

    rendered.unmount()
  })

  it('clears stores and closes EventSource on logout', async () => {
    const harness = await loadHarness(true)
    configureLoggedInStores(harness)
    const clearMessages = harness.useMessageStore.getState().clear
    const clearNotifications = harness.useNotificationStore.getState().clear

    renderHook(() => harness.useRealtimeConnection(), { reactStrictMode: false })
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1))
    const eventSource = MockEventSource.instances[0]

    act(() => {
      harness.useAuthStore.setState({ session: null })
    })

    await waitFor(() => expect(clearMessages).toHaveBeenCalledTimes(1))
    expect(clearNotifications).toHaveBeenCalledTimes(1)
    expect(eventSource?.closed).toBe(true)
  })
})
