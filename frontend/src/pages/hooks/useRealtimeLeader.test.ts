import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type LockRequest = {
  callback: () => Promise<void> | void
  resolve: () => void
  reject: (error: unknown) => void
  signal?: AbortSignal
  started: boolean
  abortListener?: () => void
}

class MockLockManager {
  private locked = false
  private readonly queue: LockRequest[] = []

  request(_name: string, options: { signal?: AbortSignal }, callback: () => Promise<void> | void): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      const request: LockRequest = {
        callback,
        resolve,
        reject,
        signal: options.signal,
        started: false,
      }
      request.abortListener = () => {
        if (request.started) {
          return
        }
        const index = this.queue.indexOf(request)
        if (index >= 0) {
          this.queue.splice(index, 1)
        }
        reject(new DOMException('Aborted', 'AbortError'))
      }
      options.signal?.addEventListener('abort', request.abortListener, { once: true })
      this.queue.push(request)
      this.runNext()
    })
  }

  private runNext() {
    if (this.locked) {
      return
    }

    const request = this.queue.shift()
    if (!request) {
      return
    }

    if (request.signal?.aborted) {
      request.reject(new DOMException('Aborted', 'AbortError'))
      this.runNext()
      return
    }

    request.started = true
    this.locked = true
    void Promise.resolve(request.callback()).then(request.resolve, request.reject).finally(() => {
      if (request.abortListener) {
        request.signal?.removeEventListener('abort', request.abortListener)
      }
      this.locked = false
      this.runNext()
    })
  }
}

function setNavigatorLocks(locks: MockLockManager | undefined) {
  Object.defineProperty(navigator, 'locks', {
    value: locks,
    configurable: true,
  })
}

async function loadHook() {
  vi.resetModules()
  return import('@/pages/hooks/useRealtimeLeader')
}

describe('useRealtimeLeader', () => {
  beforeEach(() => {
    localStorage.clear()
    setNavigatorLocks(undefined)
  })

  afterEach(() => {
    localStorage.clear()
    setNavigatorLocks(undefined)
    vi.resetModules()
  })

  it('allows only one Web Locks leader and hands off after cleanup', async () => {
    setNavigatorLocks(new MockLockManager())
    const { useRealtimeLeader } = await loadHook()

    const first = renderHook(() => useRealtimeLeader(true), { reactStrictMode: false })
    const second = renderHook(() => useRealtimeLeader(true), { reactStrictMode: false })

    await waitFor(() => expect(first.result.current).toBe(true))
    expect(second.result.current).toBe(false)

    first.unmount()
    await waitFor(() => expect(second.result.current).toBe(true))

    second.unmount()
  })

  it('releases Web Locks leadership when disabled', async () => {
    setNavigatorLocks(new MockLockManager())
    const { useRealtimeLeader } = await loadHook()

    const rendered = renderHook(({ enabled }) => useRealtimeLeader(enabled), {
      initialProps: { enabled: true },
      reactStrictMode: false,
    })

    await waitFor(() => expect(rendered.result.current).toBe(true))

    rendered.rerender({ enabled: false })

    await waitFor(() => expect(rendered.result.current).toBe(false))
    rendered.unmount()
  })

  it('uses localStorage lease fallback and removes an owned lease on cleanup', async () => {
    const { useRealtimeLeader } = await loadHook()

    const rendered = renderHook(() => useRealtimeLeader(true), { reactStrictMode: false })
    await act(async () => {})
    expect(rendered.result.current).toBe(true)
    expect(localStorage.getItem('qiwen_realtime_leader')).not.toBeNull()

    rendered.unmount()

    expect(localStorage.getItem('qiwen_realtime_leader')).toBeNull()
  })

  it('takes over an expired localStorage lease', async () => {
    localStorage.setItem(
      'qiwen_realtime_leader',
      JSON.stringify({
        tabId: 'other-tab',
        expiresAt: Date.now() - 1,
      }),
    )
    const { useRealtimeLeader } = await loadHook()

    const rendered = renderHook(() => useRealtimeLeader(true), { reactStrictMode: false })
    await act(async () => {})
    expect(rendered.result.current).toBe(true)
    rendered.unmount()
  })
})
