import { afterEach, beforeEach, vi } from 'vitest'

type WindowListenerRegistry = Map<string, Set<EventListenerOrEventListenerObject>>

const originalAddEventListener = window.addEventListener.bind(window)
const originalRemoveEventListener = window.removeEventListener.bind(window)
const originalEventSource = globalThis.EventSource
const windowListeners: WindowListenerRegistry = new Map()

window.addEventListener = ((type: string, listener: EventListenerOrEventListenerObject | null, options?: AddEventListenerOptions | boolean) => {
  if (listener) {
    const listeners = windowListeners.get(type) ?? new Set<EventListenerOrEventListenerObject>()
    listeners.add(listener)
    windowListeners.set(type, listeners)
    originalAddEventListener(type, listener, options)
  }
}) as typeof window.addEventListener

window.removeEventListener = ((type: string, listener: EventListenerOrEventListenerObject | null, options?: EventListenerOptions | boolean) => {
  if (listener) {
    windowListeners.get(type)?.delete(listener)
    originalRemoveEventListener(type, listener, options)
  }
}) as typeof window.removeEventListener

beforeEach(() => {
  vi.restoreAllMocks()
  globalThis.EventSource = originalEventSource
})

afterEach(() => {
  for (const [type, listeners] of windowListeners.entries()) {
    for (const listener of listeners) {
      originalRemoveEventListener(type, listener)
    }
  }
  windowListeners.clear()
  globalThis.EventSource = originalEventSource
  vi.unstubAllGlobals()
})
