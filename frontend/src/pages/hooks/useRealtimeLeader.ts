import { useEffect, useState } from 'react'

const realtimeLockName = 'qiwen:realtime-leader'
const leaseStorageKey = 'qiwen_realtime_leader'
const leaseTtlMs = 15_000
const leaseRenewMs = 5_000

type WebLocks = {
  request(name: string, options: { signal?: AbortSignal }, callback: () => Promise<void> | void): Promise<void>
}

type NavigatorWithLocks = Navigator & {
  locks?: WebLocks
}

type RealtimeLeaderLease = {
  tabId: string
  expiresAt: number
}

const tabId = createTabId()

function createTabId(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }

  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2)}`
}

function getWebLocks(): WebLocks | null {
  if (typeof navigator === 'undefined') {
    return null
  }

  const locks = (navigator as NavigatorWithLocks).locks
  return locks && typeof locks.request === 'function' ? locks : null
}

function readLease(): RealtimeLeaderLease | null {
  try {
    const raw = localStorage.getItem(leaseStorageKey)
    if (!raw) {
      return null
    }
    const parsed = JSON.parse(raw) as Partial<RealtimeLeaderLease>
    if (typeof parsed.tabId !== 'string' || typeof parsed.expiresAt !== 'number') {
      return null
    }

    return { tabId: parsed.tabId, expiresAt: parsed.expiresAt }
  } catch {
    return null
  }
}

function writeLease(lease: RealtimeLeaderLease): boolean {
  try {
    localStorage.setItem(leaseStorageKey, JSON.stringify(lease))
    return true
  } catch {
    return false
  }
}

function removeOwnedLease() {
  try {
    if (readLease()?.tabId === tabId) {
      localStorage.removeItem(leaseStorageKey)
    }
  } catch {
    // 忽略存储清理失败；TTL 会限制陈旧归属的持续时间。
  }
}

function canClaimLease(lease: RealtimeLeaderLease | null, now: number): boolean {
  return !lease || lease.tabId === tabId || lease.expiresAt <= now
}

function claimLease(setIsLeader: (isLeader: boolean) => void): boolean {
  const now = Date.now()
  const existingLease = readLease()
  if (!canClaimLease(existingLease, now)) {
    setIsLeader(false)
    return false
  }

  const nextLease = { tabId, expiresAt: now + leaseTtlMs }
  if (!writeLease(nextLease)) {
    setIsLeader(true)
    return true
  }

  const confirmedLease = readLease()
  const ownsLease = confirmedLease?.tabId === tabId
  setIsLeader(ownsLease)
  return ownsLease
}

function createWebLocksLeader(enabled: boolean, setIsLeader: (isLeader: boolean) => void): (() => void) | null {
  if (!enabled) {
    return null
  }

  const locks = getWebLocks()
  if (!locks) {
    return null
  }

  const controller = new AbortController()
  let active = true
  let releaseLock: (() => void) | null = null
  const releasePromise = new Promise<void>((resolve) => {
    releaseLock = resolve
  })

  void locks.request(realtimeLockName, { signal: controller.signal }, async () => {
    if (!active) {
      return
    }

    setIsLeader(true)
    try {
      await releasePromise
    } finally {
      setIsLeader(false)
    }
  }).catch((error: unknown) => {
    if (active && !(error instanceof DOMException && error.name === 'AbortError')) {
      console.error('Failed to acquire realtime leader lock.', error)
    }
  })

  return () => {
    active = false
    setIsLeader(false)
    controller.abort()
    releaseLock?.()
  }
}

function createLeaseLeader(enabled: boolean, setIsLeader: (isLeader: boolean) => void): () => void {
  if (!enabled) {
    return () => {}
  }

  claimLease(setIsLeader)

  const renewInterval = window.setInterval(() => {
    claimLease(setIsLeader)
  }, leaseRenewMs)

  const handleStorage = (event: StorageEvent) => {
    if (event.key === leaseStorageKey) {
      claimLease(setIsLeader)
    }
  }

  const handlePageHide = () => {
    removeOwnedLease()
  }

  window.addEventListener('storage', handleStorage)
  window.addEventListener('pagehide', handlePageHide)

  return () => {
    window.clearInterval(renewInterval)
    window.removeEventListener('storage', handleStorage)
    window.removeEventListener('pagehide', handlePageHide)
    removeOwnedLease()
    setIsLeader(false)
  }
}

/**
 * 从同源 tab 中选出一个作为实时 EventSource 持有者。
 */
export function useRealtimeLeader(enabled: boolean): boolean {
  const [isLeader, setIsLeader] = useState(false)

  useEffect(() => {
    if (!enabled) {
      return
    }

    const releaseWebLock = createWebLocksLeader(enabled, setIsLeader)
    if (releaseWebLock) {
      window.addEventListener('pagehide', releaseWebLock, { once: true })
      return () => {
        window.removeEventListener('pagehide', releaseWebLock)
        releaseWebLock()
      }
    }

    return createLeaseLeader(enabled, setIsLeader)
  }, [enabled])

  return enabled && isLeader
}
