import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import type { EmailAddress } from '@/objects/auth/EmailAddress'
import type { SessionResponse } from '@/objects/auth/response/SessionResponse'
import type { DisplayName } from '@/objects/user/DisplayName'
import type { Username } from '@/objects/user/Username'
import { createHttpClientError } from '@/system/api/http-client'

type Harness = Awaited<ReturnType<typeof loadHarness>>

function createSession(overrides: Partial<SessionResponse> = {}): SessionResponse {
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
    ...overrides,
  }
}

async function loadHarness(sendAPIImpl: () => Promise<SessionResponse>) {
  vi.resetModules()
  window.localStorage.clear()

  const sendAPI = vi.fn(sendAPIImpl)

  vi.doMock('@/system/api/api-message', async () => {
    const actual = await vi.importActual<typeof import('@/system/api/api-message')>('@/system/api/api-message')
    return {
      ...actual,
      sendAPI,
    }
  })

  const authStoreModule = await import('@/pages/stores/auth/UseAuthStore')
  const sessionGuardModule = await import('@/pages/hooks/useSessionGuard')

  return {
    useAuthStore: authStoreModule.useAuthStore,
    useSessionGuard: sessionGuardModule.useSessionGuard,
    sendAPI,
  }
}

async function waitForSessionRefreshAttempt(harness: Harness) {
  await waitFor(() => {
    expect(harness.sendAPI).toHaveBeenCalledTimes(1)
  })
  await act(async () => {
    await Promise.resolve()
  })
}

afterEach(() => {
  cleanup()
  vi.doUnmock('@/system/api/api-message')
})

describe('useSessionGuard', () => {
  it('keeps the cached session when session refresh cannot reach the API', async () => {
    const cachedSession = createSession()
    const harness = await loadHarness(async () => {
      throw new TypeError('Failed to fetch')
    })
    harness.useAuthStore.getState().setSession(cachedSession)

    const { result } = renderHook(() => harness.useSessionGuard())

    await waitForSessionRefreshAttempt(harness)

    expect(harness.useAuthStore.getState().session).toEqual(cachedSession)
    expect(result.current.navigationIntent).toBeNull()
  })

  it('clears the cached session only when the API confirms unauthorized', async () => {
    const cachedSession = createSession()
    const harness = await loadHarness(async () => {
      throw createHttpClientError('unauthorized', 'Auth required')
    })
    harness.useAuthStore.getState().setSession(cachedSession)

    const { result } = renderHook(() => harness.useSessionGuard())

    await waitFor(() => {
      expect(result.current.navigationIntent?.to).toBe('/login?notice=session-expired')
    })

    expect(harness.useAuthStore.getState().session).toBeNull()
  })

  it('keeps cached session but denies site manager routes for non-manager users after refresh failure', async () => {
    const cachedSession = createSession({ siteManager: false })
    const harness = await loadHarness(async () => {
      throw new TypeError('Failed to fetch')
    })
    harness.useAuthStore.getState().setSession(cachedSession)

    const { result } = renderHook(() => harness.useSessionGuard({ requireSiteManager: true }))

    await waitFor(() => {
      expect(result.current.navigationIntent?.to).toBe('/forbidden')
    })

    expect(harness.useAuthStore.getState().session).toEqual(cachedSession)
  })

  it('keeps cached site manager routes available after refresh failure', async () => {
    const cachedSession = createSession({ siteManager: true })
    const harness = await loadHarness(async () => {
      throw new TypeError('Failed to fetch')
    })
    harness.useAuthStore.getState().setSession(cachedSession)

    const { result } = renderHook(() => harness.useSessionGuard({ requireSiteManager: true }))

    await waitForSessionRefreshAttempt(harness)

    expect(harness.useAuthStore.getState().session).toEqual(cachedSession)
    expect(result.current.siteManagerSession).toEqual(cachedSession)
    expect(result.current.navigationIntent).toBeNull()
  })
})
