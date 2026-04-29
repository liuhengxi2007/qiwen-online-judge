import { describe, expect, it } from 'vitest'

import { initialSiteManageState, reduceSiteManageState } from '@/features/site-management/domain/site-manage-state'
import type { Username } from '@/features/auth/model/AuthValues'
import type { AuthUserListItem } from '@/features/user/domain/user'

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
      user: { displayName: 'Alice' } as AuthUserListItem,
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
})
