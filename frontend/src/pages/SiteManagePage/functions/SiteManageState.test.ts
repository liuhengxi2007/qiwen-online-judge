import { describe, expect, it } from 'vitest'

import { initialSiteManageState, reduceSiteManageState } from './SiteManageState'
import type { Username } from '@/objects/user/Username'
import type { ManagedUserListItem } from '@/objects/user/response/ManagedUserListItem'

describe('site-manage-state', () => {
  it('records the active username when an update starts', () => {
    const next = reduceSiteManageState(initialSiteManageState, {
      type: 'update_started',
      username: 'alice' as Username,
    })

    expect(next.updatingUsername).toBe('alice')
    expect(next.actionErrorMessage).toBe('')
    expect(next.notice).toBeNull()
  })

  it('stores a permissions notice after a successful update', () => {
    const next = reduceSiteManageState(initialSiteManageState, {
      type: 'update_succeeded',
      user: { displayName: 'Alice' } as ManagedUserListItem,
    })

    expect(next.updatingUsername).toBeNull()
    expect(next.notice).toEqual({
      kind: 'permissions_updated',
      displayName: 'Alice',
    })
  })

  it('stores redirect intents without clearing prior state', () => {
    const next = reduceSiteManageState(initialSiteManageState, {
      type: 'redirect_requested',
      intent: { to: '/site-manage', replace: true },
    })

    expect(next.navigationIntent).toEqual({ to: '/site-manage', replace: true })
  })

  it('tracks delete start and success transitions', () => {
    const started = reduceSiteManageState(
      {
        ...initialSiteManageState,
        notice: { kind: 'text', message: 'old' },
        actionErrorMessage: 'boom',
      },
      {
        type: 'delete_started',
        username: 'alice' as Username,
      },
    )

    expect(started.updatingUsername).toBe('alice')
    expect(started.notice).toBeNull()
    expect(started.actionErrorMessage).toBe('')

    const succeeded = reduceSiteManageState(started, {
      type: 'delete_succeeded',
      message: 'Deleted.',
    })

    expect(succeeded.updatingUsername).toBeNull()
    expect(succeeded.notice).toEqual({ kind: 'text', message: 'Deleted.' })
    expect(succeeded.actionErrorMessage).toBe('')
  })

  it('clears transient state when an update fails', () => {
    const next = reduceSiteManageState(
      {
        ...initialSiteManageState,
        updatingUsername: 'alice' as Username,
        notice: { kind: 'text', message: 'done' },
      },
      {
        type: 'update_failed',
        message: 'Forbidden.',
      },
    )

    expect(next.updatingUsername).toBeNull()
    expect(next.notice).toBeNull()
    expect(next.actionErrorMessage).toBe('Forbidden.')
  })
})
