import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import {
  apiCallNames,
  configureLoggedInStores,
  loadHarness,
  messagePayload,
  MockBroadcastChannel,
  MockEventSource,
  originalBroadcastChannel,
} from './useRealtimeConnection.test-utils'

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
